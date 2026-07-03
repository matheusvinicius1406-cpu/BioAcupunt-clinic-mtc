# BioAcupunt AI Platform — Provider SDK

## Purpose
This document is the canonical guide for adding new providers without changing the AI core.

## Extension Contract
New providers must implement `com.bioacupunt.ai.core.AiProvider`.

### Required members
- `id`: stable provider identifier
- `displayName`: human-readable name
- `capabilities`: `AiProviderCapabilities`
- `metadata`: `AiProviderMetadata`
- `models`: `List<AiModelDescriptor>`
- `isAvailable(): Boolean`
- `generate(request: AiRequest): Result<AiResult>`

### Optional members
- `capabilitiesForModel(modelId: String): AiProviderCapabilities`

## Registration
Register in `AppContainer.aiOrchestrator` or a dedicated provider bootstrap.
Example:
<REDACTED>

## Capabilities
Declare capabilities in `AiCapability` first.
Add new entries only when semantically stable.

## Models
Use `AiModelDescriptor` to declare context window, local/remote status, and capability set.
Model selection is capability-based.

## Security
Use `AiSecretsProvider` for API keys and tokens.
Never hardcode credentials.

## Health
Implement `com.bioacupunt.ai.data.provider.LocalProvider` for local providers.
Use `DefaultHealthCheckProvider` for health verification and update `HealthRegistry`.

## Testing
Use `MockProvider` or `FakeProvider` for deterministic tests.
Verify providers via ad-hoc Python scripts in `C:\Users\mathe\AppData\Local\Temp` if Gradle/JDK is unavailable.
