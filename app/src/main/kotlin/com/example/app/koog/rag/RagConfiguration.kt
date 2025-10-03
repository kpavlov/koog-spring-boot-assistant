package com.example.app.koog.rag

import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.executor.clients.LLMEmbeddingProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.rag.base.RankedDocumentStorage
import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
import ai.koog.rag.vector.JVMFileVectorStorage
import ai.koog.rag.vector.JVMTextDocumentEmbedder
import ai.koog.rag.vector.VectorStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class RagConfiguration(
    @param:Value("\${ai.koog.rag.embedding-store.path}")
    private val embeddingStorePath: Path,
    @param:Value("\${ai.koog.rag.knowledge-base.path}")
    private val knowledgeBasePath: Path,
) {
    @Bean
    fun embedder(embeddingProvider: LLMEmbeddingProvider): LLMEmbedder =
        LLMEmbedder(embeddingProvider, OpenAIModels.Embeddings.TextEmbedding3Small)

    @Bean
    fun vectorStorage(): VectorStorage<Path> = JVMFileVectorStorage(embeddingStorePath)

    @Bean
    fun rankedDocumentStorage(
        embedder: LLMEmbedder,
        vectorStorage: VectorStorage<Path>,
    ): RankedDocumentStorage<Path> {
        val documentEmbedder = JVMTextDocumentEmbedder(embedder)
        return EmbeddingBasedDocumentStorage(documentEmbedder, vectorStorage)
    }

    @Bean
    fun ragStorageInitializer(storage: RankedDocumentStorage<Path>): RagStorageInitializer =
        RagStorageInitializer(
            storage = storage,
            knowledgeBasePath = knowledgeBasePath,
        )
}