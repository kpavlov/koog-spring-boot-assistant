package com.example.it

import me.kpavlov.aimocks.openai.MockOpenai
import org.awaitility.Awaitility
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object TestEnvironment {
    val mockOpenai = MockOpenai(verbose = true)

    init {
        Awaitility.setDefaultTimeout(5.seconds.toJavaDuration())
        Awaitility.setDefaultPollDelay(500.milliseconds.toJavaDuration())
        Awaitility.setDefaultPollInterval(500.milliseconds.toJavaDuration())

        System.setProperty("OPENAI_API_KEY", "dummyOpenAIKey")
        System.setProperty("spring.profiles.active", "test")

        prepareForRagIngestion()
    }

    private fun prepareForRagIngestion() {
        listOf(
            "Care for Magical Trees",
            "Valley of Light",
            "Magical Bow",
            "Morning Pine Elixir",
            "Teleportation and Portals",
        ).forEach {
            mockOpenai.embeddings {
                inputContains(it)
            } responds {
                this.delay = 1.milliseconds
            }
        }
    }
}