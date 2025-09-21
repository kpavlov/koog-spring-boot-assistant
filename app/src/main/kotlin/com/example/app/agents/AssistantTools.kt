package com.example.app.agents

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

class AssistantTools : ToolSet {
    @Tool
    @LLMDescription("Returns stock price of given symbol as `[buy:sell]`, e.g. `[43.32:42.45]`")
    fun stockPrice(
        @LLMDescription("Stock symbol, e.g. APPL") symbol: String,
    ): String = "[43.32:42.45]"
}