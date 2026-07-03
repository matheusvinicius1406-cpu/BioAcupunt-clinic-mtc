# BioAcupunt AI Platform — Architectural Foundation

## Scope
This document records the foundational design decisions for the AI platform (`com.bioacupunt.ai`).
It is the source of truth for the core contracts, orchestration model, security constraints, and extension points.

## Design Goals
- Hybrid offline/online execution without provider lock-in.
- Capability-based routing, not model-name routing.
- Strict separation between clinical data, telemetry, and provider credentials.
- Extensibility without core changes: providers, tools, agents, workflows, vector stores.

## Core Contracts
- `AiRequest`: generic task envelope with capabilities, constraints, privacy, attachments.
- `AiResult`: normalized response with provider/model/cost/tool/agent metadata.
- `AiProvider`: provider contract with metadata and capability declaration.
- `AiRepository`: single entry point for generation/streaming.
- `AiTelemetry` / `AiTelemetryEvent`: structured observability.
- `AiConfigManager` / `AiSecretsProvider`: credential and policy management.

## Orchestration
- Default orchestrator: `ScoredAiOrchestrator`.
- Selection dimensions: capabilities, availability, privacy rules, cost constraints, latency constraints, context window, user/app policies.
- Scoring is pluggable via `AiScoringPolicy`.
- Routing constraints via `AiRoutingRules`.
- Fallback chain emits telemetry with `fallbackUsed=true`.

## Provider Model
- Providers declare `AiProviderMetadata` (execution type, pricing, hardware, capabilities).
- Models declare `AiModelDescriptor` with capability set and context limits.
- No core logic depends on provider/model names.

## Security and LGPD
- Credentials via `AiSecretsProvider`, default Android implementation backed by encrypted storage.
- Clinical data separation: telemetry must not contain raw clinical content unless explicitly allowed.
- Privacy restrictions: `AiPrivacyRestriction` can force local-only execution.

## Extension Points
- New provider: implement `AiProvider` and register.
- New tool: implement `AiTool` and register in `ToolRegistry`.
- New agent: implement `Agent` and register in `AgentRegistry`.
- New workflow/plugin: implement `AiPlugin` or workflow registry interface.
- New telemetry sink: implement `AiTelemetrySink` and bridge via `TelemetryBridge`.

## Compatibility
- Legacy `GeminiEngine` and `AiAgents` remain as adapters.
- New code must not depend on legacy AI classes directly.
