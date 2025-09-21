package com.example.app

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

typealias SessionId = String

object Generators {
    @OptIn(ExperimentalUuidApi::class)
    internal fun randomSessionId(): String = Uuid.random().toHexString()
}