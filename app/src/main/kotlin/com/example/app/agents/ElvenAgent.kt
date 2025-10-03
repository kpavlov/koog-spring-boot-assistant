package com.example.app.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.RollbackStrategy
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
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.PersistenceStorageProvider
import ai.koog.prompt.dsl.AttachmentBuilder
import ai.koog.prompt.dsl.Prompt
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
import com.example.app.koog.PromptTemplateProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

@Service
class ElvenAgent(
    private val promptExecutor: PromptExecutor,
    private val spanExporters: List<SpanExporter>,
    private val buildProps: BuildProperties,
    private val rankedDocumentStorage: RankedDocumentStorage<Path>,
    private val persistenceStorageProvider: PersistenceStorageProvider,
    private val promptTemplateProvider: PromptTemplateProvider,
) {
    private val log = LoggerFactory.getLogger(ElvenAgent::class.java)
    private val kotlinLogger = KotlinLogging.logger(name = "ElvenAgent")

    private val systemErrorResponse =
        javaClass.getResource("/agents/elven-assistant/system-error.md")!!.readText()

    private val moderationErrorResponse =
        javaClass.getResource("/agents/elven-assistant/moderation-error.md")!!.readText()

    @Value("\${ai.koog.agents.tracing}")
    private var enableTracing: Boolean = false

    private val tools =
        ToolRegistry {
            tools(AssistantTools())
        }

    private val strategy =
        strategy(
            name = "test-strategy",
        ) {
            val moderateInput by nodeLLMModerateMessage(
                name = "moderate-input",
                moderatingModel = OpenAIModels.Moderation.Omni,
            )
            val callLLM by nodeLLMRequest("llm-call")

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
                    transformed { moderationErrorResponse },
            )

            edge(callLLM forwardTo nodeFinish onAssistantMessage { true })
        }

    suspend fun giveAdvice(
        input: String,
        sessionId: SessionId,
    ): String =
        try {
            val relevantDocuments =
                rankedDocumentStorage
                    .mostRelevantDocuments(input, count = 3)
                    .toList()

            val systemPrompt =
                promptTemplateProvider.getPromptTemplate(
                    group = "elven-assistant",
                    id = "system",
                    version = "latest",
                )

            val agent =
                AIAgent(
                    id = sessionId,
                    promptExecutor = promptExecutor,
                    agentConfig =
                        AIAgentConfig(
                            prompt =
                                createPrompt(systemPrompt, input, relevantDocuments),
                            model = OpenAIModels.CostOptimized.GPT4_1Mini,
                            maxAgentIterations = 100,
                        ),
                    strategy = strategy,
                    toolRegistry = tools,
                ) {
                    install(Persistence) {
                        storage = persistenceStorageProvider

                        // Enable automatic checkpoint creation
                        this.enableAutomaticPersistence = true

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

                    if (enableTracing) {
                        install(Tracing) {
                            // Configure message processors to handle trace events
                            addMessageProcessor(TraceFeatureMessageLogWriter(kotlinLogger))
                        }
                    }
                }

            log.trace("Running command: {}", input)
            agent.run(input)
        } catch (e: Exception) {
            log.error("Error processing request", e)
            systemErrorResponse
        }

    private fun createPrompt(
        systemPrompt: String,
        input: String,
        relevantDocuments: List<Path>,
    ): Prompt =
        prompt("with-context") {
            system(systemPrompt)
            user {
                +"User's input: ```$input```."
                if (relevantDocuments.isNotEmpty()) {
                    +"Use attachment as relevant context"
                    attachments {
                        relevantDocuments.forEach {
                            createAttachmentFromFile(path = it)
                        }
                    }
                }
            }
        }
}

private fun AttachmentBuilder.createAttachmentFromFile(path: Path) {
    val file = File("./${path.pathString}")
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