package com.bioacupunt.ai.core

data class AiRequest(
    val prompt: String,
    val systemPrompt: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val context: Map<String, String> = emptyMap(),
    val requiredCapabilities: Set<AiCapability> = emptySet(),
    val preferLocal: Boolean = false,
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
