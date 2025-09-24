package com.example.app.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
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
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.slf4j.LoggerFactory
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service

@Service
class FinancialAgent(
    private val promptExecutor: PromptExecutor,
    private val spanExporters: List<SpanExporter>,
    private val buildProps: BuildProperties,
) {
    private val log = LoggerFactory.getLogger(FinancialAgent::class.java)
    private val kotlinLogger = KotlinLogging.logger(name = "FinancialAgent")

    private val systemPrompt =
        FinancialAgent::class.java.getResource("/prompts/financial-agent/system.md")!!.readText()

    private val tools =
        ToolRegistry {
            tools(AssistantTools())
        }

    private val strategy =
        strategy(
            name = "test-strategy",
            toolSelectionStrategy = ToolSelectionStrategy.NONE, // TODO: fix mokkcy
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
                    transformed { "Sorry, your message couldn't be processed due to content guidelines." },
            )

            edge(callLLM forwardTo nodeFinish onAssistantMessage { true })
        }

    suspend fun giveAdvice(input: String): String {
        val agent =
            AIAgent(
                promptExecutor = promptExecutor,
                agentConfig = AIAgentConfig.withSystemPrompt(systemPrompt),
                strategy = strategy,
                toolRegistry = tools,
            ) {
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

        log.debug("Running command: {}", input)
        return agent.run(input)
    }
}