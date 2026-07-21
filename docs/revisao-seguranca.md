# Revisão de segurança — serviço de proteção

Revisão da camada de proteção do app + API (2026-07-21). **Sem vulnerabilidades
críticas encontradas.** Abaixo, o que está sólido e as melhorias sugeridas.

## O que está sólido

| Camada | Mecanismo | Estado |
|---|---|---|
| Armazenamento local | `EncryptedSharedPreferences` (AES-256-GCM / AES-256-SIV) | ✅ tokens, chave Gemini e credenciais cifrados em repouso |
| Sessão (backend) | JWT access + refresh com **rotação e detecção de reuso** (`auth_service.refresh`) | ✅ token roubado e reusado → revoga todas as sessões |
| Força bruta (cliente) | `AuthThrottle` — backoff exponencial + bloqueio após 5 falhas, persiste no reboot | ✅ |
| Força bruta (servidor) | rate-limit por IP (`slowapi`) em `/login` e agora `/register` | ✅ |
| Senha | Argon2 (`PasswordHasher`) no backend; nunca armazenada em claro | ✅ |
| Documento sensível (CPF) | HMAC-SHA256 determinístico, nunca o valor cru | ✅ |
| Anti-tamper | `AppHardening` detecta root, emulador, debugger, hook (Xposed/Frida) | ✅ defesa em profundidade |
| Transporte | TLS; `network_security_config` | ✅ |
| Backup no Drive | escopo mínimo `drive.file` (só arquivos do app), defesa contra path traversal no unzip | ✅ |

## Melhorias sugeridas (não bloqueantes)

1. **`SecurePreferences.biometricPassword`** guarda a senha real (cifrada) para o
   login biométrico. Preferível trocar por um token vinculado ao dispositivo
   (`androidx.security` / KeyStore) para não reter a senha. A branch
   `claude/project-html-comparison` traz `LocalPinAuth` (PBKDF2) que caminha nessa
   direção — vale portar depois. Ver [[branches-and-sync-engine]].
2. **`AuthThrottle`** usa `System.currentTimeMillis()` — o bloqueio é burlável mudando
   o relógio do aparelho. O rate-limit do servidor compensa, mas dá para endurecer com
   `elapsedRealtime()`.
3. **`AuthInterceptor`** usa `runBlocking` para ler o token dentro do interceptor
   OkHttp. Funciona (leitura rápida do prefs cifrado), mas idealmente o token seria lido
   de forma síncrona de um cache em memória.
4. **Senha mínima de 8 chars** no cadastro. Aceitável com Argon2; considerar checagem
   de senha vazada (k-anonymity/HIBP) no futuro.

## Cadastro de conta (novo)

`POST /api/v1/auth/register` cria clínica + usuária admin e emite tokens. Falha fechada:
e-mail duplicado → 409 (sem vazar qual conta), senha < 8 → 422 antes do hasher, e-mail
normalizado (lower+trim) para não criar conta-fantasma que o login não alcança. Coberto
por 5 testes e2e.
