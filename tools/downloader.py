#!/usr/bin/env python3
"""
DOWNLOADER UNIVERSAL — baixa PDFs de múltiplas fontes públicas de saúde.

Cada fonte tem sua própria estratégia de download porque cada CMS/publicador
organiza seus PDFs de forma diferente. Este módulo abstrai isso.

Fontes suportadas:
- gov.br (Plone CMS) → /@@download/file
- WHO (CDN) → scrape da página para encontrar o PDF real
- NICE (HTML guidelines) → scrape completo da página HTML
- KDIGO → scraper do site
- ESC → scraper do site
- AHA/ACC → scraper de artigos DOI
"""
from __future__ import annotations

import hashlib
import json
import logging
import os
import re
import shutil
import subprocess
import tempfile
import time
import unicodedata
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup

log = logging.getLogger("downloader")
logging.basicConfig(level=logging.INFO, format="[%(name)s] %(levelname)s: %(message)s")

USER_AGENT = "Mozilla/5.0 (compatible; BioAcupuntExtractor/1.0; +https://bioacupunt.com)"
TIMEOUT = 120
MAX_WORKERS = 4
RETRIES = 3


@dataclass
class DownloadResult:
    """Resultado do download."""
    url_original: str
    url_pdf: str = ""
    caminho_pdf: str = ""
    html_texto: str = ""
    sucesso: bool = False
    erro: str = ""
    hash_sha256: str = ""
    cache_hit: bool = False


def _slug(texto: str) -> str:
    """Normaliza texto para nome de arquivo."""
    s = unicodedata.normalize("NFD", texto).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-zA-Z0-9]+", "_", s).strip("_").lower()[:60]


def _headers() -> dict:
    return {"User-Agent": USER_AGENT}


# ═══════════════════════════════════════════════════════════════════════
# ESTRATÉGIAS DE DOWNLOAD POR FONTE
# ═══════════════════════════════════════════════════════════════════════

def baixar_pcdt(entry: dict, cache_dir: Path) -> DownloadResult:
    """Baixa PDF do gov.br (Plone CMS). Padrão: /@@download/file"""
    url = entry.get("url") or entry.get("url_pagina", "")
    base = url.rstrip("/")
    if base.endswith("/view"):
        base = base[: -len("/view")]
    pdf_url = base + "/@@download/file"

    slug_id = entry.get("prefixo_id", _slug(entry.get("titulo", "doc")))
    dest = cache_dir / f"{slug_id}.pdf"

    if dest.exists() and dest.stat().st_size > 5000:
        return DownloadResult(
            url_original=url, url_pdf=pdf_url, caminho_pdf=str(dest),
            sucesso=True, cache_hit=True,
            hash_sha256=hashlib.sha256(dest.read_bytes()).hexdigest(),
        )

    ultimo_erro = ""
    for t in range(RETRIES):
        try:
            r = requests.get(pdf_url, headers=_headers(), timeout=TIMEOUT, stream=True)
            r.raise_for_status()
            data = r.content
            if not data.startswith(b"%PDF"):
                alt = pdf_url.replace("/@@download/file", "/download/file")
                r2 = requests.get(alt, headers=_headers(), timeout=TIMEOUT)
                r2.raise_for_status()
                data = r2.content
                if not data.startswith(b"%PDF"):
                    raise ValueError(f"Não é PDF: {url}")
            dest.write_bytes(data)
            return DownloadResult(
                url_original=url, url_pdf=pdf_url, caminho_pdf=str(dest),
                sucesso=True, hash_sha256=hashlib.sha256(data).hexdigest(),
            )
        except Exception as e:
            ultimo_erro = str(e)
            log.warning(f"  tentativa {t+1}/{RETRIES}: {e}")
            time.sleep(2 * (t + 1))

    return DownloadResult(url_original=url, sucesso=False, erro=ultimo_erro)


