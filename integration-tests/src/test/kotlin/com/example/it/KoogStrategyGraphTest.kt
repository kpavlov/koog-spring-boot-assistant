package com.example.it

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class KoogStrategyGraphTest : AbstractIntegrationTest() {
    @Test
    fun `Should get mermaid diagram`(): Unit =
        runTest {
            val response = koogClient.mermaid()

            // language=mermaid
            response shouldBe
                """
                graph TD
                    __start__["__start__"]
                    __finish__["__finish__"]
                    moderate-input["moderate-input"]
                    CallLLM["CallLLM"]
                    ExecuteTool["ExecuteTool"]
                    SendToolResult["SendToolResult"]
                
                    __start__ --> |"transformed"| moderate-input
                    moderate-input --> |"transformed"| CallLLM
                    moderate-input --> |"transformed"| __finish__
                    CallLLM --> |"transformed"| __finish__
                    CallLLM --> |"onCondition"| ExecuteTool
                    ExecuteTool --> SendToolResult
                    SendToolResult --> |"transformed"| __finish__
                    SendToolResult --> |"onCondition"| ExecuteTool
                """.trimIndent()
        }
}