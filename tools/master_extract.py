#!/usr/bin/env python3
"""
MASTER EXTRACT — Orquestrador Completo do Pipeline de Extração de Conhecimento.

Executa toda a cadeia:
  1. Gera catálogos (PCDT, NICE, WHO, KDIGO, ESC, AHA)
  2. Baixa PDFs/HTML de cada fonte
  3. Extrai seções com número de página (sourceRef = p. X)
  4. Aplica filtro de ruído + sinal clínico (Provenance = VERIFICAVEL)
  5. Gera packs JSON para a fila de curadoria

Uso:
    python tools/master_extract.py --todas           # Executa todas as fontes
    python tools/master_extract.py --fonte pcdt      # Apenas PCDT
    python tools/master_extract.py --fonte nice,who  # Múltiplas
    python tools/master_extract.py --listar          # Lista fontes disponíveis

Requer: pip install -r tools/requirements.txt
"""
from __future__ import annotations

import json
import logging
import re
import shutil
import subprocess
import sys
import tempfile
import time
import unicodedata
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field, asdict
from datetime import datetime
from pathlib import Path
from typing import Callable, Optional

import requests
from bs4 import BeautifulSoup

logging.basicConfig(level=logging.INFO, format="[%(name)s] %(levelname)s: %(message)s")
log = logging.getLogger("master")

# ═══════════════════════════════════════════════════════════════════
# CONFIG
# ═══════════════════════════════════════════════════════════════════

TOOLS_DIR = Path(__file__).parent
PROJECT_DIR = TOOLS_DIR.parent
PACKS_DIR = PROJECT_DIR / "packs"
CACHE_DIR = PROJECT_DIR / ".cache_extract"
PACKS_DIR.mkdir(parents=True, exist_ok=True)

SCRIPT_CATALOGOS = {
    "pcdt": TOOLS_DIR / "pcdt_catalogo.py",
    "nice": TOOLS_DIR / "diretrizes_nice_catalogo.py",
    "who": TOOLS_DIR / "diretrizes_who_catalogo.py",
    "kdigo": TOOLS_DIR / "diretrizes_kdigo_catalogo.py",
    "esc": TOOLS_DIR / "diretrizes_esc_catalogo.py",
    "aha": TOOLS_DIR / "diretrizes_aha_catalogo.py",
}

# ═══════════════════════════════════════════════════════════════════
# MODELOS
# ═══════════════════════════════════════════════════════════════════

@dataclass
class ExtractedItem:
    id: str
    title: str
    category: str
    summary: str
    content: str
    tags: list[str]
    citation: str
    sourceUrl: str
    sourceRef: str
    provenance: str = "VERIFICAVEL"

# ═══════════════════════════════════════════════════════════════════
# ETAPA 1: CATÁLOGO
# ═══════════════════════════════════════════════════════════════════

def gerar_catalogo(fonte: str) -> list[dict]:
    """Gera catálogo de fontes executando o script correspondente."""
    script = SCRIPT_CATALOGOS.get(fonte)
    if not script or not script.exists():
        log.error(f"Script não encontrado para fonte: {fonte}")
        return []

    with tempfile.TemporaryDirectory() as td:
        json_out = Path(td) / f"fontes_{fonte}.json"
        cmd = [sys.executable, str(script), str(json_out)]
        log.info(f"Executando: {' '.join(cmd)}")
        resultado = subprocess.run(cmd, capture_output=True, text=True)
        if resultado.returncode != 0:
            log.error(f"Falha ao catalogar {fonte}: {resultado.stderr}")
            return []
        if json_out.exists():
            entradas = json.loads(json_out.read_text(encoding="utf-8"))
            log.info(f"✓ {fonte}: {len(entradas)} entradas catalogadas")
            return entradas
    return []

# ═══════════════════════════════════════════════════════════════════
# ETAPA 2: DOWNLOAD + EXTRAÇÃO
# ═══════════════════════════════════════════════════════════════════

