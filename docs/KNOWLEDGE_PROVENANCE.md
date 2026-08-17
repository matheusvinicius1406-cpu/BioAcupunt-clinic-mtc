# Proveniência do conhecimento

Cada importação registra `originalSource`, `originalId`, `originalType`, `migrationVersion` e `importedAt`. Entidades também referenciam `KnowledgeSource`, `KnowledgeCitation` e `KnowledgeEvidence` por IDs estáveis.

Conteúdo sem fonte não deve ser apresentado como evidência clínica. A camada de explicação futura deverá resolver:

`Entity → Evidence → Citation → Source → Version`.
