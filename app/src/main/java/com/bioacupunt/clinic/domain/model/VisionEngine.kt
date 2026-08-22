package com.bioacupunt.clinic.domain.model

/**
 * Where the vision model runs.
 */
enum class VisionProviderType {
    LOCAL,
    DEVICE,
    CLOUD,
    HYBRID,
}

/**
 * A feature detected by the vision engine.
 */
data class VisionFeature(
    val name: String,
    val value: String,
    val confidence: Double,
    val region: String? = null,
)

/**
 * A region of interest detected in the image.
 */
data class VisionRegion(
    val name: String,
    val bounds: String? = null,
    val features: List<VisionFeature> = emptyList(),
    val confidence: Double = 0.0,
)

/**
 * Result from a vision engine analysis.
 *
 * Contains extracted features, detected regions, and confidence metadata.
 * Does NOT contain clinical interpretation — that comes from the Knowledge Core.
 */
data class VisionResult(
    val features: List<VisionFeature>,
    val regions: List<VisionRegion>,
    val overallConfidence: Double,
    val modelName: String,
    val modelVersion: String,
    val preprocessingVersion: String? = null,
    val inferenceVersion: String? = null,
    val processingMetadata: Map<String, String> = emptyMap(),
) {
    companion object {
        /** Vision result indicating no model was available */
        fun unavailable(): VisionResult = VisionResult(
            features = emptyList(),
            regions = emptyList(),
            overallConfidence = 0.0,
            modelName = "none",
            modelVersion = "0.0",
            processingMetadata = mapOf("status" to "MODEL_UNAVAILABLE"),
        )

        fun isUnavailable(result: VisionResult): Boolean =
            result.modelName == "none" || result.processingMetadata["status"] == "MODEL_UNAVAILABLE"
    }
}

/**
 * Abstraction over a vision analysis engine.
 *
 * Implementations may use MONAI, MediaPipe, ONNX, or any other framework.
 * The domain layer depends only on this interface, never on a specific engine.
 *
 * Critical rule: Vision output is DRAFT only.
 * Professional review is required before any observation is confirmed.
 */
interface VisionEngine {
    /** The type of provider (local, device, cloud, hybrid) */
    val providerType: VisionProviderType

    /** Human-readable name of the engine */
    val displayName: String

    /** Whether the engine is currently available (model loaded, permissions granted) */
    suspend fun isAvailable(): Boolean

    /**
     * Analyze a tongue image and extract features.
     *
     * @param imageUri App-private URI of the image
     * @param imageBytes Raw image bytes (optional, for engines that need raw input)
     * @return VisionResult with extracted features. Never returns null — returns unavailable() if no model.
     */
    suspend fun analyzeTongue(
        imageUri: String,
        imageBytes: ByteArray? = null,
    ): Result<VisionResult>

    /**
     * Analyze a generic medical image.
     */
    suspend fun analyzeImage(
        imageUri: String,
        imageBytes: ByteArray? = null,
        analysisType: String = "general",
    ): Result<VisionResult>
}

/**
 * Default no-op implementation when no vision model is available.
 * Used as fallback — never假装 ter um modelo real.
 */
class UnavailableVisionEngine : VisionEngine {
    override val providerType: VisionProviderType = VisionProviderType.LOCAL
    override val displayName: String = "Nenhum modelo disponível"

    override suspend fun isAvailable(): Boolean = false

    override suspend fun analyzeTongue(
        imageUri: String,
        imageBytes: ByteArray?,
    ): Result<VisionResult> = Result.success(VisionResult.unavailable())

    override suspend fun analyzeImage(
        imageUri: String,
        imageBytes: ByteArray?,
        analysisType: String,
    ): Result<VisionResult> = Result.success(VisionResult.unavailable())
}
