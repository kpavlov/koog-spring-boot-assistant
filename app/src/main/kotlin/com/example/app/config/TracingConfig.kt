package com.example.app.config

import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TracingConfig {
    @Bean
    fun otelSpanExporter(): SpanExporter = OtlpGrpcSpanExporter.getDefault()

    @Bean
    @ConditionalOnBooleanProperty("ai.koog.agents.tracing")
    fun loggingSpanExporter(): SpanExporter = LoggingSpanExporter.create()
}