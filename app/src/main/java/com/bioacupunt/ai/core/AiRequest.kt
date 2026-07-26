package com.bioacupunt.ai.core

data class AiRequest(
    val prompt: String,
    val systemPrompt: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val context: Map<String, String> = emptyMap(),
    val requiredCapabilities: Set<AiCapability> = emptySet(),
    val preferLocal: Boolean = false,
    /**
     * Lets a provider that supports it (currently only [com.bioacupunt.ai.data.provider
     * .CloudAiProvider], via Gemini's native Google Search grounding) search the live web
     * before answering. Only ever set by the administrative/free-chat fallback path — the
     * grounded RAG path (R2) never sets this, since it must never answer from anywhere but
     * the reviewed library. A provider without search support simply ignores this flag.
     */
    val allowWebSearch: Boolean = false,
    val preferredProviderId: String? = null,
    val modelId: String? = null,
    val taskHint: String? = null,
    val inputType: AiInputType = AiInputType.Text,
    val outputType: AiOutputType = AiOutputType.Text,
    val workflowHint: String? = null,
    val preferredAgent: String? = null,
    val preferredTool: String? = null,
    val attachments: List<AiAttachment> = emptyList(),
    val priority: AiTaskPriority = AiTaskPriority.Normal,
    val costConstraint: AiCostConstraint? = null,
    val latencyConstraint: AiLatencyConstraint? = null,
    val privacyRestriction: AiPrivacyRestriction = AiPrivacyRestriction.None
)
