package com.example.app.koog

import ai.koog.rag.vector.JVMFileDocumentEmbeddingStorage
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

suspend fun JVMFileDocumentEmbeddingStorage.storeAll(rootDir: java.nio.file.Path) {
    val self = this
    rootDir.listDirectoryEntries().forEach {
        if (it.isRegularFile() && it.isReadable()) {
            self.store(it)
        }
    }
}