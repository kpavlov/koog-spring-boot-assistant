package com.example.app.koog

import ai.koog.rag.base.DocumentStorage
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.AvailabilityState
import org.springframework.boot.availability.ReadinessState
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

@Component
internal class EmbeddingStoreReadinessIndicator(
    private val storage: DocumentStorage<Path>,
    @Value("\${ai.koog.rag.knowledge-base.path}")
    private val knowledgeBasePath: Path,
    availability: ApplicationAvailability,
) : ReadinessStateHealthIndicator(availability) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getState(applicationAvailability: ApplicationAvailability?): AvailabilityState =
        if (check()) {
            ReadinessState.ACCEPTING_TRAFFIC
        } else {
            ReadinessState.REFUSING_TRAFFIC
        }

    private fun check(): Boolean {
        // Count files in each directory
        logger.info("Running check!!!!!!")
        val knowledgeCount = knowledgeBasePath.listDirectoryEntries().count { it.toFile().isFile() }

        val documentsCount = runBlocking { storage.allDocuments().count() }

        // Storage should have all documents from knowledge base
        return (documentsCount == knowledgeCount)
    }
}