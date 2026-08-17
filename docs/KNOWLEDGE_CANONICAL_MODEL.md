# Modelo canônico de conhecimento

## Entidades iniciais

`SYMPTOM`, `PATTERN`, `SYNDROME`, `ZANG_FU`, `MERIDIAN`, `ACUPOINT`, `FORMULA`, `HERB`, `TECHNIQUE`, `PROTOCOL`, `THEORY`, `OBSERVATION`, `ANATOMY`, `DISEASE`, `CLINICAL_CASE` e `DOCUMENT`.

Tipos desconhecidos são preservados como `UNKNOWN`; adicionar tipos requer extensão do enum e teste, não uma nova base.

## Relações iniciais

`SUGGESTS`, `ASSOCIATED_WITH`, `TREATED_BY`, `BELONGS_TO`, `CONTAINS`, `CONTRAINDICATED_BY`, `RELATED_TO`, `SUPPORTED_BY`, `DERIVED_FROM`, `PART_OF`, `HAS_SYMPTOM`, `HAS_PATTERN`, `HAS_POINT`, `HAS_FORMULA` e `HAS_EVIDENCE`.

Toda relação mantém direção, evidências, confiança e provenance.
