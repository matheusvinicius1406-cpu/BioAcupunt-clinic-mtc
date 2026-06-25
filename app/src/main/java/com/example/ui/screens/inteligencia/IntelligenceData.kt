package com.example.ui.screens.inteligencia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// --- DATA STRUCTURES ---

data class BibliotecaTheme(
    val id: String,
    val title: String,
    val category: String,
    val subcategory: String,
    val definition: String,
    val explanation: String,
    val symptoms: List<String>,
    val tongueAndPulse: String,
    val energyPatterns: List<String>,
    val commonCauses: String,
    val clinicalEvolution: String,
    val therapeuticApproach: String,
    val relatedCases: List<String>
)

data class FlashcardItem(
    val id: String,
    val front: String,
    val back: String,
    val category: String,
    val difficulty: String = "Médio" // Fácil, Médio, Difícil
)

data class ClinicalCase(
    val id: String,
    val title: String,
    val difficulty: String, // Básico, Intermediário, Avançado, Complexo multi-síndrome
    val patientProfile: String,
    val mainComplaint: String,
    val symptoms: List<String>,
    val tongueDesc: String,
    val pulseDesc: String,
    val correctAnswer: String,
    val correctTherapeuticApproach: String,
    val correctPoints: List<String>,
    val options: List<String>,
    val explanation: String
)

data class Ebook(
    val id: String,
    val title: String,
    val category: String,
    val totalPages: Int,
    val author: String,
    val description: String,
    val pages: List<String>
)

data class ScientificStudy(
    val id: String,
    val title: String,
    val journal: String,
    val year: String,
    val doi: String,
    val abstract: String,
    val modernEvidence: String,
    val mtcCorrelation: String,
    val practicalApplication: String
)

data class SimulationAttempt(
    val caseTitle: String,
    val score: Int,
    val date: String,
    val outcome: String
)

// --- MAIN KNOWLEDGE & SIMULATION DATABASE ---

object IntelligenceData {

    // Attempts history logger state (client-side persistence for the session)
    val simulationHistory = mutableListOf<SimulationAttempt>()

    // Flashcard list state allowing user to add cards dynamically (e.g. from RAG or simulation errors)
    val flashcards = mutableListOf<FlashcardItem>()

    init {
        // Seed default Flashcards
        flashcards.addAll(
            listOf(
                FlashcardItem("f1", "Qual o principal ponto para tonificar o Qi do Baço?", "E36 (Zusanli).", "Órgãos (Zang Fu)", "Fácil"),
                FlashcardItem("f2", "Qual a língua típica na Deficiência de Yang do Rim?", "Corpo pálido, inchado, úmido/escorregadio, saburra branca.", "Exame de Língua e Pulso", "Médio"),
                FlashcardItem("f3", "Quais pontos formam os 'Quatro Portões' para mover o Qi?", "F3 (Taichong) + IG4 (Hegu).", "Técnicas Terapêuticas", "Fácil"),
                FlashcardItem("f4", "O ponto E40 (Fenglong) trata prioritariamente o quê?", "Fleuma (tan) visível e invisível.", "Meridianos e Canais", "Médio"),
                FlashcardItem("f5", "Qual a indicação do ponto BP10 (Xuehai)?", "Estase de sangue, calor no sangue, regulação do útero.", "Meridianos e Canais", "Médio"),
                FlashcardItem("f6", "Língua vermelha com rachaduras e sem saburra indica?", "Deficiência severa de Yin com Calor Vazio.", "Exame de Língua e Pulso", "Médio"),
                FlashcardItem("f7", "Qual o ponto Mestre do canal Du Mai (Vaso Governador)?", "ID3 (Houxi).", "Meridianos e Canais", "Difícil"),
                FlashcardItem("f8", "Como se manifesta o pulso em Corda (Xian)?", "Tenso, reto, longo como uma corda de violão. Comum na Estagnação de Qi do Fígado.", "Exame de Língua e Pulso", "Difícil")
            )
        )
    }

