package com.example.it

import com.example.it.client.model.Answer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

class ChatClient(
    val port: Int,
) {
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

    suspend fun sendMessage(
        message: String,
        expectedStatusCode: HttpStatusCode = HttpStatusCode.OK,
    ): Answer {
        val response =
            client.post("http://localhost:$port/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
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

        return response.body<Answer>()
    }

    @Serializable
    data class ChatRequest(
        val message: String,
        val chatSessionId: String? = null,
    )
}