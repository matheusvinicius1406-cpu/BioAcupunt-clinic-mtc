package com.bioacupunt.clinic.domain.model

/**
 * An anatomical region of the body.
 * Used for spatial organization in the Atlas UI.
 */
data class AnatomicalRegion(
    val id: String,
    val name: String,
    val parentRegionId: String? = null,
    val description: String = "",
    val bodySide: BodySide = BodySide.BILATERAL,
    val sourceIds: List<String> = emptyList(),
)

enum class BodySide(val label: String) {
    LEFT("Esquerdo"),
    RIGHT("Direito"),
    BILATERAL("Bilateral"),
    MIDLINE("Linha média"),
}

/**
 * An anatomical structure within a region.
 */
data class AnatomicalStructure(
    val id: String,
    val name: String,
    val type: String = "",
    val regionId: String,
    val description: String = "",
    val sourceIds: List<String> = emptyList(),
)

/**
 * A segment of a meridian connecting acupoints.
 */
data class MeridianSegment(
    val id: String,
    val meridianId: String,
    val meridianName: String,
    val order: Int,
    val startPointId: String? = null,
    val endPointId: String? = null,
    val regionId: String? = null,
    val description: String = "",
)

/**
 * Location of an acupoint on the body.
 * Coordinates are optional and prepared for future spatial features.
 */
data class AcupointLocation(
    val id: String,
    val pointCode: String,
    val pointName: String,
    val meridianId: String,
    val meridianName: String,
    /** 2D relative coordinates (for atlas UI) — x,y in 0.0–1.0 range */
    val x: Double? = null,
    val y: Double? = null,
    /** Depth for future 3D support */
    val z: Double? = null,
    val depthMm: Double? = null,
    /** Reference to anatomical structure */
    val anatomicalRegionId: String? = null,
    val anatomicalStructureId: String? = null,
    val description: String = "",
    val traditionalActions: List<String> = emptyList(),
    val indications: List<String> = emptyList(),
    val contraindications: List<String> = emptyList(),
    val sourceIds: List<String> = emptyList(),
    val evidenceIds: List<String> = emptyList(),
)

/**
 * A meridian in the MTC system.
 */
data class Meridian(
    val id: String,
    val name: String,
    val chineseName: String = "",
    val organ: String = "",
    val yinYang: String = "",
    val element: String = "",
    val bodySide: BodySide = BodySide.BILATERAL,
    val description: String = "",
    val sourceIds: List<String> = emptyList(),
)

/**
 * An atlas page/view configuration.
 */
enum class AtlasView(val label: String) {
    BODY("Corpo"),
    REGION("Região"),
    MERIDIAN("Meridiano"),
    ACUPOINT("Ponto"),
    DETAIL("Detalhe"),
}

/**
 * Navigation state for the Atlas UI.
 */
data class AtlasNavigation(
    val currentView: AtlasView = AtlasView.BODY,
    val selectedRegionId: String? = null,
    val selectedMeridianId: String? = null,
    val selectedAcupointId: String? = null,
)

/**
 * Knowledge-sourced information for an acupoint, used in Atlas detail view.
 */
data class AcupointDetail(
    val location: AcupointLocation,
    val meridian: Meridian? = null,
    val region: AnatomicalRegion? = null,
    val relatedStructures: List<AnatomicalStructure> = emptyList(),
    val evidenceCount: Int = 0,
    val sourceCount: Int = 0,
)
