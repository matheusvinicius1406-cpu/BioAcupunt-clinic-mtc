# MKIS — Validação de consistência semântica

Nomenclaturas canônicas, conflitos e regras unificadas.

---

## 1. Termos canônicos adotados

- KnowledgeNode / knowledge_node: Conceito = KnowledgeNode; tabela/coluna = knowledge_nodes/knowledge_node
- KnowledgeGraph / knowledge_graph: Conceito = KnowledgeGraph; tabela/coluna = knowledge_graph_*
- SearchContext / Search Engine: Nome oficial = SearchContext; evitar “Search Engine” nos docs
- Relationship Engine: Nome oficial = Relationship Engine (std); usar nos contratos/docs
- Embedding / Vector: Embedding é a saída do modelo; Vector é o tipo/coluna. Não trocar.
- Pipeline / Workflow: Pipeline = cadeia técnica; Workflow = estado do job. Não trocar.
- Tenant / Cliente / Workspace: Tenant é a unidade de isolamento; evitar cliente/workspace.
- IngestionJob / IngestionJob: Nome canônico = IngestionJob (aggregate).
- GraphEdge / KnowledgeGraphEdge: Nome canônico = KnowledgeGraphEdge (tabela = knowledge_graph_edges).
- EmbeddingService / EmbeddingEngine: Nome canônico = EmbeddingService.

## 2. Conflitos encontrados

- Variações “KnowledgeGraph/Knowledge Graph” ocorrem porque há documento 08 intitulado com espaço; manter documento nesse formato, mas usar `KnowledgeGraph` nos contratos.

- “Search Engine” não aparece em nenhum documento atual além da cobertura externa, então não há conflito interno; deve ser adotado como `SearchContext` de forma consistente.

- Ausência de enum canônico para `knowledge_type`, `status`, `evidence_level` em banco: emails sugerem enum mas não há lista fechada.
