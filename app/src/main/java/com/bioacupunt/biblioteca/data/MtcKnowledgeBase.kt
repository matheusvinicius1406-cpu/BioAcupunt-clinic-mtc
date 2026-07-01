package com.bioacupunt.biblioteca.data

import com.bioacupunt.biblioteca.domain.model.MtcArticle

object MtcKnowledgeBase {

    val articles: List<MtcArticle> = listOf(

        // MERIDIANOS
        MtcArticle(
            id = "mer_pulmao",
            title = "Meridiano do Pulmão (Shou Tai Yin)",
            category = "MERIDIANOS",
            summary = "Meridiano Yin da Mão com 11 pontos. Governa o Qi e a respiração.",
            content = """
# Meridiano do Pulmão (手太阴肺经 - Shǒu Tàiyīn Fèijīng)

## Classificação
- **Tipo:** Yin da Mão (Tai Yin)
- **Número de pontos:** 11 (P1 a P11)
- **Horário de pico:** 3h–5h (hora do Tigre)
- **Órgão par:** Intestino Grosso

## Trajeto
Origina-se no Jiao Médio (estômago), desce e conecta-se com o Intestino Grosso, retorna, atravessa o diafragma, pertence ao Pulmão, emerge pela axila, percorre o braço medialmente até o polegar (P11 - Shaoshang).

## Funções Principais
1. **Governa o Qi e a Respiração** (主气，司呼吸): Controla a inalação do Qi do céu
2. **Controla os canais e vasos sanguíneos**: Distribui o Qi pelo corpo
3. **Regula as vias d'água**: Propaga os fluidos pelo organismo
4. **Governa a pele e pelos**: Nutre a superfície corporal
5. **Abre para o nariz**: Reflete a saúde pulmonar

## Pontos Principais
| Ponto | Nome | Localização | Função Principal |
|-------|------|-------------|-----------------|
| P1 | Zhongfu (中府) | 1º EIC, 6 cun lateral à linha média | Mu do Pulmão, reúne o Qi |
| P5 | Chize (尺泽) | Dobra do cotovelo, lado radial | Tonifica o Yin do Pulmão |
| P7 | Lieque (列缺) | 1,5 cun acima do punho | Luo-conexão, comanda cabeça e pescoço |
| P9 | Taiyuan (太渊) | Pulso radial | Yuan-fonte, tonifica Qi e Yin do Pulmão |
| P10 | Yuji (鱼际) | Eminência tenar | Claro calor do Pulmão |
| P11 | Shaoshang (少商) | Ângulo ungueal do polegar | Jing-Poço, desobstrui garganta |

## Síndromes Comuns
- **Deficiência de Qi do Pulmão**: Tosse fraca, voz fraca, dispneia, fadiga
- **Deficiência de Yin do Pulmão**: Tosse seca, hemoptise, suor noturno
- **Vento-Frio invasor**: Tosse com fleuma clara, nariz entupido
- **Vento-Calor invasor**: Tosse com fleuma amarela, febre, dor de garganta
            """.trimIndent(),
            tags = listOf("pulmão", "meridiano", "tai yin", "qi", "respiração")
        ),

        MtcArticle(
            id = "mer_coracao",
            title = "Meridiano do Coração (Shou Shao Yin)",
            category = "MERIDIANOS",
            summary = "O Imperador dos órgãos. 9 pontos. Governa o Shen (Espírito) e o Sangue.",
            content = """
# Meridiano do Coração (手少阴心经 - Shǒu Shàoyīn Xīnjīng)

## Classificação
- **Tipo:** Yin da Mão (Shao Yin)
- **Número de pontos:** 9 (C1 a C9)
- **Horário de pico:** 11h–13h (hora do Cavalo)
- **Órgão par:** Intestino Delgado

## Funções Principais
1. **Governa o Sangue e os vasos**: Propulsão do sangue
2. **Armazena o Shen**: Mente, consciência, cognição, sono
3. **Manifesta-se na face**: Complexão facial reflete o estado do Coração
4. **Abre para a língua**: Ponta da língua reflete o Coração

## Pontos Principais
| Ponto | Nome | Localização | Função |
|-------|------|-------------|--------|
| C3 | Shaohai (少海) | Dobra medial do cotovelo | He-mar, acalma o Shen |
| C7 | Shenmen (神门) | Punho, lado ulnar | Yuan-fonte, tonifica Coração, acalma Shen |
| C8 | Shaofu (少府) | 4º/5º metacarpo | Claro calor do Coração |
| C9 | Shaochong (少冲) | Ângulo ungueal do 5º dedo | Jing-Poço, ressuscita, refresca calor |

## Síndromes
- **Deficiência de Qi do Coração**: Palpitações, dispneia, fadiga
- **Deficiência de Yang do Coração**: Palpitações, frio, extremidades frias, palidez
- **Deficiência de Sangue do Coração**: Palpitações, insônia, ansiedade, palidez
- **Deficiência de Yin do Coração**: Palpitações, suor noturno, sensação de calor à tarde
- **Fogo do Coração**: Aftas, agitação, sede, palpitações, urina amarela escura
            """.trimIndent(),
            tags = listOf("coração", "shen", "sangue", "insônia", "espírito")
        ),

        MtcArticle(
            id = "mer_figado",
            title = "Meridiano do Fígado (Zu Jue Yin)",
            category = "MERIDIANOS",
            summary = "14 pontos. Promove o fluxo livre do Qi. Fundamental nas consultas de MTC.",
            content = """
# Meridiano do Fígado (足厥阴肝经 - Zú Juéyīn Gānjīng)

## Classificação
- **Tipo:** Yin do Pé (Jue Yin)
- **Número de pontos:** 14 (F1 a F14)
- **Horário de pico:** 1h–3h (hora do Boi)
- **Órgão par:** Vesícula Biliar

## Funções Principais
1. **Promove o fluxo livre do Qi** (疏泄): Função mais importante do Fígado
2. **Armazena o Sangue**: Regula o volume sanguíneo; nutre tendões e unhas
3. **Controla os tendões**: Movimento suave e coordenado
4. **Abre para os olhos**: Visão reflete o estado do Fígado
5. **Manifesta-se nas unhas**: Estado das unhas reflete o Fígado

## Pontos Principais
| Ponto | Nome | Localização | Função |
|-------|------|-------------|--------|
| F2 | Xingjian (行间) | Entre 1º e 2º pododáctilos | Drena fogo do Fígado |
| F3 | Taichong (太冲) | Entre 1º e 2º metatarsos | Yuan-fonte, regula Qi e Sangue |
| F4 | Zhongfeng (中封) | Face medial do tornozelo | Drena umidade-calor |
| F8 | Ququan (曲泉) | Dobra medial do joelho | He-mar, tonifica Yin e Sangue do Fígado |
| F13 | Zhangmen (章门) | Extremidade da 11ª costela | Mu do Baço, Influência dos órgãos Zang |
| F14 | Qimen (期门) | 6º EIC, linha do mamilo | Mu do Fígado, regula Qi do Fígado |

## Síndromes
- **Estagnação de Qi do Fígado**: Distensão no hipocôndrio, depressão, irritabilidade, disforia
- **Hiperatividade do Yang do Fígado**: Cefaleia, vertigem, irritabilidade, hipertensão
- **Fogo do Fígado**: Olhos vermelhos, cefaleia intensa, boca amarga, constipação
- **Deficiência de Sangue do Fígado**: Visão turva, espasmos musculares, menstruação escassa
- **Umidade-Calor no Fígado/VB**: Icterícia, prurido genital, leucorreia amarela
            """.trimIndent(),
            tags = listOf("fígado", "qi", "estagnação", "taichong", "depressão", "tensão")
        ),

        // CINCO ELEMENTOS
        MtcArticle(
            id = "cinco_elem",
            title = "Teoria dos Cinco Elementos (Wu Xing)",
            category = "CINCO_ELEMENTOS",
            summary = "Madeira, Fogo, Terra, Metal, Água. A base da cosmovisão MTC e suas correspondências.",
            content = """
# Teoria dos Cinco Elementos (五行 - Wǔ Xíng)

## Conceito Fundamental
Os Cinco Elementos descrevem os cinco padrões fundamentais de movimento e transformação na natureza e no corpo humano. Não são elementos químicos, mas categorias funcionais.

## Tabela de Correspondências

| Elemento | Madeira | Fogo | Terra | Metal | Água |
|----------|---------|------|-------|-------|------|
| Órgão Yin (Zang) | Fígado | Coração | Baço-Pâncreas | Pulmão | Rim |
| Órgão Yang (Fu) | Vesícula Biliar | Int. Delgado | Estômago | Int. Grosso | Bexiga |
| Sentido | Visão | Fala | Gosto | Olfato | Audição |
| Emoção | Raiva/Frustração | Alegria/Agitação | Preocupação | Tristeza/Pesar | Medo |
| Cor | Verde | Vermelho | Amarelo | Branco | Preto/Azul |
| Estação | Primavera | Verão | Transição | Outono | Inverno |
| Clima | Vento | Calor | Umidade | Secura | Frio |
| Tecido | Tendões | Vasos | Músculos | Pele/Pelos | Ossos |
| Sabor | Ácido | Amargo | Doce | Picante | Salgado |
| Direção | Leste | Sul | Centro | Oeste | Norte |
| Som | Grito | Riso | Canto | Choro | Gemido |

## Ciclo de Geração (Sheng - 相生)
Madeira → Fogo → Terra → Metal → Água → Madeira

*"A Madeira alimenta o Fogo; o Fogo cria cinzas (Terra); a Terra contém o Metal; o Metal condensa a água; a Água nutre a Madeira"*

## Ciclo de Controle (Ke - 相克)
Madeira → Terra → Água → Fogo → Metal → Madeira

*"A Madeira penetra a Terra; a Terra absorve a Água; a Água extingue o Fogo; o Fogo derrete o Metal; o Metal corta a Madeira"*

## Aplicações Clínicas

### Relação Mãe-Filho (母子关系)
Quando um órgão está deficiente, pode "roubar" energia da Mãe ou não nutrir o Filho.

**Exemplo:** Deficiência do Rim (Água) não nutre o Fígado (Madeira) → Deficiência de Yin do Fígado

### Insulto Inverso (相乘 e 相侮)
- **Cheng (乘):** Um elemento controla em excesso o controlado
- **Wu (侮):** Um elemento fraco "instiga" seu controlador

## Na Prática Clínica
- **Tratar a Mãe para tonificar o Filho**: Tonificar Rim (Água) para tonificar Fígado (Madeira)
- **Tratar o Filho para sedar a Mãe**: Sedar Coração (Fogo) para reduzir excesso de Fígado (Madeira)
            """.trimIndent(),
            tags = listOf("cinco elementos", "wu xing", "sheng", "ke", "correspondências", "teoria")
        ),

        // BA GANG
        MtcArticle(
            id = "ba_gang_intro",
            title = "Ba Gang - Os Oito Princípios Diagnósticos",
            category = "BA_GANG",
            summary = "Yin/Yang, Interior/Exterior, Frio/Calor, Deficiência/Excesso. A base diagnóstica da MTC.",
            content = """
# Ba Gang (八纲 - Bā Gāng) — Os Oito Princípios

## Introdução
Os Oito Princípios são o sistema fundamental de diferenciação de síndromes na MTC. Todo diagnóstico deve ser elaborado considerando esses oito parâmetros.

## Os Quatro Pares

### 1. Yin (阴) e Yang (阳)
O par mais abrangente — os outros seis são manifestações de Yin e Yang.

| Yin | Yang |
|-----|------|
| Interior | Exterior |
| Frio | Calor |
| Deficiência | Excesso |
| Estático | Dinâmico |
| Crônico | Agudo |

### 2. Interior (里 Lǐ) e Exterior (表 Biǎo)
**Exterior (Biao):** Afeta pele, músculos, canais. Geralmente agudo, causado por fatores patogênicos externos.
- Sinais: Aversão ao frio, febre, dor de cabeça, pulso superficial

**Interior (Li):** Afeta os órgãos internos (Zang Fu). Mais grave e crônico.
- Sinais: Sem aversão ao frio, sintomas digestivos, emocionais, pulso profundo

### 3. Frio (寒 Hán) e Calor (热 Rè)
**Frio:**
- Língua: Pálida com saburra branca
- Pulso: Lento (Chi)
- Sinais: Aversão ao frio, membros frios, fezes moles, urina clara e abundante

**Calor:**
- Língua: Vermelha com saburra amarela
- Pulso: Rápido (Shuo)
- Sinais: Febre, sede de bebidas frias, constipação, urina amarela escura

### 4. Deficiência (虚 Xū) e Excesso (实 Shí)
**Deficiência (Xu):** Insuficiência do correto Qi
- Pulso: Fraco, fino, vazio
- Sinais: Voz fraca, fadiga, suor espontâneo, palidez

**Excesso (Shi):** Abundância do patogênico Qi
- Pulso: Forte, cheio
- Sinais: Voz forte, dor que piora com pressão, distensão

## Combinações Clínicas Frequentes
- **Exterior-Frio-Excesso**: Resfriado por vento-frio
- **Interior-Calor-Excesso**: Infecção bacteriana intensa
- **Interior-Frio-Deficiência**: Yang do Baço deficiente
- **Interior-Calor-Deficiência**: Yin do Rim deficiente com calor vazio

## Integração com Semiologia
Utilize a língua, o pulso, a cor da face, os sons e as emoções para confirmar os Ba Gang antes de chegar ao diagnóstico final.
            """.trimIndent(),
            tags = listOf("ba gang", "oito princípios", "diagnóstico", "yin yang", "frio calor", "deficiência")
        ),

        // SEMIOLOGIA LÍNGUA
        MtcArticle(
            id = "semio_lingua",
            title = "Semiologia da Língua na MTC",
            category = "LINGUA",
            summary = "Como ler a língua: corpo, saburra, cor, forma e umidade para diagnóstico.",
            content = """
# Semiologia da Língua (舌诊 - Shé Zhěn)

## Áreas Topográficas
```
    PONTA (Coração / Pulmão)
   ┌─────────────────────┐
   │   BORDAS (Fígado/VB)│
   │                     │
   │    CENTRO (Baço/Est)│
   │                     │
   └─────────────────────┘
    RAIZ (Rim / Int. Grosso)
```

## Corpo da Língua

### Cores
| Cor | Significado |
|-----|-------------|
| Rosada (normal) | Estado saudável |
| Pálida | Deficiência de Yang/Sangue, Frio |
| Vermelha | Calor (pleno ou vazio) |
| Vermelho escuro (Crimson) | Calor intenso nos órgãos Ying/Xue |
| Roxa/Azulada | Estagnação de Qi/Sangue |
| Azul-escura | Frio intenso com estagnação |

### Forma
| Forma | Significado |
|-------|-------------|
| Inchada, gordurosa | Umidade, Fleuma |
| Magra, fina | Deficiência de Sangue/Yin |
| Com fissuras | Deficiência de Yin, calor intenso |
| Com marcas dentárias | Deficiência de Qi do Baço, Umidade |
| Pontos vermelhos | Calor no Sangue |
| Língua rígida | Vento Interno |
| Língua trêmula | Deficiência de Qi/Sangue, Vento |

### Umidade
- **Úmida normal**: Fluidos normais
- **Muito úmida/escorregadia**: Umidade excessiva, Yang deficiente
- **Seca**: Deficiência de Yin, calor consome fluidos
- **Rachada/gretada**: Deficiência grave de Yin

## Saburra da Língua

### Espessura
- **Fina (normal)**: Saúde ou início de doença
- **Espessa**: Doença interior, fatores patogênicos abundantes

### Cor da Saburra
| Cor | Significado |
|-----|-------------|
| Branca | Frio, Superficial |
| Amarela | Calor, Interior |
| Cinza/Preta | Frio extremo ou Calor extremo (verificar umidade) |

### Distribuição
- **Saburra na raiz**: Calor ou umidade no intestino
- **Saburra nas bordas**: Calor no Fígado/VB
- **Saburra na ponta**: Calor no Coração/Pulmão
- **Sem saburra (língua espelhada)**: Deficiência grave de Yin/Fluidos

## Exemplos Clínicos
- **Vermelha, seca, sem saburra**: Deficiência de Yin com calor vazio (Rim, Fígado)
- **Pálida, inchada, saburra branca úmida**: Yang do Baço deficiente com Umidade
- **Roxa escura, saburra amarela espessa**: Estagnação com calor interior
- **Bordas vermelhas, saburra amarela**: Calor no Fígado/Vesícula Biliar
            """.trimIndent(),
            tags = listOf("língua", "semiologia", "diagnóstico", "saburra", "corpo da língua")
        ),

        // SEMIOLOGIA PULSO
        MtcArticle(
            id = "semio_pulso",
            title = "Semiologia do Pulso (Mai Zhen)",
            category = "PULSO",
            summary = "Os 28 pulsos clássicos, 3 posições e 3 profundidades. Fundamento diagnóstico MTC.",
            content = """
# Semiologia do Pulso (脉诊 - Mài Zhěn)

## As Três Posições

### Mão Direita
| Posição | Nome | Órgãos |
|---------|------|--------|
| Cun (寸) | Proximal | Pulmão / Int. Grosso |
| Guan (关) | Médio | Baço-Pâncreas / Estômago |
| Chi (尺) | Distal | Rim Yang / San Jiao |

### Mão Esquerda
| Posição | Nome | Órgãos |
|---------|------|--------|
| Cun (寸) | Proximal | Coração / Int. Delgado |
| Guan (关) | Médio | Fígado / Vesícula Biliar |
| Chi (尺) | Distal | Rim Yin / Bexiga |

## As Três Profundidades
1. **Superficial (Fu)**: Pressão leve — reflete o Exterior
2. **Médio (Zhong)**: Pressão média — reflete os canais
3. **Profundo (Chen)**: Pressão forte — reflete os órgãos internos

## Os Principais Pulsos Patológicos

### Pulsos de Excesso
| Pulso | Caráter. | Significado |
|-------|----------|-------------|
| Fu (浮) Superficial | Sentido com toque leve | Exterior, Vento |
| Shi (实) Cheio | Forte em todas profund. | Excesso pleno |
| Xian (弦) Tenso/Corda | Longo, duro como corda | Fígado, Dor, Fleuma |
| Hua (滑) Escorregadio | Como pérola rolando | Fleuma, Umidade, Gravidez |
| Shu (数) Rápido | >5 batimentos/respiração | Calor |
| Hong (洪) Onda grande | Grande, cheio, forte | Calor intenso |

### Pulsos de Deficiência
| Pulso | Caráter. | Significado |
|-------|----------|-------------|
| Chen (沉) Profundo | Só sentido com pressão | Interior |
| Chi (迟) Lento | <4 bat./respiração | Frio, Deficiência |
| Xu (虚) Vazio | Suave, amplo, sem força | Deficiência Qi/Sangue |
| Xi (细) Fino/Fio | Finíssimo | Deficiência Sangue/Yin |
| Ruo (弱) Fraco | Profundo, fino, fraco | Deficiência Qi e Sangue |
| Jie (结) Nodoso | Lento com pausas | Estagnação Frio/Qi |
| Dai (代) Intermitente | Pausas regulares | Órgãos deficientes |

## Interpretação Integrada
O pulso deve ser interpretado junto à língua, queixas e observação para confirmar o diagnóstico. Um único pulso raramente define o padrão — observe as três posições e profundidades.

### Padrão Yin do Rim deficiente
- Pulso Chi da mão esquerda: Fino (Xi), rápido (Shu)
- Língua: Vermelha, seca, sem saburra
- Sinais: Calor nos cinco centros, suor noturno, lombalgias
            """.trimIndent(),
            tags = listOf("pulso", "mai zhen", "diagnóstico", "semiologia", "posições", "profundidades")
        ),

        // PONTOS
        MtcArticle(
            id = "pontos_especiais",
            title = "Pontos Especiais de Acupuntura",
            category = "PONTOS",
            summary = "Yuan, Luo, Xi, Shu, Mu, He e Jing Wells. Compreenda a categoria de cada ponto.",
            content = """
# Pontos Especiais de Acupuntura

## Pontos Yuan-Fonte (原穴 - Yuán Xué)
Conectados diretamente ao Yuan Qi (Qi Original). Usados para diagnosticar e tratar disfunções dos órgãos.

| Meridiano | Ponto Yuan |
|-----------|-----------|
| Pulmão | P9 - Taiyuan |
| Intestino Grosso | IG4 - Hegu |
| Estômago | E42 - Chongyang |
| Baço | BP3 - Taibai |
| Coração | C7 - Shenmen |
| Intestino Delgado | ID4 - Wangu |
| Bexiga | B64 - Jinggu |
| Rim | R3 - Taixi |
| Pericárdio | PC7 - Daling |
| San Jiao | SJ4 - Yangchi |
| Vesícula Biliar | VB40 - Qiuxu |
| Fígado | F3 - Taichong |

## Pontos Luo-Conexão (络穴 - Luò Xué)
Conectam pares Yin-Yang. Usados quando a patologia atinge tanto o canal quanto seu par.

## Pontos Xi-Fissura (郄穴 - Xì Xué)
Acumulação de Qi e Sangue. Indicados para condições agudas e dolorosas dos respectivos meridianos.

**Regra:** Pontos Xi de meridianos Yin: tratam condições de Sangue
Pontos Xi de meridianos Yang: tratam condições de Qi e dor

## Pontos Shu-Consentimento Dorsal (背俞穴)
Na bexiga posterior, nível das vértebras. Tratam diretamente os órgãos correspondentes.

| Órgão | Ponto Shu | Vértebra |
|-------|-----------|---------|
| Pulmão | B13 | T3 |
| Pericárdio | B14 | T4 |
| Coração | B15 | T5 |
| Fígado | B18 | T9 |
| Vesícula Biliar | B19 | T10 |
| Baço | B20 | T11 |
| Estômago | B21 | T12 |
| San Jiao | B22 | L1 |
| Rim | B23 | L2 |
| Int. Grosso | B25 | L4 |
| Int. Delgado | B27 | S1 |
| Bexiga | B28 | S2 |

## Pontos Mu-Coleta Frontal (募穴)
Na face ventral do tronco. Indicados principalmente para condições dos órgãos Fu.

## Pontos He-Mar (合穴)
Onde o Qi do canal "entra no mar". Indicados para doenças dos órgãos internos.
**Regra clássica:** "Os He tratam as inversões de Qi nos Fu"

## Os Oito Pontos Confluentes (八脉交会穴)
Conectam os 12 meridianos regulares com os 8 meridianos curiosos (extra-meridianos).

| Ponto | Meridiano | Meridiano Curioso |
|-------|-----------|------------------|
| P7 - Lieque | Pulmão | Ren Mai (Vaso Concepção) |
| R6 - Zhaohai | Rim | Yin Qiao Mai |
| IG4 - Hegu | Int. Grosso | Yang Ming |
| B62 - Shenmai | Bexiga | Yang Qiao Mai |
| SJ5 - Waiguan | San Jiao | Yang Wei Mai |
| VB41 - Zulinqi | Vesícula Biliar | Dai Mai |
| PC6 - Neiguan | Pericárdio | Yin Wei Mai |
| BP4 - Gongsun | Baço | Chong Mai |
            """.trimIndent(),
            tags = listOf("pontos yuan", "pontos shu", "pontos mu", "luo", "xi", "he", "especiais")
        ),

        // TÉCNICAS
        MtcArticle(
            id = "tecnicas_agulhamento",
            title = "Técnicas de Agulhamento",
            category = "TECNICAS",
            summary = "Inserção, manipulação, De Qi, tonificação e sedação. Segurança e contraindicações.",
            content = """
# Técnicas de Agulhamento

## Material
- **Agulhas de Acupuntura**: Aço inoxidável, individuais, estéreis e descartáveis
- **Calibres comuns**: 0,16mm, 0,20mm, 0,25mm, 0,30mm
- **Comprimentos**: 13mm (0,5 cun), 25mm (1 cun), 40mm (1,5 cun), 75mm (3 cun)

## De Qi (得气) — A Chegada do Qi
Sensação de peso, distensão, dormência, formigamento ou propagação ao redor do ponto e ao longo do meridiano. Indica que o ponto foi ativado corretamente.

## Ângulos de Inserção

| Ângulo | Uso |
|--------|-----|
| 90° (perpendicular) | Ponto padrão com músculo suficiente |
| 45° (oblíquo) | Regiões com pouco músculo, pontos do rosto |
| 15° (horizontal/subcutâneo) | Crânio, pontos com pouco tecido |

## Profundidade (em cun)
Depende da localização, constituição do paciente e objetivo terapêutico. Respeite sempre a anatomia regional.

## Técnicas de Manipulação

### Tonificação (补法 - Bǔ Fǎ)
Para deficiências (Xu)
- Inserção na direção do fluxo do meridiano
- Rotação horária
- Movimento de elevação lento, descida rápida (Bu)
- Retirada rápida com fechamento do orifício
- Tempo curto de retenção (ou sem retenção)

### Sedação (泻法 - Xiè Fǎ)
Para excessos (Shi)
- Inserção contra o fluxo do meridiano
- Rotação anti-horária
- Elevação rápida, descida lenta (Xie)
- Retirada lenta sem fechar o orifício
- Tempo maior de retenção

## Contraindicações Absolutas
- Gravidez: pontos SP6, ST36, LI4, GB21, B60, B67 (entre outros)
- Distúrbios de coagulação sem controle
- Marca-passo: eletroestimulação contraindicada
- Pele com infecção local
- Estado de extrema debilidade

## Contraindicações Relativas
- Hemofilia (usar agulhas mínimas e superficiais)
- Pós-parto imediato
- Estado de esgotamento extremo
- Pós-refeição imediata (evitar manipulação intensa)

## Segurança — Acidentes Raros mas Importantes
- **Pneumotórax**: Ponto B13, P1 — nunca inserir perpendicularmente
- **Síncope por agulha**: Posição deitada preferível, especialmente em novatos
- **Agulha quebrada**: Usar agulhas descartáveis de qualidade
- **Infecção**: Técnica asséptica rigorosa

## Tempo de Retenção
- **Standard**: 20–30 minutos
- **Condições agudas**: 10–15 minutos
- **Condições crônicas/deficiências**: 30–45 minutos
            """.trimIndent(),
            tags = listOf("agulhamento", "técnicas", "de qi", "tonificação", "sedação", "segurança", "contraindicações")
        ),

        // MOXIBUSTÃO
        MtcArticle(
            id = "moxibustao",
            title = "Moxibustão (Ai Jiu)",
            category = "MOXIBUSTAO",
            summary = "Uso terapêutico da artemísia. Tipos, técnicas, indicações e contraindicações.",
            content = """
# Moxibustão (艾灸 - Ài Jiǔ)

## O que é?
Técnica terapêutica que utiliza a queima de artemísia (Artemisia argyi - Ai Ye) sobre ou próximo a pontos de acupuntura para aquecer e estimular o fluxo de Qi e Sangue.

## Formas de Artemísia

### Charuto de Moxa (艾条)
- **Puro**: 100% artemísia
- **Com medicamentos**: Adição de outras ervas
- **Comprimento**: 20cm x 1,5cm diâmetro (padrão)

### Cones de Moxa (艾炷)
- Pequenos, médios ou grandes
- Diretos (sobre a pele) ou indiretos (sobre intermediário)

## Técnicas

### 1. Moxa Indireta com Charuto

**Circulatória (旋转灸):** Move o charuto em círculos sobre o ponto
- Indicação: Áreas extensas, analgesia, Umidade

**Pardal (雀啄灸):** Aproxima e afasta rapidamente
- Indicação: Condições agudas, emergências

**Aquecedor (温和灸):** Mantém distância fixa (2–3cm) sobre o ponto
- Indicação: Tonificação, deficiências crônicas

### 2. Caixas de Moxa (盒灸)
- Posicionadas sobre região lombar, abdômen, costas
- Ideal para sessões longas e confortáveis

### 3. Moxa sobre Agulha (温针灸)
- Cone de moxa colocado na parte superior da agulha inserida
- Combina efeitos de agulhamento e calor

### 4. Moxa com Intermediários
- **Sobre sal (隔盐灸)**: Umbigo (R8) — emergências, estômago
- **Sobre gengibre (隔姜灸)**: Frio no estômago, náuseas
- **Sobre alho (隔蒜灸)**: Infecções, abcessos
- **Sobre aconitum (隔附子灸)**: Yang deficiente grave

## Indicações
- Síndromes de Frio e Umidade
- Deficiência de Yang
- Deficiência crônica de Qi e Sangue
- Dor por Frio
- Prolapse de órgãos
- Fortalecimento imunológico preventivo (R1, E36, VB39)

## Contraindicações
- Febre alta, calor excessivo
- Yin deficiente com calor vazio intenso
- Gravidez: abdômen inferior e região lombar
- Rostos de pacientes debilitados
- Pele com dermatose ativa
- Após exercício intenso imediato

## Pontos Clássicos para Moxa
- **E36 (Zusanli)**: Tonificação geral, imunidade
- **R1 (Yongquan)**: Yang do Rim
- **VB39 (Xuanzhong)**: Medula óssea
- **RM4 (Guanyuan)**: Yang e Qi Original
- **RM6 (Qihai)**: Mar do Qi
- **B23 (Shenshu)**: Yang do Rim
            """.trimIndent(),
            tags = listOf("moxibustão", "artemísia", "calor", "yang", "frio", "tonificação")
        ),

        // SÍNDROMES DE ÓRGÃOS
        MtcArticle(
            id = "sindrome_baco",
            title = "Síndromes do Baço-Pâncreas (Pi)",
            category = "SINDROME_ORGAOS",
            summary = "O Baço como base da Energia Pós-Natal. Padrões de deficiência, umidade e suas manifestações.",
            content = """
# Síndromes do Baço-Pâncreas (脾 - Pí)

## Funções Fisiológicas
1. **Transforma e Transporta** (运化): Converte alimento e água em Qi e Sangue
2. **Controla o Sangue** (统血): Mantém o Sangue dentro dos vasos
3. **Governa os Músculos e Membros**: Nutrição muscular depende do Baço
4. **Abre para a Boca**: Apetite e paladar refletem a função do Baço
5. **Manifesta-se nos Lábios**: Cor e hidratação labial
6. **O Qi do Baço sobe** (脾气主升): Mantém órgãos em posição, eleva nutrientes

## Síndrome 1: Deficiência de Qi do Baço (脾气虚)
**Sinais:** Fadiga, apetite reduzido, fezes moles, distensão abdominal pós-prandial, palidez, voz fraca, língua pálida com marcas dentárias, pulso fraco (Ruo).
**Princípio:** Tonificar o Qi do Baço
**Pontos:** E36 (Zusanli), BP3 (Taibai), RM12 (Zhongwan), BP6 (Sanyinjiao)
**Fitoterapia:** Si Jun Zi Tang (Decocção dos Quatro Cavalheiros)

## Síndrome 2: Deficiência de Yang do Baço (脾阳虚)
**Sinais:** Tudo da deficiência de Qi + sinais de frio: extremidades frias, fezes aquosas, dor abdominal que melhora com calor.
**Princípio:** Tonificar o Yang do Baço, dispersar Frio
**Pontos:** RM4 (Guanyuan) com moxa, RM6 (Qihai) com moxa, B20 (Pishu), B23 (Shenshu)

## Síndrome 3: Baço Não Controla o Sangue (脾不统血)
**Sinais:** Sangramentos crônicos (menorragia, equimoses fáceis, sangue nas fezes), fadiga, palidez.
**Princípio:** Tonificar Qi do Baço para controlar o Sangue
**Pontos:** BP1 (Yinbai) com moxa, BP6, E36, RM6

## Síndrome 4: Umidade-Fleuma Obstruindo o Baço (痰湿困脾)
**Sinais:** Sensação de peso corporal, distensão abdominal, náuseas, língua inchada com saburra espessa e gordurosa, pulso escorregadio (Hua).
**Princípio:** Fortalecer o Baço, resolver Umidade, transformar Fleuma
**Pontos:** BP9 (Yinlingquan), E40 (Fenglong), E36, RM9 (Shuifen)
**Fitoterapia:** Er Chen Tang (Decocção das Duas Antigas)

## Síndrome 5: Umidade-Calor Invadindo o Baço/Estômago (脾胃湿热)
**Sinais:** Sensação de peso e calor, náuseas, boca pegajosa e amarga, icterícia possível, língua vermelha com saburra amarela gordurosa.
**Princípio:** Clarear Calor, drenar Umidade
**Pontos:** BP9, E44 (Neiting), VB34, IG11 (Quchi)

## Diagnóstico Diferencial Prático
A chave para diferenciar é observar: presença de FRIO (Yang Xu) vs ausência (Qi Xu simples) vs presença de CALOR com umidade (Umidade-Calor) vs apenas peso/distensão sem calor nem frio extremos (Fleuma-Umidade).
            """.trimIndent(),
            tags = listOf("baço", "qi xu", "umidade", "fleuma", "síndromes", "digestão")
        ),

        MtcArticle(
            id = "sindrome_rim",
            title = "Síndromes do Rim (Shen)",
            category = "SINDROME_ORGAOS",
            summary = "O Rim como raiz da vida. Yin, Yang, Essência (Jing) e suas deficiências características.",
            content = """
# Síndromes do Rim (肾 - Shèn)

## Funções Fisiológicas
1. **Armazena a Essência** (藏精): Jing pré-natal e pós-natal, base da reprodução e desenvolvimento
2. **Governa Nascimento, Desenvolvimento e Reprodução**
3. **Produz Medula e nutre o Cérebro**: Medula óssea e "Mar da Medula" (cérebro)
4. **Governa a Água** (主水): Metabolismo hídrico, em parceria com Pulmão e Baço
5. **Recebe o Qi** (纳气): Ancora a respiração profunda enviada pelo Pulmão
6. **Abre para os Ouvidos e duas vias inferiores**
7. **Manifesta-se nos cabelos**

## Síndrome 1: Deficiência de Yin do Rim (肾阴虚)
**Sinais:** Tontura, zumbido, lombalgia, suor noturno, calor nos cinco centros (palmas, plantas, peito), boca seca à noite, língua vermelha sem saburra, pulso fino e rápido.
**Princípio:** Nutrir o Yin do Rim
**Pontos:** R3 (Taixi), R6 (Zhaohai), BP6 (Sanyinjiao), B23 (Shenshu)
**Fitoterapia:** Liu Wei Di Huang Wan (Pílula dos Seis Sabores com Rehmannia)

## Síndrome 2: Deficiência de Yang do Rim (肾阳虚)
**Sinais:** Lombalgia com sensação de frio, extremidades frias, impotência, infertilidade, edema, nictúria, fezes matinais aquosas (diarreia do galo), língua pálida e inchada, pulso profundo e fraco.
**Princípio:** Tonificar o Yang do Rim
**Pontos:** R7 (Fuliu), RM4 (Guanyuan) com moxa, Du4 (Mingmen) com moxa, B23
**Fitoterapia:** Jin Gui Shen Qi Wan (Pílula do Qi do Rim do Cofre Dourado)

## Síndrome 3: Deficiência de Essência do Rim (肾精不足)
**Sinais:** Em crianças: desenvolvimento atrasado. Em adultos: envelhecimento precoce, infertilidade, fraqueza óssea, esquecimento, cabelos brancos precoces.
**Princípio:** Tonificar a Essência (Jing)
**Pontos:** R3, B23, B52 (Zhishi), Du4
**Fitoterapia:** He Che Da Zao Wan, Gui Lu Er Xian Jiao

## Síndrome 4: Rim Não Recebe o Qi (肾不纳气)
**Sinais:** Dispneia crônica especialmente ao esforço, respiração superficial, inspiração difícil, sudorese espontânea.
**Princípio:** Tonificar Rim para receber o Qi
**Pontos:** R3, R6, Du4, RM17 (Danzhong), B23

## Síndrome 5: Deficiência de Yang do Rim com Retenção de Água (肾阳虚水泛)
**Sinais:** Edema generalizado pior em membros inferiores, oligúria, sensação de frio, língua pálida inchada.
**Princípio:** Aquecer o Yang do Rim, promover diurese
**Pontos:** R7, BP9, RM9, B22 (Sanjiaoshu)

## Nota Clínica Importante
O Rim é considerado a "raiz" (Ben 本) na MTC. Quase todas as condições crônicas eventualmente envolvem alguma deficiência do Rim, especialmente em pacientes idosos ou com doença de longa duração.
            """.trimIndent(),
            tags = listOf("rim", "yin xu", "yang xu", "jing", "essência", "envelhecimento", "lombalgia")
        ),

        // FITOTERAPIA
        MtcArticle(
            id = "fitoterapia_intro",
            title = "Fitoterapia Chinesa — Princípios e Categorias",
            category = "FITOTERAPIA",
            summary = "As categorias terapêuticas das ervas chinesas e fórmulas clássicas mais usadas.",
            content = """
# Fitoterapia Chinesa (中药学 - Zhōngyào Xué)

## Os Quatro Qi (Naturezas) das Ervas
- **Quente (热)**: Aquece o interior, dispersa Frio. Ex: Gengibre seco (Gan Jiang)
- **Morna (温)**: Tonifica Yang, move Qi/Sangue. Ex: Canela (Gui Zhi)
- **Fria (寒)**: Clareia Calor intenso. Ex: Gypsum (Shi Gao)
- **Fresca (凉)**: Clareia Calor leve. Ex: Crisântemo (Ju Hua)
- **Neutra (平)**: Sem tendência térmica marcante. Ex: Codonopsis (Dang Shen)

## Os Cinco Sabores (五味) e suas Ações
| Sabor | Ação | Exemplo |
|-------|------|---------|
| Picante (辛) | Dispersa, move Qi/Sangue | Gengibre, Hortelã |
| Doce (甘) | Tonifica, harmoniza, modera | Ginseng, Alcaçuz |
| Ácido (酸) | Astringe, consolida | Schisandra |
| Amargo (苦) | Drena, seca Umidade, purga Calor | Coptis |
| Salgado (咸) | Amolece massas, purga | Concha de ostra |

## Categorias Funcionais Principais

### 1. Ervas que Liberam o Exterior (解表药)
- **Picante-Quente** (Vento-Frio): Gui Zhi (Ramo de Canela), Ma Huang (Ephedra)
- **Picante-Fresco** (Vento-Calor): Bo He (Menta), Jin Yin Hua (Madressilva)

### 2. Ervas que Clareiam o Calor (清热药)
- Shi Gao (Gypsum) — Calor pleno do Qi
- Huang Lian (Coptis) — Fogo do Coração/Estômago
- Huang Qin (Escutelária) — Calor do Pulmão/VB
- Sheng Di Huang (Rehmannia crua) — Calor no Sangue

### 3. Ervas Tonificantes (补益药)
- **Tonificam Qi:** Ren Shen (Ginseng), Huang Qi (Astrágalo), Dang Shen (Codonopsis)
- **Tonificam Sangue:** Dang Gui (Angélica), Shu Di Huang (Rehmannia preparada)
- **Tonificam Yin:** Mai Men Dong (Ophiopogon), Sha Shen (Glehnia)
- **Tonificam Yang:** Rou Gui (Canela), Du Zhong (Eucommia)

### 4. Ervas que Movem o Qi (理气药)
- Chen Pi (Casca de tangerina seca) — Move Qi do Baço/Estômago
- Xiang Fu (Cyperus) — Move Qi do Fígado, regula menstruação

### 5. Ervas que Movem o Sangue / Resolvem Estase (活血化瘀药)
- Dan Shen (Salvia) — Move Sangue, acalma Shen
- Tao Ren (Semente de pêssego) — Quebra estase de Sangue
- Chuan Xiong (Ligusticum) — Move Qi e Sangue, alivia dor

### 6. Ervas que Transformam Fleuma (化痰药)
- Ban Xia (Pinellia) — Seca Umidade, transforma Fleuma fria
- Gua Lou (Trichosanthes) — Limpa Calor-Fleuma do peito

## Fórmulas Clássicas Essenciais

| Fórmula | Função | Indicação Principal |
|---------|--------|---------------------|
| Si Jun Zi Tang | Tonifica Qi do Baço | Fadiga, fezes moles |
| Si Wu Tang | Tonifica Sangue | Deficiência de Sangue, menstruação irregular |
| Liu Wei Di Huang Wan | Nutre Yin do Rim | Calor vazio, suor noturno |
| Xiao Yao San | Move Qi do Fígado, harmoniza Baço | Estagnação emocional, SPM |
| Ba Zhen Tang | Tonifica Qi e Sangue | Deficiência combinada |
| Gui Pi Tang | Tonifica Coração e Baço | Insônia, ansiedade, palpitações |

## Segurança e Contraindicações
- Sempre considerar interações com medicamentos convencionais
- Gravidez: muitas ervas que movem Sangue são contraindicadas (Tao Ren, Hong Hua)
- Ma Huang: cuidado em hipertensos e cardiopatas
- Sempre prescrever dentro do escopo legal de atuação profissional
            """.trimIndent(),
            tags = listOf("fitoterapia", "ervas", "fórmulas", "quatro qi", "cinco sabores", "tonificantes")
        ),

        // DIETOTERAPIA
        MtcArticle(
            id = "dietoterapia_intro",
            title = "Dietoterapia em MTC",
            category = "DIETOTERAPIA",
            summary = "Natureza térmica dos alimentos e recomendações dietéticas por padrão de desarmonia.",
            content = """
# Dietoterapia Chinesa (食疗 - Shí Liáo)

## Princípio Fundamental
Assim como as ervas, os alimentos têm natureza térmica (quente/morna/neutra/fresca/fria) e sabor, e afetam órgãos específicos. "Comida e remédio têm a mesma origem" (药食同源).

## Classificação Térmica dos Alimentos Comuns

### Alimentos Quentes/Mornos (aquecem, indicados para padrões de Frio)
Gengibre, canela, cordeiro, pimenta, alho, cebolinha, cereja, pêssego, nozes, café

### Alimentos Neutros (equilibrados, base da dieta)
Arroz, batata-doce, abóbora, cenoura, repolho, carnes magras, ovos, mel

### Alimentos Frescos/Frios (esfriam, indicados para padrões de Calor)
Pepino, melancia, pera, tofu, algas marinhas, espinafre, banana, chá verde

## Recomendações por Padrão de Desarmonia

### Para Deficiência de Qi do Baço
✅ Favorecer: arroz, batata-doce, abóbora, tâmaras, gengibre cozido, sopas
❌ Evitar: alimentos crus e frios, leite em excesso, açúcar refinado

### Para Deficiência de Yin
✅ Favorecer: pera, melancia, tofu, leite, ovos, mel, ostras
❌ Evitar: alimentos picantes/quentes em excesso, frituras, álcool

### Para Estagnação de Qi do Fígado
✅ Favorecer: hortelã, frutas cítricas, rabanete, cevada, açafrão
❌ Evitar: alimentos gordurosos, álcool em excesso, refeições muito grandes

### Para Umidade-Fleuma
✅ Favorecer: cevada, feijão azuki, rabanete, alga kombu, casca de tangerina
❌ Evitar: laticínios em excesso, doces, alimentos gordurosos e fritos

### Para Deficiência de Sangue
✅ Favorecer: fígado, carne vermelha magra, beterraba, espinafre, tâmaras vermelhas, goji berry
❌ Evitar: dietas restritivas excessivas, jejuns prolongados

## Princípios Gerais de Boa Prática Alimentar em MTC
1. **Comer morno**: Evitar excesso de alimentos gelados, que sobrecarregam o Yang do Baço
2. **Mastigar bem**: Facilita a "decocção" do Estômago
3. **Regularidade de horários**: Protege o Qi do Estômago/Baço
4. **Evitar comer com pressa ou estresse**: Compromete a digestão (Fígado invade Baço)
5. **Moderação**: Nem excesso, nem restrição extrema
6. **Sazonalidade**: Alimentos da estação nutrem de acordo com o clima vigente

## Receituário Simples — Exemplos Terapêuticos
**Congee de gengibre e jujuba** (para Frio no Baço/Estômago): arroz, gengibre fatiado, 3-5 jujubas, cozinhar até consistência cremosa.

**Sopa de barbatana de melão amargo** (para Calor de Verão): melão amargo, tofu, um pouco de gengibre para equilibrar o frio extremo.

**Chá de hortelã e crisântemo** (para Vento-Calor / Fígado): infusão simples, refrescante, bom para olhos vermelhos e cefaleia por calor.
            """.trimIndent(),
            tags = listOf("dietoterapia", "alimentos", "natureza térmica", "congee", "nutrição")
        ),

        // QIGONG
        MtcArticle(
            id = "qigong_intro",
            title = "Qigong e Tai Chi — Cultivo Energético",
            category = "QIGONG",
            summary = "Práticas de movimento, respiração e meditação para cultivar e equilibrar o Qi.",
            content = """
# Qigong (气功) e Tai Chi Chuan (太极拳)

## O que é Qigong?
Qigong significa literalmente "trabalho/cultivo do Qi". É um sistema de práticas que combina:
- Movimento físico controlado (ou imobilidade em algumas formas)
- Respiração consciente e regulada
- Concentração mental / visualização (Yi)

## Categorias de Qigong

### Qigong Dinâmico (动功)
Envolve movimento ativo. Exemplos: Ba Duan Jin (Oito Peças de Brocado), Wu Qin Xi (Jogo dos Cinco Animais)

### Qigong Estático (静功)
Práticas em postura fixa, sentado ou em pé. Exemplos: Zhan Zhuang (Postura da Árvore), meditação sentada

## Ba Duan Jin — Oito Peças de Brocado
Uma das formas mais populares e estudadas clinicamente. Os oito movimentos:
1. Segurar o céu com as duas mãos para regular o Triplo Aquecedor
2. Abrir o arco para a esquerda e direita como se atirasse flechas
3. Erguer um braço para regular Baço e Estômago
4. Olhar para trás para prevenir doenças e fadiga
5. Balançar a cabeça e mover o quadril para drenar Fogo do Coração
6. Tocar os pés com as mãos para fortalecer Rins e lombar
7. Cerrar os punhos com olhar feroz para aumentar força e Qi
8. Elevar-se nas pontas dos pés sete vezes para eliminar as cem doenças

## Tai Chi Chuan
Arte marcial interna que se tornou amplamente praticada como exercício terapêutico. Princípios:
- Movimentos lentos, contínuos e circulares
- Relaxamento (Song 松) combinado com enraizamento
- Distinção clara entre Cheio (Shi) e Vazio (Xu) no peso corporal
- Respiração abdominal natural coordenada com movimento

## Benefícios Documentados na Literatura
- Melhora do equilíbrio e prevenção de quedas em idosos
- Redução de estresse e ansiedade (efeito sobre o eixo Fígado-Shen)
- Melhora da função cardiovascular e respiratória
- Auxílio em manejo de dor crônica (especialmente osteoartrite)
- Melhora do sono e regulação do sistema nervoso autônomo

## Aplicação Clínica na Acupuntura
Praticantes de MTC frequentemente recomendam Qigong/Tai Chi como terapia complementar:
- **Estagnação de Qi do Fígado**: Ba Duan Jin movimento 2 (abrir o arco)
- **Deficiência de Qi/Yang do Rim**: Zhan Zhuang (postura da árvore) regular
- **Ansiedade / Shen perturbado**: Qigong de respiração e meditação sentada
- **Fadiga geral**: Wu Qin Xi (Jogo dos Cinco Animais), prática matinal

## Recomendações de Prescrição
- Iniciantes: 10-15 minutos diários, preferencialmente pela manhã
- Praticar em ambiente tranquilo, de preferência ao ar livre
- Roupas confortáveis, sem apertar a cintura ou abdômen
- Evitar prática imediatamente após refeições copiosas
            """.trimIndent(),
            tags = listOf("qigong", "tai chi", "ba duan jin", "respiração", "meditação", "exercício")
        ),

        // TÉCNICAS EXTRAS — AURICULOTERAPIA
        MtcArticle(
            id = "auriculoterapia",
            title = "Auriculoterapia — Microssistema da Orelha",
            category = "TECNICAS",
            summary = "O mapa somatotópico da orelha e suas aplicações terapêuticas.",
            content = """
# Auriculoterapia (耳针 - Ěr Zhēn)

## Fundamento Teórico
A orelha é considerada um microssistema que reflete todo o corpo, como um feto invertido. Todos os meridianos principais e órgãos têm representação na orelha externa (pavilhão auricular).

## Mapa Somatotópico Simplificado
- **Lóbulo**: Face, olhos, língua, dentes (região "cefálica")
- **Hélix**: Sistema digestivo (em forma de feto invertido, hélix = coluna)
- **Anti-hélix**: Coluna vertebral e membros
- **Concha**: Órgãos internos (Zang-Fu) — concha cava (tórax) e concha cymba (abdômen)
- **Trago**: Nariz, garganta, sistema endócrino
- **Fossa triangular**: Útero, sistema reprodutivo, Shenmen

## Pontos Auriculares Mais Utilizados
| Ponto | Localização | Indicação |
|-------|--------------|-----------|
| Shenmen | Fossa triangular | Ansiedade, dor, insônia — "calmante universal" |
| Ponto Zero | Centro da hélix | Equilíbrio geral do sistema |
| Rim | Concha cymba inferior | Tonificação geral, medo |
| Coração | Concha cava central | Ansiedade, palpitações |
| Estômago | Raiz da hélix | Náusea, apetite |
| Pulmão | Concha cava grande | Respiratório, pele, tabagismo |
| Fígado | Concha cava | Estagnação emocional, raiva |

## Métodos de Estimulação
1. **Agulhas semipermanentes**: Inseridas e fixadas por dias (3-5 dias típico)
2. **Sementes de Vaccaria** (auriculoterapia com sementes): Não invasivo, pressão intermitente
3. **Agulhamento convencional**: Sessão única, retenção curta
4. **Eletroestimulação auricular**: Para condições mais intensas

## Indicações Comuns
- Cessação do tabagismo (protocolo NADA)
- Controle de apetite / auxílio em perda de peso
- Ansiedade e insônia
- Dor crônica como adjuvante
- Dependências químicas (protocolo NADA — 5 pontos)

## Protocolo NADA (National Acupuncture Detoxification Association)
5 pontos bilaterais utilizados em grupo, sem diagnóstico individualizado:
Shenmen, Rim, Fígado, Pulmão, Ponto Simpático

## Cuidados e Contraindicações
- Avaliar histórico de queloides antes de agulhamento semipermanente
- Higiene rigorosa — risco de pericondrite se infeccionar
- Gravidez: usar com cautela em pontos relacionados ao útero
            """.trimIndent(),
            tags = listOf("auriculoterapia", "orelha", "shenmen", "NADA", "microssistema")
        )
    )

    fun byCategory(category: String): List<MtcArticle> =
        articles.filter { it.category == category }

    fun search(query: String): List<MtcArticle> {
        val q = query.lowercase()
        return articles.filter { article ->
            article.title.lowercase().contains(q) ||
            article.summary.lowercase().contains(q) ||
            article.content.lowercase().contains(q) ||
            article.tags.any { it.lowercase().contains(q) }
        }
    }
}
