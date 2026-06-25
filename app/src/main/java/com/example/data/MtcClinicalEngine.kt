package com.example.data

import com.example.data.local.MtcProntuaryEntity
import com.example.data.clinicalcore.MTCClinicalEngine
import com.example.data.clinicalcore.TherapyEngine
import com.example.data.clinicalcore.toClinicalRecord
import com.example.data.clinicalcore.StudyTopic
import com.example.data.clinicalcore.Flashcard
import com.example.data.clinicalcore.SimilarCase

data class MtcDiagnosisHypothesis(
    val primaryPattern: String,
    val secondaryPattern: String,
    val confidence: String, // "Baixa", "Média", "Alta"
    val explanation: String,
    val matchedSymptoms: List<String>,
    val acupuncturePoints: List<PointDetail>,
    val auriculotherapy: List<String>,
    val ventosaterapia: String,
    val tuina: String,
    val contraindications: String,
    val differentiationTips: List<DifferentiationTip>,
    val probableCauses: List<String> = emptyList(),
    val organSymptomRelations: List<String> = emptyList(),
    val therapeuticGoals: List<String> = emptyList(),
    val studyTopics: List<StudyTopic> = emptyList(),
    val automaticFlashcards: List<Flashcard> = emptyList(),
    val similarCases: List<SimilarCase> = emptyList()
)

data class PointDetail(
    val code: String,
    val name: String,
    val action: String,
    val rationale: String,
    val location: String,
    val insertion: String
)

data class DifferentiationTip(
    val patternName: String,
    val keySymptom: String,
    val comparison: String
)

object MtcClinicalEngine {

