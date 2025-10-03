package com.example.app.koog

interface PromptTemplateProvider {
    fun getPromptTemplate(
        group: String,
        id: String,
        version: String = "latest",
    ): String
}