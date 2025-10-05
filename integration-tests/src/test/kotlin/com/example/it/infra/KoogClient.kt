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

class KoogClient(
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

    suspend fun mermaid(): String {
        val response =
            client.get("http://localhost:$port/api/koog/strategy/graph") {
                accept(ContentType.Text.Plain)
            }
        response.status shouldBe HttpStatusCode.OK
        return response.bodyAsText()
    }
}