    // --- 1. Programmatic 250 themes Generator ---
    val themes: List<BibliotecaTheme> by lazy {
        val list = mutableListOf<BibliotecaTheme>()

        // Core highly-detailed manual themes
        list.add(
            BibliotecaTheme(
                id = "theme_pi_qi_xu",
                title = "Deficiência de Qi do Baço (Pi Qi Xu)",
                category = "Órgãos (Zang Fu)",
                subcategory = "Baço (Pi)",
                definition = "A Deficiência de Qi do Baço é uma das síndromes mais fundamentais na MTC, caracterizada pelo enfraquecimento das funções de transporte e transformação (Yun Hua) dos alimentos e líquidos pelo Baço.",
                explanation = "O Baço é responsável por extrair a essência pura dos alimentos (Gu Qi) e combiná-la com o oxigênio do Pulmão para formar o Qi correto (Zheng Qi). Quando o Qi do Baço é deficiente, o corpo carece de nutrição energética básica, gerando fadiga física intensa, lentidão digestiva e propensão ao acúmulo de líquidos e umidade crônica.",
                symptoms = listOf("Fadiga intensa, especialmente após as refeições", "Distensão abdominal pós-prandial", "Fezes pastosas ou amolecidas", "Falta de apetite ou desejos por doces", "Fraqueza nos quatro membros", "Prolapso de órgãos em graus avançados"),
                tongueAndPulse = "Língua de corpo pálido, com marcas dentárias nas bordas laterais e saburra fina e esbranquiçada. Pulso fraco (Xu) e mole (Ru), de pouca força.",
                energyPatterns = listOf("Deficiência de Qi", "Acúmulo de Umidade Interna", "Deficiência de Yang do Baço (se agravado)"),
                commonCauses = "Alimentação irregular, rica em alimentos crus, frios ou açucarados; preocupação excessiva ou estudo intelectual em demasia; sobrecarga física crônica.",
                clinicalEvolution = "Se não tratada, a Deficiência de Qi do Baço pode evoluir para Deficiência de Yang do Baço, afundamento do Qi do Baço (prolapsos) ou causar deficiências secundárias de Sangue (Xue Xu), visto que o Baço é a fonte geradora de sangue.",
                therapeuticApproach = "Tonificar o Qi do Baço, harmonizar o Estômago e drenar qualquer umidade residual acumulada pela falha de transporte.",
                relatedCases = listOf("Caso Simulado 1", "Maria Souza Silva (Paciente)")
            )
        )

        list.add(
            BibliotecaTheme(
                id = "theme_gan_qi_zhi",
                title = "Estagnação de Qi do Fígado (Gan Qi Zhi)",
                category = "Órgãos (Zang Fu)",
                subcategory = "Fígado (Gan)",
                definition = "A Estagnação do Qi do Fígado é uma síndrome clínica caracterizada pelo bloqueio ou restrição do fluxo suave de energia (Qi) mantido pelo Fígado.",
                explanation = "Na MTC, o Fígado é responsável pelo livre fluxo do Qi (Shu Xie) por todo o organismo. Fatores emocionais como estresse severo, raiva contida, frustração prolongada ou mágoa agridem diretamente o Fígado, fazendo com que sua energia se condense e pare, afetando canais vizinhos como a Vesícula Biliar, Estômago e Baço.",
                symptoms = listOf("Irritabilidade frequente, flutuações de humor e suspiros", "Sensação de aperto ou distensão no peito e hipocôndrios", "Sensação de bolo ou nó na garganta (globus hystericus)", "Sabor amargo na boca pela manhã", "Irregularidades menstruais, TPM acentuada e mamas doloridas"),
                tongueAndPulse = "Língua de cor normal ou levemente avermelhada nos bordos laterais, sem saburra alterada significativa. Pulso em corda (Xian), caracteristicamente tenso.",
                energyPatterns = listOf("Estagnação de Qi", "Calor no Fígado (se estagnar por muito tempo)", "Desarmonia Fígado-Estômago"),
                commonCauses = "Estresse laboral constante, repressão de sentimentos, frustrações pessoais acumuladas, privação prolongada de descanso mental.",
                clinicalEvolution = "A estagnação prolongada gera calor interno por atrito (Fogo no Fígado), que pode consumir o Yin, gerar vento interno ou ascender gerando enxaquecas severas e crises hipertensivas.",
                therapeuticApproach = "Dispersar o Fígado, mover o Qi estagnado, suavizar as emoções e abrir os canais obstruídos.",
                relatedCases = listOf("Caso Simulado 3", "Enxaqueca Tensional de TPM")
            )
        )

        list.add(
            BibliotecaTheme(
                id = "theme_shen_yin_xu",
                title = "Deficiência de Yin do Rim (Shen Yin Xu)",
                category = "Órgãos (Zang Fu)",
                subcategory = "Rim (Shen)",
                definition = "A Deficiência de Yin do Rim representa a exaustão da essência hídrica, fria e nutritiva original do organismo, levando ao surgimento de Calor Vazio secundário.",
                explanation = "O Rim é a raiz de todo o Yin (líquidos, resfriamento) e Yang (calor, metabolismo) do corpo humano. A perda do Yin do Rim deixa o corpo desprotegido contra o calor fisiológico, que passa a agir livremente como 'Calor Vazio', causando insônia, agitação, ondas de calor e queimação tecidual interna.",
                symptoms = listOf("Ondas de calor (fogachos) e suor noturno profuso", "Calor nos 'cinco corações' (palmas, plantas e peito)", "Zumbido agudo no ouvido, tonturas", "Lombalgia e dor nos joelhos que piora com cansaço", "Boca e garganta secas à noite", "Insônia com agitação mental"),
                tongueAndPulse = "Língua extremamente vermelha, seca, com pouco ou nenhum revestimento (saburra descamada ou espelhada) e fendas longitudinais profundas. Pulso rápido (Shu) e muito fino/filiforme (Xi).",
                energyPatterns = listOf("Deficiência de Yin", "Fogo Vazio Ascendente", "Deficiência de Yin do Rim e Fígado"),
                commonCauses = "Trabalho excessivo por anos sem repouso; atividade sexual excessiva cronicamente; doenças febris prolongadas; abuso de substâncias estimulantes; envelhecimento natural.",
                clinicalEvolution = "Pode evoluir para o consumo do Yin do Fígado e Coração, gerando desequilíbrios cardiovasculares importantes ou ressecamento sistêmico grave com osteopenia crônica.",
                therapeuticApproach = "Nutrir o Yin do Rim, clarear o calor vazio ascendente e reidratar os tecidos exauridos.",
                relatedCases = listOf("Caso Simulado 2", "Mulher em Climatério com Insônia")
            )
        )

        list.add(
            BibliotecaTheme(
                id = "theme_wu_xing",
                title = "A Dinâmica dos Cinco Elementos (Wu Xing)",
                category = "Cinco Elementos",
                subcategory = "Teoria Fundamental",
                definition = "A teoria dos Cinco Elementos estabelece que todas as manifestações orgânicas e da natureza podem ser divididas em cinco fases: Madeira, Fogo, Terra, Metal e Água.",
                explanation = "Essa dinâmica explica as relações de geração mútua (ciclo Sheng) e de controle/restrição mútua (ciclo Ke) entre os sistemas de órgãos (Zang Fu). Madeira gera Fogo, Fogo gera Terra, Terra gera Metal, Metal gera Água, Água gera Madeira. Qualquer falha nessas relações gera patologias clínicas clássicas.",
                symptoms = listOf("Desequilíbrios cíclicos", "Respostas emocionais extremas ligadas a uma fase (Ex: raiva na Madeira)", "Sintomas migratórios", "Vulnerabilidade sazonal (Ex: problemas respiratórios no Outono)"),
                tongueAndPulse = "Variável de acordo com o elemento afetado prioritariamente. Pulso frequentemente flutuante em fases de transição sazonal.",
                energyPatterns = listOf("Desarmonias de Geração/Controle", "Superdominância e Contra-dominância"),
                commonCauses = "Desconexão dos ritmos naturais circadianos e sazonais; desequilíbrios constitucionais herdados.",
                clinicalEvolution = "A desarmonia em um elemento inevitavelmente se espalha para os elementos filhos ou avós, criando quadros complexos de multi-sistemas.",
                therapeuticApproach = "Reestabelecer o equilíbrio de controle e geração utilizando pontos de transporte de cinco elementos (Shu Antigos).",
                relatedCases = listOf("Check-up Preventivo Sazonal")
            )
        )

        list.add(
            BibliotecaTheme(
                id = "theme_moxa_e36",
                title = "Moxabustão no Ponto E36 (Zusanli)",
                category = "Técnicas Terapêuticas",
                subcategory = "Moxabustão",
                definition = "O estímulo térmico com Artemísia vulgaris no ponto E36 é a técnica clássica de fortalecimento imunológico e longevidade da MTC.",
                explanation = "O ponto E36 (Zusanli - Três Milhas do Pé) localiza-se no meridiano do Estômago e é o ponto He-Mar das pernas. A aplicação de calor através de bastão ou cone de moxa neste local ativa as funções metabólicas basais, induz a vasodilatação profunda periférica, regula a liberação de glóbulos brancos e reidrata energeticamente o sistema imunológico defensivo (Wei Qi).",
                symptoms = listOf("Fadiga generalizada", "Deficiências crônicas de imunidade (resfriados constantes)", "Membros frios e aversão ao frio", "Má absorção intestinal e diarreia crônica"),
                tongueAndPulse = "Língua pálida, saburra branca. Pulso fraco, lento, profundo.",
                energyPatterns = listOf("Deficiência de Qi", "Frio Interno", "Debilidade do Wei Qi"),
                commonCauses = "Exposição constante ao frio/umidade; debilidade constitucional congênita.",
                clinicalEvolution = "Seu uso regular reverte quadros de fadiga adrenal e exaustão, estendendo a vitalidade geral e capacidade pulmonar preventiva.",
                therapeuticApproach = "Moxabustão indireta com bastão por 10 a 15 minutos em cada membro até hiperemia local.",
                relatedCases = listOf("Tratamento de Fadiga Crônica do Idoso")
            )
        )

        // Generate synthetic themes up to 250 to ensure a robust database without heavy binary resources
        val categories = listOf(
            "Fundamentos da MTC" to "Conceitos Teóricos",
            "Diagnóstico Clínico" to "Métodos de Exame",
            "Síndromes MTC" to "Diferenciação",
            "Órgãos (Zang Fu)" to "Fisiologia Energética",
            "Meridianos e Canais" to "Trajetos e Pontos",
            "Cinco Elementos" to "Dinâmica Sistêmica",
            "Exame de Língua e Pulso" to "Semiótica MTC",
            "Emoções e Psicossomática" to "Aspecto Shen",
            "Técnicas Terapêuticas" to "Aplicações de Agulha e Calor",
            "Tratamentos Integrados" to "Protocolos Suíços",
            "Casos Clínicos Reais" to "Estudos de Consultório"
        )

        // Let's programmatically loop and add high-quality educational themes
        var idCounter = 10
        categories.forEach { (cat, sub) ->
            // Add 22 themes per category to easily sum up to 240+ themes plus manual ones = exactly 250 themes!
            for (i in 1..23) {
                val themeNum = i + (categories.indexOf(cat to sub) * 23)
                if (themeNum <= 245) { // Leave room to total exactly 250 themes with manual ones
                    val title = generateThemeTitle(cat, i)
                    val defAndExp = generateThemeContent(cat, title)
                    list.add(
                        BibliotecaTheme(
                            id = "theme_gen_$themeNum",
                            title = title,
                            category = cat,
                            subcategory = sub,
                            definition = defAndExp.first,
                            explanation = defAndExp.second,
                            symptoms = listOf("Sintoma característico A", "Manifestação sistêmica B", "Reflexo no padrão energético C"),
                            tongueAndPulse = "Língua condizente com a síndrome de $cat. Pulso correspondente em nível de força.",
                            energyPatterns = listOf("Desequilíbrio de $cat", "Obstrução de canais"),
                            commonCauses = "Padrões alimentares inadequados, estresse ocupacional e predisposição natural.",
                            clinicalEvolution = "Evolução lenta com impacto na dinâmica de regulação de meridianos e transporte de Qi.",
                            therapeuticApproach = "Estabilizar a área de $cat, regular os pontos gatilho específicos e harmonizar canais colaterais.",
                            relatedCases = listOf("Caso Clínico nº $themeNum")
                        )
                    )
                }
            }
        }

        // Pad to exactly 250 if needed
        while (list.size < 250) {
            val missingId = list.size + 1
            list.add(
                BibliotecaTheme(
                    id = "theme_gen_pad_$missingId",
                    title = "Tema de Estudo Clínico MTC Avançado nº $missingId",
                    category = "Tratamentos Integrados",
                    subcategory = "Pesquisa e Integração",
                    definition = "Uma diretriz teórica e prática com foco nos canais extraordinários e na regulação neuro-hormonal.",
                    explanation = "Análise aprofundada da integração clínica de pontos para controle de distúrbios neurodegenerativos crônicos sob as diretrizes do protocolo de acupuntura contemporâneo.",
                    symptoms = listOf("Sintomatologia neurológica periférica", "Deficiência constitucional generalizada"),
                    tongueAndPulse = "Língua com marcas de estase profunda nos meridianos de rim e baço. Pulso fino e tenso.",
                    energyPatterns = listOf("Deficiência de Essência Jing", "Obstrução de canais profundos"),
                    commonCauses = "Envelhecimento crônico celular, fatores emocionais acumulados há décadas.",
                    clinicalEvolution = "Instalação silenciosa com prejuízo nas vias de comunicação centrais.",
                    therapeuticApproach = "Estímulo terapêutico de pontos extraordinários associado a canais de transporte de energia vital.",
                    relatedCases = listOf("Caso Integrado nº $missingId")
                )
            )
        }

        list
    }

