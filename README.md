# Fluent 11 Launcher

Launcher Android em modo paisagem inspirado no **Windows 11 Dark Mica**, construído em **Kotlin + Jetpack Compose + Material 3**, com Barra de Tarefas fixa sobre aplicativos abertos, barra flutuante de janela, painel **Copilot IA** (Firebase AI Logic / Gemini) e widgets de área de trabalho.

---

## Como baixar o aplicativo (APK) pelo GitHub

Este repositório inclui um workflow automático do **GitHub Actions** que compila o projeto, executa os testes e publica o arquivo `.apk` pronto para instalar no Android.

### Opção 1 — Download direto pela aba **Releases** (recomendado)
1. Envie (ou sincronize) este projeto para o seu repositório no GitHub pelo menu de configurações do AI Studio (**Push to GitHub**).
2. Aguarde ~2–3 minutos para o workflow **Build & Release Android APK** concluir na aba **Actions**.
3. Na página inicial do seu repositório no GitHub, clique em **Releases** (menu lateral direito) e abra **Fluent 11 Launcher (Latest Build)**.
4. Em **Assets**, baixe **`Fluent11Launcher-debug.apk`** diretamente no celular ou PC e instale no Android.

### Opção 2 — Download pela aba **Actions**
1. Abra a aba **Actions** no seu repositório do GitHub.
2. Clique na execução mais recente de **Build & Release Android APK** (ou clique em **Run workflow** para gerar um novo build sob demanda).
3. Na seção **Artifacts**, baixe **`Fluent11Launcher-APK`** (compactado em `.zip` com o `.apk` dentro).

> **Nota sobre assinatura Release (opcional):** O `Fluent11Launcher-debug.apk` já vem assinado com a chave de depuração do projeto compatível com o Firebase App Check registrado. Caso queira gerar também um `Fluent11Launcher-release.apk` com sua própria chave de publicação, adicione os segredos `UPLOAD_KEYSTORE_BASE64`, `STORE_PASSWORD` e `KEY_PASSWORD` em **Settings → Secrets and variables → Actions** no GitHub.
