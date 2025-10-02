package com.example.app.koog

import ai.koog.agents.snapshot.providers.PersistenceStorageProvider
import ai.koog.agents.snapshot.providers.file.JVMFilePersistenceStorageProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class PersistenceConfiguration {
    @Value("\${ai.koog.chat-memory.session-store.path}")
    private lateinit var sessionsStorePath: Path

    @Bean
    fun persistenceStorageProvider(): PersistenceStorageProvider =
        JVMFilePersistenceStorageProvider(
            root = sessionsStorePath,
        )
}