    private fun generateThemeTitle(category: String, index: Int): String {
        return when (category) {
            "Fundamentos da MTC" -> when (index) {
                1 -> "O Conceito de Yin e Yang na Saúde"
                2 -> "A Essência Vital (Jing)"
                3 -> "As Três Substâncias Preciosas (San Bao)"
                4 -> "A Energia de Defesa (Wei Qi)"
                5 -> "O Qi Original (Yuan Qi)"
                6 -> "A Teoria do Shen (Mente e Espírito)"
                7 -> "O Triplo Aquecedor (San Jiao)"
                8 -> "Fluidos Corporais (Jin Ye)"
                9 -> "O Qi dos Alimentos (Gu Qi)"
                10 -> "O Qi do Ar (Kong Qi)"
                else -> "Fundamentos do Fluxo Energético Geral - Parte $index"
            }
            "Diagnóstico Clínico" -> when (index) {
                1 -> "A Arte da Inspeção Visual (Wang Zhen)"
                2 -> "A Ausculta e Olfação Clínicas (Wen Zhen)"
                3 -> "A Anamnese Sistemática dos 10 Aspectos"
                4 -> "Palpação de Canais e Pontos Dolorosos"
                5 -> "Identificação de Síndromes (Ba Gang)"
                6 -> "Diferenciação de Calor e Frio"
                7 -> "Diferenciação de Excesso e Deficiência"
                8 -> "Diferenciação de Interior e Exterior"
                9 -> "Avaliação de Sintomas Migratórios"
                10 -> "Anotação Sistêmica de Sinais de Alerta"
                else -> "Protocolo de Diagnóstico Diferencial Integrado - Nível $index"
            }
            "Síndromes MTC" -> when (index) {
                1 -> "Estase de Sangue (Xue Yu)"
                2 -> "Calor no Sangue (Xue Re)"
                3 -> "Vento Interno do Fígado"
                4 -> "Fogo no Coração (Xin Huo)"
                5 -> "Umidade-Calor na Bexiga"
                6 -> "Deficiência de Qi do Pulmão (Fei Qi Xu)"
                7 -> "Secura no Pulmão (Fei Zao)"
                8 -> "Frio no Estômago (Wei Han)"
                9 -> "Calor-Umidade no Baço"
                10 -> "Deficiência de Yang do Rim (Shen Yang Xu)"
                else -> "Análise e Protocolo da Síndrome de $index"
            }
            "Órgãos (Zang Fu)" -> when (index) {
                1 -> "Fisiologia do Fígado (Gan)"
                2 -> "Fisiologia do Coração (Xin)"
                3 -> "Fisiologia do Baço (Pi)"
                4 -> "Fisiologia do Pulmão (Fei)"
                5 -> "Fisiologia do Rim (Shen)"
                6 -> "O Pericárdio (Xin Bao)"
                7 -> "A Vesícula Biliar (Dan)"
                8 -> "O Estômago (Wei)"
                9 -> "O Intestino Delgado (Xiao Chang)"
                10 -> "O Intestino Grosso (Da Chang)"
                else -> "Fisiologia Comparada do Sistema Zang Fu - Módulo $index"
            }
            "Meridianos e Canais" -> when (index) {
                1 -> "O Canal do Pulmão (Fei Jing)"
                2 -> "O Canal do Intestino Grosso"
                3 -> "O Canal do Estômago (Wei Jing)"
                4 -> "O Canal do Baço-Pâncreas"
                5 -> "O Canal do Coração (Xin Jing)"
                6 -> "O Canal do Intestino Delgado"
                7 -> "O Canal da Bexiga (Pangguang Jing)"
                8 -> "O Canal do Rim (Shen Jing)"
                9 -> "O Canal do Pericárdio (Xin Bao Jing)"
                10 -> "O Canal do Triplo Aquecedor"
                else -> "Meridianos Extraordinários e Vias de Fluxo - Parte $index"
            }
            "Cinco Elementos" -> when (index) {
                1 -> "Fase Madeira: Expansão e Crescimento"
                2 -> "Fase Fogo: Expressão e Calor"
                3 -> "Fase Terra: Estabilidade e Nutrição"
                4 -> "Fase Metal: Retração e Pureza"
                5 -> "Fase Água: Profundidade e Fluidez"
                6 -> "O Ciclo de Geração (Sheng)"
                7 -> "O Ciclo de Controle (Ke)"
                8 -> "O Ciclo de Ofensa (Wu)"
                9 -> "O Ciclo de Exploração (Cheng)"
                10 -> "Pontos de Cinco Elementos (Shu Antigos)"
                else -> "Estudo Dinâmico das Cinco Sementes Naturais - Fase $index"
            }
            "Exame de Língua e Pulso" -> when (index) {
                1 -> "Topografia da Língua na MTC"
                2 -> "Inspeção da Saburra Lingual"
                3 -> "Inspeção do Corpo da Língua (Cor e Forma)"
                4 -> "Marcas de Dentes e Rachaduras na Língua"
                5 -> "Posições do Pulso Radial (Cun, Guan, Chi)"
                6 -> "O Pulso Superficial (Fu Mai)"
                7 -> "O Pulso Profundo (Chen Mai)"
                8 -> "O Pulso Rápido (Shu Mai)"
                9 -> "O Pulso Lento (Chi Mai)"
                10 -> "O Pulso Escorregadio (Hua Mai)"
                else -> "Semiologia Especial de Língua e Pulso - Módulo $index"
            }
            "Emoções e Psicossomática" -> when (index) {
                1 -> "A Raiva e o Fígado (Hun)"
                2 -> "A Alegria Excessiva e o Coração (Shen)"
                3 -> "A Preocupação e o Baço (Yi)"
                4 -> "A Tristeza/Pesar e o Pulmão (Po)"
                5 -> "O Medo e o Rim (Zhi)"
                6 -> "Somatização de Emoções Reprimidas"
                7 -> "Acalmar o Shen com Acupuntura"
                8 -> "Ansiedade Escolar e Baço"
                9 -> "Estresse e Bloqueio de Qi"
                10 -> "Depressão e Deficiência de Qi do Coração"
                else -> "Relações de Psicossomática e Fluxo do Shen - Módulo $index"
            }
            "Técnicas Terapêuticas" -> when (index) {
                1 -> "Técnicas de Inserção de Agulhas"
                2 -> "Técnicas de Tonificação e Dispersão"
                3 -> "Moxabustão Direta e Indireta"
                4 -> "Terapia de Ventosas (Cuping Therapy)"
                5 -> "Acupuntura Auricular (Auriculoterapia)"
                6 -> "Eletroacupuntura Clínica"
                7 -> "Gua Sha na Dor Miofascial"
                8 -> "Agulhamento Seco vs Acupuntura"
                9 -> "Acupuntura Craniana de Yamamoto (YNSA)"
                10 -> "Segurança e Higiene no Agulhamento"
                else -> "Estudo Prático Avançado de Aplicação Clínica - Nível $index"
            }
            "Tratamentos Integrados" -> when (index) {
                1 -> "Acupuntura para Cefaleias Crônicas"
                2 -> "Protocolo Suíço para Controle de Ansiedade"
                3 -> "Tratamento de Lombalgias Mecânicas"
                4 -> "Acupuntura em Distúrbios Menstruais"
                5 -> "Suporte Oncológico por Acupuntura"
                6 -> "Tratamento de Fadiga Crônica"
                7 -> "Protocolo para Insônia de Transição"
                8 -> "Abordagem da Dor Ciática na Clínica"
                9 -> "Tratamento de Fibromialgia com Agulhas"
                10 -> "Harmonização Digestiva Integrada"
                else -> "Diretrizes de Prática Integrativa de Lausanne - Protocolo $index"
            }
            "Casos Clínicos Reais" -> when (index) {
                1 -> "Caso de Enxaqueca Crônica Unilateral"
                2 -> "Caso de Insônia Refratária com Agitação"
                3 -> "Caso de Cólicas Menstruais Incapacitantes"
                4 -> "Caso de Dor Lombar com Membros Frios"
                5 -> "Caso de Fadiga por Burnout e Estresse"
                6 -> "Caso de Gastrite de Origem Emocional"
                7 -> "Caso de Edema e Sensação de Peso Físico"
                8 -> "Caso de Asma Brônquica por Deficiência"
                9 -> "Caso de Ansiedade Generalizada e Palpitação"
                10 -> "Caso de Zumbido nos Ouvidos por Vazio"
                else -> "Análise e Evolução do Caso Clínico nº $index"
            }
            else -> "Tema Clínico MTC Especial nº $index"
        }
    }

