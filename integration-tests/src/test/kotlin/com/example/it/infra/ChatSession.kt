package com.example.it.infra

import com.example.it.client.model.Answer
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Abstraction for chat session that can send messages and receive responses.
 */
@OptIn(ExperimentalUuidApi::class)
interface ChatSession {
    /**
     * Send a message and wait for a response asynchronously.
     *
     * @param message The message to send
     * @return The response from the server
     */
    suspend fun sendMessage(
        message: String,
        requestId: String? = "REQ_${Uuid.random().toHexString()}",
    ): Answer = TODO("Not implemented yet")

    fun sendMessageStreaming(
        message: String,
        requestId: String? = "REQ_${Uuid.random().toHexString()}",
    ): Flow<Answer> = TODO("Not implemented yet")

    /**
     * Close the chat session and release resources.
     */
    fun close()
}