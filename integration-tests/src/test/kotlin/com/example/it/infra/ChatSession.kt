package com.example.it.infra

import com.example.it.client.model.Answer

/**
 * Abstraction for chat session that can send messages and receive responses.
 */
interface ChatSession {
    /**
     * Send a message and wait for a response asynchronously.
     *
     * @param message The message to send
     * @return The response from the server
     */
    suspend fun sendMessage(message: String): Answer

    /**
     * Close the chat session and release resources.
     */
    suspend fun close()
}