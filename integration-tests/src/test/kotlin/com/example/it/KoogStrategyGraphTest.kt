package com.example.it

import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class KoogStrategyGraphTest : AbstractIntegrationTest() {
    @Test
    fun `Should get mermaid diagram`(): Unit =
        runTest {
            val response = koogClient.mermaid()

            // language=mermaid
            response shouldContain
                """
                ---
                title: streaming-strategy
                ---
                stateDiagram
                """.trimIndent()
        }
}