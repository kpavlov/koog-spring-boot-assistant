package com.example.it

import org.junit.jupiter.api.AfterEach

abstract class AbstractIntegrationTest {
    protected val env = TestEnvironment
    protected val mockOpenai = TestEnvironment.mockOpenai
    protected val server = Server

    protected val chatClient = ChatClient(server.port)

    @AfterEach
    fun afterEach() {
        mockOpenai.verifyNoUnmatchedRequests()
    }
}