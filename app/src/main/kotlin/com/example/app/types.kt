package com.example.app

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
//
// @JvmInline
// @Serializable
// value class SessionId(
//    val value: String,
// ) {
//    companion object {
//        @OptIn(ExperimentalUuidApi::class)
//        @JvmStatic
//        fun generate(): SessionId = SessionId(Uuid.random().toHexString())
//    }
// }

typealias SessionId = String

@OptIn(ExperimentalUuidApi::class)
fun randomSessionId(): String = Uuid.random().toHexString()