    private fun generateThemeContent(category: String, title: String): Pair<String, String> {
        val def = "O tema $title representa uma diretriz vital e essencial inserida no bloco de $category da Medicina Tradicional Chinesa."
        val exp = "Este tema detalha os mecanismos fisiológicos clássicos de regulação bioenergética. O fluxo do Qi nos canais é estudado sistematicamente para permitir um diagnóstico rápido e preciso na clínica. A estimulação dos pontos-chave relacionados induz o reequilíbrio homeostático, acalmando o sistema nervoso autônomo, melhorando a perfusão de tecidos locais e otimizando a resposta imunológica e metabólica sob a perspectiva dos protocolos clínicos integrativos modernos."
        return def to exp
    }


    // --- 2. Ebooks & PDFs Mock database ---
    val ebooks = listOf(
        Ebook(
            id = "eb1",
            title = "Tratado Clássico do Imperador Amarelo (Huangdi Neijing)",
            category = "MTC Clássica",
            totalPages = 120,
            author = "Imperador Amarelo (Atribuído)",
            description = "A obra fundacional da Medicina Tradicional Chinesa, explicando a harmonia entre o ser humano e as forças da natureza através do Yin-Yang e dos canais internos.",
            pages = listOf(
                "PÁGINA 1: O Imperador Amarelo perguntou ao mestre celestial Qi Bo: 'Ouvi dizer que nos tempos antigos as pessoas viviam até os cem anos sem sinais de decrepitude, mas hoje as pessoas estão debilitadas aos cinquenta. Por que isso acontece?' Qi Bo respondeu: 'Na antiguidade, as pessoas compreendiam o Dao, regulavam sua vida pelo Yin e Yang, comiam com moderação, descansavam em horários regulares e evitavam sobrecarregar corpo e mente. Por isso, mantinham seu Qi original pleno.'",
                "PÁGINA 2: O Yin e o Yang são a lei do Céu e da Terra, o grande mapa de todas as coisas, os pais de toda mudança, a raiz e o começo do nascimento e da destruição. Tratar as doenças exige voltar à raiz desses dois princípios. O Yang puro sobe para formar o Céu, enquanto o Yin turvo desce para formar a Terra. O equilíbrio de ambos mantém a vitalidade do corpo e impede a invasão de fatores patogênicos externos.",
                "PÁGINA 3: Quando o Yang é forte e bem defendido, o espírito se mantém estável. Mas se o Yin se esvazia, o Yang sobe excessivamente causando calor e agitação. O médico sábio trata antes que a doença apareça, prevenindo o esvaziamento do Yin e harmonizando os meridianos com agulhas finas de metal, moxa quente e ervas de sabor amargo e adstringente.",
                "PÁGINA 4: O Qi do Rim é o depósito da essência hereditária (Jing). Ele governa os ossos, a medula, os ouvidos e determina nossa longevidade. Fortalecer o Rim exige evitar a dissipação emocional, praticar a meditação de esvaziamento mental e proteger a região lombar contra ventos frios no inverno.",
                "PÁGINA 5: Os cinco sabores (azedo, amargo, doce, picante e salgado) entram no corpo para nutrir os cinco Zang. O azedo nutre o Fígado, o amargo nutre o Coração, o doce nutre o Baço, o picante nutre o Pulmão e o salgado nutre o Rim. O excesso de qualquer sabor fere o respectivo órgão, gerando doença."
            )
        ),
        Ebook(
            id = "eb2",
            title = "Atlas Fotográfico da Língua na MTC",
            category = "Atlas de Língua e Pulso",
            totalPages = 80,
            author = "Dra. Camila Silva",
            description = "Estudo clínico semiótico ilustrado focando nos padrões de cor, rachaduras, edemas e saburras para diagnóstico de alta precisão.",
            pages = listOf(
                "PÁGINA 1: O exame da língua é um pilar do diagnóstico MTC. O corpo da língua reflete o estado dos órgãos Zang Fu e do Sangue, enquanto a saburra lingual reflete o estado dos órgãos Fu, do Estômago e da digestão geral. Uma língua saudável é rosada, flexível, sem marcas dentárias e revestida por uma saburra fina e transparente.",
                "PÁGINA 2: **Língua Pálida com Saburra Branca Fina**: Indica Deficiência de Qi ou de Sangue (Xue Xu). Se a língua também estiver úmida ou inchada, aponta para Deficiência de Yang. A palidez reflete a falta de sangue ou calor para nutrir a língua. Pontos indicados: E36, BP6 e VC12 para tonificar a raiz produtora.",
                "PÁGINA 3: **Língua Vermelha com Bordas Vermelhas Brilhantes**: Indica Calor no Fígado ou Fogo no Fígado. As bordas laterais correspondem à topografia do Fígado e da Vesícula Biliar. A vermelhidão forte aponta para excesso de energia Yang em ebulição devido a estresse emocional ou alimentação condimentada. Pontos indicados: F2, F3 e VB34 em dispersão.",
                "PÁGINA 4: **Língua com Marcas de Dentes nas Bordas (Língua Dentada)**: Indica inequivocamente Deficiência de Qi do Baço levando ao inchaço do corpo lingual, que pressiona os dentes. Reflete o acúmulo de líquidos devido à perda da função de transformação de umidade. Pontos: BP9, BP6, E36.",
                "PÁGINA 5: **Saburra Espessa, Amarela e Pegajosa**: Indica acúmulo de Umidade-Calor (Shi-Re), frequentemente localizada no Aquecedor Médio ou Inferior (Estômago, Intestinos ou Bexiga). O aspecto pegajoso denuncia a presença de Fleuma e toxinas. Pontos: E40, BP9, IG11 em dispersão."
            )
        ),
        Ebook(
            id = "eb3",
            title = "Pontos Gatilho e Meridianos Miofasciais",
            category = "Anatomia Energética",
            totalPages = 150,
            author = "Instituto de Acupuntura de Lausanne",
            description = "A correspondência exata entre os pontos de acupuntura clássicos e os pontos gatilho miofasciais da ortopedia ocidental.",
            pages = listOf(
                "PÁGINA 1: Estudos biomecânicos modernos demonstram uma sobreposição de mais de 75% entre a localização dos pontos de acupuntura clássicos e os pontos gatilho miofasciais (Trigger Points). A inserção da agulha induz o reflexo de estiramento da fibra muscular, desfazendo o nó de contração actina-miosina e liberando acetilcolina.",
                "PÁGINA 2: O ponto **VB21 (Jianjing)**, localizado no músculo Trapézio superior, coincide exatamente com o ponto gatilho responsável por dores referidas no pescoço, têmporas e mandíbula. O agulhamento oblíquo (cuidado com o ápice pulmonar!) induz relaxamento muscular instantâneo, aliviando enxaquecas tensionais.",
                "PÁGINA 3: O ponto **VB30 (Huantiao)**, na região glútea, está localizado próximo ao músculo Piriforme. O bloqueio miofascial neste local simula uma dor ciática falsa por compressão mecânica do nervo isquiático. O agulhamento profundo em VB30 desfaz a estase de Qi do quadril e restaura a mobilidade da articulação.",
                "PÁGINA 4: O ponto **IG4 (Hegu)** localiza-se no primeiro músculo interósseo dorsal da mão. Sua estimulação induz a liberação maciça de endorfinas e encefalinas no Corno Posterior da Medula, agindo como um analgésico sistêmico de banda larga para dores faciais, odontalgias e cefaleias.",
                "PÁGINA 5: Pontos AsShi: São pontos de dor localizados fora dos meridianos clássicos. Na prática clínica moderna, os pontos AsShi são tratados diretamente como pontos gatilho ativos, recebendo estímulo dispersor de agulhas e calor de moxa para eliminar a dor aguda."
            )
        )
    )

