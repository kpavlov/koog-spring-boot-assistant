package com.example.app.api

import com.example.app.Generators.randomSessionId
import com.example.app.agents.ElvenAgent
import com.example.app.api.model.Answer
import com.example.app.api.model.ChatRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@Component
class ChatController(
    val agent: ElvenAgent,
) : ChatApi {
    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    override suspend fun getVersion(): ResponseEntity<String> = ResponseEntity.ok("1.0")

    override suspend fun chat(
        chatRequest: ChatRequest,
        xSessionId: String?,
    ): ResponseEntity<Answer> {
        logger.info("Received chat request: {}", chatRequest)
        val sessionId = xSessionId ?: randomSessionId()
        val reply = agent.giveAdvice(chatSessionId = sessionId, input = chatRequest.message)

        val headers = HttpHeaders()
        headers.set("X-Session-Id", sessionId)

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(
                Answer(
                    message = reply,
                    chatSessionId = sessionId,
                ),
            )
    }
}