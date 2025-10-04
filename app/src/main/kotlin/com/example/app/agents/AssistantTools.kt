package com.example.app.agents

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

@Suppress("unused")
class AssistantTools : ToolSet {
    private val artifactPrices: Map<String, Int>

    init {
        // Calculate random prices at initialization (in gold pieces)
        val random = Random.Default
        artifactPrices =
            MAGICAL_ARTIFACTS.associateWith {
                random.nextInt(100, 10000)
            }
    }

    @Tool
    @LLMDescription("Returns a list of magical artifacts available for sale in the Elven marketplace")
    fun listMagicalArtifacts(): List<String> = MAGICAL_ARTIFACTS.toList()

    @Tool
    @LLMDescription("Returns the price of a specific magical artifact in gold pieces")
    fun getArtifactPrice(
        @LLMDescription("The name of the artifact") artifactName: String,
    ): String {
        val price = artifactPrices[artifactName]
        return if (price != null) {
            "$price gold pieces"
        } else {
            "Artifact not found. Please use listMagicalArtifacts() to see available items."
        }
    }

    @Tool
    @LLMDescription("Returns the current time in the Elven calendar (Fourth Age reckoning)")
    fun getElvenTime(): String {
        val now = LocalDateTime.now(ZoneId.systemDefault())

        // Fourth Age began approximately at year 1000 CE (Gregorian)
        // This is a simplified interpretation
        val fourthAgeYear = now.year - 1000

        // Elven day names (from Quenya, Calendar of Imladris)
        val dayOfWeek =
            when (now.dayOfWeek.value) {
                1 -> "Elenya (Monday)" // Stars-day
                2 -> "Anarya (Tuesday)" // Sun-day
                3 -> "Isilya (Wednesday)" // Moon-day
                4 -> "Aldúya (Thursday)" // Tree-day
                5 -> "Menelya (Friday)" // Heavens-day
                6 -> "Valanya (Saturday)" // Valar-day
                7 -> "Elenya (Sunday)" // Stars-day
                else -> "Unknown"
            }

        val hour = now.hour
        val minute = now.minute

        return buildString {
            append("Fourth Age $fourthAgeYear, ")
            append("$dayOfWeek, ")
            append("${now.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${now.dayOfMonth}, ")
            append("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
        }
    }

    companion object {
        private val MAGICAL_ARTIFACTS =
            arrayOf(
                "Phial of Galadriel",
                "Mithril Circlet of Wisdom",
                "Elven Cloak of Concealment",
                "Silver Harp of Rivendell",
                "Ring of Vilya (replica)",
                "Mirror of Seeing",
                "Lembas Bread (enchanted supply)",
            )
    }
}