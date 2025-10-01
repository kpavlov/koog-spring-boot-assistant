package com.example.it

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.milliseconds

class AiChatIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun shouldAnswerAQuestion(): Unit =
        runTest {
            val seed = nextInt()
            val question = "To be or not to be, $seed?"
            val expectedAnswer = """It's a good question: "$question""""

            mockOpenai.embeddings {
                stringInput(question)
            } responds {
                delay = 20.milliseconds
            }

            mockOpenai.moderation {
                inputContains(question)
            } responds {
                flagged = false
            }

            mockOpenai.completion {
                systemMessageContains("Elven assistant")
                userMessageContains(question)
            } responds {
                assistantContent = expectedAnswer
            }

            val response = chatClient.sendMessage(question)

            response shouldNotBeNull {
                message shouldBe expectedAnswer
                sessionId shouldNotBe null
            }
        }
}