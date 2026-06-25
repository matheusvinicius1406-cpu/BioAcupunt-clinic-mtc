package com.example.data.clinicalcore

import com.example.data.local.MtcProntuaryEntity

// ==========================================
// 🧬 2. DOMAIN MODELS (ENTIDADES PRINCIPAIS)
// ==========================================

data class PatientDomain(
    val id: String,
    val name: String,
    val age: Int,
    val gender: String,
    val mainComplaint: String,
    val createdAt: Long
)

data class ClinicalRecordEntity(
    val id: String,
    val patientId: String,

    // sintomas estruturados
    val symptoms: List<SymptomEntry>,

    // avaliação MTC
    val tongue: TongueAssessment,
    val pulse: PulseAssessment,
    val shen: ShenAssessment,

    val zangFu: ZangFuState,
    val fiveElements: FiveElementsState,
    val meridians: List<MeridianState>,

    // saída do engine
    val primaryPattern: MTCPattern?,
    val secondaryPatterns: List<MTCPattern>,

    val createdAt: Long,
    val updatedAt: Long
)

data class SymptomEntry(
    val name: String,
    val intensity: Int, // 0–10
    val frequency: String,
    val duration: String,
    val trigger: String,
    val impact: String
)

// ==========================================
// 🧬 3. AVALIAÇÕES MTC ESTRUTURADAS
// ==========================================

data class TongueAssessment(
    val bodyColor: String,
    val coatingColor: String,
    val coatingThickness: String,
    val moisture: String,
    val cracks: String,
    val teethMarks: Boolean,
    val regions: Map<String, String>
)

data class PulseAssessment(
    val depth: String,
    val strength: String,
    val rhythm: String,
    val speed: String,
    val symmetry: String
)

data class ShenAssessment(
    val emotionalStability: Int, // 0-10
    val anxietyLevel: Int, // 0-10
    val depressionLevel: Int, // 0-10
    val mentalClarity: Int, // 0-10
    val vitality: Int // 0-10
)

data class ZangFuState(
    val heart: OrganState,
    val liver: OrganState,
    val spleen: OrganState,
    val lung: OrganState,
    val kidney: OrganState
)

data class OrganState(
    val deficiency: Int, // 0-10
    val excess: Int, // 0-10
    val heat: Int, // 0-10
    val cold: Int, // 0-10
    val stagnation: Int // 0-10
)

data class FiveElementsState(
    val wood: Int, // 0-100
    val fire: Int,
    val earth: Int,
    val metal: Int,
    val water: Int
)

data class MeridianState(
    val name: String,
    val hasPain: Boolean,
    val condition: String // "Excesso", "Deficiência", "Bloqueio", "Equilibrado"
)

data class StudyTopic(val title: String, val description: String)
data class Flashcard(val front: String, val back: String)
data class SimilarCase(val patientAgeGender: String, val complaint: String, val treatmentSuccess: String)

data class MTCPattern(
    val name: String,
    val affectedOrgans: List<String>,
    val symptoms: List<String>,
    val confidence: Float, // 0.0 to 1.0
    val therapeuticPrinciple: String
)

data class MTCAnalysisResult(
    val primaryPattern: MTCPattern?,
    val secondaryPatterns: List<MTCPattern>,
    val studyTopics: List<StudyTopic> = emptyList(),
    val automaticFlashcards: List<Flashcard> = emptyList(),
    val similarCases: List<SimilarCase> = emptyList()
)

// ==========================================
// 🧠 4. MTC ENGINE (CÉREBRO DO SISTEMA)
// ==========================================

class MTCClinicalEngine {

