package com.example.app.api

import com.example.app.Generators.randomSessionId
import com.example.app.agents.ElvenAgent
import com.example.app.api.model.Answer
import com.example.app.api.model.ChatRequest
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatWebSocketHandler(
    private val agent: ElvenAgent,
    private val objectMapper: ObjectMapper,
) : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(ChatWebSocketHandler::class.java)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        // Extract session ID from X-Session-Id header during handshake
        val sessionId =
            session.handshakeInfo.headers
                .getFirst("X-Session-Id")
                ?: randomSessionId()

        // Store session
        sessions[sessionId] = session
        logger.info("WebSocket connected: sessionId=$sessionId")

        val output =
            session
                .receive()
                .map { it.payloadAsText }
                .flatMap { message ->
                    mono {
                        try {
                            val chatRequest = objectMapper.readValue(message, ChatRequest::class.java)

                            val chatSessionId = chatRequest.chatSessionId ?: sessionId

                            logger.info(
                                "Received message: ${chatRequest.message}, sessionId=$chatSessionId",
                            )

                            if (sessionId != chatSessionId) {
                                session.attributes["chatSessionId"] = chatSessionId
                                sessions.remove(sessionId)
                                sessions[chatSessionId] = session
                            }

                            val reply =
                                agent.giveAdvice(
                                    chatSessionId = chatSessionId,
                                    input = chatRequest.message,
                                )

                            val answer = Answer(message = reply, chatSessionId = chatSessionId)

                            objectMapper.writeValueAsString(answer)
                        } catch (e: Exception) {
                            logger.error("Error processing WebSocket message", e)
                            val errorAnswer =
                                Answer(
                                    message = "Error processing your request: ${e.message}",
                                    chatSessionId = sessionId,
                                )
                            objectMapper.writeValueAsString(errorAnswer)
                        }
                    }
                }.map { session.textMessage(it) }

        return session
            .send(output)
            .doFinally {
                val chatSessionId = session.attributes["chatSessionId"]
                chatSessionId?.let {
                    sessions.remove(it)
                }
                sessions.remove(sessionId)
                logger.info("WebSocket disconnected: sessionId=$sessionId, chatSessionId=$chatSessionId")
            }
    }
}