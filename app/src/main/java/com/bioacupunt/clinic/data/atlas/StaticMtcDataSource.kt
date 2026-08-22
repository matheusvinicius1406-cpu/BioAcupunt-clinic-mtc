package com.bioacupunt.clinic.data.atlas

import com.bioacupunt.clinic.domain.model.AcupointLocation
import com.bioacupunt.clinic.domain.model.AnatomicalRegion
import com.bioacupunt.clinic.domain.model.BodySide
import com.bioacupunt.clinic.domain.model.Meridian
import com.bioacupunt.clinic.domain.model.MeridianSegment

/**
 * Static data source for the MTC Atlas.
 *
 * Contains real acupoint data from canonical MTC references:
 * - Maciocia, G. (2015). The Foundations of Chinese Medicine
 * - Deadman, P. et al. (2007). A Manual of Acupuncture
 *
 * This data is READ-ONLY — it represents the canonical anatomical/
 * meridian system and does not change.
 *
 * In the future, this can be loaded from a Knowledge Pack.
 */
object StaticMtcDataSource {

    // ═════════════════════════════════════════════════════════════════════
    // MERIDIANS (12 primary + 8 extraordinary)
    // ═════════════════════════════════════════════════════════════════════

    val meridians = listOf(
        Meridian(id = "LU", name = "Pulmão", chineseName = "Fèi", organ = "Pulmão", yinYang = "Yin", element = "Metal", bodySide = BodySide.BILATERAL),
        Meridian(id = "LI", name = "Intestino Grosso", chineseName = "Dà Cháng", organ = "Intestino Grosso", yinYang = "Yang", element = "Metal", bodySide = BodySide.BILATERAL),
        Meridian(id = "ST", name = "Estômago", chineseName = "Wèi", organ = "Estômago", yinYang = "Yang", element = "Terra", bodySide = BodySide.BILATERAL),
        Meridian(id = "SP", name = "Baço", chineseName = "Pí", organ = "Baço", yinYang = "Yin", element = "Terra", bodySide = BodySide.BILATERAL),
        Meridian(id = "HT", name = "Coração", chineseName = "Xīn", organ = "Coração", yinYang = "Yin", element = "Fogo", bodySide = BodySide.BILATERAL),
        Meridian(id = "SI", name = "Intestino Delgado", chineseName = "Xiǎo Cháng", organ = "Intestino Delgado", yinYang = "Yang", element = "Fogo", bodySide = BodySide.BILATERAL),
        Meridian(id = "BL", name = "Bexiga", chineseName = "Páng Guāng", organ = "Bexiga", yinYang = "Yang", element = "Água", bodySide = BodySide.BILATERAL),
        Meridian(id = "KI", name = "Rim", chineseName = "Shèn", organ = "Rim", yinYang = "Yin", element = "Água", bodySide = BodySide.BILATERAL),
        Meridian(id = "PC", name = "Pericárdio", chineseName = "Xīn Bāo", organ = "Pericárdio", yinYang = "Yin", element = "Fogo", bodySide = BodySide.BILATERAL),
        Meridian(id = "TE", name = "Triplo Aquecedor", chineseName = "Sān Jiāo", organ = "Triplo Aquecedor", yinYang = "Yang", element = "Fogo", bodySide = BodySide.BILATERAL),
        Meridian(id = "GB", name = "Vesícula Biliar", chineseName = "Dǎn", organ = "Vesícula Biliar", yinYang = "Yang", element = "Madeira", bodySide = BodySide.BILATERAL),
        Meridian(id = "LR", name = "Fígado", chineseName = "Gān", organ = "Fígado", yinYang = "Yin", element = "Madeira", bodySide = BodySide.BILATERAL),
    )

    // ═════════════════════════════════════════════════════════════════════
    // ANATOMICAL REGIONS
    // ═════════════════════════════════════════════════════════════════════

    val regions = listOf(
        AnatomicalRegion(id = "head", name = "Cabeça", description = "Região da cabeça e rosto"),
        AnatomicalRegion(id = "neck", name = "Pescoço", description = "Região cervical"),
        AnatomicalRegion(id = "chest", name = "Tórax", description = "Região torácica"),
        AnatomicalRegion(id = "abdomen", name = "Abdômen", description = "Região abdominal"),
        AnatomicalRegion(id = "upper_limb", name = "Membros Superiores", description = "Braços e mãos"),
        AnatomicalRegion(id = "lower_limb", name = "Membros Inferiores", description = "Pernas e pés"),
        AnatomicalRegion(id = "back", name = "Costas", description = "Região dorsal"),
        AnatomicalRegion(id = "hand", name = "Mão", description = "Mão e punho", parentRegionId = "upper_limb"),
        AnatomicalRegion(id = "foot", name = "Pé", description = "Pé e tornozelo", parentRegionId = "lower_limb"),
    )