def baixar_who(entry: dict, cache_dir: Path) -> DownloadResult:
    """Baixa PDF da OMS — faz scrape da página para encontrar link do PDF no CDN."""
    url = entry.get("url") or entry.get("url_pagina", "")

    slug_id = entry.get("prefixo_id", _slug(entry.get("titulo", "doc")))
    dest = cache_dir / f"{slug_id}.pdf"

    if dest.exists() and dest.stat().st_size > 5000:
        return DownloadResult(
            url_original=url, caminho_pdf=str(dest), sucesso=True, cache_hit=True,
            hash_sha256=hashlib.sha256(dest.read_bytes()).hexdigest(),
        )

    try:
        r = requests.get(url, headers=_headers(), timeout=TIMEOUT)
        r.raise_for_status()
        soup = BeautifulSoup(r.text, "lxml")

        # WHO coloca o PDF em diversos lugares:
        # 1. Link com "Download" no texto
        pdf_link = None
        for a in soup.find_all("a", href=True):
            texto = a.get_text(strip=True).lower()
            href = a["href"]
            if "download" in texto and (href.endswith(".pdf") or ".pdf" in href):
                pdf_link = urljoin(url, href)
                break
            if href.endswith(".pdf") and ("who.int" in href or "cdn.who" in href):
                pdf_link = urljoin(url, href)
                break

        # 2. Meta tag com PDF
        if not pdf_link:
            for meta in soup.find_all("meta", attrs={"property": "og:image"}):
                content = meta.get("content", "")
                if ".pdf" in content or "publication" in content:
                    # WHO CDN pattern
                    pdf_link = content.replace("/cover/", "/pdf/").replace(".jpg", ".pdf")
                    break

        # 3. Link com ISBN na URL
        if not pdf_link:
            for a in soup.find_all("a", href=True):
                href = a["href"]
                if ".pdf" in href and ("iris" in href or "cdn.who" in href or "apps.who" in href):
                    pdf_link = urljoin(url, href)
                    break

        if pdf_link:
            log.info(f"  PDF encontrado: {pdf_link}")
            r2 = requests.get(pdf_link, headers=_headers(), timeout=TIMEOUT)
            r2.raise_for_status()
            data = r2.content
            if data.startswith(b"%PDF"):
                dest.write_bytes(data)
                return DownloadResult(
                    url_original=url, url_pdf=pdf_link, caminho_pdf=str(dest),
                    sucesso=True, hash_sha256=hashlib.sha256(data).hexdigest(),
                )

        # 4. Se não achou PDF, extrai o HTML da página como fallback
        texto = soup.get_text(separator="\n", strip=True)
        if len(texto) > 500:
            html_path = cache_dir / f"{slug_id}.html"
            html_path.write_text(r.text, encoding="utf-8")
            return DownloadResult(
                url_original=url, html_texto=texto, sucesso=True,
                erro="PDF não encontrado, HTML extraído como fallback",
            )

        return DownloadResult(url_original=url, sucesso=False, erro="PDF não encontrado na página")

    except Exception as e:
        return DownloadResult(url_original=url, sucesso=False, erro=str(e))


