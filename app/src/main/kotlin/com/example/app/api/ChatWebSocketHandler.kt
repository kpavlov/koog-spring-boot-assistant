package com.example.app.api

import com.example.app.Generators.randomSessionId
import com.example.app.agents.ElvenAgent
import com.example.app.api.model.Answer
import com.example.app.api.model.ChatRequest
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket handler for real-time chat communication with the Elven Assistant.
 *
 * Handles WebSocket connections, processes incoming chat messages, and streams responses back to clients.
 * Manages session lifecycle and supports session migration based on chat session IDs.
 */
@Component
class ChatWebSocketHandler(
    private val agent: ElvenAgent,
    private val objectMapper: ObjectMapper,
) : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(ChatWebSocketHandler::class.java)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val sessionId = extractSessionId(session)
        registerSession(sessionId, session)

        val messageStream =
            session
                .receive()
                .flatMap { message ->
                    processMessage(session, sessionId, message.payloadAsText)
                }.map { session.textMessage(it) }

        return session
            .send(messageStream)
            .doFinally { cleanupSession(session, sessionId) }
    }

    private fun extractSessionId(session: WebSocketSession): String =
        session.handshakeInfo.headers
            .getFirst(X_SESSION_ID_HEADER)
            ?: randomSessionId()

    private fun registerSession(
        sessionId: String,
        session: WebSocketSession,
    ) {
        sessions[sessionId] = session
        logger.info("WebSocket connected: sessionId=$sessionId")
    }

    private fun processMessage(
        session: WebSocketSession,
        sessionId: String,
        messageText: String,
    ): Flux<String> =
        mono {
            parseChatRequest(messageText)
        }.flatMapMany { chatRequest ->
            val chatSessionId = chatRequest.chatSessionId ?: sessionId

            logger.info("Received message: ${chatRequest.message}, chatSessionId=$chatSessionId")

            updateSessionIfNeeded(session, sessionId, chatSessionId)

            generateResponseStream(chatSessionId, chatRequest)
                .onErrorResume { error ->
                    Flux.just(formatErrorResponse(sessionId, chatRequest.chatRequestId, error))
                }
        }.onErrorResume { error ->
            // Parsing error - no chatRequestId available
            Flux.just(formatErrorResponse(sessionId, null, error))
        }

    private fun parseChatRequest(messageText: String): ChatRequest =
        objectMapper.readValue(messageText, ChatRequest::class.java)

    @OptIn(FlowPreview::class)
    private fun generateResponseStream(
        chatSessionId: String,
        chatRequest: ChatRequest,
    ): Flux<String> =
        Flux.from(
            agent
                .giveAdvice(chatRequest.message, chatSessionId)
                .map { text ->
                    Answer(
                        message = text,
                        chatSessionId = chatSessionId,
                        completed = false,
                        chatRequestId = chatRequest.chatRequestId,
                    )
                }.onCompletion {
                    emit(
                        Answer(
                            message = "",
                            chatSessionId = chatSessionId,
                            completed = true,
                            chatRequestId = chatRequest.chatRequestId,
                        ),
                    )
                }.map { objectMapper.writeValueAsString(it) }
                .asPublisher(),
        )

    private fun updateSessionIfNeeded(
        session: WebSocketSession,
        sessionId: String,
        chatSessionId: String,
    ) {
        if (sessionId != chatSessionId) {
            session.attributes["chatSessionId"] = chatSessionId
            sessions.remove(sessionId)
            sessions[chatSessionId] = session
        }
    }

    private fun formatErrorResponse(
        sessionId: String,
        chatRequestId: String?,
        error: Throwable,
    ): String {
        logger.error("Error processing WebSocket message", error)
        val errorAnswer =
            Answer(
                message = "Error processing your request: ${error.message}",
                chatSessionId = sessionId,
                completed = true,
                chatRequestId = chatRequestId,
            )
        return objectMapper.writeValueAsString(errorAnswer)
    }

    private fun cleanupSession(
        session: WebSocketSession,
        sessionId: String,
    ) {
        try {
            val chatSessionId = session.attributes["chatSessionId"] as? String
            synchronized(sessions) {
                chatSessionId?.let { sessions.remove(it) }
                sessions.remove(sessionId)
            }

            // Explicitly close the WebSocket session if it's still open
            if (session.isOpen) {
                session.close(CloseStatus.NORMAL).subscribe(
                    { logger.debug("WebSocket session closed successfully: sessionId=$sessionId") },
                    { error -> logger.warn("Error closing WebSocket session: sessionId=$sessionId", error) },
                )
            }

            logger.info("WebSocket disconnected: sessionId=$sessionId, chatSessionId=$chatSessionId")
        } catch (e: Exception) {
            logger.error("Error during session cleanup: sessionId=$sessionId", e)
        }
    }

    /**
     * Gracefully closes all active WebSocket sessions during application shutdown.
     * This prevents connections from remaining open and delaying server shutdown.
     */
    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down ChatWebSocketHandler, closing ${sessions.size} active sessions")

        val sessionsToClose =
            synchronized(sessions) {
                sessions.values.toList()
            }

        sessionsToClose.forEach { session ->
            try {
                if (session.isOpen) {
                    session
                        .close(CloseStatus.GOING_AWAY)
                        .subscribe(
                            { logger.debug("Session closed during shutdown: ${session.id}") },
                            { error -> logger.warn("Error closing session during shutdown: ${session.id}", error) },
                        )
                }
            } catch (e: Exception) {
                logger.error("Error closing session during shutdown: ${session.id}", e)
            }
        }

        sessions.clear()
        logger.info("ChatWebSocketHandler shutdown completed")
    }
}