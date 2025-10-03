package com.example.app.koog

import kotlinx.io.IOException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

class JVMFilePromptTemplateProvider(
    val rootPath: Path,
    val extension: String = "md",
) : PromptTemplateProvider {
    private val logger: Logger = LoggerFactory.getLogger(JVMFilePromptTemplateProvider::class.qualifiedName)

    override fun getPromptTemplate(
        group: String,
        id: String,
        version: String,
    ): String {
        val templatePath =
            rootPath
                .resolve(group)
                .resolve(id)
                .resolve("$version.$extension")
        logger.debug(
            "Reading prompt template: group={}, id={}, version={}, path={}",
            group,
            id,
            version,
            templatePath,
        )
        try {
            return templatePath.toFile().readText(Charsets.UTF_8)
        } catch (e: IOException) {
            logger.error("Can't load prompt template {}: {}", templatePath.toAbsolutePath(), e.message, e)
            throw e
        }
    }
}