package com.bioacupunt.educacao.data

import com.bioacupunt.educacao.domain.model.Flashcard

/**
 * Os 12 flashcards fixos, movidos verbatim de `FlashcardsScreen.kt`. Conteúdo revisado
 * por humano (R4) — read-only, `builtin = true`. Novas entradas fixas só entram por
 * edição manual deste arquivo, nunca por geração; cards da médica vivem na tabela
 * `flashcards` (ver [com.bioacupunt.educacao.data.local.FlashcardEntity]).
 *
 * O campo `dificuldade` que existia na tela morreu aqui: era "médio" fixo em todos os
 * 12 cards — nunca carregou informação real. A caixa do Leitner é o sinal honesto de
 * dificuldade agora.
 */
object BuiltinFlashcards {
    val cards: List<Flashcard> = listOf(
        Flashcard(
            key = "builtin_de_qi",
            front = "O que é o De Qi?",
            back = "Sensação de peso, distensão, dormência ou formigamento ao redor do ponto durante o agulhamento, indicando que o Qi chegou.",
            category = "Técnicas",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_ba_gang",
            front = "Quais são os Oito Princípios (Ba Gang)?",
            back = "Yin/Yang, Interior/Exterior, Frio/Calor, Deficiência/Excesso. São a base do diagnóstico diferencial em MTC.",
            category = "Ba Gang",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_figado_funcao",
            front = "Qual a função principal do Fígado em MTC?",
            back = "Promover o livre fluxo do Qi (疏泄). Também armazena o Sangue, controla tendões e abre para os olhos.",
            category = "Órgãos",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_zusanli",
            front = "Localização do Zusanli (E36)?",
            back = "4 cun abaixo da patela, 1 cun lateral à crista da tíbia. Principal ponto de tonificação geral.",
            category = "Pontos",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_figado_horario",
            front = "Qual o horário de pico do meridiano do Fígado?",
            back = "1h às 3h da manhã (hora do Boi). Ideal para tratar condições do Fígado com moxibustão neste horário.",
            category = "Meridianos",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_lingua_vermelha",
            front = "O que indica uma língua vermelha sem saburra?",
            back = "Deficiência de Yin com calor vazio. Comum em deficiência de Yin do Rim ou Fígado.",
            category = "Semiologia",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_cinco_elementos",
            front = "Quais são os Cinco Elementos e seus órgãos?",
            back = "Madeira→Fígado/VB, Fogo→Coração/ID, Terra→Baço/E, Metal→Pulmão/IG, Água→Rim/B",
            category = "Cinco Elementos",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_pulso_xian",
            front = "O que é o pulso Xian (弦)?",
            back = "Pulso tenso como corda de arco. Indica estagnação de Qi do Fígado, dor, ou presença de Fleuma.",
            category = "Pulso",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_pontos_yuan",
            front = "O que são os pontos Yuan-Fonte?",
            back = "Pontos que acumulam Yuan Qi (Qi Original). Usados para diagnosticar e tratar os órgãos internos correspondentes.",
            category = "Pontos Especiais",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_mu_figado",
            front = "Qual o Mu do Fígado?",
            back = "F14 - Qimen, localizado no 6° espaço intercostal, na linha medioclavicular. Tratar diretamente o Fígado.",
            category = "Pontos Especiais",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_qi_sangue",
            front = "Diferença entre Qi e Sangue (Xue)?",
            back = "Qi é Yang, impalpável, move e transforma. Sangue é Yin, nutre e umidifica. O Qi move o Sangue; o Sangue é a mãe do Qi.",
            category = "Teoria",
            builtin = true,
        ),
        Flashcard(
            key = "builtin_shen",
            front = "O que é o Shen?",
            back = "O Espírito/Mente armazenado no Coração. Controla a consciência, cognição, memória e sono. Reflete-se nos olhos.",
            category = "Teoria",
            builtin = true,
        ),
    )
}
