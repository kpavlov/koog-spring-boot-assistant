package com.example.it

import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.springframework.boot.SpringApplication
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.ApplicationContext
import org.springframework.web.context.WebApplicationContext

object Server {
    val port: Int
        get() = (applicationContext as ReactiveWebServerApplicationContext).webServer.port

    private var applicationContext: ApplicationContext

    init {
        System.setProperty("ai.koog.openai.base-url", TestEnvironment.mockOpenai.baseUrl())

        applicationContext =
            SpringApplication.run(
                com.example.app.Application::class.java,
                "--server.port=0",
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