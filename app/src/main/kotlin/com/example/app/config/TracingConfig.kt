package com.example.app.config

import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TracingConfig {
    @Bean
    fun otelSpanExporter(): SpanExporter = OtlpGrpcSpanExporter.getDefault()

    @Bean
    fun loggingSpanExporter(): SpanExporter = LoggingSpanExporter.create()
}