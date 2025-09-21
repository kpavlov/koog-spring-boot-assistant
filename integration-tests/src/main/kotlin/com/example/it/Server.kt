package com.example.it

import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext

object Server {
    val port: Int

    private var applicationContext: ApplicationContext

    init {
        System.setProperty("ai.koog.openai.base-url", TestEnvironment.mockOpenai.baseUrl())

        port = 8080
        applicationContext =
            SpringApplication.run(
                com.example.app.Application::class.java,
                "--server.port=$port",
            )
    }

    fun awaitServerIsRunning() {
        val chatClient = ChatClient(port)
        await
            .ignoreExceptions()
            .until {
                runBlocking {
                    chatClient.version() == "1.0"
                }
            }
    }
}