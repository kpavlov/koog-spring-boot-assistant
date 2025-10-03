package com.example.it

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class AbstractIntegrationTest {
    protected val env = TestEnvironment
    protected val mockOpenai = TestEnvironment.mockOpenai
    protected val server = Server

    protected val chatClient = ChatClient(server.port)

//    @BeforeEach
//    fun awaitServer() {
//        server.awaitServerIsRunning()
//    }

    @AfterEach
    fun afterEach() {
        mockOpenai.verifyNoUnmatchedRequests()
    }
}