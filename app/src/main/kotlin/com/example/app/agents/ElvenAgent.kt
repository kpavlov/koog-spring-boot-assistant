package com.example.app.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.RollbackStrategy
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMModerateMessage
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import ai.koog.agents.snapshot.feature.Persistency
import ai.koog.agents.snapshot.providers.PersistencyStorageProvider
import ai.koog.agents.snapshot.providers.file.JVMFilePersistencyStorageProvider
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Attachment
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.rag.base.RankedDocumentStorage
import ai.koog.rag.base.mostRelevantDocuments
import com.example.app.SessionId
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.pathString

@Service
class ElvenAgent(
    private val promptExecutor: PromptExecutor,
    private val spanExporters: List<SpanExporter>,
    private val buildProps: BuildProperties,
    private val rankedDocumentStorage: RankedDocumentStorage<Path>,
) {
    private val log = LoggerFactory.getLogger(ElvenAgent::class.java)
    private val kotlinLogger = KotlinLogging.logger(name = "ElvenAgent")

    @Value("\${ai.koog.chat-memory.session-store.path}")
    private lateinit var sessionsStorePath: Path
    private val systemPrompt =
        ElvenAgent::class.java.getResource("/prompts/elven-assistant/system.md")!!.readText()

    private val tools =
        ToolRegistry {
            tools(AssistantTools())
        }

    private fun getPersistencyStorageProvider(sessionId: SessionId): PersistencyStorageProvider =
        JVMFilePersistencyStorageProvider(
            root = sessionsStorePath,
            persistenceId = sessionId,
        )

    private val strategy =
        strategy(
            name = "test-strategy",
            toolSelectionStrategy = ToolSelectionStrategy.NONE, // TODO: fix mokksy
        ) {
            val callLLM by nodeLLMRequest("test-llm-call")

            val moderateInput by nodeLLMModerateMessage(
                moderatingModel = OpenAIModels.Moderation.Omni,
            )

            edge(
                nodeStart forwardTo moderateInput transformed {
                    Message.User(it, metaInfo = RequestMetaInfo.Empty)
                },
            )

            edge(
                moderateInput forwardTo callLLM
                    onCondition { !it.moderationResult.isHarmful }
                    transformed { it.message.content },
            )

            edge(
                moderateInput forwardTo nodeFinish
                    onCondition { it.moderationResult.isHarmful }
                    transformed { "Forgive me, mellon, but your message defies our sacred guidelines." },
            )

            edge(callLLM forwardTo nodeFinish onAssistantMessage { true })
        }

    suspend fun giveAdvice(
        input: String,
        sessionId: SessionId,
    ): String {
        val relevantDocuments =
            rankedDocumentStorage
                .mostRelevantDocuments(input, count = 3)
                .toList()

        val agent =
            AIAgent(
                id = sessionId,
                promptExecutor = promptExecutor,
                agentConfig =
                    AIAgentConfig(
                        prompt =
                            prompt("context") {
                                system(systemPrompt)
                                if (relevantDocuments.isNotEmpty()) {
                                    user {
                                        +"User's input: ```$input```."
                                        +"Use attachment as relevant context"
                                        attachments {
                                            relevantDocuments.forEach {
                                                val file = java.io.File("./${it.pathString}")
                                                val text = file.readText()
                                                attachment(
                                                    Attachment.File(
                                                        content = AttachmentContent.PlainText(text),
                                                        format = "md",
                                                        mimeType = "text/plain",
                                                        fileName = file.name,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                        model = OpenAIModels.CostOptimized.GPT4_1Mini,
                        maxAgentIterations = 100,
                    ),
                strategy = strategy,
                toolRegistry = tools,
            ) {
                install(Persistency) {
                    storage = getPersistencyStorageProvider(sessionId)

                    // Enable automatic checkpoint creation
                    this.enableAutomaticPersistency = true

                    // We preserve message history on restore
                    this.rollbackStrategy = RollbackStrategy.MessageHistoryOnly
                }

                install(OpenTelemetry) {
                    setServiceInfo(serviceName = buildProps.name, serviceVersion = buildProps.version)
                    // Configuration options here
                    setVerbose(true)
                    spanExporters.forEach {
                        addSpanExporter(it)
                    }
                }

                install(Tracing) {
                    // Configure message processors to handle trace events
                    addMessageProcessor(TraceFeatureMessageLogWriter(kotlinLogger))
                }
            }

        log.trace("Running command: {}", input)
        return agent.run(input)
    }
}