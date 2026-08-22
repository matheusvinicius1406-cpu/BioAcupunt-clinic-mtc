package com.bioacupunt.clinic.data.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.bioacupunt.clinic.domain.model.VisionEngine
import com.bioacupunt.clinic.domain.model.VisionProviderType
import com.bioacupunt.clinic.domain.model.VisionRegion
import com.bioacupunt.clinic.domain.model.VisionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * MediaPipe-based VisionEngine adapter for tongue image analysis.
 *
 * Architecture:
 * - Uses MediaPipe Image Analysis for feature extraction
 * - Produces structured VisionResult with features and regions
 * - NEVER produces clinical interpretation (that's Knowledge Core's job)
 * - Output is always DRAFT — requires professional review
 *
 * When MediaPipe is unavailable, returns VisionResult.unavailable().
 * This adapter degrades gracefully — never crashes.
 */
class MediaPipeVisionAdapter(
    private val context: Context,
) : VisionEngine {

    override val providerType: VisionProviderType = VisionProviderType.LOCAL
    override val displayName: String = "MediaPipe Vision"

    // MediaPipe classifier would be initialized here in production
    // For now, we validate the pipeline structure and return unavailable
    // when the actual model isn't loaded

    private var isModelLoaded = false

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        // In production: check if MediaPipe model is loaded and GPU is available
        // For now: check if we can access the context
        try {
            context.cacheDir.exists()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun analyzeTongue(
        imageUri: String,
        imageBytes: ByteArray?,
    ): Result<VisionResult> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmap(imageUri, imageBytes)
                ?: return@withContext Result.success(VisionResult.unavailable())

            // Validate image quality
            val qualityCheck = validateImageQuality(bitmap)
            if (!qualityCheck.isValid) {
                return@withContext Result.success(
                    VisionResult(
                        features = emptyList(),
                        regions = emptyList(),
                        overallConfidence = 0.0,
                        modelName = displayName,
                        modelVersion = "1.0",
                        processingMetadata = mapOf(
                            "status" to "IMAGE_REJECTED",
                            "reason" to qualityCheck.reason,
                        ),
                    )
                )
            }

            // In production: run MediaPipe image analysis here
            // For now: return a structured result indicating processing capability
            val result = VisionResult(
                features = emptyList(), // Would be populated by MediaPipe
                regions = listOf(
                    VisionRegion(
                        name = "tongue_tip",
                        confidence = 0.0,
                    ),
                    VisionRegion(
                        name = "tongue_center",
                        confidence = 0.0,
                    ),
                    VisionRegion(
                        name = "tongue_root",
                        confidence = 0.0,
                    ),
                ),
                overallConfidence = 0.0,
                modelName = displayName,
                modelVersion = "1.0",
                processingMetadata = mapOf(
                    "status" to "MODEL_NOT_LOADED",
                    "image_width" to bitmap.width.toString(),
                    "image_height" to bitmap.height.toString(),
                    "quality_check" to "PASSED",
                ),
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.success(VisionResult.unavailable())
        }
    }

    override suspend fun analyzeImage(
        imageUri: String,
        imageBytes: ByteArray?,
        analysisType: String,
    ): Result<VisionResult> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmap(imageUri, imageBytes)
                ?: return@withContext Result.success(VisionResult.unavailable())

            val result = VisionResult(
                features = emptyList(),
                regions = emptyList(),
                overallConfidence = 0.0,
                modelName = displayName,
                modelVersion = "1.0",
                processingMetadata = mapOf(
                    "status" to "MODEL_NOT_LOADED",
                    "analysis_type" to analysisType,
                ),
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.success(VisionResult.unavailable())
        }
    }

    // --- Internal helpers ---

    private fun loadBitmap(imageUri: String, imageBytes: ByteArray?): Bitmap? {
        return try {
            if (imageBytes != null) {
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } else {
                val uri = Uri.parse(imageUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class ImageQualityResult(
        val isValid: Boolean,
        val reason: String = "",
    )

    private fun validateImageQuality(bitmap: Bitmap): ImageQualityResult {
        // Minimum size check
        if (bitmap.width < 224 || bitmap.height < 224) {
            return ImageQualityResult(false, "Image too small: ${bitmap.width}x${bitmap.height}")
        }

        // Aspect ratio check (tongue photos should be roughly square or portrait)
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        if (aspectRatio > 3.0 || aspectRatio < 0.3) {
            return ImageQualityResult(false, "Unusual aspect ratio: $aspectRatio")
        }

        return ImageQualityResult(true)
    }
}
