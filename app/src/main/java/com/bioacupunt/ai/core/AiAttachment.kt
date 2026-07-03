package com.bioacupunt.ai.core

data class AiAttachment(
    val type: AiInputType,
    val uri: String? = null,
    val content: ByteArray? = null,
    val mimeType: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
