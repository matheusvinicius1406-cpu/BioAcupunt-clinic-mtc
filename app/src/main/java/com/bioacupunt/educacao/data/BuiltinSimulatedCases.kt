package com.bioacupunt.educacao.data

import com.bioacupunt.educacao.domain.model.SimulatedCase

/**
 * O caso clínico fixo, movido verbatim de `SimuladorScreen.kt`. Conteúdo revisado por
 * humano (R4) — read-only, `builtin = true`. Novos casos fixos só entram por edição
 * manual deste arquivo; casos aprovados pela médica na Curadoria vivem na tabela
 * `simulated_cases` (ver [com.bioacupunt.educacao.data.local.SimulatedCaseEntity]).
 */
object BuiltinSimulatedCases {
    val cases: List<SimulatedCase> = listOf(
        SimulatedCase(
            key = "builtin_estagnacao_qi_figado",
            title = "Paciente F.S., 38 anos",
            category = "Zang Fu",
            builtin = true,
            vignette = """
**Queixa Principal:**
Dor no hipocôndrio direito, distensão abdominal, irritabilidade e suspiros frequentes há 3 meses.

**História:**
Trabalho estressante, conflitos familiares recentes. Ciclos menstruais irregulares com cólicas e coágulos. Insônia com dificuldade para adormecer.

**Semiologia:**
• Língua: Levemente roxa nas bordas, saburra fina branca
• Pulso: Xian (tenso como corda) em guan esquerdo
• Face: Tez levemente acinzentada
            """.trimIndent(),
            questions = listOf(
                "Qual o padrão de desarmonia mais provável?",
                "Quais são os princípios de tratamento?",
                "Sugira 5 pontos com justificativa.",
            ),
            answerKey = """
**Padrão:** Estagnação de Qi do Fígado (肝气郁结)

**Justificativa:**
• Dor no hipocôndrio → região do Fígado/VB
• Irritabilidade e suspiros → Qi do Fígado estagnado
• Pulso Xian no Guan esquerdo → Fígado comprometido
• Língua levemente roxa → início de estagnação de Sangue
• Menstruação irregular com coágulos → Qi estagnado afeta Sangue

**Princípios de Tratamento:**
1. Mover o Qi do Fígado (疏肝理气)
2. Acalmar o Shen
3. Regular o Sangue (se sangue estagnado)

**Pontos Sugeridos:**
• F3 (Taichong) — Yuan do Fígado, principal ponto para mover Qi
• F14 (Qimen) — Mu do Fígado, desobstrui o Qi do Fígado
• VB34 (Yanglingquan) — He da VB, alivia hipocôndrio
• PC6 (Neiguan) — Acalma o Shen, alivia distensão
• SP6 (Sanyinjiao) — Move Qi e Sangue, regula menstruação
            """.trimIndent(),
        ),
    )
}
