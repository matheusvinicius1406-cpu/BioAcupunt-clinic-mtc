# Deduplicação e conflitos

A deduplicação é determinística e não usa LLM. A ordem é:

1. ID canônico explícito (`pattern.*`, `symptom.*`, `point.*` e IDs futuros aprovados).
2. Tipo + nome normalizado, removendo acentos, pontuação e diferenças de caixa.
3. Aliases e IDs de origem são preservados como provenance.
4. Conteúdo normalizado igual é duplicação.
5. Mesmo ID com conteúdo diferente é conflito; nenhuma fonte é descartada.

Conflitos exigem revisão futura. O Core não promove automaticamente uma versão divergente.
