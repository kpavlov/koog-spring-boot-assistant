package com.example.it

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.milliseconds

class AiChatFailuresTest : AbstractIntegrationTest() {
    @Test
    fun `Should handle embedding request failure`(): Unit =
        runTest {
            val seed = nextInt()
            val question = "To be or not to be, $seed?"
            val expectedAnswer = "Alas, I cannot help thee now, mellon."

            mockOpenai.moderation {
                inputContains(question)
            } responds {
                flagged = false
            }

            mockOpenai.embeddings {
                stringInput(question)
            } respondsError {
                httpStatus = HttpStatusCode.InternalServerError
                body = ""
                delay = 42.milliseconds
            }

            val response = chatClient.sendMessage(question)

            response shouldNotBeNull {
                message shouldBe expectedAnswer
                sessionId shouldNotBe null
            }
        }

    @Test
    fun `Should handle moderation request failure`(): Unit =
        runTest {
            val seed = nextInt()
            val question = "To be or not to be, $seed?"
            val expectedAnswer = "Alas, I cannot help thee now, mellon."

            mockOpenai.embeddings {
                stringInput(question)
            } responds {
                delay = 1.milliseconds
            }

            mockOpenai.moderation {
                inputContains(question)
            } respondsError {
                body = ""
                httpStatus = HttpStatusCode.InternalServerError
            }

            val response = chatClient.sendMessage(question)

            response shouldNotBeNull {
                message shouldBe expectedAnswer
                sessionId shouldNotBe null
            }
        }

    @Test
    fun `Should handle LLM request failure`(): Unit =
        runTest {
            val seed = nextInt()
            val question = "To be or not to be, $seed?"
            val expectedAnswer = "Alas, I cannot help thee now, mellon."

            mockOpenai.embeddings {
                stringInput(question)
            } responds {
                delay = 1.milliseconds
            }

            mockOpenai.moderation {
                inputContains(question)
            } responds {
                flagged = false
            }

            mockOpenai.completion {
                systemMessageContains("Elven assistant")
                userMessageContains(question)
            } respondsError {
                body = ""
                httpStatus = HttpStatusCode.InternalServerError
            }

            val response = chatClient.sendMessage(question)

            response shouldNotBeNull {
                message shouldBe expectedAnswer
                sessionId shouldNotBe null
            }
        }
}