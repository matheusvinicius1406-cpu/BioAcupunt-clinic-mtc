#!/usr/bin/env bash
#
# pin_models.sh — baixa os modelos abertos, calcula SHA-256 e imprime as linhas
# exatas para colar em LocalModelCatalog.kt.
#
# POR QUE ISSO EXISTE
# -------------------
# O app se recusa a carregar qualquer modelo cujo SHA-256 não esteja fixado no
# código-fonte (ver ModelIntegrity: falha fechada, sempre). Isso não é burocracia:
# o arquivo do modelo é *executado* por um runtime nativo em C++. Um .litertlm
# corrompido ou trocado é, no melhor caso, um crash no meio da consulta — e, no
# pior, entrada controlada por terceiros dentro de código nativo.
#
# Enquanto os hashes não forem fixados, LocalModelCatalog.verifiable fica vazio,
# nenhum modelo local é oferecido, e o app cai para a nuvem. Isso é intencional.
#
# COMO USAR
# ---------
#   1. Aceite as licenças no Hugging Face (Gemma e Llama EXIGEM aceite; Qwen/Phi não).
#   2. export HF_TOKEN=hf_xxx
#   3. ./scripts/pin_models.sh
#   4. Cole a saída em LocalModelCatalog.kt
#   5. Suba os arquivos de out/ para o seu backend em /models/
#
# ATENÇÃO — LICENÇA
# -----------------
# Ao servir esses arquivos do seu próprio backend, a BioAcupunt vira REDISTRIBUIDORA.
# Gemma (Gemma Terms) e Llama (Llama Community License) vinculam quem redistribui.
# Qwen e Phi são Apache 2.0/MIT — bem menos encrenca. Se tiver dúvida jurídica,
# comece só pelos Apache/MIT.

set -euo pipefail

OUT_DIR="${OUT_DIR:-out/models}"
mkdir -p "$OUT_DIR"

if [[ -z "${HF_TOKEN:-}" ]]; then
  echo "ERRO: exporte HF_TOKEN (necessário para modelos com aceite de licença)." >&2
  exit 1
fi

# repo:arquivo:id_no_catalogo
MODELS=(
  "litert-community/Qwen2.5-1.5B-Instruct:qwen2.5-1.5b-instruct.litertlm:qwen2.5-1.5b-instruct"
  "litert-community/Phi-4-mini-instruct:phi-4-mini-instruct.litertlm:phi-4-mini-instruct"
  "litert-community/Gemma3-1B-IT:gemma-3-1b-it-int4.task:gemma-3-1b-it-int4"
  "litert-community/gemma-4-E2B-it-litert-lm:gemma-4-E2B-it.litertlm:gemma-4-e2b-it"
)

echo "== Baixando modelos =="
for entry in "${MODELS[@]}"; do
  IFS=':' read -r repo file id <<< "$entry"
  dest="$OUT_DIR/$file"

  if [[ -f "$dest" ]]; then
    echo "  [cache] $file"
  else
    echo "  [get]   $file  <- $repo"
    # -f: falha em HTTP 4xx/5xx em vez de salvar uma página de erro como .task
    curl -fL --progress-bar \
      -H "Authorization: Bearer $HF_TOKEN" \
      -o "$dest.part" \
      "https://huggingface.co/$repo/resolve/main/$file?download=true"
    mv "$dest.part" "$dest"   # rename atômico: só vira o nome real se completou
  fi
done

echo
echo "== Cole no LocalModelCatalog.kt =="
echo
for entry in "${MODELS[@]}"; do
  IFS=':' read -r repo file id <<< "$entry"
  dest="$OUT_DIR/$file"
  size=$(stat -c%s "$dest" 2>/dev/null || stat -f%z "$dest")
  hash=$(sha256sum "$dest" 2>/dev/null | cut -d' ' -f1 || shasum -a 256 "$dest" | cut -d' ' -f1)

  echo "  // $id"
  echo "  sizeBytes = ${size}L,"
  echo "  sha256 = \"$hash\","
  echo
done

echo "== Suba estes arquivos para <SEU_BACKEND>/models/ =="
ls -lh "$OUT_DIR"
