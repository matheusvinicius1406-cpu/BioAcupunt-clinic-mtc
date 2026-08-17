# BioAcupunt Knowledge Core

O Knowledge Core é a única fronteira canônica para conhecimento. `BibliotecaNodeEntity`, `KnowledgeNodeEntity`/MKIS e packs são fontes legadas de importação durante a transição, não contratos que novos módulos devem consultar diretamente.

## Contratos

- `KnowledgeEntity`: identidade, tipo, conteúdo, aliases, versão e referências.
- `KnowledgeRelation`: relação dirigida entre duas entidades.
- `KnowledgeSource`, `KnowledgeCitation`, `KnowledgeEvidence`: cadeia de origem.
- `KnowledgeProvenance`: origem histórica e versão da migração.
- `KnowledgeVersion`: estado editorial e timestamps.
- `KnowledgeRepository`: leitura canônica para id, busca, relações e observação.

## Persistência v25

O Room recebeu tabelas aditivas `knowledge_core_*`. A migração não remove nem altera as tabelas antigas. A população é explícita via `KnowledgeCoreImporter`, permitindo auditoria antes de trocar as escritas legadas.

GraphRAG, vector database e agentes estão fora desta fase.
