package com.example.app.api

import com.example.app.Generators.randomSessionId
import com.example.app.agents.ElvenAgent
import com.example.app.api.model.Answer
import com.example.app.api.model.ChatRequest
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class ChatController(
    val agent: ElvenAgent,
) : ChatApi {
    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    override suspend fun getVersion(): ResponseEntity<String> = ResponseEntity.ok("1.0")

    override suspend fun chat(
        chatRequest: ChatRequest,
        xSessionID: String?,
    ): ResponseEntity<Answer> {
        logger.info("Received chat request: {}", chatRequest)
        val sessionId = xSessionID ?: randomSessionId()

        // run the agent
        val reply =
            agent
                .giveAdvice(
                    chatSessionId = sessionId,
                    input = chatRequest.message,
                ).toList()
                .joinToString(separator = "")

        val headers = HttpHeaders()
        headers.set(X_SESSION_ID_HEADER, sessionId)

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(
                Answer(
                    message = reply,
                    chatSessionId = sessionId,
                    completed = true,
                    chatRequestId = chatRequest.chatRequestId,
                ),
            )
    }
}