package com.example.app.agents

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMModerateMessage
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreaming
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Configuration

@Configuration
class AgentConfiguration {
    fun streamingAgentStrategy(processFrame: (StreamFrame.Append) -> Unit): AIAgentGraphStrategy<String, Any> =
        strategy(
            name = "streaming-strategy",
        ) {
            val moderateInput by nodeLLMModerateMessage(
                name = "moderate-input",
                moderatingModel = OpenAIModels.Moderation.Omni,
            )
            val executeMultipleTools by nodeExecuteMultipleTools(parallelTools = true)
            val nodeStreaming by nodeLLMRequestStreaming()
            val nodeSendResults by node<Flow<StreamFrame>, List<Message.Response>> { frames ->
                var end: StreamFrame.End? = null

                frames
                    .mapNotNull {
                        when (it) {
                            is StreamFrame.Append -> {
                                processFrame(it)
                                null
                            }

                            is StreamFrame.End -> {
                                end = it
                                null
                            }

                            is StreamFrame.ToolCall -> it
                        }
                    }.map {
                        Message.Tool.Call(
                            id = it.id,
                            tool = it.name,
                            content = it.content,
                            metaInfo =
                                end?.metaInfo ?: ResponseMetaInfo.Empty,
                        )
                    }.toList()
            }

            edge(
                nodeStart forwardTo moderateInput transformed {
                    Message.User(it, metaInfo = RequestMetaInfo.Empty)
                },
            )

            edge(
                moderateInput forwardTo nodeStreaming
                    onCondition { !it.moderationResult.isHarmful }
                    transformed { it.message.content },
            )

            edge(
                moderateInput forwardTo nodeFinish
                    onCondition { it.moderationResult.isHarmful }
                    transformed { "" }, // handles on Agent level
            )

            val sendToolResults by nodeLLMSendMultipleToolResults()

            edge(nodeStreaming forwardTo nodeSendResults)
            edge(nodeSendResults forwardTo executeMultipleTools onMultipleToolCalls { true })
            edge(executeMultipleTools forwardTo sendToolResults)
            edge(
                nodeSendResults forwardTo nodeFinish onCondition {
                    it.filterIsInstance<Message.Tool.Call>().isEmpty()
                },
            )
        }
}