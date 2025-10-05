package com.example.app.koog.strategy

import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMModerateMessage
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class CreateMermaidDiagramTest {
    private val strategy =
        strategy(
            name = "test-strategy",
        ) {
            val moderateInput by nodeLLMModerateMessage(
                name = "moderate-input",
                moderatingModel = OpenAIModels.Moderation.Omni,
            )
            val nodeCallLLM by nodeLLMRequest("CallLLM")

            val nodeExecuteTool by nodeExecuteTool("ExecuteTool")
            val nodeSendToolResult by nodeLLMSendToolResult("SendToolResult")

            edge(
                nodeStart forwardTo moderateInput transformed {
                    Message.User(it, metaInfo = RequestMetaInfo.Empty)
                },
            )

            edge(
                moderateInput forwardTo nodeCallLLM
                    onCondition { !it.moderationResult.isHarmful }
                    transformed { it.message.content },
            )

            edge(
                moderateInput forwardTo nodeFinish
                    onCondition { it.moderationResult.isHarmful }
                    transformed { "Moderation Error" },
            )

            edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
            edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
            edge(nodeExecuteTool forwardTo nodeSendToolResult)
            edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
            edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
        }

    @Test
    fun `Should create a mermaid diagram`() {
        val diagram = strategy.createMermaidDiagram()

        // println(diagram)

        diagram shouldBe
            """
            graph TD
                __start__["__start__"]
                __finish__["__finish__"]
                moderate-input["moderate-input"]
                CallLLM["CallLLM"]
                ExecuteTool["ExecuteTool"]
                SendToolResult["SendToolResult"]
            
                __start__ --> |"transformed"| moderate-input
                moderate-input --> |"transformed"| CallLLM
                moderate-input --> |"transformed"| __finish__
                CallLLM --> |"transformed"| __finish__
                CallLLM --> |"onCondition"| ExecuteTool
                ExecuteTool --> SendToolResult
                SendToolResult --> |"transformed"| __finish__
                SendToolResult --> |"onCondition"| ExecuteTool
            """.trimIndent()
    }
}