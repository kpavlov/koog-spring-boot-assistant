package com.example.app.koog.propmts

interface PromptTemplateProvider {
    fun getPromptTemplate(
        group: String,
        id: String,
        version: String = "latest",
    ): String
}