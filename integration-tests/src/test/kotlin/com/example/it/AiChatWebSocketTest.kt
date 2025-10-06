package com.example.it

import com.example.it.infra.WebSocketChatClient
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.milliseconds

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

            mockOpenai.completion {
                systemMessageContains("witty and wise Elven assistant guiding adventurers")
                userMessageContains(question)
            } responds {
                assistantContent = expectedAnswer
            }

            val responseFlow = wsClient.sendMessageStreaming(question)

            val responseString =
                responseFlow
                    .map { it.message }
                    .toList()
                    .joinToString(separator = "")
                    .trim()

            responseString shouldBe expectedAnswer
        }
}