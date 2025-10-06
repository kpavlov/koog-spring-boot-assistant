package com.example.app.api

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import com.example.app.koog.strategy.createMermaidDiagram
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class KoogController(
    private val agentStrategy: AIAgentGraphStrategy<String, Any>,
) : KoogApi {
    private val logger = LoggerFactory.getLogger(KoogController::class.java)

    override suspend fun getStrategyGraph(): ResponseEntity<String> {
        logger.info("Received request for strategy graph")

        val mermaidDiagram = agentStrategy.createMermaidDiagram()

        return ResponseEntity.ok(mermaidDiagram)
    }
}