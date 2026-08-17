# Migração Library + MKIS + Knowledge Nodes

| Modelo atual | Conceito canônico | Adapter | Destino | Observações |
|---|---|---|---|---|
| `BibliotecaNodeEntity` / `BibliotecaNode` | `KnowledgeEntity` | `LibraryAdapter` | `knowledge_core_entities` | Conteúdo curado; status publicado na importação compatível |
| `KnowledgeNodeEntity` | `KnowledgeEntity` | `MkisAdapter` | `knowledge_core_entities` | Preserva tipo, categoria, source, checksum e versão em metadata/provenance |
| MKIS node legado | `KnowledgeEntity` | `KnowledgeNodeAdapter` | `knowledge_core_entities` | Alias explícito do adapter MKIS; não duplica regras |
| `MtcArticle` / packs | `DOCUMENT` ou tipo MTC mapeável | pendente | importer de packs | Não importar automaticamente nesta etapa; requer revisão de origem/licença |
| `biblioteca_nodes` / `knowledge_nodes` | fontes históricas | DAOs legados | mantidos | Nenhuma tabela legada foi apagada |

Canonicalização usa ID explícito estável quando disponível; caso contrário usa `tipo + nome normalizado`. Nomes iguais convergem para uma entidade. Conteúdo divergente gera `KnowledgeConflict` e preserva todas as proveniências.

## Estado da transição

- Read source: tabelas legadas até a população validada do Core.
- Migration source: adapters explícitos.
- Write source: legado durante esta primeira entrega; `KnowledgeCoreImporter` é o único caminho de importação canônica preparado.
- Canonical read: `KnowledgeRepository`/`RoomKnowledgeRepository`.