def extrair_de_html(entry: dict, cache_dir: Path) -> list[ExtractedItem]:
    """Extrai seções de uma página HTML de diretriz clínica."""
    url = entry.get("url") or entry.get("url_pagina", "")
    slug_id = entry.get("prefixo_id", "doc")
    fonte = entry.get("fonte", "")
    categoria = entry.get("categoria", "CLINICA_MEDICA")

    try:
        r = requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, timeout=120)
        r.raise_for_status()
        soup = BeautifulSoup(r.text, "lxml")

        # Remove elementos não-clínicos
        for tag in soup.find_all(["script", "style", "nav", "footer", "header", "aside", "noscript"]):
            tag.decompose()

        # Tenta encontrar o conteúdo principal
        main = (soup.find("main") or soup.find("article") or
                soup.find("div", class_=re.compile(r"(content|guidance|recommend|section|body)", re.I)) or
                soup.find("div", id=re.compile(r"(content|guidance)", re.I)))

        if main:
            content_elem = main
        else:
            content_elem = soup

        # Extrai seções por cabeçalhos (h1-h3)
        items: list[ExtractedItem] = []
        headers = content_elem.find_all(["h1", "h2", "h3", "h4", "h5", "h6"])
        if not headers:
            texto = content_elem.get_text(separator="\n", strip=True)
            items.extend(_recortar_texto(texto, slug_id, categoria, url, fonte, entry.get("tags", [])))
        else:
            for i, h in enumerate(headers):
                titulo = h.get_text(strip=True)[:120]
                if not titulo:
                    continue
                # Pega o texto entre este header e o próximo
                body_parts = []
                irmão = h.find_next_sibling()
                prox_header = h.find_next(["h1", "h2", "h3"])
                while irmão and irmão != prox_header:
                    if irmão.name not in ("script", "style", "nav"):
                        body_parts.append(irmão.get_text(separator=" ", strip=True))
                    irmão = irmão.find_next_sibling()
                body = "\n".join(body_parts)
                body = _limpar(body)
                if len(body) < 200:
                    continue

                item = ExtractedItem(
                    id=f"{slug_id}_{i+1:04d}",
                    title=titulo[:120],
                    category=categoria,
                    summary=" ".join(body.split())[:240],
                    content=body[:2400],
                    tags=entry.get("tags", []),
                    citation=f"{entry.get('titulo', '')} — {titulo}",
                    sourceUrl=url,
                    sourceRef=titulo[:80],
                    provenance="VERIFICAVEL",
                )
                items.append(item)

        # Se extraiu muito pouco, usa o texto completo recortado
        if len(items) < 2:
            texto = content_elem.get_text(separator="\n", strip=True)
            items = _recortar_texto(texto, slug_id, categoria, url, fonte, entry.get("tags", []))

        log.info(f"  → {len(items)} seções extraídas de HTML")
        return items

    except Exception as e:
        log.warning(f"  Falha ao extrair HTML {url}: {e}")
        return []


def extrair_de_pdf(entry: dict, pdf_path: str) -> list[ExtractedItem]:
    """Extrai seções de um PDF usando pdftotext."""
    if not shutil.which("pdftotext"):
        log.warning("  pdftotext não encontrado. Use: apt install poppler-utils")
        return []

    slug_id = entry.get("prefixo_id", "doc")
    categoria = entry.get("categoria", "CLINICA_MEDICA")
    url = entry.get("url") or entry.get("url_pagina", "")
    fonte = entry.get("fonte", "")

    pdf = Path(pdf_path)
    txt = pdf.with_suffix(".txt")

    try:
        subprocess.run(
            ["pdftotext", "-layout", "-enc", "UTF-8", str(pdf), str(txt)],
            check=True, capture_output=True, timeout=120,
        )
        texto = txt.read_text(encoding="utf-8", errors="replace")
    except Exception as e:
        log.warning(f"  Falha pdftotext: {e}")
        return []

    # Separa por páginas (form feed = \f)
    paginas = texto.split("\f")
    HEADING = re.compile(
        r"^\s{0,8}((?:[\dIVXL]+\.){0,4}[\dIVXL]+\s+[A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\n]{4,80}"
        r"|[A-ZÁÉÍÓÚÂÊÔÃÕÇ][A-ZÁÉÍÓÚÂÊÔÃÕÇ\s\-/,]{8,70})\s*$"
    )
    VETO = re.compile(
        r"portaria\s+conjunta|aprova\s+o\s+protocolo|di[áa]rio\s+oficial|"
        r"termo\s+de\s+esclarecimento|assinatura\s+do\s+(?:paciente|respons)|"
        r"equipe\s+t[ée]cnica|elabora[çc][ãa]o\s+t[ée]cnica",
        re.I,
    )
    SINAIS = {
        "posologia": re.compile(
            r"\b\d[\d.,]*\s?(?:mg|mcg|µg|g|ml|mL|UI|mmol|mEq)\b"
            r"|\bmg\s?/\s?(?:kg|m²|m2|dia|dL)\b",
        ),
        "criterio": re.compile(
            r"crit[ée]rios?\s+de\s+(?:diagn[óo]stic|inclus[ãa]o|exclus[ãa]o)"
            r"|define-se|considera-se|diagn[óo]stico", re.I,
        ),
        "conduta": re.compile(
            r"recomenda-se|est[áa]\s+indicad[ao]|deve-se"
            r"|tratamento\s+(?:de|da|do|com|deve|primeira\s+linha)", re.I,
        ),
    }

    items: list[ExtractedItem] = []
    for pageno, page in enumerate(paginas, start=1):
        lines = page.splitlines()
        marks = [i for i, l in enumerate(lines) if HEADING.match(l)]

        if not marks:
            body = "\n".join(l.rstrip() for l in lines if l.strip())
            body = _limpar(body)
            if len(body) < 320:
                continue
            # Filtro de veto
            if VETO.search(body):
                continue
            # Sinal clínico
            presentes = [n for n, rx in SINAIS.items() if rx.search(body)]
            if len(presentes) < 2:
                continue
            n = len(items) + 1
            items.append(ExtractedItem(
                id=f"{slug_id}_{n:04d}",
                title=f"{entry.get('titulo', '')[:60]} — p. {pageno}",
                category=categoria,
                summary=" ".join(body.split())[:240],
                content=body[:2400],
                tags=entry.get("tags", []),
                citation=f"{entry.get('titulo', '')}, p. {pageno}",
                sourceUrl=url,
                sourceRef=f"p. {pageno}",
                provenance="VERIFICAVEL",
            ))
            continue

        for n_m, start in enumerate(marks):
            end = marks[n_m + 1] if n_m + 1 < len(marks) else len(lines)
            title = lines[start].strip()
            body = "\n".join(l.rstrip() for l in lines[start + 1:end] if l.strip())
            body = _limpar(body)
            if len(body) < 320:
                continue
            if VETO.search(body) or VETO.search(title.lower()):
                continue
            presentes = [n for n, rx in SINAIS.items() if rx.search(body)]
            if len(presentes) < 2:
                continue
            n = len(items) + 1
            items.append(ExtractedItem(
                id=f"{slug_id}_{n:04d}",
                title=(title[:100] or f"p. {pageno}"),
                category=categoria,
                summary=" ".join(body.split())[:240],
                content=body[:2400],
                tags=entry.get("tags", []),
                citation=f"{entry.get('titulo', '')}, p. {pageno}",
                sourceUrl=url,
                sourceRef=f"p. {pageno}",
                provenance="VERIFICAVEL",
            ))

    log.info(f"  → {len(items)} seções extraídas de PDF")
    return items


