package com.example.app.api

import com.example.app.agents.AgentConfiguration
import com.example.app.koog.strategy.createMermaidDiagram
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class KoogController(
    agentConfiguration: AgentConfiguration,
) : KoogApi {
    private val logger = LoggerFactory.getLogger(KoogController::class.java)

    private val agentStrategy = agentConfiguration.streamingAgentStrategy { }

    @Cacheable
    override suspend fun getStrategyGraph(): ResponseEntity<String> {
        logger.info("Received request for strategy graph")

        val mermaidDiagram = agentStrategy.createMermaidDiagram()

        return ResponseEntity.ok(mermaidDiagram)
    }
}