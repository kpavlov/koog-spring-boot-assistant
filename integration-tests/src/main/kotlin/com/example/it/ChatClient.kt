package com.example.it

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
import kotlinx.serialization.Serializable

class ChatClient(
    val port: Int,
) {
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

    suspend fun sendMessage(message: String): Answer {
        val response =
            client.post("http://localhost:$port/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        message = message,
                    ),
                )
            }

//        response shouldHaveStatus HttpStatusCode.OK
        response.status shouldBe HttpStatusCode.OK

        return response.body()
    }

    @Serializable
    data class ChatRequest(
        val message: String,
        val sessionId: String? = null,
    )

    @Serializable
    data class Answer(
        val message: String,
        val sessionId: String,
    )
}