def baixar_nice(entry: dict, cache_dir: Path) -> DownloadResult:
    """Extrai conteúdo NICE — guidelines são HTML, não PDF.

    NICE não serve PDFs das diretrizes principais (apenas sumários executivos).
    Extraímos o conteúdo HTML completo da guideline.
    """
    url = entry.get("url") or entry.get("url_pagina", "")
    slug_id = entry.get("prefixo_id", _slug(entry.get("titulo", "doc")))
    html_path = cache_dir / f"{slug_id}.html"

    if html_path.exists():
        texto = html_path.read_text(encoding="utf-8")
        return DownloadResult(
            url_original=url, html_texto=texto, sucesso=True, cache_hit=True,
        )

    try:
        r = requests.get(url, headers=_headers(), timeout=TIMEOUT)
        r.raise_for_status()
        soup = BeautifulSoup(r.text, "lxml")

        # Remove elementos não-clínicos
        for tag in soup.find_all(["script", "style", "nav", "footer", "header", "aside"]):
            tag.decompose()

        # Tenta encontrar o conteúdo principal
        main = (soup.find("main") or soup.find("article") or
                soup.find("div", class_=re.compile(r"(content|guidance|recommend)")) or
                soup.find("div", id=re.compile(r"(content|guidance)")))

        if main:
            texto = main.get_text(separator="\n", strip=True)
        else:
            texto = soup.get_text(separator="\n", strip=True)

        # Salva o HTML original e o texto extraído
        html_path.write_text(r.text, encoding="utf-8")
        txt_path = cache_dir / f"{slug_id}.txt"
        txt_path.write_text(texto, encoding="utf-8")

        if len(texto) > 1000:
            return DownloadResult(
                url_original=url, html_texto=texto, sucesso=True,
                erro="HTML (NICE não serve PDF para guidelines completas)",
            )

        return DownloadResult(url_original=url, sucesso=False, erro=f"Conteúdo insuficiente: {len(texto)} chars")

    except Exception as e:
        return DownloadResult(url_original=url, sucesso=False, erro=str(e))


def baixar_html_generico(entry: dict, cache_dir: Path) -> DownloadResult:
    """Download genérico para sites que servem conteúdo HTML (KDIGO, ESC, AHA)."""
    url = entry.get("url") or entry.get("url_pagina", "")
    slug_id = entry.get("prefixo_id", _slug(entry.get("titulo", "doc")))
    html_path = cache_dir / f"{slug_id}.html"
    txt_path = cache_dir / f"{slug_id}.txt"

    # Tenta baixar PDF primeiro
    pdf_dest = cache_dir / f"{slug_id}.pdf"
    if pdf_dest.exists() and pdf_dest.stat().st_size > 5000:
        return DownloadResult(
            url_original=url, caminho_pdf=str(pdf_dest), sucesso=True, cache_hit=True,
            hash_sha256=hashlib.sha256(pdf_dest.read_bytes()).hexdigest(),
        )

    # Tenta PDF
    try:
        r = requests.get(url, headers=_headers(), timeout=TIMEOUT, allow_redirects=True)
        r.raise_for_status()
        ct = r.headers.get("Content-Type", "")
        if "pdf" in ct or r.content.startswith(b"%PDF"):
            pdf_dest.write_bytes(r.content)
            return DownloadResult(
                url_original=url, url_pdf=url, caminho_pdf=str(pdf_dest),
                sucesso=True, hash_sha256=hashlib.sha256(r.content).hexdigest(),
            )
    except Exception:
        pass

    # Fallback: extrai HTML
    try:
        r = requests.get(url, headers=_headers(), timeout=TIMEOUT)
        r.raise_for_status()
        soup = BeautifulSoup(r.text, "lxml")
        for tag in soup.find_all(["script", "style", "nav", "footer"]):
            tag.decompose()

        # Procura link de PDF na página
        for a in soup.find_all("a", href=True):
            href = a["href"]
            if ".pdf" in href.lower():
                pdf_link = urljoin(url, href)
                try:
                    r2 = requests.get(pdf_link, headers=_headers(), timeout=TIMEOUT)
                    r2.raise_for_status()
                    if r2.content.startswith(b"%PDF"):
                        pdf_dest.write_bytes(r2.content)
                        return DownloadResult(
                            url_original=url, url_pdf=pdf_link, caminho_pdf=str(pdf_dest),
                            sucesso=True, hash_sha256=hashlib.sha256(r2.content).hexdigest(),
                        )
                except Exception:
                    pass

        main = (soup.find("main") or soup.find("article") or soup)
        texto = main.get_text(separator="\n", strip=True)
        html_path.write_text(r.text, encoding="utf-8")
        txt_path.write_text(texto, encoding="utf-8")

        if len(texto) > 1000:
            return DownloadResult(
                url_original=url, html_texto=texto, sucesso=True,
                erro="HTML (PDF não encontrado automaticamente)",
            )
        return DownloadResult(url_original=url, sucesso=False, erro=f"Conteúdo insuficiente ({len(texto)} chars)")

    except Exception as e:
        return DownloadResult(url_original=url, sucesso=False, erro=str(e))


