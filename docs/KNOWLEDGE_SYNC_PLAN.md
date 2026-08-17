# Plano de sincronização do Knowledge Core

Fora do escopo de ativação da Fase 1B. O Core deve sincronizar conteúdo, não dados clínicos de pacientes.

- Identidade: `id` canônico + `version` + checksum do conteúdo.
- Unidade: Knowledge Pack versionado e assinado; relações e provenance acompanham o pack.
- Direção: servidor editorial publica; dispositivo valida checksum/assinatura e instala atomicamente.
- Conflito: conteúdo divergente nunca sobrescreve silenciosamente; fica em revisão.
- Offline: o último pack publicado permanece consultável localmente.
- Sync clínico: continua usando o mecanismo existente; não misturar com knowledge sync.
- Auditoria: registrar pack, versão, origem, resultado de validação e rollback.
