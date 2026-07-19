# Backup no Google Drive

Há **dois caminhos** de backup, e os dois já estão no app (Ajustes → aba Clínica):

## 1. Backup via seletor do sistema (funciona já, sem configuração)

*Ajustes → Backup e restauração → Fazer backup / Restaurar.*

Abre o seletor de arquivos do Android. O **Google Drive** aparece ali como destino
(basta ter o app do Drive instalado). Não precisa login, OAuth nem nada. É o caminho
recomendado para começar hoje.

## 2. Login com a conta Google + Drive da conta (precisa da SUA configuração)

*Ajustes → Conta Google (Drive) → Entrar com Google.*

Aqui a médica **loga na conta Google** e o backup vai direto para o Drive dela via API
(escopo mínimo `drive.file` — o app só vê os arquivos que ele mesmo criou). Todo o código
está pronto. Mas, por segurança, o Google **exige** que exista um cliente OAuth ligado ao
app. **Isso só você pode criar** — nenhum código substitui esse passo. Sem ele, "Entrar
com Google" retorna erro de configuração (código 10, DEVELOPER_ERROR).

### Passo a passo (uma vez)

1. Acesse <https://console.cloud.google.com/> e crie (ou escolha) um projeto.
2. **APIs e serviços → Biblioteca →** ative a **Google Drive API**.
3. **APIs e serviços → Tela de consentimento OAuth:** configure (tipo "Externo" serve),
   adicione a médica como usuária de teste (ou publique).
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

### Limitações honestas

- Só roda em aparelho com **Google Play Services** (a maioria dos Androids; não em
  emuladores sem GApps).
- Não dá para testar isto no ambiente de build do Claude (sem Play Services, sem a sua
  conta) — a validação real é você rodar num celular depois do passo acima.
- Enquanto o cliente OAuth não existir, use o **caminho 1** (seletor), que independe disso.
