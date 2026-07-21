# Backup no Google Drive

O backup do banco clínico fica em **Ajustes → aba Segurança → Backup no Google Drive**.

A médica **loga na conta Google** e o backup vai direto para o Drive dela via API, com
escopo mínimo `drive.file` — o app só enxerga os arquivos que ele mesmo criou, nunca o
resto do Drive dela. "Backup" empacota o banco (checkpoint do WAL + zip) e envia;
"Restaurar" baixa o backup mais recente, sobrescreve o banco e reinicia o app.

Todo o código está pronto (`GoogleDriveClient`, `BackupManager`, `BackupArchive`,
`AppRestarter`, wired em `AppContainer` e na `BackupCard`). Mas, por segurança, o Google
**exige** que exista um cliente OAuth ligado ao app. **Isso só você pode criar** — nenhum
código substitui esse passo. Sem ele, "Entrar com Google" retorna erro de configuração
(código 10, DEVELOPER_ERROR).

## Passo a passo (uma vez, na sua conta Google Cloud)

1. Acesse <https://console.cloud.google.com/> e crie (ou escolha) um projeto.
2. **APIs e serviços → Biblioteca →** ative a **Google Drive API**.
3. **APIs e serviços → Tela de consentimento OAuth:** configure (tipo "Externo" serve),
   adicione a médica como usuária de teste (ou publique o app).
4. **APIs e serviços → Credenciais → Criar credenciais → ID do cliente OAuth →
   Android.** Preencha:
   - **Nome do pacote:** o `applicationId` do app (veja em `app/build.gradle.kts`).
   - **Impressão digital SHA-1:** da chave que assina o APK. Para debug:
     ```
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
       -storepass android -keypass android
     ```
     Para release, use a SHA-1 do seu keystore de release. Pode cadastrar as duas.
5. Pronto. Não é preciso colar nenhuma chave no app — o Google associa o cliente pelo
   par (pacote + SHA-1). Reinstale o app assinado com essa chave e o login passa a funcionar.

## Limitações honestas

- Só roda em aparelho com **Google Play Services** (a maioria dos Androids; não em
  emuladores sem GApps).
- Não dá para testar o login/upload real no ambiente de build do Claude (sem Play
  Services, sem a sua conta) — a validação real é rodar num celular depois do passo acima.
  O que **é** testado aqui: o empacotamento/desempacotamento do backup (`BackupArchiveTest`,
  round-trip do zip) e a compilação de toda a camada.
