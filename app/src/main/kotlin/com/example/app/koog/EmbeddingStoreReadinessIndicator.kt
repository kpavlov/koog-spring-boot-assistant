package com.example.app.koog

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.AvailabilityState
import org.springframework.boot.availability.ReadinessState
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

@Component
internal class EmbeddingStoreReadinessIndicator(
    @Value("\${ai.koog.rag.embedding-store.path}")
    private val embeddingStorePath: Path,
    @Value("\${ai.koog.rag.knowledge-base.path}")
    private val knowledgeBasePath: Path,
    availability: ApplicationAvailability,
) : ReadinessStateHealthIndicator(availability) {
    override fun getState(applicationAvailability: ApplicationAvailability?): AvailabilityState {
        if (check() == 0) {
            return ReadinessState.ACCEPTING_TRAFFIC
        } else {
            return ReadinessState.REFUSING_TRAFFIC
        }
    }

    private fun check(): Int {
        val documentsPath = embeddingStorePath.resolve("documents")
        val vectorsPath = embeddingStorePath.resolve("vectors")

        // Check if directories exist
        if (!documentsPath.exists() || !vectorsPath.exists()) {
            return 1 // Directories don't exist
        }

        // Count files in each directory
        val knowledgeBasePath = documentsPath.listDirectoryEntries().filter { it.toFile().isFile() }

        val documentFiles = documentsPath.listDirectoryEntries().filter { it.toFile().isFile() }
        val vectorFiles = vectorsPath.listDirectoryEntries().filter { it.toFile().isFile() }

        val documentsCount = documentFiles.size
        val vectorsCount = vectorFiles.size
        val knowledgeCount = knowledgeBasePath.size

        // Both folders should have same number of files (5)
        if (documentsCount != knowledgeCount || vectorsCount != knowledgeCount) {
            return 2 // File count mismatch
        }

        // Both folders should have the same count
        if (documentsCount != vectorsCount) {
            return 3 // Count mismatch between folders
        }

        return 0
    }
}