    fun analyze(record: ClinicalRecordEntity): MTCAnalysisResult {
        val patterns = mutableListOf<MTCPattern>()

        patterns += detectQiDeficiency(record)
        patterns += detectYangDeficiency(record)
        patterns += detectBloodStagnation(record)
        patterns += detectDampness(record)
        patterns += detectYinDeficiency(record)
        patterns += detectLiverQiStagnation(record)

        val primary = patterns.maxByOrNull { it.confidence }

        val primaryName = primary?.name ?: "Equilíbrio Dinâmico Geral"
        val topics = mutableListOf<StudyTopic>()
        val flashcards = mutableListOf<Flashcard>()
        val cases = mutableListOf<SimilarCase>()

        when (primaryName) {
            "Deficiência de Qi (Qi Xu)" -> {
                topics.add(StudyTopic("A Fisiologia do Qi do Baço", "Estudo sobre como o Baço (Pi) extrai o Gu Qi dos alimentos e o transforma em energia vital em conjunto com o Pulmão."))
                topics.add(StudyTopic("Moxabustão no Ponto E36 (Zusanli)", "Uso de calor terapêutico para fortalecer o Qi do Baço e aumentar a imunidade sistêmica."))
                
                flashcards.add(Flashcard("Qual o principal ponto para tonificar o Qi do Baço?", "E36 (Zusanli)."))
                flashcards.add(Flashcard("Quais as características da língua na Deficiência de Qi do Baço?", "Língua pálida, com marcas dentárias nas bordas e saburra fina e branca."))
                
                cases.add(SimilarCase("Mulher, 42 anos", "Fadiga extrema, digestão lenta e fezes amolecidas. Tratado com tonificação de E36, BP6 e VC12.", "Recuperação de 80% da vitalidade após 5 sessões."))
                cases.add(SimilarCase("Homem, 35 anos", "Cansaço muscular crônico e distensão abdominal. Tratado com acupuntura sistêmica e moxabustão.", "Melhora de energia em 4 sessões."))
            }
            "Deficiência de Yang (Yang Xu)" -> {
                topics.add(StudyTopic("O Fogo do Portão da Vida (Mingmen)", "A importância do calor primordial do Rim Yang para aquecer todos os órgãos e manter o metabolismo ativo."))
                topics.add(StudyTopic("Diferença entre Deficiência de Yin e Yang do Rim", "Estudo comparativo focado em aversão ao frio versus calor noturno e a conduta de aquecimento."))

                flashcards.add(Flashcard("Qual ponto aquece o Yang do Rim e o Mingmen?", "VG4 (Mingmen) e B23 (Shenshu)."))
                flashcards.add(Flashcard("Qual a língua típica na Deficiência de Yang?", "Pálida, inchada e muito úmida/escorregadia."))

                cases.add(SimilarCase("Homem, 58 anos", "Dor lombar fria, aversão ao frio e urina abundante clara. Tratado com moxa em VG4, B23 e VC4.", "Eliminação da dor lombar e sensação de frio constante em 6 sessões."))
            }
            "Estagnação de Sangue (Xue Yu)" -> {
                topics.add(StudyTopic("Teoria de Estase de Sangue na MTC", "Diferenciação entre estagnação de Qi (funcional) e estase de Sangue (física/localizada)."))
                topics.add(StudyTopic("Mecanismo do Ponto BP10 (Xuehai)", "Como o 'Mar de Sangue' atua na ativação da circulação e eliminação de coágulos e dores fixas."))

                flashcards.add(Flashcard("Quais pontos formam a combinação 'Quatro Portões' para mover o Qi?", "F3 (Taichong) e IG4 (Hegu)."))
                flashcards.add(Flashcard("Qual a principal característica da dor por Estase de Sangue?", "Dor em pontada, fixa e localizada, geralmente piorando à noite."))

                cases.add(SimilarCase("Mulher, 31 anos", "Dismenorreia severa com sangue escuro e coágulos. Tratado com BP10, F3, IG4 e B17 em dispersão.", "Redução de 90% das cólicas menstruais no ciclo seguinte."))
            }
            "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> {
                topics.add(StudyTopic("A Origem da Umidade na MTC", "Como a falha no transporte de líquidos pelo Baço gera acúmulo de umidade e fleuma nos tecidos."))
                topics.add(StudyTopic("Estudo do Ponto E40 (Fenglong)", "Análise do principal ponto de transformação de fleuma visível e invisível."))

                flashcards.add(Flashcard("Qual o ponto mestre para drenar umidade no Aquecedor Inferior?", "BP9 (Yinlingquan)."))
                flashcards.add(Flashcard("Qual saburra lingual indica acúmulo de Umidade?", "Saburra espessa e pegajosa (branca ou amarela)."))

                cases.add(SimilarCase("Homem, 50 anos", "Sensação de peso na cabeça, edemas nos tornozelos e muco. Tratado com E40, BP9 e BP6.", "Redução expressiva do inchaço em 5 sessões."))
            }
            "Deficiência de Yin (Yin Xu)" -> {
                topics.add(StudyTopic("A Nutrição do Yin e a Água do Rim", "Como repouso adequado, meditação e fitoterapia reabastecem a base hídrica vital."))
                topics.add(StudyTopic("O Calor Vazio e o Efeito no Shen", "Análise de como a falta de Yin permite a ascensão de fogo fictício, causando insônia e ansiedade."))

                flashcards.add(Flashcard("Qual ponto nutre o Yin do Rim e abre o Yin Qiao Mai?", "R6 (Zhaohai) e R3 (Taixi)."))
                flashcards.add(Flashcard("O que indica uma língua vermelha e sem saburra?", "Deficiência severa de Yin com Calor Vazio."))

                cases.add(SimilarCase("Mulher, 48 anos", "Insônia severa, suores noturnos e agitação mental. Tratada com R3, R6, C7 e BP6.", "Sono regular restabelecido em 7 sessões."))
            }
            "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> {
                topics.add(StudyTopic("A Psicossomática do Fígado na MTC", "Estudo da relação entre estresse, raiva contida, frustração e o bloqueio físico do fluxo de Qi."))
                topics.add(StudyTopic("Uso de Aromaterapia e Acupuntura Integradas", "Benefícios do óleo essencial de lavanda associado ao agulhamento do ponto F3 (Taichong)."))

                flashcards.add(Flashcard("Qual o principal ponto do canal do Fígado para estresse?", "F3 (Taichong)."))
                flashcards.add(Flashcard("Qual pulso é patognomônico de estagnação de Qi do Fígado?", "Pulso em corda (Xian)."))

                cases.add(SimilarCase("Mulher, 38 anos", "Cefaleia tensional lateral, estresse severo e TPM intensa. Tratada com F3, VB34, PC6 e IG4.", "Resolução completa das crises de cefaleia em 4 sessões."))
            }
            else -> {
                topics.add(StudyTopic("Harmonização Preventiva de Meridianos", "A manutenção da saúde através do estímulo regular dos pontos fundamentais para longevidade."))
                flashcards.add(Flashcard("Por que o ponto E36 é chamado de ponto da longevidade?", "Porque estimula fortemente o Qi original, a imunidade e retarda o envelhecimento orgânico."))
                cases.add(SimilarCase("Homem, 45 anos", "Check-up energético preventivo para fadiga leve de trabalho.", "Aumento do vigor físico em 3 sessões."))
            }
        }

        return MTCAnalysisResult(
            primaryPattern = primary,
            secondaryPatterns = if (primary != null) patterns - primary else patterns,
            studyTopics = topics,
            automaticFlashcards = flashcards,
            similarCases = cases
        )
    }

    // --- 1. Deficiência de Qi ---
    private fun detectQiDeficiency(record: ClinicalRecordEntity): MTCPattern {
        var score = 0f
        val symptomsFound = mutableListOf<String>()

        // Check symptoms
        record.symptoms.forEach { s ->
            val name = s.name.lowercase()
            if (name.contains("fadiga") || name.contains("cansaço") || name.contains("fraqueza")) {
                score += 0.3f
                symptomsFound.add("Fadiga/Cansaço relatados")
            }
            if (name.contains("falta de ar") || name.contains("respiração fraca")) {
                score += 0.2f
                symptomsFound.add("Falta de ar / Respiração fraca")
            }
        }

        // Check tongue
        if (record.tongue.bodyColor.lowercase().contains("pálida") || record.tongue.bodyColor.lowercase().contains("palida")) {
            score += 0.25f
            symptomsFound.add("Língua Pálida")
        }
        if (record.tongue.teethMarks) {
            score += 0.2f
            symptomsFound.add("Marcas Dentárias na Língua")
        }

        // Check pulse
        if (record.pulse.strength.lowercase().contains("frac") || record.pulse.depth.lowercase().contains("profund")) {
            score += 0.2f
            symptomsFound.add("Pulso Fraco/Profundo")
        }

        // Check ZangFu
        if (record.zangFu.spleen.deficiency > 4 || record.zangFu.lung.deficiency > 4) {
            score += 0.3f
            symptomsFound.add("Deficiência nos sistemas de Zang-Fu (Baço/Pulmão)")
        }

        val finalConfidence = score.coerceIn(0f, 1f)

        return MTCPattern(
            name = "Deficiência de Qi (Qi Xu)",
            affectedOrgans = listOf("Baço (Pi)", "Pulmão (Fei)"),
            symptoms = symptomsFound.distinct(),
            confidence = finalConfidence,
            therapeuticPrinciple = "Tonificar o Qi sistêmico, fortalecer o Baço (Pi) e impulsionar a energia defensiva (Wei Qi)."
        )
    }

    // --- 2. Deficiência de Yang ---
    private fun detectYangDeficiency(record: ClinicalRecordEntity): MTCPattern {
        var score = 0f
        val symptomsFound = mutableListOf<String>()

        record.symptoms.forEach { s ->
            val name = s.name.lowercase()
            if (name.contains("frio") || name.contains("membros frios") || name.contains("gelado")) {
                score += 0.35f
                symptomsFound.add("Aversão ao Frio / Extremidades Geladas")
            }
            if (name.contains("lombar") && name.contains("fria")) {
                score += 0.25f
                symptomsFound.add("Região Lombar Fria")
            }
        }

        if (record.tongue.bodyColor.lowercase().contains("pálida") || record.tongue.bodyColor.lowercase().contains("palida")) {
            score += 0.2f
            symptomsFound.add("Língua Pálida")
        }
        if (record.tongue.moisture.lowercase().contains("úmida") || record.tongue.moisture.lowercase().contains("escorregadia")) {
            score += 0.2f
            symptomsFound.add("Língua Muito Úmida/Escorregadia")
        }

        if (record.pulse.speed.lowercase().contains("lent") || record.pulse.depth.lowercase().contains("profund")) {
            score += 0.2f
            symptomsFound.add("Pulso Lento e Profundo")
        }

        if (record.zangFu.kidney.cold > 4 || record.zangFu.spleen.cold > 4) {
            score += 0.25f
            symptomsFound.add("Frio interno acumulado nos Rins/Baço")
        }

        val finalConfidence = score.coerceIn(0f, 1f)

        return MTCPattern(
            name = "Deficiência de Yang (Yang Xu)",
            affectedOrgans = listOf("Rim (Shen)", "Baço (Pi)"),
            symptoms = symptomsFound.distinct(),
            confidence = finalConfidence,
            therapeuticPrinciple = "Aquecer o Yang original, dispersar o Frio Interno acumulado e reativar a chama do Portão da Vida (Mingmen)."
        )
    }

    // --- 3. Estagnação de Sangue / Estase ---
    private fun detectBloodStagnation(record: ClinicalRecordEntity): MTCPattern {
        var score = 0f
        val symptomsFound = mutableListOf<String>()

        record.symptoms.forEach { s ->
            val name = s.name.lowercase()
            if (name.contains("dor fixa") || name.contains("dor em facada") || s.impact.lowercase().contains("intensa")) {
                score += 0.3f
                symptomsFound.add("Dor Localizada, Fixa e em Facada")
            }
        }

        if (record.tongue.bodyColor.lowercase().contains("púrpura") || record.tongue.bodyColor.lowercase().contains("roxa") || record.tongue.bodyColor.lowercase().contains("azulada")) {
            score += 0.4f
            symptomsFound.add("Língua Púrpura ou Roxa")
        }
        if (record.tongue.regions.getOrDefault("Bordas", "").lowercase().contains("estase") || record.tongue.regions.getOrDefault("Bordas", "").lowercase().contains("roxa")) {
            score += 0.25f
            symptomsFound.add("Bordas Linguais Roxas com Pontos de Estase")
        }

        if (record.pulse.rhythm.lowercase().contains("irregular") || record.pulse.strength.lowercase().contains("tens") || record.pulse.strength.lowercase().contains("corda")) {
            score += 0.2f
            symptomsFound.add("Pulso Irregular ou Áspero (Se) / Corda (Xian)")
        }

        if (record.zangFu.liver.stagnation > 4 || record.zangFu.heart.stagnation > 4) {
            score += 0.25f
            symptomsFound.add("Estagnação nos canais do Fígado/Coração")
        }

        val finalConfidence = score.coerceIn(0f, 1f)

        return MTCPattern(
            name = "Estagnação de Sangue (Xue Yu)",
            affectedOrgans = listOf("Fígado (Gan)", "Coração (Xin)"),
            symptoms = symptomsFound.distinct(),
            confidence = finalConfidence,
            therapeuticPrinciple = "Mover o Sangue, eliminar a estase dolorosa, desobstruir os colaterais e restaurar o fluxo livre nos meridianos."
        )
    }

    // --- 4. Acúmulo de Umidade ---
    private fun detectDampness(record: ClinicalRecordEntity): MTCPattern {
        var score = 0f
        val symptomsFound = mutableListOf<String>()

        record.symptoms.forEach { s ->
            val name = s.name.lowercase()
            if (name.contains("peso") || name.contains("pesantez") || name.contains("inchado")) {
                score += 0.3f
                symptomsFound.add("Sensação de Peso Corporal ou Edemas")
            }
            if (name.contains("digestão lenta") || name.contains("empachamento") || name.contains("enjoo")) {
                score += 0.25f
                symptomsFound.add("Digestão lenta com secreções/mucosa")
            }
        }

        if (record.tongue.coatingColor.lowercase().contains("pegajosa") || record.tongue.coatingColor.lowercase().contains("espessa") || record.tongue.coatingThickness.lowercase().contains("espessa")) {
            score += 0.35f
            symptomsFound.add("Saburra Pegajosa ou Espessa na Língua")
        }

        if (record.pulse.strength.lowercase().contains("escorregadi") || record.pulse.strength.lowercase().contains("cheio")) {
            score += 0.25f
            symptomsFound.add("Pulso Escorregadio (Hua) - Acúmulo de Umidade")
        }

        if (record.zangFu.spleen.excess > 4 || record.zangFu.spleen.cold > 4) {
            score += 0.25f
            symptomsFound.add("Sobrecarga de umidade fria no Baço (Pi)")
        }

        val finalConfidence = score.coerceIn(0f, 1f)

        return MTCPattern(
            name = "Acúmulo de Umidade/Fleuma (Shi/Tan)",
            affectedOrgans = listOf("Baço (Pi)", "Pulmão (Fei)", "Estômago (Wei)"),
            symptoms = symptomsFound.distinct(),
            confidence = finalConfidence,
            therapeuticPrinciple = "Fortalecer o Baço, harmonizar o Estômago, drenar a umidade sistêmica e transformar a fleuma retida."
        )
    }

    // --- 5. Deficiência de Yin ---
    private fun detectYinDeficiency(record: ClinicalRecordEntity): MTCPattern {
        var score = 0f
        val symptomsFound = mutableListOf<String>()

        record.symptoms.forEach { s ->
            val name = s.name.lowercase()
            if (name.contains("insônia") || name.contains("calor noturno") || name.contains("suor noturno")) {
                score += 0.3f
                symptomsFound.add("Suor Noturno ou Insônia Ativa")
            }
            if (name.contains("boca seca") || name.contains("garganta seca")) {
                score += 0.2f
                symptomsFound.add("Boca e Garganta Secas")
            }
        }

        if (record.tongue.bodyColor.lowercase().contains("vermelha") || record.tongue.bodyColor.lowercase().contains("escarlate")) {
            score += 0.3f
            symptomsFound.add("Língua Vermelha")
        }
        if (record.tongue.coatingColor.lowercase().contains("ausente") || record.tongue.coatingColor.lowercase().contains("sem saburra") || record.tongue.moisture.lowercase().contains("seca")) {
            score += 0.3f
            symptomsFound.add("Saburra Escassa/Ausente ou Língua Seca (Calor Vazio)")
        }

        if (record.pulse.speed.lowercase().contains("rápido") || record.pulse.speed.lowercase().contains("rapid")) {
            score += 0.2f
            symptomsFound.add("Pulso Rápido (Shu)")
        }

        if (record.zangFu.kidney.deficiency > 4 && record.zangFu.kidney.heat > 4) {
            score += 0.25f
            symptomsFound.add("Deficiência de Yin com ascensão de Calor Vazio nos Rins")
        }

        val finalConfidence = score.coerceIn(0f, 1f)

        return MTCPattern(
            name = "Deficiência de Yin (Yin Xu)",
            affectedOrgans = listOf("Rim (Shen)", "Coração (Xin)", "Fígado (Gan)"),
            symptoms = symptomsFound.distinct(),
            confidence = finalConfidence,
            therapeuticPrinciple = "Nutrir o Yin profundo, resfriar o Calor Vazio por deficiência, acalmar o Shen e umedecer os tecidos secos."
        )
    }

    // --- 6. Estagnação de Qi do Fígado ---
    private fun detectLiverQiStagnation(record: ClinicalRecordEntity): MTCPattern {
        var score = 0f
        val symptomsFound = mutableListOf<String>()

        record.symptoms.forEach { s ->
            val name = s.name.lowercase()
            if (name.contains("estresse") || name.contains("irritabilidade") || name.contains("tensão") || name.contains("angústia")) {
                score += 0.3f
                symptomsFound.add("Tensão Emocional, Estresse ou Irritabilidade")
            }
            if (name.contains("distensão") || name.contains("suspiros") || name.contains("oposto")) {
                score += 0.25f
                symptomsFound.add("Suspiros frequentes e dor/distensão hipocôndrica")
            }
        }

        if (record.tongue.regions.getOrDefault("Bordas", "").lowercase().contains("vermelha") || record.tongue.regions.getOrDefault("Bordas", "").lowercase().contains("laterais")) {
            score += 0.3f
            symptomsFound.add("Laterais da Língua Vermelhas (Fígado)")
        }

        if (record.pulse.strength.lowercase().contains("corda") || record.pulse.strength.lowercase().contains("tenso")) {
            score += 0.3f
            symptomsFound.add("Pulso em Corda (Xian) - Típico de Fígado")
        }

        if (record.zangFu.liver.stagnation > 4) {
            score += 0.25f
            symptomsFound.add("Bloqueio de Qi no Fígado (Gan Qi Zhi)")
        }

        val finalConfidence = score.coerceIn(0f, 1f)

        return MTCPattern(
            name = "Estagnação de Qi do Fígado (Gan Qi Zhi)",
            affectedOrgans = listOf("Fígado (Gan)", "Vesícula Biliar (Dan)"),
            symptoms = symptomsFound.distinct(),
            confidence = finalConfidence,
            therapeuticPrinciple = "Promover o livre fluxo de Qi do Fígado, suavizar os tendões, aliviar a tensão emocional e acalmar o Shen."
        )
    }
}

