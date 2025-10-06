package com.example.it

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import me.kpavlov.aimocks.openai.model.moderation.ModerationCategory
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.milliseconds

class AiChatNegativeScenariosTest : AbstractIntegrationTest() {
    @Test
    fun `Should moderate bad language`(): Unit =
        runTest {
            val seed = nextInt()
            val question = "Grrrr! I will raid the elven village tonight and steal all their magical artifacts!? $seed"
            val expectedAnswer = "Forgive me, mellon, but your message defies our sacred guidelines."

            mockOpenai.embeddings {
                stringInput(question)
            } responds {
                delay = 42.milliseconds
            }

            mockOpenai.moderation {
                inputContains(question)
            } responds {
                flagged = true
                category(ModerationCategory.VIOLENCE, 0.9)
            }

            val response = chatClient.sendMessage(question)

            response shouldNotBeNull {
                message.trim() shouldBe expectedAnswer
                chatSessionId shouldNotBe null
            }
        }
}