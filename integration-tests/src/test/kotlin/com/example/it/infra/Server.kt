package com.example.it

import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.springframework.boot.SpringApplication
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext
import org.springframework.context.ApplicationContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object Server {
    val port: Int
        get() = (applicationContext as ReactiveWebServerApplicationContext).webServer.port

    private var applicationContext: ApplicationContext

    init {
        System.setProperty("ai.koog.openai.base-url", TestEnvironment.mockOpenai.baseUrl())

        applicationContext =
            SpringApplication
                .run(
                    com.example.app.Application::class.java,
                    "--server.port=0",
                )

//        if (System.getenv("CI") != null) {
//            await.timeout(3.seconds.toJavaDuration())
//        }

//        awaitServerIsRunning()
    }

    fun awaitServerIsRunning() {
        val chatClient = ChatClient(port)
// /*
//        await
//            .ignoreExceptions()
//            .alias("Server is healthy.")
//            .pollInterval(500.milliseconds.toJavaDuration())
//            .timeout(15.seconds.toJavaDuration())
//            .until {
//                runBlocking {
//                    chatClient.healthy()
//                }
//            }
// */
//

        await
            .ignoreExceptions()
            .alias("API is ready")
            .pollInterval(500.milliseconds.toJavaDuration())
            .timeout(10.seconds.toJavaDuration())
            .until {
                runBlocking {
                    chatClient.version() == "1.0"
                }
            }
    }
}