def _recortar_texto(texto: str, slug_id: str, categoria: str, url: str,
                    fonte: str, tags: list[str]) -> list[ExtractedItem]:
    """Recorta texto longo em chunks de ~2000 caracteres."""
    items = []
    # Divide em parágrafos
    paragrafos = [p.strip() for p in texto.split("\n\n") if len(p.strip()) > 200]
    for i, p in enumerate(paragrafos[:50]):  # max 50 parágrafos
        items.append(ExtractedItem(
            id=f"{slug_id}_{i+1:04d}",
            title=f"{fonte[:60]} — seção {i+1}",
            category=categoria,
            summary=p[:240],
            content=p[:2400],
            tags=tags,
            citation=fonte,
            sourceUrl=url,
            sourceRef=f"seção {i+1}",
            provenance="VERIFICAVEL",
        ))
    return items


def _limpar(texto: str) -> str:
    texto = re.sub(r"[ \t]{3,}", "  ", texto)
    texto = re.sub(r"\n{3,}", "\n\n", texto)
    return texto.strip()


# ═══════════════════════════════════════════════════════════════════
# ETAPA 3: PROCESSAR UMA ENTRADA DO CATÁLOGO
# ═══════════════════════════════════════════════════════════════════

def processar_entrada(entry: dict, cache_dir: Path) -> list[ExtractedItem]:
    """Processa uma entrada do catálogo: download + extração."""
    url = entry.get("url") or entry.get("url_pagina", "")
    fonte = entry.get("fonte", "")

    # Determina estratégia baseada na fonte
    is_pcdt = "gov.br" in url
    is_who = "who.int" in url
    is_nice = "nice.org.uk" in url

    if is_pcdt or is_who:
        # Tenta baixar PDF
        slug_id = entry.get("prefixo_id", "doc")
        pdf_path = cache_dir / f"{slug_id}.pdf"

        if pdf_path.exists() and pdf_path.stat().st_size > 5000:
            return extrair_de_pdf(entry, str(pdf_path))

        # Download
        try:
            if is_pcdt:
                base = url.rstrip("/")
                if base.endswith("/view"):
                    base = base[: -len("/view")]
                pdf_url = base + "/@@download/file"
            else:
                pdf_url = url

            r = requests.get(pdf_url, headers={"User-Agent": "Mozilla/5.0"},
                           timeout=120, stream=True)
            r.raise_for_status()
            data = r.content
            if not data.startswith(b"%PDF"):
                if is_who:
                    # WHO fallback: scrape da página
                    soup = BeautifulSoup(r.text, "lxml")
                    for a in soup.find_all("a", href=True):
                        h = a["href"]
                        if ".pdf" in h and ("who.int" in h or "cdn" in h):
                            r2 = requests.get(h, headers={"User-Agent": "Mozilla/5.0"},
                                            timeout=120)
                            data = r2.content
                            break
                if not data.startswith(b"%PDF"):
                    return extrair_de_html(entry, cache_dir)

            pdf_path.write_bytes(data)
            return extrair_de_pdf(entry, str(pdf_path))
        except Exception as e:
            log.warning(f"  Download falhou, fallback HTML: {e}")
            return extrair_de_html(entry, cache_dir)
    else:
        # NICE e demais: HTML
        return extrair_de_html(entry, cache_dir)


