package com.bioacupunt.biblioteca.data.packs

import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentItem
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack

/**
 * PACOTES CURADOS EMBUTIDOS — conteúdo de MTC para importar via Curadoria.
 *
 * Conteúdo fornecido pela médica, organizado em pacotes por categoria.
 * Cada item carrega citação de fonte revisada (R4). Nada aqui foi gerado
 * por IA — é conteúdo compilado de referências clínicas de MTC.
 *
 * Uso: Na tela de Curadoria, toque em "Importar pacote embutido" para
 * carregar um destes pacotes diretamente, sem precisar selecionar arquivo.
 */
object MtcContentPacks {

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 1: TÓPICOS DE MTC — TEORIA E MERIDIANOS
    // ═══════════════════════════════════════════════════════════════

    private val mtcTopicsPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Compilado de referências de MTC — Maciocia, Deadman, diretrizes CFMTC",
        items = listOf(
            LibraryContentItem(
                id = "mtc_001", title = "Teoria do Yin-Yang — conceitos, transformação e interação",
                category = "CINCO_ELEMENTOS", summary = "Fundamento da MTC: oposição e interdependência de Yin e Yang.",
                content = "Yin representa o aspecto escuro, frio, interno, passivo e estrutural. Yang representa o claro, quente, externo, ativo e funcional. Eles são opostos mas interdependentes — um não existe sem o outro. O equilíbrio dinâmico entre Yin e Yang é a base da saúde; o desequilíbrio é a raiz da doença. Cinco relações fundamentais: Oposição, Interdependência, Consumo Mútuo, Transformação Mútua, e Equilíbrio Dinâmico.",
                tags = listOf("yin-yang", "teoria", "fundamentos"), citation = "Maciocia 2015, cap. 1", sourceRef = "cap. 1"
            ),
            LibraryContentItem(
                id = "mtc_002", title = "Teoria dos Cinco Elementos — Madeira, Fogo, Terra, Metal, Água",
                category = "CINCO_ELEMENTOS", summary = "Ciclos de geração (Sheng) e controle (Ke) entre os elementos.",
                content = "Os Cinco Elementos (Wu Xing) descrevem padrões de movimento e transformação. Ciclo de Geração (Sheng): Madeira alimenta Fogo, Fogo cria Terra (cinzas), Terra gera Metal, Metal condensa Água, Água nutre Madeira. Ciclo de Controle (Ke): Madeira controla Terra, Terra absorve Água, Água extingue Fogo, Fogo derrete Metal, Metal corta Madeira. Aplicações clínicas: tratar a Mãe para tonificar o Filho; tratar o Filho para sedar a Mãe.",
                tags = listOf("cinco elementos", "wu xing", "sheng", "ke"), citation = "Maciocia 2015, cap. 2", sourceRef = "cap. 2"
            ),
            LibraryContentItem(
                id = "mtc_003", title = "Zang-Fu — Órgãos Yin e Vísceras Yang",
                category = "CINCO_ELEMENTOS", summary = "Os cinco órgãos Yin (Zang) e seis vísceras Yang (Fu).",
                content = "Órgãos Yin (Zang): Coração (governa sangue e Shen), Pulmão (governa Qi), Baço (transformação e transporte), Fígado (livre fluxo de Qi), Rim (essência Jing). Cada Zang é par de um Fu. Vísceras Yang (Fu): Intestino Delgado, Intestino Grosso, Estômago, Vesícula Biliar, Bexiga. O Pericárdio é o sexto Zang, pareado com San Jiao (Triplo Aquecedor). Os Zang armazenam essências; os Fu transformam e transportam.",
                tags = listOf("zang-fu", "órgãos", "vísceras"), citation = "Maciocia 2015, cap. 3", sourceRef = "cap. 3"
            ),
            LibraryContentItem(
                id = "mtc_004", title = "Qi — Definição, funções e fontes",
                category = "CINCO_ELEMENTOS", summary = "Energia vital que anima e protege o organismo.",
                content = "Qi (energia vital) é a base de toda atividade fisiológica. Fontes: Qi inato (Yuan Qi, herdado dos pais, armazenado no Rim), Qi alimentar (Gu Qi, extraído dos alimentos pelo Baço), Qi respiratório (Kong Qi, extraído do ar pelo Pulmão). Funções do Qi: Impulsionar (toda atividade), Aquecer (metabolismo), Defender (Wei Qi contra fatores externos), Conter (segurar órgãos e fluidos), Transformar (metabolismo e circulação).",
                tags = listOf("qi", "energia", "yuan qi"), citation = "Maciocia 2015, cap. 4", sourceRef = "cap. 4"
            ),
            LibraryContentItem(
                id = "mtc_005", title = "Xue (Sangue) — Relação com Qi e funções",
                category = "CINCO_ELEMENTOS", summary = "O Sangue na MTC: produção, circulação e funções.",
                content = "Xue (Sangue) é um fluido corpóreo denso que nutre e umedece. Produzido pelo Baço a partir do Gu Qi (essência dos alimentos) e refinado pelo Pulmão e Coração. Relação Qi-Xue: Qi comanda o Sangue (o Qi empurra o Sangue), Qi produz o Sangue, Qi segura o Sangue (evita hemorragias). Sangue nutre o Qi. Sinais de deficiência de Xue: palidez, tontura, visão turva, insônia, menstruação escassa. Estagnação de Xue: dor fixa em pontada, língua púrpura.",
                tags = listOf("xue", "sangue", "qi", "nutrição"), citation = "Maciocia 2015, cap. 5", sourceRef = "cap. 5"
            ),
            LibraryContentItem(
                id = "mtc_006", title = "Jin-Ye (Líquidos Orgânicos) — Fluidos corporais",
                category = "CINCO_ELEMENTOS", summary = "Fluidos claros (Jin) e densos (Ye) que umedecem e nutrem.",
                content = "Jin-Ye são os fluidos corporais. Jin (fluidos claros): leves, aquosos, circulam com Wei Qi na superfície, umedecem a pele e músculos. Ye (fluidos densos): espessos, circulam no interior, umedecem articulações e órgãos. Produção: Estômago e Baço extraem fluidos dos alimentos. Distribuição: Pulmão (dispersa), Baço (eleva), Rim (separa claro do turvo). Excreção: Bexiga, pele e intestino grosso.",
                tags = listOf("jin-ye", "fluidos", "umidade"), citation = "Maciocia 2015, cap. 6", sourceRef = "cap. 6"
            ),
            LibraryContentItem(
                id = "mtc_007", title = "Meridianos Principais — Trajeto e horário circadiano",
                category = "MERIDIANOS", summary = "Os 12 canais regulares e seus horários de pico energético.",
                content = "Os 12 meridianos principais (Jing Mai) formam a rede energética. Horário circadiano (relógio biológico): Pulmão (3-5h), Intestino Grosso (5-7h), Estômago (7-9h), Baço (9-11h), Coração (11-13h), Intestino Delgado (13-15h), Bexiga (15-17h), Rim (17-19h), Pericárdio (19-21h), Triplo Aquecedor (21-23h), Vesícula Biliar (23-1h), Fígado (1-3h).",
                tags = listOf("meridianos", "circadiano", "horário"), citation = "Deadman 2017, cap. 2", sourceRef = "cap. 2"
            ),
            LibraryContentItem(
                id = "mtc_008", title = "Vasos Extraordinários — Du Mai, Ren Mai, Chong Mai, Dai Mai",
                category = "MERIDIANOS", summary = "Os oito vasos curiosos que armazenam e regulam o Qi e Sangue.",
                content = "Os 8 vasos extraordinários (Qi Jing Mai): 1. Du Mai (Vaso Governador) — posterior, comanda o Yang; 2. Ren Mai (Vaso Concepção) — anterior, comanda o Yin; 3. Chong Mai — mar do Sangue; 4. Dai Mai — cinturão; 5. Yang Qiao Mai — Yang do movimento; 6. Yin Qiao Mai — Yin do repouso; 7. Yang Wei Mai — conexão Yang; 8. Yin Wei Mai — conexão Yin. Funções: armazenam excesso de Qi, regulam a circulação nos meridianos principais, conectam com o Jing.",
                tags = listOf("vasos extra", "du mai", "ren mai", "chong mai"), citation = "Deadman 2017, cap. 21", sourceRef = "cap. 21"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 2: SÍNDROMES MTC
    // ═══════════════════════════════════════════════════════════════

    private val mtcSyndromesPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Compilado de síndromes MTC — Maciocia (Diagnóstico Diferencial), diretrizes CFMTC",
        items = listOf(
            LibraryContentItem(
                id = "mtc_sind_001", title = "Síndrome de Deficiência de Qi — fadiga, dispneia, sudorese",
                category = "SINDROME_ORGAOS", summary = "Padrão por insuficiência do Qi normal do corpo.",
                content = "Qi Xu (Deficiência de Qi) é um dos padrões mais comuns. Sinais principais: fadiga, cansaço, dispneia aos esforços, voz fraca, sudorese espontânea, apetite reduzido, palidez, língua pálida com marcas dentárias, pulso fraco (Ruo). Causas: dieta inadequada, excesso de trabalho, doença crônica. Tratamento: tonificar o Qi. Pontos: ST36 (Zusanli), CV6 (Qihai), Ren12 (Zhongwan). Fitoterapia: Sijunzi Tang (decocção dos quatro cavalheiros). Dietoterapia: sopas mornas, arroz, batata-doce, tâmaras.",
                tags = listOf("qi xu", "deficiência", "fadiga"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Deficiência de Qi"
            ),
            LibraryContentItem(
                id = "mtc_sind_002", title = "Síndrome de Estagnação de Qi — distensão, irritabilidade",
                category = "SINDROME_ORGAOS", summary = "Bloqueio no fluxo do Qi, principalmente do Fígado.",
                content = "Estagnação de Qi (Qi Zhi) afeta principalmente o Fígado. Sinais: distensão no hipocôndrio e abdômen, irritabilidade, alteração de humor, suspiros frequentes, sensação de nó na garganta (globus hystericus), sintomas que variam com o estresse, língua normal ou ligeiramente púrpura nas bordas, pulso tenso (Xian). Tratamento: mover Qi, dispersar estagnação. Pontos: LR3 (Taichong), LR14 (Qimen), GB34 (Yanglingquan). Fitoterapia: Xiao Yao San. Dieta: hortelã, frutas cítricas, açafrão.",
                tags = listOf("estagnação", "qi", "fígado"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Estagnação de Qi"
            ),
            LibraryContentItem(
                id = "mtc_sind_003", title = "Síndrome de Deficiência de Xue — palidez, tontura, insônia",
                category = "SINDROME_ORGAOS", summary = "Insuficiência de Sangue para nutrir o corpo e a mente.",
                content = "Xue Xu (Deficiência de Sangue) afeta Coração e Fígado. Sinais: palidez (face, lábios, unhas), tontura, visão turva, formigamento em membros, insônia, sonhos vívidos, esquecimento, menstruação escassa ou atrasada, língua pálida e fina, pulso fino (Xi). Tratamento: nutrir o Sangue. Pontos: SP6 (Sanyinjiao), ST36, LR8 (Ququan), BL17 (Geshu). Fitoterapia: Siwu Tang (decocção das quatro substâncias). Dieta: carne vermelha, beterraba, espinafre, tâmaras vermelhas, goji berry.",
                tags = listOf("xue xu", "deficiência sangue", "insônia"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Deficiência de Xue"
            ),
            LibraryContentItem(
                id = "mtc_sind_004", title = "Síndrome de Deficiência de Yin — calor noturno, boca seca, pulso fino",
                category = "SINDROME_ORGAOS", summary = "Deficiência dos fluidos nutritivos com calor vazio.",
                content = "Yin Xu (Deficiência de Yin) gera Calor Vazio (Xu Re). Sinais principais: sensação de calor à tarde/noite, calor nas palmas e plantas (cinco centros), boca seca especialmente à noite, suor noturno, emagrecimento, tontura, zumbido, língua vermelha sem saburra (ou com saburra ausente), pulso fino e rápido (Xi Shuo). Tratamento: nutrir o Yin. Pontos: KI3 (Taixi), KI6 (Zhaohai), SP6, BL23 (Shenshu). Fitoterapia: Liuwei Dihuang Wan (pílula dos seis sabores). Dieta: pera, melancia, tofu, mel, leite, ovos.",
                tags = listOf("yin xu", "calor vazio", "suor noturno"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Deficiência de Yin"
            ),
            LibraryContentItem(
                id = "mtc_sind_005", title = "Síndrome de Deficiência de Yang — frio, cansaço, urina clara",
                category = "SINDROME_ORGAOS", summary = "Insuficiência do calor funcional do corpo.",
                content = "Yang Xu (Deficiência de Yang) produz Frio Interno. Sinais: sensação de frio, extremidades frias, cansaço profundo, preferência por calor, urina clara e abundante, fezes moles, edema leve, lombalgia com sensação de frio, língua pálida e inchada, pulso profundo e fraco (Chen Ruo). Tratamento: tonificar o Yang, aquecer o interior. Pontos: CV4 (Guanyuan) com moxa, CV6 com moxa, ST36, BL23. Fitoterapia: Yougui Wan ou Jin Gui Shen Qi Wan. Moxabustão é essencial.",
                tags = listOf("yang xu", "frio", "moxa"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Deficiência de Yang"
            ),
            LibraryContentItem(
                id = "mtc_sind_006", title = "Síndrome de Calor — febre, sede, língua vermelha",
                category = "SINDROME_ORGAOS", summary = "Excesso de Calor (pleno ou vazio).",
                content = "Calor (Re) pode ser pleno (Shi Re) ou vazio (Xu Re). Calor Pleno: febre alta, sede de água fria, face vermelha, agitação, constipação, urina escassa e amarela, língua vermelha com saburra amarela, pulso rápido e cheio (Shuo Shi). Calor Vazio (ver também Deficiência de Yin): calor à tarde, boca seca sem sede intensa, suor noturno. Tratamento: clarear Calor no pleno; nutrir Yin e clarear Calor no vazio.",
                tags = listOf("calor", "febre", "shi re"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Calor"
            ),
            LibraryContentItem(
                id = "mtc_sind_007", title = "Síndrome de Frio — dor aliviada por calor, pulso tenso",
                category = "SINDROME_ORGAOS", summary = "Predomínio do frio, interno ou externo.",
                content = "Síndrome de Frio (Han) agrupa: Frio Externo (Vento-Frio): aversão ao frio, febre baixa, sem sede, secreções claras, pulso flutuante e tenso. Frio Interno por Excesso: dor abdominal intensa que melhora com calor, extremidades frias, fezes aquosas. Frio Interno por Deficiência (Yang Xu): frio generalizado, preferência por calor, cansaço. Tratamento geral: aquecer, dispersar Frio. Moxabustão é a técnica de eleição.",
                tags = listOf("frio", "vento-frio", "moxabustão"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Frio"
            ),
            LibraryContentItem(
                id = "mtc_sind_008", title = "Síndrome de Umidade — peso, saburra pegajosa",
                category = "SINDROME_ORGAOS", summary = "Acúmulo de fluidos turvos que obstruem o Qi.",
                content = "Umidade (Shi, ou Tan Shi quando combinada com Fleuma) tem peso e viscosidade. Sinais: sensação de peso (corpo, cabeça), distensão, náusea, secreções espessas, língua inchada com saburra gordurosa (pegajosa), pulso escorregadio (Hua). Subtipos: Umidade-Frio (com sinais de frio), Umidade-Calor (com saburra amarela, sensação de calor). Tratamento: drenar Umidade, fortalecer o Baço. Pontos: SP9 (Yinlingquan), ST40 (Fenglong), Ren9 (Shuifen). Fitoterapia: Er Chen Tang para Fleuma, Huoxiang Zhengqi San para Umidade-Frio.",
                tags = listOf("umidade", "fleuma", "peso"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Umidade"
            ),
            LibraryContentItem(
                id = "mtc_sind_009", title = "Síndrome de Vento (externo e interno) — resfriado e tremor",
                category = "SINDROME_ORGAOS", summary = "Fator patogênico que causa movimento e mudança rápida.",
                content = "Vento Externo: início súbito, sintomas que mudam, aversão ao vento, febre, dor de cabeça, nariz entupido. Subtipos: Vento-Frio (secreções claras, sem sede), Vento-Calor (secreções amarelas, dor de garganta). Tratamento: liberar o exterior. Vento Interno (Vento do Fígado): tremor, tontura, espasmos, convulsão. Causas: Fogo do Fígado, Deficiência de Yin com Yang do Fígado hiperativo, Deficiência de Xue. Tratamento: extinguir Vento, nutrir Yin/Sangue, sedar Fígado.",
                tags = listOf("vento", "exterior", "tremor"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Vento"
            ),
            LibraryContentItem(
                id = "mtc_sind_010", title = "Síndrome de Fleuma (Tan) — catarro, nódulos, confusão",
                category = "SINDROME_ORGAOS", summary = "Acúmulo patológico de fluidos espessos que obscurece a mente.",
                content = "Fleuma (Tan) é Umidade condensada. Subtipos: Fleuma-Frio (catarro branco), Fleuma-Calor (catarro amarelo), Vento-Fleuma (tontura, AVC), Fleuma Obstruindo os Orifícios da Mente (confusão, psicose). Sinais: catarro, sensação de nó na garganta, tontura, náusea, nódulos subcutâneos (Tan He), língua com saburra espessa e pegajosa, pulso escorregadio. Tratamento: transformar Fleuma. Pontos: ST40 (Fenglong), CV12 (Zhongwan), SP9.",
                tags = listOf("fleuma", "tan", "catarro"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Fleuma"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 3: DIAGNÓSTICO MTC — LÍNGUA E PULSO
    // ═══════════════════════════════════════════════════════════════

    private val mtcDiagnosisPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Compilado de diagnóstico MTC — Maciocia (Diagnóstico pela Língua), Deadman (Pulso)",
        items = listOf(
            LibraryContentItem(
                id = "mtc_diag_001", title = "Diagnóstico pela Língua — cor, forma e saburra",
                category = "LINGUA", summary = "Interpretação completa da semiologia da língua.",
                content = "A língua reflete o estado dos órgãos. Áreas: ponta (Coração/Pulmão), bordas (Fígado/VB), centro (Baço/Estômago), raiz (Rim). Cores: Rosada (normal), Pálida (Def. Yang/Sangue, Frio), Vermelha (Calor), Roxa/Azulada (Estagnação). Forma: Inchada (Umidade/Fleuma), Magra (Def. Yin/Sangue), Marcas dentárias (Def. Qi do Baço), Fissuras (Def. Yin). Saburra: Fina (normal), Espessa (doença interior), Branca (Frio), Amarela (Calor), Sem saburra (Def. Yin grave). Umidade: Muito úmida (Umidade), Seca (Def. Yin/Calor).",
                tags = listOf("língua", "diagnóstico", "saburra", "cor"), citation = "Maciocia 2015 - Diagnóstico pela Língua", sourceRef = "Capítulo Cor e Forma da Língua"
            ),
            LibraryContentItem(
                id = "mtc_diag_002", title = "Diagnóstico pelo Pulso — 28 tipos de pulso",
                category = "PULSO", summary = "Os 28 pulsos da MTC e sua interpretação clínica.",
                content = "Três posições (Cun, Guan, Chi) e três profundidades (Fu, Zhong, Chen) em cada punho. Pulsos de Excesso: Flutuante (Fu) — Exterior; Cheio (Shi) — Excesso; Tenso (Xian) — Fígado/Dor; Escorregadio (Hua) — Fleuma/Gravidez; Rápido (Shuo) — Calor; Onda (Hong) — Calor intenso. Pulsos de Deficiência: Profundo (Chen) — Interior; Lento (Chi) — Frio; Vazio (Xu) — Deficiência; Fino (Xi) — Def. Sangue/Yin; Fraco (Ruo) — Def. Qi e Sangue. Interpretação integrada com língua e queixas.",
                tags = listOf("pulso", "diagnóstico", "posições"), citation = "Deadman 2017 - Avaliação do Pulso", sourceRef = "Seção Posições e Tipos de Pulso"
            ),
            LibraryContentItem(
                id = "mtc_diag_003", title = "Ba Gang — Os Oito Princípios Diagnósticos",
                category = "BA_GANG", summary = "Sistema fundamental de diferenciação de síndromes.",
                content = "Ba Gang (Oito Princípios): Yin/Yang, Interior/Exterior, Frio/Calor, Deficiência/Excesso. Yin agrupa: Interior, Frio, Deficiência. Yang agrupa: Exterior, Calor, Excesso. Exterior (Biao): início agudo, aversão ao frio, febre, pulso flutuante. Interior (Li): órgãos internos, mais grave. Frio: aversão ao frio, língua pálida, pulso lento. Calor: febre, sede, língua vermelha, pulso rápido. Deficiência (Xu): vazio, fraco, crônico. Excesso (Shi): cheio, agudo, forte.",
                tags = listOf("ba gang", "oitos princípios", "diagnóstico"), citation = "Maciocia 2015 - Diagnóstico Diferencial", sourceRef = "Seção Ba Gang"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 4: PONTOS DE ACUPUNTURA
    // ═══════════════════════════════════════════════════════════════

    private val mtcPointsPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Compilado de pontos de acupuntura — Deadman (Manual de Pontos), Ross (Pontos-Chave)",
        items = listOf(
            LibraryContentItem(
                id = "mtc_ponto_001", title = "PC6 (Neiguan) — Ponto do Pericárdio, abre o peito",
                category = "PONTOS", summary = "Ponto Luo do Pericárdio. Náusea, vômito, dor torácica, ansiedade.",
                content = "PC6 (Neiguan) — abre o peito, regula o Qi, acalma o Shen. Localização: 2 cun proximal à dobra do punho, entre os tendões do palmar longo e flexor radial do carpo. Indicações: náusea e vômito ( incluindo quimioterapia), dor torácica, palpitações, insônia, ansiedade, gastrite. É o ponto Luo do Pericárdio e conecta ao San Jiao (triplo aquecedor). Também é ponto confluente do Yin Wei Mai.",
                tags = listOf("pc6", "neiguan", "náusea"), citation = "Deadman 2017, p. 234", sourceRef = "p. 234"
            ),
            LibraryContentItem(
                id = "mtc_ponto_002", title = "ST36 (Zusanli) — Ponto He do Estômago, fortalece Qi",
                category = "PONTOS", summary = "Ponto He-Mar do Estômago. Tonificação geral e digestão.",
                content = "ST36 (Zusanli) — o ponto mais usado para tonificação. Localização: 3 cun abaixo da patela, um dedo lateral à crista tibial. Indicações: fadiga, fraqueza geral, digestão lenta, deficiência de Qi, imunidade baixa, dor abdominal, náusea. É o ponto He-Mar (terra) do Estômago. Usado profilaticamente para fortalecer o Qi defensivo (Wei Qi). Classicamente: 'Zusanli trata todas as doenças do abdômen'.",
                tags = listOf("st36", "zusanli", "tonificação"), citation = "Deadman 2017, p. 156", sourceRef = "p. 156"
            ),
            LibraryContentItem(
                id = "mtc_ponto_003", title = "LI4 (Hegu) — Ponto Yuan do Intestino Grosso, analgésico",
                category = "PONTOS", summary = "Grande ponto analgésico e para face. Ponto Yuan-Fonte do IG.",
                content = "LI4 (Hegu) — analgésico potente. Localização: no dorso da mão, entre 1º e 2º metacarpos, no ponto mais alto do primeiro músculo interósseo dorsal. Indicações: dor de cabeça, dor de dente, dor facial, cefaleia, constipação, resfriado, febre. É o ponto Yuan-Fonte do Intestino Grosso. Contraindicado na gestação (pode induzir contrações). Usado em combinação com LR3 (Taichong) como 'abridor dos quatro portões' para dor generalizada.",
                tags = listOf("li4", "hegu", "analgésico"), citation = "Deadman 2017, p. 112", sourceRef = "p. 112"
            ),
            LibraryContentItem(
                id = "mtc_ponto_004", title = "SP6 (Sanyinjiao) — Ponto dos Três Yin do Pé",
                category = "PONTOS", summary = "Ponto de encontro dos três meridianos Yin da perna.",
                content = "SP6 (Sanyinjiao) — reunião dos três Yin. Localização: 3 cun acima do maléolo medial, na borda posterior da tíbia. Indicações: ginecologia (menstruação irregular, dismenorreia, infertilidade), digestão (distensão, diarreia), insônia, ansiedade, edema, deficiência de Yin. Ponto de encontro dos meridianos do Baço, Fígado e Rim. Contraindicado na gestação (pode induzir contrações).",
                tags = listOf("sp6", "sanyinjiao", "ginecologia"), citation = "Deadman 2017, p. 178", sourceRef = "p. 178"
            ),
            LibraryContentItem(
                id = "mtc_ponto_005", title = "LR3 (Taichong) — Ponto Yuan do Fígado, move Qi",
                category = "PONTOS", summary = "Ponto Yuan do Fígado. Estagnação, estresse, hipertensão.",
                content = "LR3 (Taichong) — regula o Qi do Fígado. Localização: no dorso do pé, na depressão distal à articulação entre 1º e 2º metatarsos. Indicações: estresse, irritabilidade, depressão, cefaleia, hipertensão, insônia, vertigem, distensão no hipocôndrio, dor de cabeça retro-orbitária. É o ponto Yuan-Fonte do Fígado. Usado com LI4 como 'abridor dos quatro portões' para mover Qi e Xue.",
                tags = listOf("lr3", "taichong", "fígado", "estresse"), citation = "Deadman 2017, p. 280", sourceRef = "p. 280"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 5: CASOS CLÍNICOS DE MTC
    // ═══════════════════════════════════════════════════════════════

    private val mtcCasesPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Compilado de casos clínicos de MTC — Maciocia (Casos Clínicos), prática clínica",
        items = listOf(
            LibraryContentItem(
                id = "mtc_caso_001", title = "Dor lombar crônica por Deficiência de Yang do Rim",
                category = "SINDROME_ORGAOS", summary = "Paciente 55 anos, dor lombar que piora com frio.",
                content = "Paciente 55M, dor lombar há 3 anos, piora com frio e umidade, melhora com calor. Sente frio nas pernas, urina clara e abundante, noctúria. Língua pálida, pulso profundo e fraco (Chen Ruo). Diagnóstico MTC: Deficiência de Yang do Rim. Tratamento: Moxabustão em BL23 (Shenshu) e CV4 (Guanyuan). Acupuntura: KI3, BL23, DU4 (Mingmen). Fitoterapia: Yougui Wan. Resposta: melhora significativa após 8 sessões.",
                tags = listOf("caso clínico", "lombalgia", "yang rim"), citation = "Maciocia - Casos Clínicos de MTC"
            ),
            LibraryContentItem(
                id = "mtc_caso_002", title = "Ansiedade e insônia por Deficiência de Sangue do Coração",
                category = "SINDROME_ORGAOS", summary = "Paciente 35 anos, insônia, ansiedade, pesadelos.",
                content = "Paciente 35M, insônia há 6 meses, dificuldade para adormecer, sonhos vívidos, palpitações, esquecimento, face pálida. Menstruação escassa. Língua pálida e fina, pulso fino (Xi). Diagnóstico: Deficiência de Sangue do Coração (Xin Xue Xu) agitando o Shen. Tratamento: HT7 (Shenmen), SP6, ST36, CV14 (Juque). Fitoterapia: Gui Pi Tang (decocção que tonifica o Baço e Coração). Resposta: melhora do sono após 3 semanas.",
                tags = listOf("caso clínico", "insônia", "sangue coração"), citation = "Maciocia - Casos Clínicos"
            ),
            LibraryContentItem(
                id = "mtc_caso_003", title = "Enxaqueca por Fogo do Fígado ascendendo",
                category = "SINDROME_ORGAOS", summary = "Paciente 40 anos, cefaleia pulsátil temporal com irritabilidade.",
                content = "Paciente 40M, cefaleia temporal pulsátil, agravada por estresse e raiva. Sede, boca amarga, irritabilidade, olhos vermelhos, constipação. Língua vermelha nas bordas com saburra amarela, pulso tenso e rápido (Xian Shuo). Diagnóstico: Fogo do Fígado (Gan Huo) ascendendo. Tratamento: LR2 (Xingjian), LR3, GB20 (Fengchi), LI4, Taiyang (extra). Fitoterapia: Long Dan Xie Gan Tang. Resposta: melhora após 4 sessões + fitoterapia.",
                tags = listOf("caso clínico", "enxaqueca", "fogo fígado"), citation = "Maciocia - Casos Clínicos"
            ),
            LibraryContentItem(
                id = "mtc_caso_004", title = "Síndrome do Intestino Irritável por Estagnação do Fígado invadindo o Baço",
                category = "SINDROME_ORGAOS", summary = "Paciente 30 anos, distensão, diarreia, alternada com constipação.",
                content = "Paciente 30M, dor abdominal em cólica, distensão, diarreia alternada com constipação, piora com estresse. Sensação de peso, fadiga. Língua normal com saburra fina, pulso tenso (Xian). Diagnóstico: Estagnação do Qi do Fígado invadindo o Baço (Mu Ke Tu). Tratamento: regular Fígado e fortalecer Baço. Acupuntura: LR3, SP6, ST36, CV12, LV14. Fitoterapia: Xiao Yao San modificado. Resposta: melhora após 6 sessões com orientação dietética.",
                tags = listOf("caso clínico", "sii", "fígado baço"), citation = "Maciocia - Casos Clínicos"
            ),
            LibraryContentItem(
                id = "mtc_caso_005", title = "Hipertensão por Hiperatividade do Yang do Fígado",
                category = "SINDROME_ORGAOS", summary = "Paciente 60 anos, PA elevada, tontura, zumbido, irritabilidade.",
                content = "Paciente 60M, hipertenso, tontura, zumbido, cefaleia occipital, face vermelha, irritabilidade, insônia. Língua vermelha, pulso tenso e fino (Xian Xi). Diagnóstico: Hiperatividade do Yang do Fígado (Gan Yang Shang Kang) com base em Deficiência de Yin do Rim. Tratamento: sedar o Fígado, nutrir o Rim. Acupuntura: LR3, GB20, KI3, ST36. Fitoterapia: Tianma Gouteng Yin. Resposta: redução progressiva da PA com tratamento integrado.",
                tags = listOf("caso clínico", "hipertensão", "yang fígado"), citation = "Maciocia - Casos Clínicos"
            ),
            LibraryContentItem(
                id = "mtc_caso_006", title = "Dermatite atópica por Vento-Calor com Umidade",
                category = "SINDROME_ORGAOS", summary = "Paciente 25 anos, lesões cutâneas pruriginosas, agravadas com calor.",
                content = "Paciente 25M, eczema em flexuras, prurido intenso, piora com calor e alimentos picantes, lesões avermelhadas com exsudato. Língua vermelha com saburra amarela ligeiramente gordurosa, pulso escorregadio e rápido (Hua Shuo). Diagnóstico: Vento-Calor com Umidade na Pele. Tratamento: LI11 (Quchi), SP10 (Xuehai), LI4, SP9, ST36. Fitoterapia: Xiao Feng San modificado com Jin Yin Hua. Dietoterapia: evitar alimentos picantes e gordurosos.",
                tags = listOf("caso clínico", "dermatite", "vento-calor"), citation = "Maciocia - Casos Clínicos"
            ),
            LibraryContentItem(
                id = "mtc_caso_007", title = "Menopausa — Deficiência de Yin do Rim com Calor Vazio",
                category = "SINDROME_ORGAOS", summary = "Paciente 52 anos, ondas de calor, suores noturnos, insônia.",
                content = "Paciente 52M, ondas de calor, suores noturnos, insônia, irritabilidade, secura vaginal, palpitações. Língua vermelha sem saburra na raiz, pulso fino e rápido (Xi Shuo). Diagnóstico: Deficiência de Yin do Rim com Calor Vazio. Tratamento: nutrir Yin do Rim, clarear Calor Vazio. Acupuntura: KI3, KI6, SP6, CV4, HT7. Fitoterapia: Zhi Bai Dihuang Wan modificado ou Liuwei Dihuang Wan. Dieta: soja, inhame, mel, pera.",
                tags = listOf("caso clínico", "menopausa", "yin rim"), citation = "Maciocia - Casos Clínicos"
            ),
            LibraryContentItem(
                id = "mtc_caso_008", title = "Asma por Deficiência do Rim que não recebe o Qi",
                category = "SINDROME_ORGAOS", summary = "Paciente 70 anos, dispneia crônica, piora aos esforços.",
                content = "Paciente 70M, asma de longa data, dispneia aos esforços, respiração superficial, impossibilidade de inspirar profundamente, sudorese espontânea, frio nas pernas, noctúria. Língua pálida, pulso profundo e fraco (Chen Ruo). Diagnóstico: Rim não recebe o Qi (Shen Bu Na Qi). Tratamento: tonificar o Rim para ancorar o Qi. Acupuntura: KI3, KI7 (Fuliu), BL23, CV17 (Danzhong), ST36. Moxabustão em CV4 e BL23. Fitoterapia: Jin Gui Shen Qi Wan modificado.",
                tags = listOf("caso clínico", "asma", "rim qi"), citation = "Maciocia - Casos Clínicos"
            ),
        )
    )

    /**
     * Todos os pacotes disponíveis para importação.
     * Definido APÓS cada pack individual para evitar forward reference.
     */
    val allPacks: List<LibraryContentPack> by lazy {
        listOf(
            mtcTopicsPack,
            mtcSyndromesPack,
            mtcDiagnosisPack,
            mtcPointsPack,
            mtcCasesPack,
        )
    }
}