# ═══════════════════════════════════════════════════════════════════════
# DISPATCH
# ═══════════════════════════════════════════════════════════════════════

def detectar_fonte(entry: dict) -> str:
    """Detecta a fonte baseada na URL."""
    url = entry.get("url", "") or entry.get("url_pagina", "")
    url_lower = url.lower()

    if "gov.br" in url_lower:
        return "pcdt"
    if "who.int" in url_lower:
        return "who"
    if "nice.org.uk" in url_lower:
        return "nice"
    if "kdigo.org" in url_lower:
        return "html"
    if "escardio.org" in url_lower:
        return "html"
    if "ahajournals.org" in url_lower or "acc.org" in url_lower or "heart.org" in url_lower:
        return "html"
    return "html"


def baixar(entry: dict, cache_dir: Path) -> DownloadResult:
    """Download automático detectando a fonte."""
    fonte = detectar_fonte(entry)
    log.info(f"[{fonte}] {entry.get('titulo', '?')[:60]}")

    if fonte == "pcdt":
        return baixar_pcdt(entry, cache_dir)
    elif fonte == "who":
        return baixar_who(entry, cache_dir)
    elif fonte == "nice":
        return baixar_nice(entry, cache_dir)
    else:
        return baixar_html_generico(entry, cache_dir)


def baixar_lote(entries: list[dict], cache_dir: Path, workers: int = 4) -> list[DownloadResult]:
    """Baixa múltiplas fontes em paralelo."""
    cache_dir.mkdir(parents=True, exist_ok=True)
    resultados: list[DownloadResult] = []
    total = len(entries)

    with ThreadPoolExecutor(max_workers=workers) as pool:
        futuros = {pool.submit(baixar, e, cache_dir): e for e in entries}
        for n, fut in enumerate(as_completed(futuros), 1):
            entry = futuros[fut]
            try:
                res = fut.result()
                resultados.append(res)
                status = "✅" if res.sucesso else "❌"
                log.info(f"  [{n}/{total}] {status} {entry.get('titulo', '?')[:50]}")
            except Exception as e:
                log.error(f"  [{n}/{total}] 💥 {entry.get('titulo', '?')[:50]}: {e}")
                resultados.append(DownloadResult(
                    url_original=entry.get("url", ""), sucesso=False, erro=str(e),
                ))

    return resultados


# ═══════════════════════════════════════════════════════════════════════
# CLI
# ═══════════════════════════════════════════════════════════════════════

def main():
    import sys
    if len(sys.argv) < 3:
        print("Uso: python tools/downloader.py fontes.json cache_dir/")
        sys.exit(1)

    fontes_path = Path(sys.argv[1])
    cache_dir = Path(sys.argv[2])
    entries = json.loads(fontes_path.read_text(encoding="utf-8"))

    resultados = baixar_lote(entries, cache_dir)
    ok = sum(1 for r in resultados if r.sucesso)
    log.info(f"Total: {ok}/{len(resultados)} downloads bem-sucedidos")

    # Relatório
    relatorio = cache_dir / "download_report.json"
    relatorio.write_text(
        json.dumps(
            [{"url": r.url_original, "sucesso": r.sucesso, "erro": r.erro, "cache_hit": r.cache_hit}
             for r in resultados],
            indent=1, ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    log.info(f"Relatório: {relatorio}")


if __name__ == "__main__":
    main()
