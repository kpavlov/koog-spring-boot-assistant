package com.example.app.api

import com.example.app.Generators.randomSessionId
import com.example.app.SessionId
import com.example.app.agents.FinancialAgent
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
class ChatApi(
    val agent: FinancialAgent,
) {
    private val logger = LoggerFactory.getLogger(ChatApi::class.java)

    @GetMapping("/api/version", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun version(): String = "1.0"

    @PostMapping(
        "/api/chat",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    suspend fun chat(
        @RequestBody request: ChatRequest,
    ): Answer {
        logger.info("Received chat request: {}", request)
        val sessionId = request.sessionId ?: randomSessionId()
        val reply = agent.giveAdvice(request.message)
        return Answer(message = reply, sessionId = sessionId)
    }

    @Serializable
    data class ChatRequest(
        val message: String,
        val sessionId: SessionId? = null,
    )

    @Serializable
    data class Answer(
        val message: String,
        val sessionId: SessionId,
    )
}