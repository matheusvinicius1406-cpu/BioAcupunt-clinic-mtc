# Relatório de migração do Knowledge Core

**Estado:** infraestrutura implementada; importação de dados reais ainda não executada.

| Métrica | Resultado |
|---|---:|
| Entidades fonte processadas | 0 |
| Entidades canônicas persistidas por batch | 0 |
| Duplicações | 0 |
| Conflitos | 0 |
| Não mapeados | 0 |
| Relações migradas | 0 |
| Erros | 0 |
| Warnings | 0 |

O zero é intencional: não existe ainda um job autorizado para varrer e promover todas as fontes legadas. `KnowledgeCoreImporter` produz `KnowledgeMergeResult`, incluindo duplicações e conflitos, para que o próximo passo execute a migração observável sem perda silenciosa.
