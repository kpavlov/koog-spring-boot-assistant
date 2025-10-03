package com.example.app.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
internal class CorsConfig {
    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig =
            CorsConfiguration().apply {
                allowedOriginPatterns = listOf("*")
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
                allowCredentials = true
                maxAge = 3600L
            }

        val source =
            UrlBasedCorsConfigurationSource().apply {
                registerCorsConfiguration("/**", corsConfig)
            }

        return CorsWebFilter(source)
    }
}