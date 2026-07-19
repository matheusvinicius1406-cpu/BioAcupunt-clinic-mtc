# Pipeline de ingestão da Biblioteca

Como adicionar conteúdo novo à biblioteca **sem violar a R4** (nenhum conteúdo clínico
gerado por IA entra numa fonte que a médica trata como confiável).

## Princípio

> Conteúdo vem de fonte real revisada por humano (Maciocia, Deadman, diretrizes).
> Você constrói o **pipeline de ingestão**, nunca o conteúdo. — `CLAUDE.md`, R4

Nenhuma IA escreve nem aprova artigo. O fluxo é:

1. **Curadoria humana** monta um *pacote de conteúdo* (`.json`) a partir de fontes
   revisadas, com a **citação** de cada item.
2. Em **Biblioteca → Curadoria → Importar pacote curado**, a curadora escolhe o arquivo.
3. O portão determinístico `LibraryIngestion` (Kotlin puro, testado) valida cada item.
   Item sem citação, sem fonte, sem título/conteúdo, ou de categoria inválida é
   **rejeitado na entrada** — nunca chega à fila.
4. Os itens válidos entram como **PENDENTE**. *Encenar não é publicar.*
5. A médica revisa cada pendente e toca **Aprovar** ou **Rejeitar**.
6. Só o **aprovado** entra no acervo consultável e no RAG — no próximo início do app.

## Formato do pacote (`.json`)

Cada `citation` é obrigatória. `category` deve ser um dos nomes de `MtcCategory`
(`MERIDIANOS`, `PONTOS`, `CINCO_ELEMENTOS`, `BA_GANG`, `SINDROME_ORGAOS`, `LINGUA`,
`PULSO`, `TECNICAS`, `FITOTERAPIA`, `MOXIBUSTAO`, `DIETOTERAPIA`, `QIGONG`).

```json
{
  "source": "SUBSTITUA — a obra/diretriz revisada de onde veio este lote",
  "items": [
    {
      "id": "SUBSTITUA-id-unico-e-estavel",
      "title": "SUBSTITUA — título do artigo",
      "category": "PONTOS",
      "summary": "SUBSTITUA — resumo curto",
      "content": "SUBSTITUA — corpo em Markdown (títulos, listas, tabelas são renderizados)",
      "tags": ["SUBSTITUA", "palavras-chave"],
      "citation": "SUBSTITUA — referência específica revisada (obra, edição, página)"
    }
  ]
}
```

> Os valores acima são **placeholders** de propósito. Este arquivo é documentação —
> não é ingerido pelo app. Preencha com conteúdo de fonte real revisada.

## Por que local, e não n8n a cada minuto

A automação "sub-agentes de IA populando a cada minuto via n8n" foi **redesenhada**:
ela colidia com a R4 (IA gerando conteúdo clínico) e com o modo offline-first do app
(n8n é um servidor externo que precisa ficar no ar). No lugar, a ingestão é **local e
sob demanda**, com aval humano. Se no futuro houver um servidor de conteúdo curado, o
mesmo formato de pacote pode ser entregue por sync — o portão de revisão continua o mesmo.
