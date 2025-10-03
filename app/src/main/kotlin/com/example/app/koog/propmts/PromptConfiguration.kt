package com.example.app.koog.propmts

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class PromptConfiguration {
    @Value("\${ai.koog.prompt-template.path}")
    private lateinit var promptTemplatePath: Path

    @Bean
    fun promptProvider(): PromptTemplateProvider = JVMFilePromptTemplateProvider(rootPath = promptTemplatePath)
}