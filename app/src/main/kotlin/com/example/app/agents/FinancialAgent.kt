package com.example.app.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.prompt.executor.model.PromptExecutor
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
            val nodeSendInput by nodeLLMRequest("test-llm-call")

            edge(nodeStart forwardTo nodeSendInput)
            edge(nodeSendInput forwardTo nodeFinish onAssistantMessage { true })
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
                        // TODO: does it work?
                        addSpanExporter(it)
                    }
                }
            }

        log.debug("Running command: {}", input)
        return agent.run(input)
    }
}