    fun analyze(prontuary: MtcProntuaryEntity): MtcDiagnosisHypothesis {
        // Translate Flat Room Entity to Domain ClinicalRecordEntity
        val record = prontuary.toClinicalRecord()

        // 🧠 Run MTCClinicalEngine (Real Clinical Core v1)
        val coreEngine = MTCClinicalEngine()
        val analysisResult = coreEngine.analyze(record)

        val primaryPattern = analysisResult.primaryPattern
        val secondaryPatterns = analysisResult.secondaryPatterns

        // 🌿 Run TherapyEngine
        val therapyEngine = TherapyEngine()
        val therapyPlan = primaryPattern?.let { therapyEngine.generatePlan(it) }

        // Construct diagnosis hypothesis
        val primaryName = primaryPattern?.name ?: "Equilíbrio Dinâmico Geral"
        val secondaryName = secondaryPatterns.firstOrNull()?.name ?: "Ausência de co-fatores expressivos"

        val confidenceStr = when {
            primaryPattern == null -> "Baixa"
            primaryPattern.confidence >= 0.7f -> "Alta"
            primaryPattern.confidence >= 0.4f -> "Média"
            else -> "Baixa"
        }

        val explanation = primaryPattern?.therapeuticPrinciple ?: "Recomenda-se tratamento preventivo focado em harmonização dos meridianos principais para manutenção do bem-estar e imunidade."

        val matchedSymptoms = (primaryPattern?.symptoms ?: emptyList()) + (secondaryPatterns.flatMap { it.symptoms })

        // Detailed Acupuncture Points
        val pts = mutableListOf<PointDetail>()
        when (primaryName) {
            "Deficiência de Qi (Qi Xu)" -> {
                pts.add(PointDetail("E36", "Zusanli", "Tonifica fortemente o Qi do Baço, Estômago e o Sangue", "Ponto primordial de tonificação sistêmica. Impulsiona a imunidade e as funções digestivas.", "Na face anterolateral da perna, 3 tsun abaixo de E35 (olho lateral do joelho), a um dedo transverso lateral à crista anterior da tíbia.", "Agulhamento perpendicular de 1.0 a 1.5 tsun, ou aplicação de moxabustão para aquecimento profundo."))
                pts.add(PointDetail("BP6", "Sanyinjiao", "Fortalece o Baço, drena a umidade e harmoniza o fígado e rins", "Ponto de cruzamento dos 3 meridianos Yin da perna. Excelente para nutrir o sangue e regular fluidos.", "Na face medial da perna, 3 tsun acima do ápice do maléolo medial, imediatamente atrás da borda posterior da tíbia.", "Agulhamento perpendicular de 1.0 a 1.5 tsun. Contraindicado na gestação."))
                pts.add(PointDetail("VC12", "Zhongwan", "Harmoniza o Estômago e regula o Aquecedor Médio", "Ponto Mu Frontal do Estômago e ponto de influência das vísceras (Fu). Promove a digestão e resolve a estagnação alimentar.", "Na linha média anterior do abdômen, 4 tsun acima do centro do umbigo.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("B20", "Pishu", "Ponto Shu Dorsal do Baço, tonifica o Qi e o Yang do Baço", "Estimula diretamente o órgão Baço, auxiliando na retenção do sangue nos vasos e na elevação do Qi.", "No dorso, abaixo do processo espinhoso da 11ª vértebra torácica (T11), 1.5 tsun lateral à linha média posterior.", "Agulhamento oblíquo em direção à coluna de 0.5 a 0.8 tsun (cuidado com risco de pneumotórax)."))
                pts.add(PointDetail("VC6", "Qihai", "Tonifica o Qi original e o Yuan Qi", "O 'Mar de Qi' é essencial para fadiga sistêmica extrema, exaustão física e colapso de energia vital.", "Na linha média anterior do abdômen, 1.5 tsun inferior ao centro do umbigo.", "Agulhamento perpendicular de 0.8 a 1.2 tsun."))
            }
            "Deficiência de Yang (Yang Xu)" -> {
                pts.add(PointDetail("VG4", "Mingmen", "Aquece a Porta da Vida, tonifica o Yang original do Rim", "Portão da Vida. Ponto máximo de ativação térmica e metabólica do corpo. Indicado para aversão severa ao frio.", "Na linha média posterior, na depressão abaixo do processo espinhoso da 2ª vértebra lombar (L2).", "Agulhamento perpendicular ou oblíquo para cima de 0.5 a 1.0 tsun, ou uso intenso de moxabustão."))
                pts.add(PointDetail("B23", "Shenshu", "Ponto Shu Dorsal do Rim, tonifica o Yin e o Yang do Rim", "Sustenta a base energética vital (adrenais). Fortalece a região lombar, os ossos e melhora a audição.", "No dorso, abaixo do processo espinhoso da 2ª vértebra lombar (L2), 1.5 tsun lateral à linha média posterior.", "Agulhamento perpendicular ou ligeiramente oblíquo de 0.5 a 1.0 tsun."))
                pts.add(PointDetail("VC4", "Guanyuan", "Fortalece o Yang e o Qi original, nutre o Yin e o Sangue", "Ponto de cruzamento dos meridianos Yin do pé com o Vaso Concepção. Grande regulador do Aquecedor Inferior.", "Na linha média anterior do abdômen, 3 tsun inferior ao centro do umbigo.", "Agulhamento perpendicular de 0.8 a 1.2 tsun."))
                pts.add(PointDetail("E36", "Zusanli", "Tonifica fortemente o Qi do Baço, o Estômago e o Sangue", "Ponto primordial de tonificação sistêmica. Adiciona suporte de fogo ao Baço para gerar energia.", "Na face anterolateral da perna, 3 tsun abaixo de E35, a um dedo transverso lateral à crista anterior da tíbia.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("R3", "Taixi", "Nutre o Yin, tonifica o Yang e fortalece a Essência (Jing) do Rim", "Ponto Fonte (Yuan) do Rim. Essencial para reidratar o corpo e fortalecer a região lombar.", "Na face medial do tornozelo, na depressão profunda entre o ápice do maléolo medial e a borda anterior do tendão do calcâneo.", "Agulhamento perpendicular de 0.5 a 1.0 tsun."))
            }
            "Estagnação de Sangue (Xue Yu)" -> {
                pts.add(PointDetail("F3", "Taichong", "Promove o livre fluxo de Qi do Fígado, pacifica o vento e acalma a mente", "Ponto Fonte (Yuan) e Shu Corrente do Fígado. Principal ponto para dissipar tensão física, estresse e espasmos.", "No dorso do pé, na depressão distal à junção do primeiro e segundo ossos metatárseos.", "Agulhamento perpendicular de 0.5 a 1.0 tsun."))
                pts.add(PointDetail("IG4", "Hegu", "Dispersa vento exterior, regula canais e move o Qi", "Ponto Fonte (Yuan) do Intestino Grosso. Forma a célebre combinação 'Quatro Portões' com F3 para ativar intensamente a circulação de Qi.", "No dorso da mão, no ponto médio do lado radial do segundo osso metacarpal.", "Agulhamento perpendicular de 0.5 a 1.0 tsun. Contraindicado na gestação."))
                pts.add(PointDetail("BP10", "Xuehai", "Regula e move o Sangue, drena o calor", "O 'Mar de Sangue' é o principal ponto para tratar estase e desordens vasculares.", "Com o joelho flexionado, na parte interna da coxa, 2 tsun acima da borda superomedial da patela.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("B17", "Geshu", "Ponto de influência Hui do Sangue, resolve estases", "Ponto crucial nas costas para mover o sangue e desfazer nódulos de estagnação crônica.", "Abaixo do processo espinhoso de T7, 1.5 tsun lateral à linha média posterior.", "Agulhamento oblíquo de 0.5 a 0.8 tsun."))
                pts.add(PointDetail("PC6", "Neiguan", "Acalma o Shen, harmoniza o Coração e abre o tórax", "Ponto de conexão (Luo) do Pericárdio. Abre o tórax e move o Qi do Coração contra dores fixas.", "Na face anterior do antebraço, 2 tsun acima da prega transversal do punho, entre os tendões dos músculos palmar longo e flexor radial do carpo.", "Agulhamento perpendicular de 0.5 a 1.0 tsun."))
            }
            "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> {
                pts.add(PointDetail("BP9", "Yinlingquan", "Drena a umidade acumulada e beneficia o Aquecedor Inferior", "Ponto He-Mar do Baço. Principal ponto para eliminar a umidade retida por deficiência de transporte.", "Na face interna da perna, na depressão posterior e inferior ao côndilo medial da tíbia.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("E40", "Fenglong", "Transforma a fleuma e umidade no corpo todo", "Ponto Luo de conexão do Estômago. Ponto mestre na MTC para dissolver acúmulos de fleuma visível ou invisível.", "Na face anterolateral da perna, 8 tsun acima do maléolo lateral, dois dedos transversos lateralmente à crista da tíbia.", "Agulhamento perpendicular de 1.0 to 1.5 tsun."))
                pts.add(PointDetail("BP6", "Sanyinjiao", "Fortalece o Baço, drena a umidade e harmoniza o fígado e rins", "Excelente ponto de apoio para mover os fluidos de todo o corpo e fortalecer a digestão.", "Na face medial da perna, 3 tsun acima do ápice do maléolo medial.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("VC12", "Zhongwan", "Harmoniza o Estômago e resolve a umidade no Aquecedor Médio", "Evita o acúmulo de novos resíduos de umidade a partir de uma digestão ineficiente.", "Na linha média anterior do abdômen, 4 tsun acima do centro do umbigo.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("B20", "Pishu", "Ponto Shu Dorsal do Baço, tonifica o Qi e a transformação de água", "Estimula diretamente a capacidade do Baço de transportar e transformar a umidade.", "No dorso, abaixo do processo espinhoso de T11, 1.5 tsun lateral à linha média.", "Agulhamento oblíquo de 0.5 a 0.8 tsun."))
            }
            "Deficiência de Yin (Yin Xu)" -> {
                pts.add(PointDetail("R3", "Taixi", "Nutre o Yin, tonifica o Yang e fortalece a Essência (Jing) do Rim", "Ponto Fonte (Yuan) do Rim. Essencial para reidratar o corpo e combater o Calor Vazio.", "Na face medial do tornozelo, na depressão profunda entre o ápice do maléolo medial e a borda anterior do tendão do calcâneo.", "Agulhamento perpendicular de 0.5 a 1.0 tsun."))
                pts.add(PointDetail("R6", "Zhaohai", "Nutre o Yin do Rim, acalma o Shen e umedece a garganta", "Ponto de abertura do vaso maravilhoso Yin Qiao Mai. Excelente para tratar calor por deficiência.", "Na face medial do pé, na depressão imediatamente inferior ao ápice do maléolo medial.", "Agulhamento perpendicular de 0.3 a 0.5 tsun."))
                pts.add(PointDetail("BP6", "Sanyinjiao", "Fortalece o Baço e nutre intensamente o Yin dos três canais do pé", "Promove a nutrição de fluidos e sangue, reabastecendo o Yin exaurido.", "Na face medial da perna, 3 tsun acima do ápice do maléolo medial.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("C7", "Shenmen", "Acalma o Shen e clareia o fogo do Coração", "Excelente para conter a ansiedade ativa e a insônia por deficiência de Yin (Calor Vazio).", "Na face anterior do punho, na prega transversal distal, na borda radial do tendão do flexor ulnar do carpo.", "Agulhamento perpendicular de 0.3 a 0.5 tsun."))
                pts.add(PointDetail("VC4", "Guanyuan", "Fortalece o Yang e o Qi original, nutre o Yin e o Sangue", "Concentra a essência vital (Jing) e ajuda a resfriar o calor vazio no abdômen.", "Na linha média anterior do abdômen, 3 tsun inferior ao centro do umbigo.", "Agulhamento perpendicular de 0.8 a 1.2 tsun."))
            }
            "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> {
                pts.add(PointDetail("F3", "Taichong", "Promove o livre fluxo de Qi do Fígado, pacifica o vento e acalma a mente", "Ponto primordial para dissipar tensão física, estresse, frustração e espasmos musculares.", "No dorso do pé, na depressão distal à junção do primeiro e segundo ossos metatárseos.", "Agulhamento perpendicular de 0.5 a 1.0 tsun."))
                pts.add(PointDetail("PC6", "Neiguan", "Acalma o Shen, harmoniza o Coração e abre o tórax", "Excelente para ansiedade, palpitações e opressão no peito causadas por estresse.", "Na face anterior do antebraço, 2 tsun acima da prega transversal do punho.", "Agulhamento perpendicular de 0.5 a 1.0 tsun."))
                pts.add(PointDetail("VB34", "Yanglingquan", "Harmoniza tendões e articulações, drena canais laterais", "Ponto de Influência (Hui) dos tendões. Excelente para relaxamento da musculatura tensionada pelo estresse crônico.", "Na face lateral da perna, na depressão anterior e inferior à cabeça da fíbula.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("IG4", "Hegu", "Dispersa vento exterior, regula canais e move o Qi", "Trabalha com o F3 para ativar a circulação sistêmica de energia.", "No dorso da mão, no ponto médio do lado radial do segundo osso metacarpal.", "Agulhamento perpendicular de 0.5 a 1.0 tsun."))
                pts.add(PointDetail("B18", "Ganshu", "Ponto Shu Dorsal do Fígado, regula a estagnação de Qi", "Regula diretamente o órgão Fígado nas costas, aliviando irritabilidade.", "No dorso, abaixo do processo espinhoso de T9, 1.5 tsun lateral à linha média.", "Agulhamento oblíquo de 0.5 a 0.8 tsun."))
            }
            else -> {
                pts.add(PointDetail("E36", "Zusanli", "Tonifica fortemente o Qi do Baço, o Estômago e o Sangue", "Ponto primordial de tonificação sistêmica. Impulsiona a imunidade.", "Na face anterolateral da perna, 3 tsun abaixo de E35.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
                pts.add(PointDetail("IG4", "Hegu", "Dispersa vento exterior, regula canais e move o Qi", "Excelente para harmonização sistêmica diária.", "No dorso da mão.", "Agulhamento perpendicular de 0.5 a 1.0 tsun."))
                pts.add(PointDetail("BP6", "Sanyinjiao", "Fortalece o Baço, drena a umidade e harmoniza os canais", "Equilíbrio geral dos meridianos Yin das pernas.", "Na face medial da perna, 3 tsun acima do ápice do maléolo medial.", "Agulhamento perpendicular de 1.0 a 1.5 tsun."))
            }
        }

        // Auriculotherapy
        val auriculo = primaryPattern?.let {
            val list = mutableListOf<String>()
            when (it.name) {
                "Deficiência de Qi (Qi Xu)" -> list.addAll(listOf("Baço", "Estômago", "Shenmen", "Simpático", "Subcórtex"))
                "Deficiência de Yang (Yang Xu)" -> list.addAll(listOf("Rim", "Baço", "Glândula Adrenal", "Shenmen", "Subcórtex"))
                "Estagnação de Sangue (Xue Yu)" -> list.addAll(listOf("Fígado", "Coração", "Shenmen", "Subcórtex", "Pelve"))
                "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> list.addAll(listOf("Baço", "Rim", "Triplo Aquecedor", "Subcórtex", "Shenmen"))
                "Deficiência de Yin (Yin Xu)" -> list.addAll(listOf("Rim", "Coração", "Fígado", "Shenmen", "Endócrino"))
                "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> list.addAll(listOf("Fígado", "Vesícula Biliar", "Shenmen", "Ansiedade", "Cérebro"))
                else -> list.addAll(listOf("Shenmen", "Simpático", "Subcórtex"))
            }
            list
        } ?: listOf("Shenmen", "Simpático", "Subcórtex")

        // Cupping, Tuina, Contraindications
        val ventosa = therapyPlan?.cuppingStrategy ?: "Aplicação preventiva leve ao longo do meridiano da Bexiga."
        val tuina = therapyPlan?.tuinaStrategy ?: "Deslizamento suave nas costas para relaxamento miofascial."
        val contraindicacoes = if (primaryName.contains("Yang") || primaryName.contains("Qi")) {
            "Sem contraindicações absolutas. Evitar dispersão forte de energia ou sangrias. Foco total em tonificação."
        } else if (primaryName.contains("Yin") || primaryName.contains("Fogo")) {
            "Contraindicado o uso de moxabustão direta ou calor excessivo. Evitar sucção de ventosas prolongada."
        } else {
            "Evitar agulhamento doloroso. Contraindicado em gestantes se usar pontos abortivos como IG4 e BP6."
        }

        // Differentiation Tips
        val tips = listOf(
            DifferentiationTip(
                "Deficiência de Qi (Baço)",
                "Marcas Dentárias e Fadiga",
                "A fadiga melhora com repouso leve. Língua apresenta marcas dentárias claras por retenção de umidade e pulso fraco."
            ),
            DifferentiationTip(
                "Estagnação de Qi (Fígado)",
                "Pulso Tenso & Flutuação Emocional",
                "Os sintomas físicos pioram com o estresse emocional. O pulso apresenta-se tenso em corda (Xian) e as laterais da língua ficam vermelhas."
            ),
            DifferentiationTip(
                "Deficiência de Yin (Rim)",
                "Língua sem Saburra & Calor Vazio",
                "Apresenta suor noturno, calor nas palmas e plantas, pulso rápido e língua vermelha com pouca ou nenhuma saburra."
            ),
            DifferentiationTip(
                "Deficiência de Yang (Rim)",
                "Aversão ao Frio & Língua Úmida",
                "Extremidades frias que melhoram com calor local. Língua pálida, inchada, úmida com pulso profundo e lento."
            )
        )

        // Probable Causes
        val causes = mutableListOf<String>()
        val estiloNormalized = prontuary.estiloVida.lowercase()
        val medsNormalized = prontuary.medicamentos.lowercase()
        if (estiloNormalized.contains("doce") || estiloNormalized.contains("açúcar") || estiloNormalized.contains("irregular")) {
            causes.add("Alimentação inadequada ou consumo excessivo de doces enfraquecendo o Qi do Baço.")
        }
        if (estiloNormalized.contains("estresse") || estiloNormalized.contains("trabalho") || estiloNormalized.contains("pressão")) {
            causes.add("Sobrecarga mental e estresse diário levando ao bloqueio do fluxo do Qi do Fígado.")
        }
        if (estiloNormalized.contains("noite") || estiloNormalized.contains("dorme tarde") || estiloNormalized.contains("café")) {
            causes.add("Privação de sono e estimulantes desgastando o Yin profundo do Rim e do Coração.")
        }
        if (medsNormalized.isNotEmpty() && medsNormalized != "nenhum") {
            causes.add("Uso continuado de medicamentos químicos que demandam depuração metabólica.")
        }
        if (causes.isEmpty()) {
            causes.add("Desgaste energético constitucional ou acúmulo de fatores patogênicos externos.")
        }

        // Organ Symptom Relations
        val relations = mutableListOf<String>()
        when (primaryName) {
            "Deficiência de Qi (Qi Xu)" -> {
                relations.add("Baço (Pi) -> Incapaz de produzir Qi e Sangue suficientes, gerando fadiga física.")
                relations.add("Estômago (Wei) -> Recebe os alimentos mas a transformação é lenta, causando distensão.")
            }
            "Deficiência de Yang (Yang Xu)" -> {
                relations.add("Rim (Shen) -> Deficiência de Yang reduz o calor metabólico geral do corpo.")
                relations.add("Baço (Pi) -> Sem o calor do Rim, o Baço perde energia para digerir, gerando fezes amolecidas.")
            }
            "Estagnação de Sangue (Xue Yu)" -> {
                relations.add("Fígado (Gan) -> A estagnação prolongada de Qi gera estase de Sangue nos colaterais.")
                relations.add("Coração (Xin) -> A estase bloqueia a circulação torácica, causando dor fixa ou palpitações.")
            }
            "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> {
                relations.add("Baço (Pi) -> Deficiência no transporte de água gera umidade estagnada.")
                relations.add("Pulmão (Fei) -> O pulmão armazena a fleuma gerada pelo Baço, dificultando a respiração.")
            }
            "Deficiência de Yin (Yin Xu)" -> {
                relations.add("Rim (Shen) -> Deficiência de Yin gera fraqueza lombar e calor nos ossos.")
                relations.add("Coração (Xin) -> Falta de Yin do Rim impede o controle do Fogo do Coração, gerando insônia.")
            }
            "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> {
                relations.add("Fígado (Gan) -> Estagnação do Qi gera opressão torácica e flutuações de humor.")
                relations.add("Vesícula Biliar (Dan) -> Rigidez e tensão lateral nos trapézios e pescoço.")
            }
            else -> {
                relations.add("Sistemas em harmonia dinâmica constitucional estável.")
            }
        }

        // Therapeutic Goals
        val goals = when (primaryName) {
            "Deficiência de Qi (Qi Xu)" -> listOf("Tonificar o Qi do Baço", "Drenar a umidade interna", "Otimizar o trânsito digestivo", "Restabelecer a energia vital")
            "Deficiência de Yang (Yang Xu)" -> listOf("Aquecer o Yang do Rim", "Dispersar o Frio Interno", "Fortalecer o Yang Original (Mingmen)", "Combater a letargia")
            "Estagnação de Sangue (Xue Yu)" -> listOf("Mover o Sangue", "Dissipar a estase", "Harmonizar o fluxo de Qi", "Aliviar dores fixas")
            "Acúmulo de Umidade/Fleuma (Shi/Tan)" -> listOf("Resolver a umidade", "Transformar a fleuma", "Fortalecer a digestão do Baço")
            "Deficiência de Yin (Yin Xu)" -> listOf("Nutrir o Yin do Rim", "Limpar o Calor Vazio", "Ancorar a mente (Shen)", "Preservar os fluidos corporais")
            "Estagnação de Qi do Fígado (Gan Qi Zhi)" -> listOf("Promover o livre fluxo de Qi do Fígado", "Acalmar a mente (Shen)", "Dissipar a estagnação de energia")
            else -> listOf("Manter o livre fluxo de Qi e Sangue", "Preservar a imunidade (Wei Qi)", "Harmonizar o Aquecedor Médio")
        }

        return MtcDiagnosisHypothesis(
            primaryPattern = primaryName,
            secondaryPattern = secondaryName,
            confidence = confidenceStr,
            explanation = explanation,
            matchedSymptoms = matchedSymptoms.distinct(),
            acupuncturePoints = pts,
            auriculotherapy = auriculo,
            ventosaterapia = ventosa,
            tuina = tuina,
            contraindications = contraindicacoes,
            differentiationTips = tips,
            probableCauses = causes.distinct(),
            organSymptomRelations = relations.distinct(),
            therapeuticGoals = goals.distinct(),
            studyTopics = analysisResult.studyTopics,
            automaticFlashcards = analysisResult.automaticFlashcards,
            similarCases = analysisResult.similarCases
        )
    }
}