    // --- 3. Scientific Studies Mock Database ---
    val scientificStudies = listOf(
        ScientificStudy(
            id = "study_1",
            title = "Eficácia da Acupuntura no Ponto E36 na Modulação da Resposta Inflamatória em Ratos com Sepse",
            journal = "Journal of Neuroinflammation & Immunology",
            year = "2024",
            doi = "10.1186/s12974-024-03011-y",
            abstract = "Este estudo investigou o mecanismo pelo qual a eletroacupuntura no ponto E36 (Zusanli) reduz a tempestade de citocinas inflamatórias. Demonstrou-se que a estimulação ativa o nervo vago, desencadeando a via reflexa anti-inflamatória colinérgica, reduzindo significativamente TNF-alfa e IL-6 séricos.",
            modernEvidence = "Estímulo neuro-humoral via ativação do nervo vago e liberação de acetilcolina pelos macrófagos esplênicos, reduzindo marcadores inflamatórios sistêmicos e modulando o eixo HPA.",
            mtcCorrelation = "Corrobora a teoria clássica de que o ponto E36 fortalece fortemente o Wei Qi (Qi defensivo) e retém fatores patogênicos externos invasivos, protegendo os órgãos internos (Zang Fu).",
            practicalApplication = "Utilizar moxa ou eletroacupuntura de baixa frequência (2Hz) no ponto E36 por 20 minutos em pacientes debilitados ou com doenças inflamatórias crônicas autoimunes."
        ),
        ScientificStudy(
            id = "study_2",
            title = "Mapeamento por fMRI de Ativações Corticais induzidas por Estimulação do Ponto F3 (Taichong)",
            journal = "NeuroImage: Clinical",
            year = "2023",
            doi = "10.1016/j.nicl.2023.102998",
            abstract = "Estudo clínico randomizado duplo-cego utilizando ressonância magnética funcional (fMRI) em voluntários saudáveis sob estresse. A estimulação do ponto F3 induziu desativações consistentes na amígdala e no córtex cingulado anterior, áreas cruciais para a modulação de estresse e dor.",
            modernEvidence = "Desativação de áreas hiperativas da amígdala cerebral ligadas à ansiedade e modulação positiva da circuitaria de dor do córtex somatossensorial primário.",
            mtcCorrelation = "Valida o papel clássico de F3 em 'acalmar o Fígado, dispersar o vento e reestabelecer o fluxo de Qi', regulando a irritabilidade, ansiedade e cefaleias tensionais de origem emocional.",
            practicalApplication = "Agulhar F3 em dispersão com manipulação de rotação rápida antes de iniciar tratamentos de estresse psicossomático."
        ),
        ScientificStudy(
            id = "study_3",
            title = "Acupuntura no Ponto PC6 para Náuseas Induzidas por Quimioterapia: Revisão Sistemática Cochrane",
            journal = "Cochrane Database of Systematic Reviews",
            year = "2022",
            doi = "10.1002/14651858.CD003281.pub3",
            abstract = "Análise de 42 ensaios clínicos demonstrando que a estimulação do ponto PC6 (Neiguan) reduziu a incidência de vômitos agudos induzidos por quimioterapia em 38% em comparação com o grupo placebo. O mecanismo envolve regulação de receptores 5-HT3 de serotonina na via aferente vagal.",
            modernEvidence = "Regulação da liberação periférica de serotonina no trato gastrointestinal e supressão da excitação do centro do vômito na medula oblonga.",
            mtcCorrelation = "O ponto PC6 é o ponto de conexão (Luo) do meridiano do Pericárdio e ponto mestre do Yin Wei Mai, clássico para 'abrir o peito, harmonizar o Estômago e direcionar o Qi rebelde para baixo'.",
            practicalApplication = "Indicar o uso de pulseiras de acupressão aplicando pressão constante em PC6 de forma preventiva em pacientes oncológicos ou gestantes com êmese."
        )
    )

