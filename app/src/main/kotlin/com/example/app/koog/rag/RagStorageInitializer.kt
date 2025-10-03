package com.example.app.koog.rag

import ai.koog.rag.base.RankedDocumentStorage
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class RagStorageInitializer(
    private val storage: RankedDocumentStorage<Path>,
    private val knowledgeBasePath: Path,
) {
    private val logger: Logger = LoggerFactory.getLogger(RagStorageInitializer::class.java)
    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val MAX_CONCURRENT_FILES = 3 // Adjust based on your needs
    }

    @PostConstruct
    fun init() {
        initScope.launch {
            try {
                initDocumentStorage()
                logger.info("RAG storage initialization completed successfully")
            } catch (e: Exception) {
                logger.error("Failed to initialize RAG storage", e)
            }
        }
    }

    @EventListener(ContextClosedEvent::class)
    fun cleanup() {
        initScope.cancel()
    }

    private suspend fun initDocumentStorage() {
        logger.info("Knowledge base path: ${knowledgeBasePath.toAbsolutePath()}")

        val files = knowledgeBasePath.toFile().listFiles()
        if (files == null || files.isEmpty()) {
            logger.warn("No files found in knowledge base: ${knowledgeBasePath.toAbsolutePath()}")
            return
        }

        logger.info("Processing ${files.size} files with max concurrency: $MAX_CONCURRENT_FILES")

        val semaphore = Semaphore(MAX_CONCURRENT_FILES)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        files
            .asFlow()
            .map { file ->
                semaphore.withPermit {
                    logger.info("Adding file: ${file.absolutePath}")
                    try {
                        storage.store(file.toPath())
                        successCount.incrementAndGet()
                        Result.success(file)
                    } catch (e: Exception) {
                        logger.error("Error adding file: ${file.absolutePath}", e)
                        failureCount.incrementAndGet()
                        Result.failure<java.io.File>(e)
                    }
                }
            }.collect()

        logger.info("File processing completed: ${successCount.get()} successful, ${failureCount.get()} failed")
    }
}