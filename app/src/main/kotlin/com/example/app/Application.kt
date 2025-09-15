package com.example.app

import ai.koog.spring.KoogAutoConfiguration
import com.example.app.agents.FinancialAgent
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(KoogAutoConfiguration::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}