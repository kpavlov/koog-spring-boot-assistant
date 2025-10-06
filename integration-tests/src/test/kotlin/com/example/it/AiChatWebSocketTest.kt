package com.example.it

import com.example.it.infra.WebSocketChatClient
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

class AiChatWebSocketTest : AbstractIntegrationTest() {
    private val wsClient: WebSocketChatClient = WebSocketChatClient(port = server.port)

    @BeforeEach
    fun openWebSocket() {
        wsClient.connect()
    }

    @AfterEach
    fun closeWebSocket() {
        wsClient.close()
    }

    @Test
    fun `Should answer a Question via WebSocket`(): Unit =
        runTest {
            val seed = nextInt()
            val question = "To be or not to be, $seed?"
            val expectedAnswer = "It's a good question: $question"
            val expectedTokens =
                expectedAnswer
                    .split(" ")
                    .map { "$it " }

            mockOpenai.embeddings {
                stringInput(question)
            } responds {
                delay = 40.milliseconds
            }

            mockOpenai.moderation {
                inputContains(question)
            } responds {
                flagged = false
            }

            val delayBetweenChunks = 500.milliseconds
            mockOpenai.completion {
                systemMessageContains("witty and wise Elven assistant guiding adventurers")
                userMessageContains(question)
            } respondsStream {
                responseFlow =
                    expectedTokens
                        .asFlow()
                        .onEach { delay(delayBetweenChunks) }
            }

            val responseFlow = wsClient.sendMessageStreaming(question)

            val responseWithTime =
                measureTimedValue {
                    responseFlow
                        .map { it.message }
                        .filterNot { it.isBlank() }
                        .toList()
                }

            responseWithTime.value shouldBe expectedTokens

            responseWithTime.duration shouldBeGreaterThanOrEqualTo
                (delayBetweenChunks * responseWithTime.value.size)
        }
}