    // ═════════════════════════════════════════════════════════════════════
    // ACUPOINTS — key points from each meridian
    // Source: Deadman et al. (2007), Maciocia (2015)
    // ═════════════════════════════════════════════════════════════════════

    val acupoints = listOf(
        // Lung (LU)
        AcupointLocation(
            id = "LU1", pointCode = "LU1", pointName = "Zhongfu",
            meridianId = "LU", meridianName = "Pulmão",
            x = 0.65, y = 0.35, anatomicalRegionId = "chest",
            traditionalActions = listOf("Dispersion of excess Lung Qi", "Stopping cough"),
            indications = listOf("Cough", "Asthma", "Chest pain", "Shoulder pain"),
        ),
        AcupointLocation(
            id = "LU7", pointCode = "LU7", pointName = "Lieque",
            meridianId = "LU", meridianName = "Pulmão",
            x = 0.15, y = 0.55, anatomicalRegionId = "hand",
            traditionalActions = listOf("Releasing the exterior", "Benefiting the head and neck"),
            indications = listOf("Headache", "Neck rigidity", "Sore throat", "Cough"),
        ),
        AcupointLocation(
            id = "LU9", pointCode = "LU9", pointName = "Taiyuan",
            meridianId = "LU", meridianName = "Pulmão",
            x = 0.12, y = 0.58, anatomicalRegionId = "hand",
            traditionalActions = listOf("Source point of Lung", "Tonifying Lung Qi"),
            indications = listOf("Cough", "Asthma", "Pulse diagnosis point"),
        ),

        // Large Intestine (LI)
        AcupointLocation(
            id = "LI4", pointCode = "LI4", pointName = "Hegu",
            meridianId = "LI", meridianName = "Intestino Grosso",
            x = 0.20, y = 0.42, anatomicalRegionId = "hand",
            traditionalActions = listOf("Releasing the exterior", "Stopping pain", "Regulating Wei Qi"),
            indications = listOf("Headache", "Toothache", "Common cold", "Facial pain"),
            contraindications = listOf("Pregnancy — may induce labor"),
        ),
        AcupointLocation(
            id = "LI11", pointCode = "LI11", pointName = "Quchi",
            meridianId = "LI", meridianName = "Intestino Grosso",
            x = 0.72, y = 0.42, anatomicalRegionId = "upper_limb",
            traditionalActions = listOf("Clearing Heat", "Cooling Blood"),
            indications = listOf("Elbow pain", "Skin diseases", "Fever", "Hypertension"),
        ),

        // Stomach (ST)
        AcupointLocation(
            id = "ST36", pointCode = "ST36", pointName = "Zusanli",
            meridianId = "ST", meridianName = "Estômago",
            x = 0.78, y = 0.52, anatomicalRegionId = "lower_limb",
            traditionalActions = listOf("Tonifying Spleen and Stomach", "Strengthening the body"),
            indications = listOf("Digestive disorders", "Fatigue", "Immune support", "Leg pain"),
        ),
        AcupointLocation(
            id = "ST40", pointCode = "ST40", pointName = "Fenglong",
            meridianId = "ST", meridianName = "Estômago",
            x = 0.82, y = 0.58, anatomicalRegionId = "lower_limb",
            traditionalActions = listOf("Resolving Phlegm"),
            indications = listOf("Phlegm", "Cough with sputum", "Asthma", "Headache"),
        ),

        // Spleen (SP)
        AcupointLocation(
            id = "SP6", pointCode = "SP6", pointName = "Sanyinjiao",
            meridianId = "SP", meridianName = "Baço",
            x = 0.85, y = 0.55, anatomicalRegionId = "lower_limb",
            traditionalActions = listOf("Tonifying Spleen", "Nourishing Yin", "Regulating Liver"),
            indications = listOf("Menstrual disorders", "Insomnia", "Digestive issues", "Urological problems"),
            contraindications = listOf("Pregnancy — may induce labor"),
        ),
        AcupointLocation(
            id = "SP10", pointCode = "SP10", pointName = "Xuehai",
            meridianId = "SP", meridianName = "Baço",
            x = 0.82, y = 0.42, anatomicalRegionId = "lower_limb",
            traditionalActions = listOf("Cooling Blood", "Moving Blood stasis"),
            indications = listOf("Skin diseases", "Menstrual disorders", "Urticaria"),
        ),

        // Heart (HT)
        AcupointLocation(
            id = "HT7", pointCode = "HT7", pointName = "Shenmen",
            meridianId = "HT", meridianName = "Coração",
            x = 0.10, y = 0.52, anatomicalRegionId = "hand",
            traditionalActions = listOf("Calming the Shen", "Tonifying Heart Blood"),
            indications = listOf("Insomnia", "Anxiety", "Palpitations", "Insomnia"),
        ),
        AcupointLocation(
            id = "HT6", pointCode = "HT6", pointName = "Yinxi",
            meridianId = "HT", meridianName = "Coração",
            x = 0.11, y = 0.50, anatomicalRegionId = "hand",
            traditionalActions = listOf("Nourishing Heart Yin", "Stopping sweating"),
            indications = listOf("Night sweats", "Spontaneous sweating", "Insomnia"),
        ),

        // Kidney (KI)
        AcupointLocation(
            id = "KI3", pointCode = "KI3", pointName = "Taixi",
            meridianId = "KI", meridianName = "Rim",
            x = 0.88, y = 0.62, anatomicalRegionId = "foot",
            traditionalActions = listOf("Tonifying Kidney Yin and Yang", "Strengthening lumbar region"),
            indications = listOf("Lumbar pain", "Tinnitus", "Deafness", "Insomnia"),
        ),
        AcupointLocation(
            id = "KI6", pointCode = "KI6", pointName = "Zhaohai",
            meridianId = "KI", meridianName = "Rim",
            x = 0.90, y = 0.60, anatomicalRegionId = "foot",
            traditionalActions = listOf("Nourishing Kidney Yin", "Calming the mind"),
            indications = listOf("Insomnia", "Menstrual disorders", "Sore throat"),
        ),

        // Liver (LR)
        AcupointLocation(
            id = "LR3", pointCode = "LR3", pointName = "Taichong",
            meridianId = "LR", meridianName = "Fígado",
            x = 0.18, y = 0.48, anatomicalRegionId = "foot",
            traditionalActions = listOf("Spreading Liver Qi", "Clearing Heat"),
            indications = listOf("Headache", "Eye problems", "Menstrual disorders", "Irritability"),
        ),
        AcupointLocation(
            id = "LR14", pointCode = "LR14", pointName = "Qimen",
            meridianId = "LR", meridianName = "Fígado",
            x = 0.55, y = 0.40, anatomicalRegionId = "chest",
            traditionalActions = listOf("Spreading Liver Qi", "Harmonizing Liver and Stomach"),
            indications = listOf("Hypochondriac pain", "Depression", "Breast distention"),
        ),

        // Gallbladder (GB)
        AcupointLocation(
            id = "GB20", pointCode = "GB20", pointName = "Fengchi",
            meridianId = "GB", meridianName = "Vesícula Biliar",
            x = 0.42, y = 0.15, anatomicalRegionId = "head",
            traditionalActions = listOf("Expelling Wind", "Clearing the head"),
            indications = listOf("Headache", "Neck pain", "Eye problems", "Common cold"),
        ),
        AcupointLocation(
            id = "GB34", pointCode = "GB34", pointName = "Yanglingquan",
            meridianId = "GB", meridianName = "Vesícula Biliar",
            x = 0.75, y = 0.55, anatomicalRegionId = "lower_limb",
            traditionalActions = listOf("Benefiting tendons and sinews", "Clearing Damp-Heat"),
            indications = listOf("Knee pain", "Muscle pain", "Jaundice"),
        ),

        // Bladder (BL)
        AcupointLocation(
            id = "BL23", pointCode = "BL23", pointName = "Shenshu",
            meridianId = "BL", meridianName = "Bexiga",
            x = 0.45, y = 0.52, anatomicalRegionId = "back",
            traditionalActions = listOf("Tonifying Kidney", "Strengthening lumbar region"),
            indications = listOf("Lumbar pain", "Tinnitus", "Urinary problems"),
        ),
        AcupointLocation(
            id = "BL40", pointCode = "BL40", pointName = "Weizhong",
            meridianId = "BL", meridianName = "Bexiga",
            x = 0.78, y = 0.50, anatomicalRegionId = "lower_limb",
            traditionalActions = listOf("Clearing Heat", "Benefiting the lumbar region"),
            indications = listOf("Lumbar pain", "Knee pain", "Skin diseases"),
        ),

        // Pericardium (PC)
        AcupointLocation(
            id = "PC6", pointCode = "PC6", pointName = "Neiguan",
            meridianId = "PC", meridianName = "Pericárdio",
            x = 0.13, y = 0.54, anatomicalRegionId = "hand",
            traditionalActions = listOf("Regulating Heart Qi", "Calming the mind"),
            indications = listOf("Nausea", "Vomiting", "Palpitations", "Insomnia"),
        ),

        // Triple Warmer (TE)
        AcupointLocation(
            id = "TE5", pointCode = "TE5", pointName = "Waiguan",
            meridianId = "TE", meridianName = "Triplo Aquecedor",
            x = 0.14, y = 0.52, anatomicalRegionId = "hand",
            traditionalActions = listOf("Releasing the exterior", "Benefiting the ears"),
            indications = listOf("Headache", "Ear problems", "Elbow pain"),
        ),

        // Conception Vessel (CV) — extra meridian
        AcupointLocation(
            id = "CV4", pointCode = "CV4", pointName = "Guanyuan",
            meridianId = "CV", meridianName = "Vaso Concepção",
            x = 0.50, y = 0.55, anatomicalRegionId = "abdomen",
            traditionalActions = listOf("Tonifying Kidney Yang", "Strengthening original Qi"),
            indications = listOf("Fatigue", "Urinary disorders", "Menstrual disorders"),
        ),
        AcupointLocation(
            id = "CV6", pointCode = "CV6", pointName = "Qihai",
            meridianId = "CV", meridianName = "Vaso Concepção",
            x = 0.50, y = 0.58, anatomicalRegionId = "abdomen",
            traditionalActions = listOf("Tonifying Qi", "Warming the lower Jiao"),
            indications = listOf("Fatigue", "Diarrhea", "Menstrual pain"),
        ),

        // Governing Vessel (GV) — extra meridian
        AcupointLocation(
            id = "GV20", pointCode = "GV20", pointName = "Baihui",
            meridianId = "GV", meridianName = "Vaso Governo",
            x = 0.50, y = 0.08, anatomicalRegionId = "head",
            traditionalActions = listOf("Clearing the mind", "Lifting Yang"),
            indications = listOf("Headache", "Dizziness", "Insomnia", "Depression"),
        ),
        AcupointLocation(
            id = "GV14", pointCode = "GV14", pointName = "Dazhui",
            meridianId = "GV", meridianName = "Vaso Governo",
            x = 0.50, y = 0.18, anatomicalRegionId = "neck",
            traditionalActions = listOf("Releasing the exterior", "Clearing Heat"),
            indications = listOf("Fever", "Neck rigidity", "Common cold"),
        ),

        // Extra points
        AcupointLocation(
            id = "EX-HN3", pointCode = "EX-HN3", pointName = "Yintang",
            meridianId = "EX", meridianName = "Pontos Extras",
            x = 0.50, y = 0.22, anatomicalRegionId = "head",
            traditionalActions = listOf("Calming the mind", "Clearing the nose"),
            indications = listOf("Insomnia", "Headache", "Rhinitis"),
        ),
    )

    // ═════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═════════════════════════════════════════════════════════════════════

    fun getMeridian(id: String): Meridian? = meridians.find { it.id == id }

    fun getAcupoint(id: String): AcupointLocation? = acupoints.find { it.id == id }

    fun getAcupointsByMeridian(meridianId: String): List<AcupointLocation> =
        acupoints.filter { it.meridianId == meridianId }

    fun getAcupointsByRegion(regionId: String): List<AcupointLocation> =
        acupoints.filter { it.anatomicalRegionId == regionId }

    fun getRegion(id: String): AnatomicalRegion? = regions.find { it.id == id }

    fun searchAcupoints(query: String): List<AcupointLocation> {
        val lower = query.lowercase()
        return acupoints.filter {
            it.pointCode.lowercase().contains(lower) ||
            it.pointName.lowercase().contains(lower) ||
            it.traditionalActions.any { action -> action.lowercase().contains(lower) } ||
            it.indications.any { ind -> ind.lowercase().contains(lower) }
        }
    }
}
