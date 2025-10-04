package com.example.it.infra

import com.example.it.client.model.Answer
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import org.awaitility.kotlin.await
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.net.URI

/**
 * WebSocket-based chat client implementation using Spring WebFlux ReactorNettyWebSocketClient.
 *
 * @param uri The WebSocket endpoint URI (e.g., "ws://localhost:8080/ws/chat")
 * @param sessionId Optional session ID to include in handshake headers
 */
class WebSocketChatClient(
    port: Int,
    private val uri: String = "ws://localhost:$port/ws/chat",
) : ChatSession {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = ReactorNettyWebSocketClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val outgoingMessages = Sinks.many().multicast().onBackpressureBuffer<String>()
    private val incomingChannel = Channel<String>(Channel.UNLIMITED)

    private var webSocketSession: WebSocketSession? = null
    private var isConnected = false

    val sessionId: String?
        get() = webSocketSession?.id

    /**
     * Connects to the WebSocket server and establishes the communication channel.
     */
    fun connect() {
        if (isConnected) return

        val headers =
            org.springframework.http.HttpHeaders().apply {
                sessionId?.let {
                    add("X-Session-Id", it)
                }
            }

        client
            .execute(
                URI.create(uri),
                headers,
            ) { session ->
                webSocketSession = session

                // Handle incoming messages
                val receive =
                    session
                        .receive()
                        .map { it.payloadAsText }
                        .doOnNext { message ->
                            logger.debug("Received message: {}", message)
                            incomingChannel.trySend(message)
                        }.then()

                // Handle outgoing messages
                val send =
                    session.send(
                        outgoingMessages
                            .asFlux()
                            .doOnNext { message -> logger.debug("Sending message: {}", message) }
                            .map { session.textMessage(it) },
                    )

                // Keep connection alive until both send and receive are complete
                Flux.merge(receive, send).then()
            }.subscribe(
                { logger.info("WebSocket connection established") },
                { error -> logger.error("WebSocket connection error", error) },
                { logger.info("WebSocket connection closed") },
            )

        // Wait a bit for the connection to be established
        await.until {
            sessionId != null
        }
        isConnected = true
    }

    override suspend fun sendMessage(message: String): Answer {
        connect()

        val request =
            ChatRequest(
                message = message,
            )
        val requestJson = json.encodeToString(request)

        // Send message
        outgoingMessages.tryEmitNext(requestJson)

        // Wait for response
        val responseJson = incomingChannel.receive()
        val response = json.decodeFromString<Answer>(responseJson)

        return response
    }

    override suspend fun close() {
        if (isConnected) {
            outgoingMessages.tryEmitComplete()
            incomingChannel.close()
            isConnected = false
            webSocketSession = null
        }
    }

    @kotlinx.serialization.Serializable
    private data class ChatRequest(
        val message: String,
    )
}