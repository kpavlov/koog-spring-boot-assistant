package com.example.it

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.milliseconds

class AiChatFailuresTest : AbstractIntegrationTest() {
    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 418, 500, 503])
    fun `Should handle embedding request failure`(errorStatusCode: Int): Unit =
        runTest {
            val seed = nextInt()
            val question = "RAG should fail, $seed?"
            val expectedAnswer = "Alas, I cannot help thee now, mellon."

            mockOpenai.moderation {
                inputContains(question)
            } responds {
                flagged = false
            }

            mockOpenai.embeddings {
                stringInput(question)
            } respondsError {
                httpStatusCode = errorStatusCode
                body = ""
                delay = 42.milliseconds
            }

            val response = chatClient.sendMessage(message = question, expectedStatusCode = HttpStatusCode.OK)

            response shouldNotBeNull {
                message shouldBe expectedAnswer
                chatSessionId shouldNotBe null
            }
        }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 418, 500, 503])
    fun `Should handle moderation request failure`(errorStatusCode: Int): Unit =
        runTest {
            val seed = nextInt()
            val question = "Moderation should fail, $seed?"
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
                httpStatusCode = errorStatusCode
            }

            val response =
                chatClient.sendMessage(
                    message = question,
                    expectedStatusCode = HttpStatusCode.OK,
                )

            response shouldNotBeNull {
                message shouldBe expectedAnswer
                chatSessionId shouldNotBe null
            }
        }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 418, 500, 503])
    fun `Should handle LLM request failure`(errorStatusCode: Int): Unit =
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
                systemMessageContains("witty and wise Elven assistant guiding adventurers")
                userMessageContains(question)
            } respondsError {
                httpStatusCode = errorStatusCode
                contentType = ContentType.Text.EventStream
                body = emptyFlow<String>()
            }

            mockOpenai.completion {
                systemMessageContains("Elven assistant")
                userMessageContains(question)
            } respondsError {
                body = ""
                httpStatusCode = errorStatusCode
            }

            val response =
                chatClient.sendMessage(
                    message = question,
                    expectedStatusCode = HttpStatusCode.OK,
                )

            response shouldNotBeNull {
                message shouldBe expectedAnswer
                chatSessionId shouldNotBe null
            }
        }
}