// ==========================================
// 🧠 5. GERADOR DE PLANO TERAPÊUTICO
// ==========================================

class TherapyEngine {

    fun generatePlan(pattern: MTCPattern): TherapyPlan {
        return TherapyPlan(
            principle = pattern.therapeuticPrinciple,
            acupunctureStrategy = generateAcupunctureStrategy(pattern),
            cuppingStrategy = generateCuppingStrategy(pattern),
            tuinaStrategy = generateTuinaStrategy(pattern),
            auriculoStrategy = generateAuriculoStrategy(pattern)
        )
    }

    private fun generateAcupunctureStrategy(pattern: MTCPattern): String {
        return when (pattern.name) {
            "Deficiência de Qi (Qi Xu)" -> """
                📍 Pontos Principais:
                - E36 (Zusanli) - 3 tsun abaixo de E35: Tonifica fortemente o Qi do Baço, Estômago e o Sangue. Impulsiona a energia de defesa. (Estímulo de Tonificação suave, aceita Moxa).
                - BP6 (Sanyinjiao) - 3 tsun acima do maléolo medial: Harmoniza Baço, Rim e Fígado. Nutre o Sangue e fluidos.
                - VC12 (Zhongwan) - Na linha média, 4 tsun acima do umbigo: Fortalece o Aquecedor Médio e acalma o Estômago.
                - B20 (Pishu) - 1.5 tsun lateral ao processo espinhoso de T11: Estimulação Shu Dorsal direta do Baço.
                - VC6 (Qihai) - 1.5 tsun abaixo do umbigo: Tonifica o Qi Original (Yuan Qi) sistêmico contra fadiga.
            """.trimIndent()

            "Deficiência de Yang (Yang Xu)" -> """
                📍 Pontos Principais com Foco Térmico (Moxa Recomendada!):
                - VG4 (Mingmen) - Abaixo do processo espinhoso de L2: Aquece o Yang original e estimula a Porta da Vida.
                - B23 (Shenshu) - 1.5 tsun lateral a L2: Shu Dorsal do Rim, aquece e ancora a lombar e adrenais.
                - VC4 (Guanyuan) - 3 tsun abaixo do umbigo: Tonifica o Qi original e aquece o Aquecedor Inferior.
                - E36 (Zusanli) - 3 tsun abaixo de E35: Adiciona suporte de fogo ao Baço para gerar energia.
                - R3 (Taixi) - Depressão entre maléolo interno e tendão: Nutre a base energética e aquece com moxa indireta.
            """.trimIndent()

            "Estagnação de Sangue (Xue Yu)" -> """
                📍 Pontos de Desobstrução Ativa (Estímulo de Dispersão):
                - F3 (Taichong) - Depressão distal entre 1º e 2º metatarsos: Libera o fluxo de Qi e Sangue do Fígado.
                - IG4 (Hegu) - No dorso da mão, entre o 1º e 2º metacarpais: Forma os 'Quatro Portões' com F3 para ativar a circulação.
                - BP10 (Xuehai) - 2 tsun acima da borda superomedial da patela: 'Mar de Sangue' - Nutre, move e refresca o Sangue.
                - B17 (Geshu) - 1.5 tsun lateral a T7: Ponto de influência Hui do Sangue, resolve estases agudas e crônicas.
                - PC6 (Neiguan) - 2 tsun acima da prega do punho: Abre o tórax, move o Qi do Coração e Pericárdio, desfaz nódulos de dor.
            """.trimIndent()

            "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> """
                📍 Pontos de Resolução e Drenagem:
                - BP9 (Yinlingquan) - Na depressão posterior e inferior ao côndilo medial da tíbia: Ponto chave para drenar umidade no Aquecedor Inferior.
                - E40 (Fenglong) - 8 tsun acima do maléolo lateral: Ponto principal em toda MTC para dissolver fleuma/muco acumulado (visível ou invisível).
                - BP6 (Sanyinjiao) - 3 tsun acima do maléolo medial: Move e drena fluidos estagnados.
                - VC12 (Zhongwan) - 4 tsun acima do umbigo: Seca a umidade do Estômago e Baço.
                - B20 (Pishu) - 1.5 tsun lateral a T11: Fortalece o transporte e transformação hídrica do Baço.
            """.trimIndent()

            "Deficiência de Yin (Yin Xu)" -> """
                📍 Pontos de Nutrição Profunda e Ancoramento (Seda de calor):
                - R3 (Taixi) - Entre o maléolo interno e o tendão de Aquiles: Ponto Yuan do Rim, nutre a essência profunda Yin.
                - R6 (Zhaohai) - Depressão inferior ao maléolo medial: Abre o Yin Qiao Mai, arrefece o Calor Vazio e reidrata a garganta e olhos.
                - BP6 (Sanyinjiao) - 3 tsun acima do maléolo medial: Nutrição máxima do Yin das pernas.
                - C7 (Shenmen) - Face anterior do punho: Acalma a agitação mental provocada pelo Calor Vazio que ascende ao Coração.
                - VC4 (Guanyuan) - 3 tsun abaixo do umbigo: Concentra a energia e reidrata o abdômen inferior.
            """.trimIndent()

            "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> """
                📍 Pontos de Harmonização Emocional e Livre Fluxo:
                - F3 (Taichong) - Entre 1º e 2º metatarsos: Desobstrução do Qi Hepático estagnado por estresse.
                - PC6 (Neiguan) - 2 tsun acima da prega do punho: Suaviza o Pericárdio, alivia angústia e palpitações no peito.
                - VB34 (Yanglingquan) - Abaixo da cabeça da fíbula: Ponto Hui dos Tendões, alivia rigidez física lateral.
                - IG4 (Hegu) - Dorso da mão: Promove circulação geral de Qi no corpo.
                - B18 (Ganshu) - 1.5 tsun lateral a T9: Ponto Shu Dorsal do Fígado, acalma a mente e equilibra o humor.
            """.trimIndent()

            else -> """
                📍 Pontos de Manutenção Preventiva (Harmonização):
                - E36 (Zusanli) - Fortalecimento da imunidade e vitalidade geral.
                - IG4 (Hegu) - Regulação do fluxo de Qi nos meridianos.
                - BP6 (Sanyinjiao) - Harmonização orgânica e circulação sanguínea.
                - VC12 (Zhongwan) - Equilíbrio digestivo preventivo.
            """.trimIndent()
        }
    }

    private fun generateCuppingStrategy(pattern: MTCPattern): String {
        return when (pattern.name) {
            "Deficiência de Qi (Qi Xu)", "Deficiência de Yang (Yang Xu)" -> 
                "Ventosaterapia seca, de leve sucção ou deslizamento suave e rápido nos pontos Shu dorsais B20 (Pishu), B21 (Weishu) e B23 (Shenshu). Se houver moxa, aplicar após a ventosa leve para injetar calor térmico profundo."
            "Estagnação de Sangue (Xue Yu)", "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> 
                "Ventosaterapia deslizante forte com óleo essencial de bétula ou copaíba ao longo de todo o canal da Bexiga, com foco em B18 (Ganshu) e B17 (Geshu) para liberar estase física severa e pontos gatilhos de estresse."
            "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> 
                "Ventosaterapia fixa de média intensidade sobre B20 (Pishu) e B21 (Weishu) por 10 minutos para drenar fluidos de umidade e estimular o metabolismo."
            else -> 
                "Aplicação leve de ventosas fixas no dorso para descompressão miofascial e indução de relaxamento geral (foco em B15, B18 e B23)."
        }
    }

    private fun generateTuinaStrategy(pattern: MTCPattern): String {
        return when (pattern.name) {
            "Deficiência de Qi (Qi Xu)", "Deficiência de Yang (Yang Xu)" -> 
                "Manobras lentas e suaves de tonificação, como pressões sustentadas (An Fa) e fricções circulares (Rou Fa) no sentido horário ao longo do abdômen e canal do Baço (membros inferiores) para nutrir."
            "Estagnação de Sangue (Xue Yu)", "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> 
                "Manobras de dispersão e deslizamento profundo, amassamento (Nie Fa) vigoroso nos ombros, trapézios e região escapular para liberar rigidez, além de percussão leve (Pai Fa) para mover o Qi."
            "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> 
                "Deslizamentos lineares lentos no trajeto ascendente do meridiano do Baço (BP) para ajudar na drenagem hídrica."
            else -> 
                "Massagem relaxante rítmica nas costas e pescoço, focada na liberação do Shenmen e relaxamento do sistema nervoso simpático."
        }
    }

    private fun generateAuriculoStrategy(pattern: MTCPattern): String {
        val points = when (pattern.name) {
            "Deficiência de Qi (Qi Xu)" -> listOf("Baço (Pi)", "Estômago (Wei)", "Shenmen", "Simpático", "Subcórtex")
            "Deficiência de Yang (Yang Xu)" -> listOf("Rim (Shen)", "Baço (Pi)", "Glândula Adrenal", "Shenmen", "Subcórtex")
            "Estagnação de Sangue (Xue Yu)" -> listOf("Fígado (Gan)", "Coração (Xin)", "Shenmen", "Subcórtex", "Pelve")
            "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> listOf("Baço (Pi)", "Rim (Shen)", "Triplo Aquecedor", "Subcórtex", "Shenmen")
            "Deficiência de Yin (Yin Xu)" -> listOf("Rim (Shen)", "Coração (Xin)", "Fígado (Gan)", "Shenmen", "Endócrino")
            "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> listOf("Fígado (Gan)", "Vesícula Biliar (Dan)", "Shenmen", "Ansiedade", "Cérebro")
            else -> listOf("Shenmen", "Simpático", "Subcórtex", "Endócrino")
        }
        return "Aplicação de sementes de mostarda ou esferas de ouro nos pontos auriculares: " + points.joinToString(", ") + ". Instruir o paciente a massagear cada ponto 3 vezes ao dia por 1 minuto."
    }
}

// ==========================================
// 💡 TherapyPlan
// ==========================================

data class TherapyPlan(
    val principle: String,
    val acupunctureStrategy: String,
    val cuppingStrategy: String,
    val tuinaStrategy: String,
    val auriculoStrategy: String
)

// ==========================================
// 🧠 HELPER DE CONVERSÃO PARA PRONTUÁRIO
// ==========================================

fun MtcProntuaryEntity.toClinicalRecord(): ClinicalRecordEntity {
    val symptomList = mutableListOf<SymptomEntry>()
    if (this.queixaPrincipal.isNotBlank()) {
        symptomList.add(
            SymptomEntry(
                name = this.queixaPrincipal,
                intensity = this.sintomaIntensidade,
                frequency = this.sintomaFrequencia,
                duration = this.sintomaDuracao,
                trigger = this.sintomaGatilhos,
                impact = this.sintomaImpactoFuncional
            )
        )
    }
    if (this.sintomasFisicos.isNotBlank()) {
        symptomList.add(
            SymptomEntry(
                name = "Sintomas Físicos: ${this.sintomasFisicos}",
                intensity = this.sintomaIntensidade,
                frequency = this.sintomaFrequencia,
                duration = this.sintomaDuracao,
                trigger = this.sintomaGatilhos,
                impact = this.sintomaImpactoFuncional
            )
        )
    }

    val tongueAssessment = TongueAssessment(
        bodyColor = this.linguaCorpo,
        coatingColor = this.linguaSaburra,
        coatingThickness = if (this.linguaSaburra.lowercase().contains("espess")) "Espessa" else "Fina",
        moisture = this.linguaUmidade,
        cracks = this.linguaFissuras,
        teethMarks = this.linguaMarcasDentarias || this.linguaBordas.lowercase().contains("dentead"),
        regions = mapOf("Bordas" to this.linguaBordas, "Região" to this.linguaRegioes)
    )

    val pulseAssessment = PulseAssessment(
        depth = this.pulsoProfundidade,
        strength = this.pulsoForca,
        rhythm = this.pulsoRitmo,
        speed = this.pulsoVelocidade,
        symmetry = this.pulsoLateralidade
    )

    val shenAssessment = ShenAssessment(
        emotionalStability = if (this.shenEstabilidadeEmocional.lowercase() == "estável") 8 else 4,
        anxietyLevel = when (this.shenAnsiedade.lowercase()) {
            "intensa" -> 9
            "moderada" -> 6
            "leve" -> 3
            else -> 0
        },
        depressionLevel = when (this.shenDepressao.lowercase()) {
            "intensa" -> 9
            "moderada" -> 6
            "leve" -> 3
            else -> 0
        },
        mentalClarity = if (this.shenClarezaMental.lowercase() == "normal") 8 else 4,
        vitality = if (this.shenVitalidadeEspiritual.lowercase() == "normal") 8 else 4
    )

    val zangFuState = ZangFuState(
        heart = OrganState(
            deficiency = if (this.zangFuHeartEstado.lowercase().contains("def")) 6 else 0,
            excess = if (this.zangFuHeartEstado.lowercase().contains("exc") || this.zangFuHeartEstado.lowercase().contains("fogo")) 6 else 0,
            heat = if (this.zangFuHeartEstado.lowercase().contains("calor") || this.zangFuHeartEstado.lowercase().contains("fogo")) 6 else 0,
            cold = if (this.zangFuHeartEstado.lowercase().contains("frio")) 6 else 0,
            stagnation = if (this.zangFuHeartEstado.lowercase().contains("estag")) 6 else 0
        ),
        liver = OrganState(
            deficiency = if (this.zangFuLiverEstado.lowercase().contains("def")) 6 else 0,
            excess = if (this.zangFuLiverEstado.lowercase().contains("exc") || this.zangFuLiverEstado.lowercase().contains("fogo")) 6 else 0,
            heat = if (this.zangFuLiverEstado.lowercase().contains("calor") || this.zangFuLiverEstado.lowercase().contains("fogo")) 6 else 0,
            cold = if (this.zangFuLiverEstado.lowercase().contains("frio")) 6 else 0,
            stagnation = if (this.zangFuLiverEstado.lowercase().contains("estag")) 6 else 0
        ),
        spleen = OrganState(
            deficiency = if (this.zangFuSpleenEstado.lowercase().contains("def")) 6 else 0,
            excess = if (this.zangFuSpleenEstado.lowercase().contains("exc")) 6 else 0,
            heat = if (this.zangFuSpleenEstado.lowercase().contains("calor")) 6 else 0,
            cold = if (this.zangFuSpleenEstado.lowercase().contains("frio") || this.zangFuSpleenEstado.lowercase().contains("umid")) 6 else 0,
            stagnation = if (this.zangFuSpleenEstado.lowercase().contains("estag")) 6 else 0
        ),
        lung = OrganState(
            deficiency = if (this.zangFuLungEstado.lowercase().contains("def")) 6 else 0,
            excess = if (this.zangFuLungEstado.lowercase().contains("exc")) 6 else 0,
            heat = if (this.zangFuLungEstado.lowercase().contains("calor")) 6 else 0,
            cold = if (this.zangFuLungEstado.lowercase().contains("frio")) 6 else 0,
            stagnation = if (this.zangFuLungEstado.lowercase().contains("estag")) 6 else 0
        ),
        kidney = OrganState(
            deficiency = if (this.zangFuKidneyEstado.lowercase().contains("def")) 6 else 0,
            excess = if (this.zangFuKidneyEstado.lowercase().contains("exc")) 6 else 0,
            heat = if (this.zangFuKidneyEstado.lowercase().contains("calor") || this.zangFuKidneyEstado.lowercase().contains("yin")) 6 else 0,
            cold = if (this.zangFuKidneyEstado.lowercase().contains("frio") || this.zangFuKidneyEstado.lowercase().contains("yang")) 6 else 0,
            stagnation = if (this.zangFuKidneyEstado.lowercase().contains("estag")) 6 else 0
        )
    )

    val fiveElementsState = FiveElementsState(
        wood = this.nivelMadeira,
        fire = this.nivelFogo,
        earth = this.nivelTerra,
        metal = this.nivelMetal,
        water = this.nivelAgua
    )

    val meridianList = this.meridianos.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { name ->
        MeridianState(
            name = name,
            hasPain = this.meridianosDorTrajeto.lowercase().contains(name.lowercase()),
            condition = when {
                this.meridianosExcesso.lowercase().contains(name.lowercase()) -> "Excesso"
                this.meridianosDeficiencia.lowercase().contains(name.lowercase()) -> "Deficiência"
                else -> "Equilibrado"
            }
        )
    }

    return ClinicalRecordEntity(
        id = this.patientId,
        patientId = this.patientId,
        symptoms = symptomList,
        tongue = tongueAssessment,
        pulse = pulseAssessment,
        shen = shenAssessment,
        zangFu = zangFuState,
        fiveElements = fiveElementsState,
        meridians = meridianList,
        primaryPattern = null,
        secondaryPatterns = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
