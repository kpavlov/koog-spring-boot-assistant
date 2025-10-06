package com.example.it

import com.example.it.client.model.Answer
import com.example.it.client.model.ChatRequest
import com.example.it.infra.ChatSession
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatClient(
    val port: Int,
) : ChatSession {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val client =
        HttpClient {
            followRedirects = true
            install(ContentNegotiation) {
                json()
            }
        }

    suspend fun version(): String {
        val response =
            client.get("http://localhost:$port/api/version") {
                accept(ContentType.Text.Plain)
            }
        return response.bodyAsText()
    }

    override suspend fun sendMessage(
        message: String,
        requestId: String?,
    ): Answer = sendMessage(message, requestId, expectedStatusCode = HttpStatusCode.OK)

    override fun close() {
        client.close()
    }

    suspend fun sendMessage(
        message: String,
        requestId: String? = "REQ_${Uuid.random().toHexString()}",
        expectedStatusCode: HttpStatusCode = HttpStatusCode.OK,
    ): Answer {
        val response =
            client.post("http://localhost:$port/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        chatRequestId = requestId,
                        message = message,
                    ),
                )
            }

        if (response.status != expectedStatusCode) {
            logger.error(
                "Received unexpected response: Headers: {}\nBody:\n{}",
                response.headers,
                response.bodyAsText(),
            )
        }
        response.status shouldBe expectedStatusCode
        val answer = response.body<Answer>()

        answer.chatRequestId shouldBe requestId

        return answer
    }
}