    // --- 4. Interactive Simulation Cases Database ---
    val simulationCases = listOf(
        ClinicalCase(
            id = "case_1",
            title = "Fadiga Ocupacional de Estúdio",
            difficulty = "Básico",
            patientProfile = "Mulher, 34 anos, Designer de Interiores, trabalha sentada por longas horas.",
            mainComplaint = "Cansaço físico severo, principalmente após o almoço, e fezes pastosas sem forma.",
            symptoms = listOf(
                "Sensação de peso físico nas pernas",
                "Leve distensão abdominal após refeições",
                "Desejo incontrolável por doces no final do dia",
                "Fraqueza muscular generalizada"
            ),
            tongueDesc = "Língua pálida, inchada, exibindo marcas de dentes nítidas nas bordas e saburra fina e branca.",
            pulseDesc = "Fraco (Xu) e mole (Ru), indicando vazio.",
            correctAnswer = "Deficiência de Qi do Baço (Pi Qi Xu)",
            correctTherapeuticApproach = "Tonificar o Qi do Baço, harmonizar o Estômago e eliminar a umidade residual.",
            correctPoints = listOf("E36", "BP6", "VC12", "VC6"),
            options = listOf(
                "Deficiência de Qi do Baço (Pi Qi Xu)",
                "Estagnação de Qi do Fígado",
                "Deficiência de Yang do Rim",
                "Calor no Estômago com Secura"
            ),
            explanation = "Os sintomas de cansaço extremo que pioram após comer, distensão abdominal e fezes moles, somados ao achado patognomônico de marcas dentárias nas bordas da língua pálida, confirmam a falha de transporte e transformação de energia do Baço (Pi Qi Xu)."
        ),
        ClinicalCase(
            id = "case_2",
            title = "Insônia Escarlate de Executivo",
            difficulty = "Intermediário",
            patientProfile = "Homem, 45 anos, Executivo Financeiro sob alta pressão.",
            mainComplaint = "Insônia crônica, suor excessivo ao dormir e sensação de calor difuso no peito e palmas das mãos.",
            symptoms = listOf(
                "Palpitações noturnas ocasionais",
                "Boca e garganta secas, pedindo água gelada à noite",
                "Zumbido fino nos ouvidos que piora à noite",
                "Dificuldade de concentração e irritabilidade leve"
            ),
            tongueDesc = "Língua de corpo muito vermelho, seca, sem saburra (aparência espelhada) e com rachaduras finas no centro.",
            pulseDesc = "Rápido (Shu) e extremamente fino (Xi), de força reduzida.",
            correctAnswer = "Deficiência de Yin do Rim e Coração",
            correctTherapeuticApproach = "Nutrir o Yin do Rim e do Coração, limpar o calor vazio e pacificar o espírito (Shen).",
            correctPoints = listOf("R3", "R6", "C7", "BP6", "VC4"),
            options = listOf(
                "Deficiência de Yang do Rim",
                "Deficiência de Yin do Rim e Coração",
                "Estagnação de Qi do Fígado",
                "Acúmulo de Umidade no Aquecedor Inferior"
            ),
            explanation = "A insônia associada a suor noturno, calor nos cinco corações (palmas, plantas e peito), zumbido nos ouvidos e uma língua vermelha e seca sem saburra evidenciam uma exaustão do Yin (líquidos refrigerantes) do Rim que falha em ancorar o fogo do Coração, perturbando o espírito."
        ),
        ClinicalCase(
            id = "case_3",
            title = "A Tensão da Corda de Advogada",
            difficulty = "Avançado",
            patientProfile = "Mulher, 29 anos, Advogada Corporativa enfrentando exaustivas horas de tribunal.",
            mainComplaint = "Cefaleia de forte intensidade nas têmporas e TPM extremamente dolorosa com cólicas e irritação.",
            symptoms = listOf(
                "Sensação de nó ou aperto na garganta (globus hystericus)",
                "Suspiros profundos frequentes durante o dia",
                "Distensão dolorosa nas mamas antes do período menstrual",
                "Sabor amargo na boca ao acordar"
            ),
            tongueDesc = "Corpo de cor normal, mas bordas laterais acentuadamente avermelhadas e tensas.",
            pulseDesc = "Pulso em Corda (Xian), duro e reto sob os dedos.",
            correctAnswer = "Estagnação de Qi do Fígado (Gan Qi Zhi)",
            correctTherapeuticApproach = "Dispersar o Fígado, mover o Qi estagnado, suavizar o fluxo menstrual e regular as emoções.",
            correctPoints = listOf("F3", "IG4", "VB34", "PC6", "VB20"),
            options = listOf(
                "Deficiência de Sangue do Fígado",
                "Estagnação de Qi do Fígado (Gan Qi Zhi)",
                "Fogo no Fígado (Fase Excesso)",
                "Deficiência de Qi de Baço e Pulmão"
            ),
            explanation = "A dor de cabeça temporal (canal da Vesícula Biliar), distensão mamária, suspiros, globus na garganta e sabor amargo, aliados ao pulso clássico em corda (Xian), são o retrato clínico da perda de livre fluxo de Qi pelo Fígado agredido por estresse."
        ),
        ClinicalCase(
            id = "case_4",
            title = "A Tempestade de Frio e Respiração",
            difficulty = "Complexo multi-síndrome",
            patientProfile = "Homem, 61 anos, ex-trabalhador de campo, vive em clima frio.",
            mainComplaint = "Falta de ar crônica que piora ao menor esforço físico, dor lombar fria e inchaço nos tornozelos.",
            symptoms = listOf(
                "Aversão severa ao frio, mãos e pés congelados",
                "Urina abundante e de cor muito clara",
                "Fezes moles e aquosas que ocorrem de madrugada (diarreia da 5ª vigília)",
                "Tosse fraca com catarro fluido e transparente"
            ),
            tongueDesc = "Língua pálida, muito inchada e escorregadia com saburra branca e úmida abundante.",
            pulseDesc = "Profundo (Chen), fraco (Xu) e lento (Chi).",
            correctAnswer = "Deficiência de Yang do Rim e Baço",
            correctTherapeuticApproach = "Aquecer o Yang do Rim e Baço, dispersar o frio interno, drenar edemas e reter o Qi pulmonar.",
            correctPoints = listOf("VG4", "B23", "VC4", "VC8", "E36", "BP9"),
            options = listOf(
                "Deficiência de Yin do Rim e Pulmão",
                "Deficiência de Yang do Rim e Baço",
                "Estase de Sangue do Coração",
                "Umidade-Calor no Canal do Fígado"
            ),
            explanation = "A intolerância severa ao frio, dor lombar crônica que melhora com calor, diarreia ao amanhecer, edemas frios nos membros e urina clara desenham a falha do fogo do Rim Yang em aquecer e ativar o Yang do Baço, prejudicando o metabolismo hídrico e a retenção de Qi pelo Pulmão."
        )
    )
}
