# Execução da migração do Knowledge Core

Esta fase entrega a infraestrutura de migração, não uma promoção silenciosa de todo o acervo.

## Ordem operacional

1. Ler fontes legadas via `LibraryAdapter`, `MkisAdapter` e `KnowledgeNodeAdapter`.
2. Canonicalizar deterministicamente por tipo/nome/ID estável.
3. Executar `KnowledgeCanonicalizer.merge`.
4. Revisar `KnowledgeConflict`.
5. Persistir com `KnowledgeCoreImporter`.
6. Comparar contagens, IDs, hashes/checksums e provenance.
7. Habilitar leitura canônica por contexto.
8. Só então alterar escrita e desativar leituras legadas.

O batch deve ser idempotente: reexecutar o mesmo conjunto substitui pelo mesmo ID e não cria uma segunda entidade conceitual.