# ═══════════════════════════════════════════════════════════════════
# ETAPA 4: GERAR PACK
# ═══════════════════════════════════════════════════════════════════

def gerar_pack(fonte: str, entradas: list[dict], items: list[ExtractedItem]) -> dict:
    """Gera um pack JSON no formato esperado pela curadoria."""
    if not items:
        return {"source": fonte, "items": []}

    # Nome da fonte principal
    if entradas:
        nome_fonte = entradas[0].get("fonte", fonte)
    else:
        nome_fonte = fonte

    # Converte para dict
    pack = {
        "source": nome_fonte,
        "items": [asdict(i) for i in items],
    }
    return pack


# ═══════════════════════════════════════════════════════════════════
# CLI
# ═══════════════════════════════════════════════════════════════════

def listar():
    print("\nFONTES DISPONÍVEIS PARA EXTRAÇÃO:")
    print("─" * 50)
    for slug, script in SCRIPT_CATALOGOS.items():
        status = "✓" if script.exists() else "✗"
        print(f"  [{status}] {slug}")
    print()
    print("USO:")
    print("  python tools/master_extract.py --todas")
    print("  python tools/master_extract.py --fonte pcdt")
    print("  python tools/master_extract.py --fonte nice,who")
    print("  python tools/master_extract.py --listar")


def main():
    if len(sys.argv) < 2 or "--listar" in sys.argv:
        listar()
        return

    fontes_para_executar = []
    if "--todas" in sys.argv:
        fontes_para_executar = list(SCRIPT_CATALOGOS.keys())
    elif "--fonte" in sys.argv:
        idx = sys.argv.index("--fonte") + 1
        if idx < len(sys.argv):
            fontes_para_executar = [f.strip() for f in sys.argv[idx].split(",")]

    if not fontes_para_executar:
        listar()
        return

    for fonte in fontes_para_executar:
        log.info(f"\n{'='*60}")
        log.info(f"PROCESSANDO FONTE: {fonte.upper()}")
        log.info(f"{'='*60}")

        # Etapa 1: Catálogo
        entradas = gerar_catalogo(fonte)
        if not entradas:
            log.error(f"Nenhuma entrada catalogada para {fonte}")
            continue

        # Cache por fonte
        cache_fonte = CACHE_DIR / fonte
        cache_fonte.mkdir(parents=True, exist_ok=True)

        # Etapa 2: Download + Extração (paralelo)
        log.info(f"Extraindo {len(entradas)} entradas...")
        todos_items: list[ExtractedItem] = []
        falhas = 0

        with ThreadPoolExecutor(max_workers=3) as pool:
            futuros = {pool.submit(processar_entrada, e, cache_fonte): e for e in entradas}
            for n, fut in enumerate(as_completed(futuros), 1):
                entry = futuros[fut]
                try:
                    items = fut.result()
                    todos_items.extend(items)
                    status = f"{len(items)} itens" if items else "vazio"
                    log.info(f"  [{n}/{len(entradas)}] {status} — {entry.get('titulo', '?')[:50]}")
                except Exception as e:
                    falhas += 1
                    log.warning(f"  [{n}/{len(entradas)}] ❌ {entry.get('titulo', '?')[:50]}: {e}")

        # Etapa 3: Gerar pack
        pack = gerar_pack(fonte, entradas, todos_items)
        pack_path = PACKS_DIR / f"pack_{fonte}.json"
        pack_path.write_text(
            json.dumps([pack], ensure_ascii=False, indent=1),
            encoding="utf-8",
        )

        log.info(f"\n✓ {fonte}: {len(todos_items)} itens extraídos ({falhas} falhas)")
        log.info(f"  Pack salvo: {pack_path}")

    # Resumo final
    print(f"\n{'='*60}")
    print("RESUMO DA EXTRAÇÃO")
    print(f"{'='*60}")
    for fonte in fontes_para_executar:
        pack_path = PACKS_DIR / f"pack_{fonte}.json"
        if pack_path.exists():
            dados = json.loads(pack_path.read_text(encoding="utf-8"))
            total = sum(len(p.get("items", [])) for p in dados)
            print(f"  ✓ {fonte}: {total} itens → {pack_path}")
        else:
            print(f"  ✗ {fonte}: falhou")


if __name__ == "__main__":
    main()
