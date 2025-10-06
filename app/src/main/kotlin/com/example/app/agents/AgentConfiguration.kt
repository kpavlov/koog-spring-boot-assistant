package com.example.app.agents

import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMModerateMessage
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreamingAndSendResults
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AgentConfiguration {
    @Bean
    fun streamingAgentStrategy() =
        strategy(
            name = "streaming-strategy",
        ) {
            val moderateInput by nodeLLMModerateMessage(
                name = "moderate-input",
                moderatingModel = OpenAIModels.Moderation.Omni,
            )
            val executeMultipleTools by nodeExecuteMultipleTools(parallelTools = true)
            val nodeStreaming by nodeLLMRequestStreamingAndSendResults()

            val mapStringToRequests by node<String, List<Message.Request>> { input ->
                listOf(Message.User(content = input, metaInfo = RequestMetaInfo.Empty))
            }

            val applyRequestToSession by node<List<Message.Request>, List<Message.Request>> { input ->
                llm.writeSession {
                    updatePrompt {
                        input
                            .filterIsInstance<Message.User>()
                            .forEach {
                                user(it.content)
                            }

                        tool {
                            input
                                .filterIsInstance<Message.Tool.Result>()
                                .forEach {
                                    result(it)
                                }
                        }
                    }
                    input
                }
            }

            val mapToolCallsToRequests by node<List<ReceivedToolResult>, List<Message.Request>> { input ->
                input.map { it.toMessage() }
            }

            edge(
                nodeStart forwardTo moderateInput transformed {
                    Message.User(it, metaInfo = RequestMetaInfo.Empty)
                },
            )

            edge(
                moderateInput forwardTo mapStringToRequests
                    onCondition { !it.moderationResult.isHarmful }
                    transformed { it.message.content },
            )

            edge(
                moderateInput forwardTo nodeFinish
                    onCondition { it.moderationResult.isHarmful }
                    transformed { "" }, // handles on Agent level
            )

            edge(mapStringToRequests forwardTo applyRequestToSession)
            edge(applyRequestToSession forwardTo nodeStreaming)
            edge(nodeStreaming forwardTo executeMultipleTools onMultipleToolCalls { true })
            edge(executeMultipleTools forwardTo mapToolCallsToRequests)
            edge(mapToolCallsToRequests forwardTo applyRequestToSession)
            edge(
                nodeStreaming forwardTo nodeFinish onCondition {
                    it.filterIsInstance<Message.Tool.Call>().isEmpty()
                },
            )
        }
}