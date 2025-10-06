package com.example.it.infra

import com.example.it.client.model.Answer
import com.example.it.client.model.ChatRequest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.json.Json
import org.awaitility.kotlin.await
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.net.URI
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * WebSocket-based chat client implementation using Spring WebFlux ReactorNettyWebSocketClient.
 *
 * @param uri The WebSocket endpoint URI (e.g., "ws://localhost:8080/ws/chat")
 * @param sessionId Optional session ID to include in handshake headers
 */
@OptIn(ExperimentalUuidApi::class)
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

    private var chatSessionId: String? = null
    val sessionId: String?
        get() = webSocketSession?.id

    /**
     * Connects to the WebSocket server and establishes the communication channel.
     */
    fun connect(chatSessionId: String = "CHAT_${Uuid.random().toHexString()}") {
        if (isConnected) return

        val headers =
            org.springframework.http.HttpHeaders().apply {
                sessionId?.let {
                    add("X-Session-ID", it)
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
        this.chatSessionId = chatSessionId
        isConnected = true
    }

    override suspend fun sendMessage(
        message: String,
        requestId: String?,
    ): Answer {
        connect()

        val request =
            ChatRequest(
                message = message,
                chatSessionId = chatSessionId,
                chatRequestId = requestId,
            )
        val requestJson = json.encodeToString(request)

        // Send message
        outgoingMessages.tryEmitNext(requestJson)

        // Wait for response
        val responseJson = incomingChannel.receive()
        val answer = json.decodeFromString<Answer>(responseJson)

        answer.chatRequestId shouldBe requestId

        return answer
    }

    override fun sendMessageStreaming(
        message: String,
        requestId: String?,
    ): Flow<Answer> {
        connect()

        val request =
            ChatRequest(
                message = message,
                chatSessionId = chatSessionId,
                chatRequestId = requestId,
                streaming = true,
            )
        val requestJson = json.encodeToString(request)

        // Send a message
        outgoingMessages.tryEmitNext(requestJson)

        // Wait for response and terminate when completed
        return incomingChannel
            .receiveAsFlow()
            .map { json.decodeFromString<Answer>(it) }
            .onEach { answer -> answer.chatRequestId shouldBe requestId }
            .transformWhile { answer ->
                emit(answer)
                !answer.completed // Continue while not completed
            }
    }

    override fun close() {
        if (isConnected) {
            outgoingMessages.tryEmitComplete()
            incomingChannel.close()
            isConnected = false
            chatSessionId = null
            webSocketSession = null
        }
    }
}