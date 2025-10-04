package com.example.app.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig(
    private val chatWebSocketHandler: ChatWebSocketHandler,
) {
    @Bean
    fun webSocketHandlerMapping(): HandlerMapping {
        val map = mapOf("/ws/chat" to chatWebSocketHandler)
        val handlerMapping = SimpleUrlHandlerMapping(map, 1)

        // Configure CORS for WebSocket
        val corsConfiguration = CorsConfiguration()
        corsConfiguration.allowedOrigins = listOf("*", "http://localhost:3000")
        corsConfiguration.allowedMethods = listOf("*")
        corsConfiguration.allowedHeaders = listOf("*")
        corsConfiguration.allowCredentials = false

        handlerMapping.setCorsConfigurations(mapOf("/**" to corsConfiguration))

        return handlerMapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter()
}