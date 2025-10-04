package com.example.app.agents

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
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