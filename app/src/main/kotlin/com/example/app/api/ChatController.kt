package com.example.app.api

import com.example.app.Generators.randomSessionId
import com.example.app.agents.ElvenAgent
import com.example.app.api.model.Answer
import com.example.app.api.model.ChatRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody

@Component
class ChatController(
    val agent: ElvenAgent,
) : ChatApi {
    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    override suspend fun getVersion(): ResponseEntity<String> = ResponseEntity.ok("1.0")

    override suspend fun chat(
        @Valid @RequestBody chatRequest: ChatRequest,
    ): ResponseEntity<Answer> {
        logger.info("Received chat request: {}", chatRequest)
        val sessionId = chatRequest.sessionId ?: randomSessionId()
        val reply = agent.giveAdvice(sessionId = sessionId, input = chatRequest.message)
        return ResponseEntity.ok(Answer(message = reply, sessionId = sessionId))
    }
}