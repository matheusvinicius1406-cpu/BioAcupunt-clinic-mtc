#!/usr/bin/env python3
"""
Corrige caracteres estranhos e erros de formatação nos packs JSON da biblioteca.

Problemas corrigidos:
1. Literal \n (backslash + n) → quebra de linha real no conteúdo
2. Quebras de linha triplas ou mais → no máximo 1 linha vazia
3. Markdown: negrito não fechado (** sem fechamento)
4. Entidades HTML: &amp; → &, &lt; → <, &gt; → >
5. Espaços no início/fim do conteúdo

Uso:
  python scripts/fix_pack_content.py               # corrige tudo
  python scripts/fix_pack_content.py --dry-run     # apenas mostra o que mudaria
"""

import json
import glob
import os
import re
import sys

ASSETS_DIR = "app/src/main/assets/packs"


def fix_markdown(text: str) -> str:
    """Corrige erros de markdown no texto."""
    if not text:
        return text

    # Fecha negrito não fechado (**texto sem fechamento)
    bold_count = text.count("**")
    if bold_count % 2 != 0:
        # Adiciona ** no final para fechar o último negrito
        text += "**"

    # Entidades HTML comuns
    text = text.replace("&amp;", "&")
    text = text.replace("&lt;", "<")
    text = text.replace("&gt;", ">")
    text = text.replace("&quot;", '"')
    text = text.replace("&#39;", "'")
    text = text.replace("&nbsp;", " ")

    return text


def fix_content(text: str) -> str:
    """Corrige um texto: normaliza newlines e whitespace."""
    if not text:
        return text

    # 1. Substitui literal \n (backslash + n) por quebra de linha real
    text = text.replace("\\n", "\n")

    # 2. Corrige erros de markdown
    text = fix_markdown(text)

    # 3. Quebras de linha triplas+ → no máximo 1 linha vazia
    text = re.sub(r"\n{3,}", "\n\n", text)

    # 4. Remove espaços no início/fim
    text = text.strip()

    return text


def fix_json_file(filepath: str, is_pcdt: bool = False, dry_run: bool = False) -> int:
    """Corrige um arquivo JSON de pack. Retorna número de campos corrigidos."""
    with open(filepath, encoding="utf-8") as f:
        data = json.load(f)

    corrected_count = 0
    changes = []

    if is_pcdt:
        packs = data
        for pack in packs:
            for item in pack.get("items", []):
                for field in ["content", "summary", "title"]:
                    if field in item and item[field]:
                        original = item[field]
                        fixed = fix_content(original)
                        if fixed != original:
                            item[field] = fixed
                            corrected_count += 1
                            changes.append(f"    {item['id']}:{field}")
    else:
        items = data.get("items", [])
        for item in items:
            for field in ["content", "summary", "title"]:
                if field in item and item[field]:
                    original = item[field]
                    fixed = fix_content(original)
                    if fixed != original:
                        item[field] = fixed
                        corrected_count += 1
                        changes.append(f"    {item['id']}:{field}")

    if corrected_count > 0:
        fname = os.path.basename(filepath)
        print(f"[{fname}] {corrected_count} campos corrigidos:")
        for c in changes:
            print(c)
        if not dry_run:
            with open(filepath, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        print()

    return corrected_count


def main():
    dry_run = "--dry-run" in sys.argv
    if dry_run:
        print("=== MODO PREVIEW (--dry-run) ===")
    else:
        print("=== CORRIGINDO PACKS ===")

    total = 0

    # corrige acesso aberto
    open_access_files = sorted(
        glob.glob(os.path.join(ASSETS_DIR, "open_access", "*.json"))
    )
    print(f"Acesso aberto: {len(open_access_files)} arquivos")
    for filepath in open_access_files:
        total += fix_json_file(filepath, is_pcdt=False, dry_run=dry_run)

    # corrige PCDT
    pcdt_path = os.path.join(ASSETS_DIR, "pack_pcdt.json")
    if os.path.exists(pcdt_path):
        print(f"PCDT: 1 arquivo")
        total += fix_json_file(pcdt_path, is_pcdt=True, dry_run=dry_run)

    status = "[PREVIEW] " if dry_run else ""
    print(f"{status}Total: {total} campos corrigidos.")


if __name__ == "__main__":
    main()
