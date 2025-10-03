package com.example.app.koog

import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.executor.clients.LLMEmbeddingProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.rag.base.DocumentStorage
import ai.koog.rag.base.RankedDocumentStorage
import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
import ai.koog.rag.vector.InMemoryVectorStorage
import ai.koog.rag.vector.JVMFileVectorStorage
import ai.koog.rag.vector.JVMTextDocumentEmbedder
import ai.koog.rag.vector.VectorStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import kotlin.io.path.Path

@Configuration
class RagConfiguration {
    private val logger: Logger = LoggerFactory.getLogger(RagConfiguration::class.java)

    @Value("\${ai.koog.rag.embedding-store.path}")
    lateinit var embeddingStorePath: Path

    @Value("\${ai.koog.rag.knowledge-base.path}")
    lateinit var knowledgeBasePath: Path

    @Bean
    fun embedder(embeddingProvider: LLMEmbeddingProvider): LLMEmbedder =
        LLMEmbedder(embeddingProvider, OpenAIModels.Embeddings.TextEmbedding3Small)

    @Bean
    fun vectorStorage(): VectorStorage<Path> = JVMFileVectorStorage(Path("./data/knowledge"))

    @Bean
    fun rankedDocumentStorage(embedder: LLMEmbedder): RankedDocumentStorage<Path> {
        val documentEmbedder = JVMTextDocumentEmbedder(embedder)
        val storage = EmbeddingBasedDocumentStorage(documentEmbedder, InMemoryVectorStorage())

        initDocumentStorage(embeddingStorePath = embeddingStorePath, storage = storage)
        return storage
    }

    private fun initDocumentStorage(
        embeddingStorePath: Path,
        storage: DocumentStorage<Path>,
    ) {
        logger.info("Initializing ${storage::class.simpleName}: ${embeddingStorePath.toAbsolutePath()}")
        runBlocking(Dispatchers.IO) {
            knowledgeBasePath.toFile().listFiles()?.forEach { file ->
                logger.info("Adding file: ${file.absolutePath}")
                storage.store(file.toPath())
            }

            storage
                .allDocuments()
                .collect {
                    logger.info("Stored document: ${it.toAbsolutePath()}")
                }
        }
    }
}