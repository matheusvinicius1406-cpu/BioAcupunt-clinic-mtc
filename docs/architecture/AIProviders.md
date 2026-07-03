# BioAcupunt AI Platform — Provider Framework

## Overview
The Provider Framework supports remote APIs, local execution, and hybrid modes.
Providers are discovered via registries, selected by `ScoredAiOrchestrator`, and authenticated via `AiSecretsProvider`.

## Request Flow
1. UI/domain creates `AiRequest`
2. `AiRepositoryImpl.generate()` delegates to `AiOrchestrator.execute()`
3. Orchestrator resolves candidates using:
    - capabilities
    - routing rules
    - metadata
    - health status
    - score policy
4. Provider generates response
5. Telemetry records structured event

## Provider Lifecycle
- Registration
- Initialization
- Authentication through `AiSecretsProvider`
- Health check through `HealthCheckProvider`
- Execution
- Monitoring
- Discard/reinit when invalidated

## Health System
- `ProviderHealth` stores status, latency, success rate, fallback reason
- `HealthRegistry` keeps latest health data
- `DefaultHealthCheckProvider` populates health registry

## Security
- Credentials via `AndroidAiSecretsProvider` backed by encrypted preferences
- Clinical telemetry separation through structured events
- Privacy restrictions enforce local-only execution when requested

## Observability
- `AiTelemetrySink` / `TelemetryBridge` route structured telemetry
- Events include provider/model/capabilities/tools/latency/cost/fallback/error metadata
