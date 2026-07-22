#!/usr/bin/env python3
"""
KNOWLEDGE EXTRACTOR — Sistema multi-agente de extração de conhecimento.

Arquitetura de agentes especializados que trabalham em pipeline:

┌─────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ AGENTE       │  │ AGENTE       │  │ AGENTE       │  │ AGENTE       │  │ AGENTE       │
│ CATALOGADOR  │→ │ DOWNLOADER   │→ │ EXTRAIDOR    │→ │ VALIDADOR    │→ │ EMPACOTADOR  │
│ (mapeia      │  │ (baixa PDFs  │  │ (extrai      │  │ (filtra      │  │ (gera        │
│  fontes)     │  │  com        │  │  seções      │  │  ruído,      │  │  pack.json   │
│              │  │  retry)     │  │  c/ página)  │  │  avalia      │  │  para        │
│              │  │              │  │              │  │  sinal      │  │  curadoria)  │
│              │  │              │  │              │  │  clínico)   │  │              │
└─────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
                                                       ↓
                                                 ┌──────────────┐
                                                 │ AGENTE       │
                                                 │ ILUSTRADOR   │
                                                 │ (gera SVG    │
                                                 │  anatômico   │
                                                 │  e diagramas)│
                                                 └──────────────┘

Uso:
    python tools/knowledge_extractor.py --fonte nice --output packs/nice
    python tools/knowledge_extractor.py --fonte who --output packs/who
    python tools/knowledge_extractor.py --fonte kdigo --output packs/kdigo
    python tools/knowledge_extractor.py --fonte esc --output packs/esc
    python tools/knowledge_extractor.py --fonte aha --output packs/aha
    python tools/knowledge_extractor.py --fonte todas --output packs/ --svgs
"""
from __future__ import annotations

import hashlib
import json
import logging
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import unicodedata
import urllib.request
from abc import ABC, abstractmethod
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field, asdict
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Callable, Optional

logging.basicConfig(level=logging.INFO, format="[%(name)s] %(levelname)s: %(message)s")
log = logging.getLogger("extractor")

# ═══════════════════════════════════════════════════════════════
# MODELOS
# ═══════════════════════════════════════════════════════════════

class Provenance(Enum):
    VERIFICAVEL = "VERIFICAVEL"
    RASCUNHO = "RASCUNHO"

@dataclass
class CatalogEntry:
    """Uma fonte catalogada — URL, título, categoria, etc."""
    url: str
    url_pagina: str
    titulo: str
    categoria: str = "CLINICA_MEDICA"
    fonte: str = ""
    prefixo_id: str = ""
    tags: list[str] = field(default_factory=list)
    # Metadata do download
    pdf_path: str = ""
    hash_sha256: str = ""
    download_ok: bool = False
    error: str = ""

@dataclass
class ExtractedSection:
    """Uma seção extraída de um documento, com número da página."""
    id: str
    title: str
    category: str
    summary: str
    content: str
    tags: list[str]
    citation: str
    sourceUrl: str
    sourceRef: str
    provenance: str = Provenance.RASCUNHO.value
    # Métricas de qualidade
    clinical_signals: list[str] = field(default_factory=list)
    words: int = 0
    accepted: bool = False
    reject_reason: str = ""

# ═══════════════════════════════════════════════════════════════
# AGENTE 1: CATALOGADOR — mapeia fontes disponíveis
# ═══════════════════════════════════════════════════════════════

class AgenteCatalogador:
    """Agente responsável por catalogar fontes de diretrizes clínicas.
    
    Cargo: Catalogador-Chefe de Fontes Clínicas
    Função: Mantém o inventário de todas as fontes disponíveis para extração,
    organizadas por especialidade e prioridade clínica.
    """
    
    FONTES = {
        "pcdt": {
            "script": "pcdt_catalogo.py",
            "descricao": "Protocolos Clínicos e Diretrizes Terapêuticas (MS Brasil)",
            "prioridade": 1,
        },
        "nice": {
            "script": "diretrizes_nice_catalogo.py",
            "descricao": "NICE Guidelines (Reino Unido)",
            "prioridade": 2,
        },
        "who": {
            "script": "diretrizes_who_catalogo.py",
            "descricao": "WHO Guidelines (Organização Mundial da Saúde)",
            "prioridade": 3,
        },
        "kdigo": {
            "script": "diretrizes_kdigo_catalogo.py",
            "descricao": "KDIGO Guidelines (Nefrologia Global)",
            "prioridade": 4,
        },
        "esc": {
            "script": "diretrizes_esc_catalogo.py",
            "descricao": "ESC Guidelines (Cardiologia Europeia)",
            "prioridade": 5,
        },
        "aha": {
            "script": "diretrizes_aha_catalogo.py",
            "descricao": "AHA/ACC Guidelines (Cardiologia Americana)",
            "prioridade": 6,
        },
    }
    
    def __init__(self, tools_dir: Path):
        self.tools_dir = tools_dir
        self.entries: dict[str, list[CatalogEntry]] = {}
    
    def catalogar(self, fonte: str) -> list[CatalogEntry]:
        """Executa o script de catálogo e carrega as entradas."""
        if fonte not in self.FONTES:
            # Tenta carregar um arquivo JSON diretamente
            json_path = Path(fonte)
            if json_path.exists():
                return self._load_json(json_path)
            log.error(f"Fonte desconhecida: {fonte}")
            return []
        
        info = self.FONTES[fonte]
        script = self.tools_dir / info["script"]
        
        if not script.exists():
            log.error(f"Script não encontrado: {script}")
            return []
        
        with tempfile.TemporaryDirectory() as td:
            json_out = Path(td) / "fontes.json"
            cmd = [sys.executable, str(script), str(json_out)]
            log.info(f"Executando: {' '.join(cmd)}")
            result = subprocess.run(cmd, capture_output=True, text=True)
            if result.returncode != 0:
                log.error(f"Falha ao catalogar {fonte}: {result.stderr}")
                return []
            entries = self._load_json(json_out)
            log.info(f"Catálogo {fonte}: {len(entries)} entradas")
            return entries
    
    def _load_json(self, path: Path) -> list[CatalogEntry]:
        data = json.loads(path.read_text(encoding="utf-8"))
        if isinstance(data, dict):
            data = [data]
        return [CatalogEntry(**item) if isinstance(item, dict) else item for item in data]
    
    def listar_disponiveis(self) -> str:
        lines = ["FONTES DISPONÍVEIS PARA EXTRAÇÃO:", "─" * 50]
        for slug, info in sorted(self.FONTES.items(), key=lambda x: x[1]["prioridade"]):
            lines.append(f"  [{slug}] {info['descricao']} (prioridade {info['prioridade']})")
        return "\n".join(lines)

# ═══════════════════════════════════════════════════════════════
# AGENTE 2: DOWNLOADER — baixa PDFs com estratégia por fonte
# ═══════════════════════════════════════════════════════════════

class AgenteDownloader:
    """Agente responsável por baixar PDFs das fontes catalogadas.
    
    Cargo: Downloader-Chefe de Documentos
    Função: Obtém os PDFs originais de cada fonte, usando a estratégia
    de download apropriada para cada site. Implementa retry, hash
    verification e cache local.
    """
    
    USER_AGENT = "Mozilla/5.0 (compatible; BioAcupunt-extractor/1.0)"
    MAX_TENTATIVAS = 3
    TIMEOUT = 180
    MAX_WORKERS = 6
    
    def __init__(self, cache_dir: Path):
        self.cache_dir = cache_dir
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        self._check_pdftotext()
    
    def _check_pdftotext(self):
        self.pdftotext = shutil.which("pdftotext")
        if not self.pdftotext:
            log.warning("pdftotext não encontrado. Instale poppler-utils.")
    
    def baixar(self, entry: CatalogEntry) -> CatalogEntry:
        """Baixa o PDF de uma entrada catalogada."""
        url = self._resolver_url(entry)
        dest = self.cache_dir / f"{entry.prefixo_id or hashlib.md5(url.encode()).hexdigest()[:12]}.pdf"
        
        if dest.exists() and dest.stat().st_size > 1000:
            entry.pdf_path = str(dest)
            entry.hash_sha256 = hashlib.sha256(dest.read_bytes()).hexdigest()
            entry.download_ok = True
            return entry
        
        for tentativa in range(self.MAX_TENTATIVAS):
            try:
                log.info(f"Baixando [{tentativa+1}/{self.MAX_TENTATIVAS}]: {url}")
                req = urllib.request.Request(url, headers={"User-Agent": self.USER_AGENT})
                with urllib.request.urlopen(req, timeout=self.TIMEOUT) as r:
                    data = r.read()
                
                if not data.startswith(b"%PDF"):
                    # Tenta padrão alternativo
                    alt_url = self._tentar_alternativo(entry)
                    if alt_url:
                        req = urllib.request.Request(alt_url, headers={"User-Agent": self.USER_AGENT})
                        with urllib.request.urlopen(req, timeout=self.TIMEOUT) as r2:
                            data = r2.read()
                    if not data.startswith(b"%PDF"):
                        raise ValueError(f"Não é PDF: {url}")
                
                dest.write_bytes(data)
                entry.pdf_path = str(dest)
                entry.hash_sha256 = hashlib.sha256(data).hexdigest()
                entry.download_ok = True
                return entry
                
            except Exception as e:
                log.warning(f"Tentativa {tentativa+1} falhou: {e}")
                if tentativa < self.MAX_TENTATIVAS - 1:
                    time.sleep(2 * (tentativa + 1))
        
        entry.error = str(e)  # type: ignore
        return entry
    
    def _resolver_url(self, entry: CatalogEntry) -> str:
        """Resolve a URL de download baseada na fonte."""
        url = entry.url
        # PCDT (gov.br Plone): padrão /@@download/file
        if "gov.br" in url and "/pcdt/" in url:
            base = url.rstrip("/")
            if base.endswith("/view"):
                base = base[: -len("/view")]
            return base + "/@@download/file"
        # NICE: padrão /resources/pdf
        if "nice.org.uk" in url:
            return url.rstrip("/") + "/resources/pdf"
        # WHO: tentativa de URL direta
        if "who.int" in url and "/i/item/" in url:
            return url
        # KDIGO, ESC, AHA: retorna URL da página
        return url
    
    def _tentar_alternativo(self, entry: CatalogEntry) -> Optional[str]:
        """Tenta URL alternativa quando a principal falha."""
        url = entry.url
        if "nice.org.uk" in url:
            return url.rstrip("/") + "/resources"
        if "who.int" in url:
            return url.replace("/i/item/", "/i//item/") + "/download"
        return None

# ═══════════════════════════════════════════════════════════════
# AGENTE 3: EXTRAIDOR — extrai seções com número de página
# ═══════════════════════════════════════════════════════════════

class AgenteExtraidor:
    """Agente responsável por extrair seções de texto de PDFs.
    
    Cargo: Extraidor-Chefe de Conteúdo
    Função: Converte PDFs em texto, identifica seções por cabeçalho,
    extrai conteúdo com número da página (sourceRef), e prepara
    para o filtro de qualidade.
    """
    
    MIN_CHARS = 320
    MAX_CHARS = 2400
    HEADING = re.compile(r"^\s{0,8}((?:[\dIVXL]+\.){0,4}[\dIVXL]+\s+[A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\n]{4,80}|[A-ZÁÉÍÓÚÂÊÔÃÕÇ][A-ZÁÉÍÓÚÂÊÔÃÕÇ\s\-/,]{8,70})\s*$")
    
    def extrair(self, entry: CatalogEntry, pdftotext: str) -> list[ExtractedSection]:
        """Extrai seções de um PDF já baixado."""
        if not entry.pdf_path:
            return []
        
        pdf = Path(entry.pdf_path)
        txt = pdf.with_suffix(".txt")
        
        try:
            subprocess.run([pdftotext, "-layout", "-enc", "UTF-8", str(pdf), str(txt)],
                          check=True, capture_output=True, timeout=60)
            text = txt.read_text(encoding="utf-8", errors="replace")
            pages = text.split("\f")
        except Exception as e:
            log.error(f"Falha ao extrair texto de {pdf.name}: {e}")
            return []
        
        sections: list[ExtractedSection] = []
        for pageno, page in enumerate(pages, start=1):
            for title, body in self._secoes_da_pagina(page):
                body = self._limpar(body)
                if len(body) < self.MIN_CHARS:
                    continue
                body = body[:self.MAX_CHARS]
                n = len(sections) + 1
                heading = title.strip() or f"{entry.titulo} — p. {pageno}"
                sections.append(ExtractedSection(
                    id=f"{entry.prefixo_id}_{n:04d}",
                    title=heading[:120],
                    category=entry.categoria,
                    summary=" ".join(body.split())[:240],
                    content=body,
                    tags=entry.tags,
                    citation=f"{entry.titulo}, p. {pageno}",
                    sourceUrl=entry.url_pagina,
                    sourceRef=f"p. {pageno}",
                    provenance=Provenance.VERIFICAVEL.value,
                    words=len(body.split()),
                ))
        
        return sections
    
    def _secoes_da_pagina(self, page: str) -> list[tuple[str, str]]:
        lines = page.splitlines()
        marks = [i for i, l in enumerate(lines) if self.HEADING.match(l)]
        if not marks:
            body = "\n".join(l.rstrip() for l in lines if l.strip())
            return [("", body)] if body.strip() else []
        
        out = []
        for n, start in enumerate(marks):
            end = marks[n + 1] if n + 1 < len(marks) else len(lines)
            title = lines[start].strip()
            body = "\n".join(l.rstrip() for l in lines[start + 1:end] if l.strip())
            if body.strip():
                out.append((title, body))
        return out
    
    def _limpar(self, text: str) -> str:
        text = re.sub(r"[ \t]{3,}", "  ", text)
        return re.sub(r"\n{3,}", "\n\n", text).strip()

# ═══════════════════════════════════════════════════════════════
# AGENTE 4: VALIDADOR — filtra ruído, avalia sinal clínico
# ═══════════════════════════════════════════════════════════════

class AgenteValidador:
    """Agente responsável por filtrar ruído e validar qualidade clínica.
    
    Cargo: Validador-Chefe de Qualidade Clínica
    Função: Aplica o filtro de ruído (veto de tabelas, referências,
    cabeçalhos administrativos) e exige sinal clínico mínimo (dose,
    critério diagnóstico, conduta, segurança, monitorização).
    Aplica a regra dos 2 sinais: um sinal pode ser coincidência
    lexical; dois sinais distintos indicam texto que conclui algo.
    """
    
    # Sinais clínicos exigidos para aprovação
    SINAIS: dict[str, re.Pattern] = {
        "posologia": re.compile(
            r"\b\d[\d.,]*\s?(?:mg|mcg|µg|g|ml|mL|UI|mmol|mEq)\b"
            r"|\bmg\s?/\s?(?:kg|m²|m2|dia|dL)\b|\bUI\s?/\s?(?:kg|dia)\b"
        ),
        "criterio": re.compile(
            r"crit[ée]rios?\s+de\s+(?:diagn[óo]stic|inclus[ãa]o|exclus[ãa]o)"
            r"|define-se|considera-se|diagn[óo]stico\s+(?:[ée]|de|se|diferencial)"
            r"|confirma[çc][ãa]o\s+diagn[óo]stica",
            re.I,
        ),
        "conduta": re.compile(
            r"recomenda-se|est[áa]\s+indicad[ao]|deve-se"
            r"|tratamento\s+(?:de|da|do|com|deve|primeira\s+linha)"
            r"|esquema\s+terap[êe]utico|conduta\s+cl[íi]nica|manejo",
            re.I,
        ),
        "seguranca": re.compile(
            r"contraindica[çc][ãa]o|efeitos?\s+adversos?|rea[çc][õo]es?\s+adversas?"
            r"|intera[çc][ãa]o\s+medicamentosa|risco\s+de|toxicidade"
            r"|suspend[eo]r?|descontinua[çc][ãa]o",
            re.I,
        ),
        "monitorizacao": re.compile(
            r"monitoriza[çc][ãa]o|acompanhamento|seguimento"
            r"|reavalia[çc][ãa]o|exames?\s+(?:de\s+)?(?:controle|laboratoriais)",
            re.I,
        ),
    }
    
    # VETO: uma ocorrência já condena a seção
    VETO: dict[str, re.Pattern] = {
        "capa/portaria": re.compile(
            r"portaria\s+conjunta|aprova\s+o\s+protocolo\s+cl[íi]nico"
            r"|di[áa]rio\s+oficial|fica\s+aprovado"
            r"|esta\s+portaria\s+entra\s+em\s+vigor",
            re.I,
        ),
        "termo_consentimento": re.compile(
            r"termo\s+de\s+esclarecimento\s+e\s+responsabilidade"
            r"|declaro\s+que\s+fui\s+claramente|assinatura\s+do\s+(?:paciente|respons)",
            re.I,
        ),
        "expediente": re.compile(
            r"equipe\s+t[ée]cnica|elabora[çc][ãa]o\s+t[ée]cnica"
            r"|secret[áa]rio\s+de\s+aten[çc][ãa]o\s+especializada",
            re.I,
        ),
    }
    
    DESCARTES: dict[str, re.Pattern] = {
        "referencias": re.compile(r"\bet\s+al\b|doi:\s*10\.|\bISBN\b|dispon[íi]vel\s+em:", re.I),
    }
    
    REF_NUM = re.compile(r"^\s*\d{1,3}[\.\)]\s+[A-ZÀ-Ú][\wÀ-ú'\-]+,?\s+[A-Z]{1,3}[\.,]", re.M)
    SUMARIO = re.compile(r"\.{4,}\s*\d+\s*$", re.M)
    CID = re.compile(r"\b[A-Z]\d{2}(?:\.\d)?\b")
    
    def validar(self, section: ExtractedSection) -> ExtractedSection:
        """Aplica o filtro de ruído e valida a seção."""
        t = section.title.lower()
        body = section.content
        linhas = [l for l in body.splitlines() if l.strip()]
        
        # 1. Veto explícito
        if re.match(r"^\s*(refer[êe]ncias|bibliografia|sum[áa]rio|[íi]ndice|ap[êe]ndice)\b", t):
            section.reject_reason = "seção não-clínica"
            return section
        
        for motivo, rx in self.VETO.items():
            if rx.search(body) or rx.search(t):
                section.reject_reason = motivo
                return section
        
        for motivo, rx in self.DESCARTES.items():
            hits = sum(bool(rx.search(l)) for l in linhas)
            if hits / len(linhas) > 0.25 or rx.search(t):
                section.reject_reason = motivo
                return section
        
        if len(self.SUMARIO.findall(body)) >= 3:
            section.reject_reason = "sumário"
            return section
        
        if len(self.REF_NUM.findall(body)) >= 3:
            section.reject_reason = "referências numeradas"
            return section
        
        if len(self.CID.findall(body)) >= 6 and len(body.split()) < 160:
            section.reject_reason = "lista CID"
            return section
        
        # 2. Qualidade estrutural
        palavras = body.split()
        if len(palavras) < 45:
            section.reject_reason = "curto demais"
            return section
        
        sem_espaco = re.sub(r"\s", "", body)
        letras = sum(c.isalpha() for c in sem_espaco)
        if letras / len(sem_espaco) < 0.62:
            section.reject_reason = "tabela desmontada"
            return section
        
        alfa = [c for c in body if c.isalpha()]
        if alfa and sum(c.isupper() for c in alfa) / len(alfa) > 0.45:
            section.reject_reason = "maiúsculas excessivas"
            return section
        
        if "." not in body and not re.search(r"^\s*[-•–]\s", body, re.M):
            section.reject_reason = "sem estrutura de frase"
            return section
        
        # 3. Sinal clínico — exige 2 sinais distintos
        presentes = [nome for nome, rx in self.SINAIS.items() if rx.search(body)]
        section.clinical_signals = presentes
        
        if not presentes:
            section.reject_reason = "sem sinal clínico"
            return section
        if len(presentes) < 2:
            section.reject_reason = f"sinal clínico fraco ({presentes[0]})"
            return section
        
        section.accepted = True
        section.provenance = Provenance.VERIFICAVEL.value
        return section

# ═══════════════════════════════════════════════════════════════
# AGENTE 5: ILUSTRADOR — gera SVG ilustrativos
# ═══════════════════════════════════════════════════════════════

class AgenteIlustrador:
    """Agente responsável por gerar ilustrações SVG para cada assunto.
    
    Cargo: Ilustrador-Chefe de Conteúdo Visual
    Função: Gera diagramas SVG específicos para cada categoria MTC,
    incluindo meridianos, pontos, elementos, língua, pulso, etc.
    Cada SVG é auto-contido, responsivo e estilizado.
    """
    
    SVG_DIR = "app/src/main/res/drawable"
    
    def gerar_todas(self, base_dir: Path) -> list[dict]:
        """Gera SVGs para todas as categorias MTC."""
        svgs = []
        
        svgs.append(self._meridianos_svg(base_dir))
        svgs.append(self._cinco_elementos_svg(base_dir))
        svgs.append(self._lingua_svg(base_dir))
        svgs.append(self._pulso_svg(base_dir))
        svgs.append(self._ba_gang_svg(base_dir))
        svgs.append(self._pontos_acupuntura_svg(base_dir))
        svgs.append(self._moxabustao_svg(base_dir))
        svgs.append(self._tecnicas_agulhamento_svg(base_dir))
        svgs.append(self._qigong_svg(base_dir))
        svgs.append(self._clinica_medica_svg(base_dir))
        
        return svgs
    
    def _salvar_svg(self, path: Path, content: str, nome: str) -> dict:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        log.info(f"SVG gerado: {path.name}")
        return {"nome": nome, "path": str(path), "tamanho": len(content)}
    
    def _meridianos_svg(self, base: Path) -> dict:
        path = base / "mtc_meridianos.svg"
        # Gera os marcadores do relógio separadamente para evitar nested '''
        clock_marks = ''.join(
            '<line x1="150" y1="40" x2="150" y2="70" transform="rotate(%d 150 140)" stroke="#444" stroke-width="1"/>' % (h * 30)
            + '<text x="150" y="55" transform="rotate(%d 150 140)" text-anchor="middle" fill="#888" font-size="8">%02dh</text>' % (h, h)
            for h in range(0, 24, 2)
        )
        svg_body = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
    <linearGradient id="mer1" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#e94560"/><stop offset="1" stop-color="#ff6b6b"/></linearGradient>
    <linearGradient id="mer2" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0f3460"/><stop offset="1" stop-color="#53a8b6"/></linearGradient>
    <linearGradient id="mer3" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#2d6a4f"/><stop offset="1" stop-color="#52b788"/></linearGradient>
    <linearGradient id="mer4" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#e6a817"/><stop offset="1" stop-color="#ffd60a"/></linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">MERIDIANOS PRINCIPAIS</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Os 12 Canais Regulares da Medicina Tradicional Chinesa</text>
  <g transform="translate(120, 85)">
    <ellipse cx="60" cy="30" rx="35" ry="40" fill="none" stroke="#555" stroke-width="1.5"/>
    <circle cx="48" cy="25" r="3" fill="#666"/><circle cx="72" cy="25" r="3" fill="#666"/>
    <path d="M58 30 L60 38 L62 30" fill="none" stroke="#555" stroke-width="1"/>
    <path d="M25 70 L95 70 L100 200 L20 200 Z" fill="none" stroke="#555" stroke-width="1.5"/>
    <path d="M25 90 L5 160 L5 200" fill="none" stroke="#555" stroke-width="1.5"/>
    <path d="M95 90 L115 160 L115 200" fill="none" stroke="#555" stroke-width="1.5"/>
    <path d="M30 200 L25 320 L55 350" fill="none" stroke="#555" stroke-width="1.5"/>
    <path d="M90 200 L95 320 L65 350" fill="none" stroke="#555" stroke-width="1.5"/>
    <path d="M60 40 Q50 60, 35 70 Q20 90, 15 100 L15 120 Q15 150, 25 200 L25 250 L25 280 L20 320" fill="none" stroke="url(#mer1)" stroke-width="2.5" stroke-dasharray="8,4" opacity="0.8"/>
    <circle cx="15" cy="100" r="3" fill="#e94560"/><text x="18" y="97" fill="#e94560" font-size="7">LU1</text>
    <circle cx="25" cy="200" r="3" fill="#e94560"/><text x="28" y="197" fill="#e94560" font-size="7">LU5</text>
    <circle cx="25" cy="280" r="3" fill="#e94560"/><text x="28" y="277" fill="#e94560" font-size="7">LU9</text>
    <path d="M60 70 Q55 100, 50 130 Q45 150, 35 170 L25 200" fill="none" stroke="url(#mer2)" stroke-width="2.5" stroke-dasharray="6,3" opacity="0.8"/>
    <circle cx="35" cy="170" r="3" fill="#0f3460"/><text x="38" y="167" fill="#0f3460" font-size="7">HT7</text>
    <path d="M75 40 Q85 60, 90 80 Q95 110, 100 150 Q105 200, 95 250 Q90 300, 75 320 L65 350" fill="none" stroke="url(#mer4)" stroke-width="2.5" stroke-dasharray="10,5" opacity="0.8"/>
    <circle cx="90" cy="80" r="3" fill="#e6a817"/><text x="93" y="77" fill="#e6a817" font-size="7">ST36</text>
    <circle cx="95" cy="200" r="3" fill="#e6a817"/><text x="98" y="197" fill="#e6a817" font-size="7">ST25</text>
    <path d="M55 70 Q48 100, 42 150 Q38 200, 30 250 Q25 300, 35 320 L45 350" fill="none" stroke="url(#mer3)" stroke-width="2.5" stroke-dasharray="7,3" opacity="0.8"/>
    <circle cx="42" cy="150" r="3" fill="#2d6a4f"/><text x="45" y="147" fill="#2d6a4f" font-size="7">SP6</text>
  </g>
  <g transform="translate(460, 100)">
    <text x="150" y="0" text-anchor="middle" fill="#e0e0e0" font-family="sans-serif" font-size="14" font-weight="bold">RELÓGIO CIRCADIANO</text>
    <circle cx="150" cy="140" r="100" fill="none" stroke="#333" stroke-width="2"/>''' + clock_marks + '''
    <g font-family="sans-serif" font-size="8" fill="#ccc" text-anchor="middle">
      <text x="150" y="45" fill="#e94560">Pulmao 3-5h</text>
      <text x="220" y="80" fill="#53a8b6">IG 5-7h</text>
      <text x="245" y="140" fill="#e6a817">E 7-9h</text>
      <text x="220" y="200" fill="#52b788">Baco 9-11h</text>
      <text x="150" y="230" fill="#ff6b6b">Coracao 11-13h</text>
      <text x="80" y="200" fill="#53a8b6">ID 13-15h</text>
      <text x="55" y="140" fill="#e6a817">Bexiga 15-17h</text>
      <text x="80" y="80" fill="#52b788">Rim 17-19h</text>
    </g>
  </g>
  <g transform="translate(300, 540)">
    <line x1="0" y1="0" x2="20" y2="0" stroke="#e94560" stroke-width="2" stroke-dasharray="8,4"/>
    <text x="25" y="4" fill="#ccc" font-size="9">Pulmao (Fei)</text>
    <line x1="120" y1="0" x2="140" y2="0" stroke="#53a8b6" stroke-width="2" stroke-dasharray="6,3"/>
    <text x="145" y="4" fill="#ccc" font-size="9">Coracao (Xin)</text>
    <line x1="250" y1="0" x2="270" y2="0" stroke="#52b788" stroke-width="2" stroke-dasharray="7,3"/>
    <text x="275" y="4" fill="#ccc" font-size="9">Baco (Pi)</text>
    <line x1="360" y1="0" x2="380" y2="0" stroke="#e6a817" stroke-width="2" stroke-dasharray="10,5"/>
    <text x="385" y="4" fill="#ccc" font-size="9">Estomago (Wei)</text>
  </g>
</svg>'''
        return self._salvar_svg(path, svg_body, "Meridianos")
    
    def _cinco_elementos_svg(self, base: Path) -> dict:
        path = base / "mtc_cinco_elementos.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
    <radialGradient id="madeira" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#2d6a4f"/><stop offset="100" stop-color="#40916c"/></radialGradient>
    <radialGradient id="fogo" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#e63946"/><stop offset="100" stop-color="#ff6b6b"/></radialGradient>
    <radialGradient id="terra" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#c8950c"/><stop offset="100" stop-color="#e6a817"/></radialGradient>
    <radialGradient id="metal" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#adb5bd"/><stop offset="100" stop-color="#dee2e6"/></radialGradient>
    <radialGradient id="agua" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#0f3460"/><stop offset="100" stop-color="#53a8b6"/></radialGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">CINCO ELEMENTOS (WU XING)</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Ciclos de Geração (Sheng) e Controle (Ke)</text>
  
  <!-- Ciclo de Geração (externo) -->
  <g transform="translate(400, 310)">
    <!-- Flechas de geração (Sheng) - verde -->
    <path d="M0 -150 Q150 -150, 155 0" fill="none" stroke="#52b788" stroke-width="2" stroke-dasharray="8,4" marker-end="none"/>
    <path d="M155 0 Q150 150, 0 155" fill="none" stroke="#52b788" stroke-width="2" stroke-dasharray="8,4"/>
    <path d="M0 155 Q-150 150, -155 0" fill="none" stroke="#52b788" stroke-width="2" stroke-dasharray="8,4"/>
    <path d="M-155 0 Q-150 -150, 0 -155" fill="none" stroke="#52b788" stroke-width="2" stroke-dasharray="8,4"/>
    <path d="M0 0 L0 -155" fill="none" stroke="#52b788" stroke-width="2" stroke-dasharray="8,4" opacity="0"/>
    
    <!-- Flechas de controle (Ke) - vermelho -->
    <path d="M0 -150 L0 155" fill="none" stroke="#e63946" stroke-width="1.5" stroke-dasharray="3,6" opacity="0.5"/>
    <path d="M155 0 L-155 0" fill="none" stroke="#e63946" stroke-width="1.5" stroke-dasharray="3,6" opacity="0.5"/>
    <line x1="80" y1="-80" x2="-80" y2="80" stroke="#e63946" stroke-width="1.5" stroke-dasharray="3,6" opacity="0.5"/>
    <line x1="-80" y1="-80" x2="80" y2="80" stroke="#e63946" stroke-width="1.5" stroke-dasharray="3,6" opacity="0.5"/>
    
    <!-- Madeira (topo) -->
    <circle cx="0" cy="-140" r="45" fill="url(#madeira)" opacity="0.9"/>
    <text x="0" y="-145" text-anchor="middle" fill="#fff" font-family="serif" font-size="16" font-weight="bold">🌳</text>
    <text x="0" y="-125" text-anchor="middle" fill="#fff" font-family="sans-serif" font-size="11" font-weight="bold">MADEIRA</text>
    <text x="0" y="-112" text-anchor="middle" fill="#ddd" font-family="sans-serif" font-size="8">Fígado · Vesícula</text>
    
    <!-- Fogo (direita) -->
    <circle cx="140" cy="0" r="45" fill="url(#fogo)" opacity="0.9"/>
    <text x="140" y="-5" text-anchor="middle" fill="#fff" font-family="serif" font-size="16" font-weight="bold">🔥</text>
    <text x="140" y="15" text-anchor="middle" fill="#fff" font-family="sans-serif" font-size="11" font-weight="bold">FOGO</text>
    <text x="140" y="28" text-anchor="middle" fill="#ddd" font-family="sans-serif" font-size="8">Coração · ID</text>
    
    <!-- Terra (baixo) -->
    <circle cx="0" cy="140" r="45" fill="url(#terra)" opacity="0.9"/>
    <text x="0" y="135" text-anchor="middle" fill="#fff" font-family="serif" font-size="16" font-weight="bold">⛰️</text>
    <text x="0" y="155" text-anchor="middle" fill="#fff" font-family="sans-serif" font-size="11" font-weight="bold">TERRA</text>
    <text x="0" y="168" text-anchor="middle" fill="#ddd" font-family="sans-serif" font-size="8">Baço · Estômago</text>
    
    <!-- Metal (esquerda) -->
    <circle cx="-140" cy="0" r="45" fill="url(#metal)" opacity="0.9"/>
    <text x="-140" y="-5" text-anchor="middle" fill="#333" font-family="serif" font-size="16" font-weight="bold">⚔️</text>
    <text x="-140" y="15" text-anchor="middle" fill="#333" font-family="sans-serif" font-size="11" font-weight="bold">METAL</text>
    <text x="-140" y="28" text-anchor="middle" fill="#555" font-family="sans-serif" font-size="8">Pulmão · IG</text>
    
    <!-- Água (centro) -->
    <circle cx="0" cy="0" r="35" fill="url(#agua)" opacity="0.9"/>
    <text x="0" y="-4" text-anchor="middle" fill="#fff" font-family="serif" font-size="12" font-weight="bold">💧</text>
    <text x="0" y="10" text-anchor="middle" fill="#fff" font-family="sans-serif" font-size="9" font-weight="bold">ÁGUA</text>
    <text x="0" y="20" text-anchor="middle" fill="#ddd" font-family="sans-serif" font-size="7">Rim · Bexiga</text>
  </g>
  
  <!-- Legenda -->
  <g transform="translate(250, 540)">
    <line x1="0" y1="0" x2="20" y2="0" stroke="#52b788" stroke-width="2" stroke-dasharray="8,4"/>
    <text x="25" y="4" fill="#52b788" font-size="9">Sheng (Geração)</text>
    <line x1="180" y1="0" x2="200" y2="0" stroke="#e63946" stroke-width="1.5" stroke-dasharray="3,6"/>
    <text x="205" y="4" fill="#e63946" font-size="9">Ke (Controle)</text>
  </g>
</svg>'''
        return self._salvar_svg(path, svg, "Cinco Elementos")
    
    def _lingua_svg(self, base: Path) -> dict:
        path = base / "mtc_diagnostico_lingua.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
    <radialGradient id="lingua" cx="50%" cy="40%" r="50%"><stop offset="0" stop-color="#f2a7c3"/><stop offset="70%" stop-color="#d4739d"/><stop offset="100" stop-color="#b05b7d"/></radialGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">DIAGNÓSTICO PELA LÍNGUA</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Semiologia completa na Medicina Tradicional Chinesa</text>
  
  <!-- Língua -->
  <g transform="translate(250, 110)">
    <path d="M0 20 Q-100 30, -130 80 Q-150 130, -120 180 Q-80 230, 0 250 Q80 230, 120 180 Q150 130, 130 80 Q100 30, 0 20 Z" fill="url(#lingua)" stroke="#333" stroke-width="2"/>
    
    <!-- Divisão por zonas -->
    <path d="M0 55 Q-85 65, -110 100" fill="none" stroke="#fff" stroke-width="0.8" stroke-dasharray="4,3" opacity="0.4"/>
    <path d="M0 55 Q85 65, 110 100" fill="none" stroke="#fff" stroke-width="0.8" stroke-dasharray="4,3" opacity="0.4"/>
    
    <path d="M0 130 Q-100 140, -120 170" fill="none" stroke="#fff" stroke-width="0.8" stroke-dasharray="4,3" opacity="0.4"/>
    <path d="M0 130 Q100 140, 120 170" fill="none" stroke="#fff" stroke-width="0.8" stroke-dasharray="4,3" opacity="0.4"/>
    
    <!-- Labels das zonas -->
    <text x="0" y="85" text-anchor="middle" fill="#fff" font-size="8" font-weight="bold">PONTA</text>
    <text x="0" y="95" text-anchor="middle" fill="#fcc" font-size="7">Coração/Pulmão</text>
    
    <text x="0" y="155" text-anchor="middle" fill="#fff" font-size="8" font-weight="bold">CENTRO</text>
    <text x="0" y="165" text-anchor="middle" fill="#fcc" font-size="7">Baço/Estômago</text>
    
    <text x="0" y="225" text-anchor="middle" fill="#fff" font-size="8" font-weight="bold">RAIZ</text>
    <text x="0" y="235" text-anchor="middle" fill="#fcc" font-size="7">Rim</text>
    
    <text x="-105" y="130" text-anchor="middle" fill="#fcc" font-size="7" transform="rotate(-20 -105 130)">Bordas: Fígado/VB</text>
    <text x="105" y="130" text-anchor="middle" fill="#fcc" font-size="7" transform="rotate(20 105 130)">Bordas: Fígado/VB</text>
  </g>
  
  <!-- Tabela de cores -->
  <g transform="translate(430, 100)">
    <text x="0" y="0" fill="#e0e0e0" font-size="13" font-weight="bold">CORES DA LÍNGUA</text>
    <rect x="0" y="15" width="30" height="20" rx="4" fill="#f2a7c3" stroke="#ddd" stroke-width="1"/>
    <text x="35" y="30" fill="#ccc" font-size="10">Rosada — Normal (saudável)</text>
    
    <rect x="0" y="45" width="30" height="20" rx="4" fill="#e8d5c4" stroke="#ddd" stroke-width="1"/>
    <text x="35" y="60" fill="#ccc" font-size="10">Pálida — Def. Yang/Sangue, Frio</text>
    
    <rect x="0" y="75" width="30" height="20" rx="4" fill="#d43f52" stroke="#ddd" stroke-width="1"/>
    <text x="35" y="90" fill="#ccc" font-size="10">Vermelha — Calor</text>
    
    <rect x="0" y="105" width="30" height="20" rx="4" fill="#6b2a5e" stroke="#ddd" stroke-width="1"/>
    <text x="35" y="120" fill="#ccc" font-size="10">Roxa/Azulada — Estagnação</text>
    
    <text x="0" y="160" fill="#e0e0e0" font-size="13" font-weight="bold">SABURRA</text>
    <text x="0" y="180" fill="#ccc" font-size="10">Fina (normal) · Espessa (doença)</text>
    <text x="0" y="195" fill="#ccc" font-size="10">Branca (Frio) · Amarela (Calor)</text>
    <text x="0" y="210" fill="#ccc" font-size="10">Ausente (Def. Yin grave)</text>
    <text x="0" y="225" fill="#ccc" font-size="10">Gordurosa (Umidade/Fleuma)</text>
    
    <text x="0" y="260" fill="#e0e0e0" font-size="13" font-weight="bold">FORMA</text>
    <text x="0" y="280" fill="#ccc" font-size="10">Inchada → Umidade/Fleuma</text>
    <text x="0" y="295" fill="#ccc" font-size="10">Magra → Def. Yin/Sangue</text>
    <text x="0" y="310" fill="#ccc" font-size="10">Marcas dentárias → Def. Qi do Baço</text>
    <text x="0" y="325" fill="#ccc" font-size="10">Fissuras → Def. Yin</text>
  </g>
</svg>'''
        return self._salvar_svg(path, svg, "Diagnóstico pela Língua")
    
    def _pulso_svg(self, base: Path) -> dict:
        path = base / "mtc_diagnostico_pulso.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">DIAGNÓSTICO PELO PULSO</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">As 28 qualidades de pulso na Medicina Tradicional Chinesa</text>
  
  <!-- Braços e posições -->
  <g transform="translate(50, 100)">
    <!-- Braço direito -->
    <path d="M10 40 L80 40 L100 100 L120 200" fill="none" stroke="#555" stroke-width="2"/>
    <path d="M15 80 L80 80 L100 110" fill="none" stroke="#555" stroke-width="1.5"/>
    
    <!-- Braço esquerdo -->
    <path d="M290 40 L220 40 L200 100 L180 200" fill="none" stroke="#555" stroke-width="2"/>
    <path d="M285 80 L220 80 L200 110" fill="none" stroke="#555" stroke-width="1.5"/>
    
    <!-- Posições do pulso esquerdo -->
    <text x="255" y="85" fill="#e6a817" font-size="9" font-weight="bold">Cun (E)</text>
    <text x="232" y="85" fill="#e6a817" font-size="9" font-weight="bold">Guan (E)</text>
    <text x="208" y="85" fill="#e6a817" font-size="9" font-weight="bold">Chi (E)</text>
    
    <!-- Posições do pulso direito -->
    <text x="55" y="85" fill="#e94560" font-size="9" font-weight="bold">Cun (D)</text>
    <text x="78" y="85" fill="#e94560" font-size="9" font-weight="bold">Guan (D)</text>
    <text x="102" y="85" fill="#e94560" font-size="9" font-weight="bold">Chi (D)</text>
    
    <!-- Mãos -->
    <circle cx="150" cy="120" r="25" fill="none" stroke="#555" stroke-width="1.5"/>
    <circle cx="150" cy="120" r="3" fill="#666"/>
    
    <text x="150" y="170" text-anchor="middle" fill="#e0e0e0" font-size="10" font-weight="bold">Pulso Esquerdo</text>
    <text x="150" y="185" text-anchor="middle" fill="#ccc" font-size="8">Coração · Fígado · Rim</text>
    
    <text x="150" y="210" text-anchor="middle" fill="#e0e0e0" font-size="10" font-weight="bold">Pulso Direito</text>
    <text x="150" y="225" text-anchor="middle" fill="#ccc" font-size="8">Pulmão · Baço · Rim</text>
  </g>
  
  <!-- Tipos de pulso -->
  <g transform="translate(370, 90)">
    <text x="0" y="0" fill="#e0e0e0" font-size="14" font-weight="bold">TIPOS DE PULSO</text>
    
    <text x="0" y="25" fill="#e94560" font-size="11" font-weight="bold">PULSOS DE EXCESSO (SHI)</text>
    <text x="0" y="42" fill="#ccc" font-size="10">Fu (Flutuante) — Exterior</text>
    <text x="0" y="58" fill="#ccc" font-size="10">Shi (Cheio) — Excesso pleno</text>
    <text x="0" y="74" fill="#ccc" font-size="10">Xian (Tenso) — Fígado/Dor</text>
    <text x="0" y="90" fill="#ccc" font-size="10">Hua (Escorregadio) — Fleuma</text>
    <text x="0" y="106" fill="#ccc" font-size="10">Shuo (Rápido) — Calor</text>
    <text x="0" y="122" fill="#ccc" font-size="10">Hong (Onda) — Calor intenso</text>
    
    <text x="0" y="152" fill="#53a8b6" font-size="11" font-weight="bold">PULSOS DE DEFICIÊNCIA (XU)</text>
    <text x="0" y="169" fill="#ccc" font-size="10">Chen (Profundo) — Interior</text>
    <text x="0" y="185" fill="#ccc" font-size="10">Chi (Lento) — Frio</text>
    <text x="0" y="201" fill="#ccc" font-size="10">Xu (Vazio) — Deficiência geral</text>
    <text x="0" y="217" fill="#ccc" font-size="10">Xi (Fino) — Def. Sangue/Yin</text>
    <text x="0" y="233" fill="#ccc" font-size="10">Ruo (Fraco) — Def. Qi e Sangue</text>
    
    <text x="0" y="263" fill="#e0e0e0" font-size="11" font-weight="bold">PROFUNDIDADES</text>
    <text x="0" y="280" fill="#ccc" font-size="10">Fu (Superficial) — 1º nível</text>
    <text x="0" y="296" fill="#ccc" font-size="10">Zhong (Médio) — 2º nível</text>
    <text x="0" y="312" fill="#ccc" font-size="10">Chen (Profundo) — 3º nível</text>
  </g>
</svg>'''
        return self._salvar_svg(path, svg, "Diagnóstico pelo Pulso")
    
    def _ba_gang_svg(self, base: Path) -> dict:
        """Gera SVG dos Oito Princípios (Ba Gang)."""
        path = base / "mtc_ba_gang.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
    <linearGradient id="yang" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#e63946"/><stop offset="1" stop-color="#ff6b6b"/></linearGradient>
    <linearGradient id="yin" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0f3460"/><stop offset="1" stop-color="#53a8b6"/></linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">BA GANG — OS OITO PRINCÍPIOS</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Sistema fundamental de diferenciação de síndromes na MTC</text>
  
  <!-- Diagrama Yin-Yang central -->
  <g transform="translate(400, 200)">
    <circle cx="0" cy="0" r="60" fill="none" stroke="#555" stroke-width="2"/>
    <path d="M0 -60 A60 60 0 0 1 0 60 A30 30 0 0 0 0 0 A30 30 0 0 1 0 -60 Z" fill="url(#yin)"/>
    <path d="M0 -60 A60 60 0 0 0 0 60 A30 30 0 0 1 0 0 A30 30 0 0 0 0 -60 Z" fill="url(#yang)"/>
    <circle cx="0" cy="-30" r="8" fill="#0f3460"/>
    <circle cx="0" cy="30" r="8" fill="#e63946"/>
    <text x="0" y="-75" text-anchor="middle" fill="#e63946" font-size="16" font-weight="bold">YANG</text>
    <text x="0" y="85" text-anchor="middle" fill="#53a8b6" font-size="16" font-weight="bold">YIN</text>
    
    <!-- Exterior → Yang -->
    <text x="-160" y="-100" fill="#e63946" font-size="12" font-weight="bold">EXTERIOR</text>
    <text x="-160" y="-85" fill="#ccc" font-size="9">Início agudo, febre</text>
    <line x1="-90" y1="-85" x2="-60" y2="-65" stroke="#e63946" stroke-width="1"/>
    
    <!-- Interior → Yin -->
    <text x="130" y="-100" fill="#53a8b6" font-size="12" font-weight="bold">INTERIOR</text>
    <text x="130" y="-85" fill="#ccc" font-size="9">Órgãos internos</text>
    <line x1="90" y1="-85" x2="65" y2="-65" stroke="#53a8b6" stroke-width="1"/>
    
    <!-- Calor → Yang -->
    <text x="120" y="85" fill="#e63946" font-size="12" font-weight="bold">CALOR</text>
    <text x="120" y="100" fill="#ccc" font-size="9">Sede, língua vermelha</text>
    <line x1="80" y1="85" x2="60" y2="65" stroke="#e63946" stroke-width="1"/>
    
    <!-- Frio → Yin -->
    <text x="-120" y="85" fill="#53a8b6" font-size="12" font-weight="bold">FRIO</text>
    <text x="-120" y="100" fill="#ccc" font-size="9">Aversão ao frio</text>
    <line x1="-80" y1="85" x2="-60" y2="65" stroke="#53a8b6" stroke-width="1"/>
    
    <!-- Excesso → Yang -->
    <text x="-160" y="5" fill="#e63946" font-size="12" font-weight="bold">EXCESSO</text>
    <text x="-160" y="20" fill="#ccc" font-size="9">Cheio, agudo, forte</text>
    <line x1="-90" y1="10" x2="-60" y2="10" stroke="#e63946" stroke-width="1"/>
    
    <!-- Deficiência → Yin -->
    <text x="130" y="5" fill="#53a8b6" font-size="12" font-weight="bold">DEFICIÊNCIA</text>
    <text x="130" y="20" fill="#ccc" font-size="9">Vazio, fraco, crônico</text>
    <line x1="90" y1="10" x2="60" y2="10" stroke="#53a8b6" stroke-width="1"/>
  </g>
  
  <!-- Tabela comparativa -->
  <g transform="translate(50, 360)">
    <text x="0" y="0" fill="#e0e0e0" font-size="14" font-weight="bold">CLASSIFICAÇÃO</text>
    <rect x="0" y="10" width="200" height="22" rx="4" fill="#e63946" opacity="0.3"/>
    <rect x="200" y="10" width="200" height="22" rx="4" fill="#0f3460" opacity="0.3"/>
    <text x="100" y="25" text-anchor="middle" fill="#e63946" font-size="11" font-weight="bold">YANG</text>
    <text x="300" y="25" text-anchor="middle" fill="#53a8b6" font-size="11" font-weight="bold">YIN</text>
    
    <text x="10" y="50" fill="#ccc" font-size="10">Exterior (Biao)</text>
    <text x="210" y="50" fill="#ccc" font-size="10">Interior (Li)</text>
    <line x1="0" y1="55" x2="400" y2="55" stroke="#333" stroke-width="0.5"/>
    
    <text x="10" y="72" fill="#ccc" font-size="10">Calor (Re)</text>
    <text x="210" y="72" fill="#ccc" font-size="10">Frio (Han)</text>
    <line x1="0" y1="77" x2="400" y2="77" stroke="#333" stroke-width="0.5"/>
    
    <text x="10" y="94" fill="#ccc" font-size="10">Excesso (Shi)</text>
    <text x="210" y="94" fill="#ccc" font-size="10">Deficiência (Xu)</text>
    <line x1="0" y1="99" x2="400" y2="99" stroke="#333" stroke-width="0.5"/>
    
    <text x="10" y="116" fill="#ccc" font-size="10">Febre, sede, agitação</text>
    <text x="210" y="116" fill="#ccc" font-size="10">Frio, cansaço, quietude</text>
    <line x1="0" y1="121" x2="400" y2="121" stroke="#333" stroke-width="0.5"/>
    
    <text x="10" y="138" fill="#ccc" font-size="10">Pulso ↑ (rápido, cheio)</text>
    <text x="210" y="138" fill="#ccc" font-size="10">Pulso ↓ (lento, fraco)</text>
  </g>
</svg>'''
        return self._salvar_svg(path, svg, "Ba Gang")
    
    def _pontos_acupuntura_svg(self, base: Path) -> dict:
        path = base / "mtc_pontos_acupuntura.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">PONTOS PRINCIPAIS DE ACUPUNTURA</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Localização e indicações dos principais pontos</text>
  
  <!-- Grid de pontos -->
  <g transform="translate(40, 100)">
    <!-- PC6 -->
    <rect x="0" y="0" width="160" height="75" rx="8" fill="#16213e" stroke="#e63946" stroke-width="1"/>
    <circle cx="20" cy="20" r="8" fill="#e63946"/>
    <text x="33" y="24" fill="#e63946" font-size="9" font-weight="bold">PC6 (Neiguan)</text>
    <text x="10" y="42" fill="#ccc" font-size="8">Pericárdio · 2 cun acima do</text>
    <text x="10" y="55" fill="#ccc" font-size="8">punho. Náusea, vômito, dor</text>
    <text x="10" y="68" fill="#ccc" font-size="8">torácica, ansiedade.</text>
    
    <!-- ST36 -->
    <rect x="180" y="0" width="160" height="75" rx="8" fill="#16213e" stroke="#52b788" stroke-width="1"/>
    <circle cx="200" cy="20" r="8" fill="#52b788"/>
    <text x="213" y="24" fill="#52b788" font-size="9" font-weight="bold">ST36 (Zusanli)</text>
    <text x="190" y="42" fill="#ccc" font-size="8">Estômago · 3 cun abaixo da</text>
    <text x="190" y="55" fill="#ccc" font-size="8">patela. Tonificação geral,</text>
    <text x="190" y="68" fill="#ccc" font-size="8">fadiga, imunidade.</text>
    
    <!-- LI4 -->
    <rect x="360" y="0" width="160" height="75" rx="8" fill="#16213e" stroke="#ffd60a" stroke-width="1"/>
    <circle cx="380" cy="20" r="8" fill="#ffd60a"/>
    <text x="393" y="24" fill="#ffd60a" font-size="9" font-weight="bold">LI4 (Hegu)</text>
    <text x="370" y="42" fill="#ccc" font-size="8">Intestino Grosso · Entre 1°</text>
    <text x="370" y="55" fill="#ccc" font-size="8">e 2° metacarpos. Analgésico,</text>
    <text x="370" y="68" fill="#ccc" font-size="8">cefaleia, face.</text>
    
    <!-- SP6 -->
    <rect x="540" y="0" width="160" height="75" rx="8" fill="#16213e" stroke="#53a8b6" stroke-width="1"/>
    <circle cx="560" cy="20" r="8" fill="#53a8b6"/>
    <text x="573" y="24" fill="#53a8b6" font-size="9" font-weight="bold">SP6 (Sanyinjiao)</text>
    <text x="550" y="42" fill="#ccc" font-size="8">Baço · 3 cun acima do</text>
    <text x="550" y="55" fill="#ccc" font-size="8">maléolo medial. Ginecologia,</text>
    <text x="550" y="68" fill="#ccc" font-size="8">digestão, insônia.</text>
    
    <!-- LR3 -->
    <rect x="0" y="95" width="160" height="75" rx="8" fill="#16213e" stroke="#2d6a4f" stroke-width="1"/>
    <circle cx="20" cy="115" r="8" fill="#2d6a4f"/>
    <text x="33" y="119" fill="#2d6a4f" font-size="9" font-weight="bold">LR3 (Taichong)</text>
    <text x="10" y="137" fill="#ccc" font-size="8">Fígado · Entre 1° e 2°</text>
    <text x="10" y="150" fill="#ccc" font-size="8">metatarsos. Estresse,</text>
    <text x="10" y="163" fill="#ccc" font-size="8">hipertensão, cefaleia.</text>
    
    <!-- HT7 -->
    <rect x="180" y="95" width="160" height="75" rx="8" fill="#16213e" stroke="#e63946" stroke-width="1"/>
    <circle cx="200" cy="115" r="8" fill="#e63946"/>
    <text x="213" y="119" fill="#e63946" font-size="9" font-weight="bold">HT7 (Shenmen)</text>
    <text x="190" y="137" fill="#ccc" font-size="8">Coração · Prega do punho.</text>
    <text x="190" y="150" fill="#ccc" font-size="8">Insônia, ansiedade,</text>
    <text x="190" y="163" fill="#ccc" font-size="8">palpitações, Shen.</text>
    
    <!-- KI3 -->
    <rect x="360" y="95" width="160" height="75" rx="8" fill="#16213e" stroke="#0f3460" stroke-width="1"/>
    <circle cx="380" cy="115" r="8" fill="#0f3460"/>
    <text x="393" y="119" fill="#0f3460" font-size="9" font-weight="bold">KI3 (Taixi)</text>
    <text x="370" y="137" fill="#ccc" font-size="8">Rim · Maléolo medial.</text>
    <text x="370" y="150" fill="#ccc" font-size="8">Def. Rim, lombalgia,</text>
    <text x="370" y="163" fill="#ccc" font-size="8">tontura, zumbido.</text>
    
    <!-- GV20 -->
    <rect x="540" y="95" width="160" height="75" rx="8" fill="#16213e" stroke="#e6a817" stroke-width="1"/>
    <circle cx="560" cy="115" r="8" fill="#e6a817"/>
    <text x="573" y="119" fill="#e6a817" font-size="9" font-weight="bold">GV20 (Baihui)</text>
    <text x="550" y="137" fill="#ccc" font-size="8">Vaso Governador · Topo</text>
    <text x="550" y="150" fill="#ccc" font-size="8">da cabeça. Clareza</text>
    <text x="550" y="163" fill="#ccc" font-size="8">mental, cefaleia.</text>
  </g>
  
  <!-- Legenda de segurança -->
  <g transform="translate(40, 300)">
    <text x="0" y="0" fill="#e63946" font-size="10" font-weight="bold">⚠️ SEGURANÇA</text>
    <text x="0" y="18" fill="#ccc" font-size="9">LI4 e SP6 são contraindicados na gestação</text>
    <text x="0" y="33" fill="#ccc" font-size="9">(podem induzir contrações uterinas).</text>
    <text x="0" y="48" fill="#ccc" font-size="9">Moxabustão em CV4 durante gestação requer</text>
    <text x="0" y="63" fill="#ccc" font-size="9">avaliação criteriosa (ponto de virada).</text>
    <text x="0" y="78" fill="#ccc" font-size="9">Pontos da face e crânio: agulhamento</text>
    <text x="0" y="93" fill="#ccc" font-size="9">superficial para evitar pneumotórax.</text>
  </g>
</svg>'''
        return self._salvar_svg(path, svg, "Pontos de Acupuntura")
    
    def _moxabustao_svg(self, base: Path) -> dict:
        path = base / "mtc_moxabustao.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
    <radialGradient id="fogo1" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#ffd60a"/><stop offset="50%" stop-color="#e63946"/><stop offset="100" stop-color="#dc2f02"/></radialGradient>
    <radialGradient id="fumo" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#fff" opacity="0.3"/><stop offset="100" stop-color="#fff" opacity="0"/></radialGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">MOXIBUSTÃO (JIU)</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Terapia térmica com Artemísia (Ai Ye) · Aquecimento dos pontos</text>
  
  <!-- Ilustração principal -->
  <g transform="translate(250, 80)">
    <!-- Cone de moxa -->
    <ellipse cx="100" cy="250" rx="60" ry="10" fill="#4a3000" opacity="0.5"/>
    <path d="M45 250 L70 100 L130 100 L155 250 Z" fill="#8b6914" stroke="#6b4f12" stroke-width="1.5"/>
    <path d="M55 250 L75 110 L125 110 L145 250 Z" fill="#a6821a"/>
    <path d="M65 250 L80 120 L120 120 L135 250 Z" fill="#c49a1f"/>
    
    <!-- Fogo no topo -->
    <ellipse cx="100" cy="95" rx="20" ry="25" fill="url(#fogo1)" opacity="0.9"/>
    <ellipse cx="95" cy="88" rx="8" ry="12" fill="#ffd60a" opacity="0.8"/>
    <ellipse cx="100" cy="90" rx="3" ry="6" fill="#fff" opacity="0.9"/>
    
    <!-- Fumaça -->
    <path d="M85 70 Q75 40, 80 20 Q85 0, 90 -20 Q95 -40, 100 -60" fill="none" stroke="#fff" stroke-width="1" opacity="0.2" stroke-dasharray="4,4"/>
    <path d="M115 70 Q125 40, 120 20 Q115 0, 110 -20" fill="none" stroke="#fff" stroke-width="1" opacity="0.15" stroke-dasharray="3,5"/>
    
    <!-- Calor radiante -->
    <path d="M70 130 Q90 140, 100 130 Q110 140, 130 130" fill="none" stroke="#ffd60a" stroke-width="1" opacity="0.3"/>
    <path d="M60 150 Q90 160, 100 150 Q110 160, 140 150" fill="none" stroke="#e63946" stroke-width="1" opacity="0.2"/>
    
    <!-- Pele -->
    <path d="M20 250 Q100 240, 180 250 Q200 255, 200 265 Q100 260, 0 265 Z" fill="#d4739d" stroke="#b05b7d" stroke-width="1"/>
    
    <!-- Ponto de aplicação -->
    <circle cx="100" cy="255" r="5" fill="#e63946" opacity="0.6"/>
    <circle cx="100" cy="255" r="10" fill="none" stroke="#e63946" stroke-width="0.5" opacity="0.3"/>
  </g>
  
  <!-- Informações -->
  <g transform="translate(460, 90)">
    <text x="0" y="0" fill="#e0e0e0" font-size="14" font-weight="bold">MOXABUSTÃO DIRETA</text>
    <text x="0" y="20" fill="#ccc" font-size="10">Cone de Artemísia sobre o ponto</text>
    
    <text x="0" y="50" fill="#e0e0e0" font-size="12" font-weight="bold">TÉCNICAS</text>
    <text x="0" y="68" fill="#ccc" font-size="10">• Moxa direta (sobre a pele)</text>
    <text x="0" y="84" fill="#ccc" font-size="10">• Moxa indireta (com gengibre/sal)</text>
    <text x="0" y="100" fill="#ccc" font-size="10">• Bastão de moxa (aquecimento)</text>
    <text x="0" y="116" fill="#ccc" font-size="10">• Caixa de moxa (áreas amplas)</text>
    
    <text x="0" y="146" fill="#e0e0e0" font-size="12" font-weight="bold">INDICAÇÕES</text>
    <text x="0" y="164" fill="#ccc" font-size="10">• Def. de Yang (frio, cansaço)</text>
    <text x="0" y="180" fill="#ccc" font-size="10">• Síndromes de Frio</text>
    <text x="0" y="196" fill="#ccc" font-size="10">• Dor lombar (BL23)</text>
    <text x="0" y="212" fill="#ccc" font-size="10">• Fadiga crônica (ST36)</text>
    <text x="0" y="228" fill="#ccc" font-size="10">• Virada de feto (BL67)</text>
  </g>
</svg>'''
        return self._salvar_svg(path, svg, "Moxibustão")
    
    def _tecnicas_agulhamento_svg(self, base: Path) -> dict:
        path = base / "mtc_tecnicas_agulhamento.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">TÉCNICAS DE AGULHAMENTO</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Métodos de estimulação e manipulação de agulhas</text>
  
  <!-- Grid de técnicas -->
  <g transform="translate(40, 100)">
    <!-- Técnica 1 -->
    <rect x="0" y="0" width="220" height="110" rx="10" fill="#16213e" stroke="#e63946" stroke-width="1"/>
    <text x="15" y="22" fill="#e63946" font-size="12" font-weight="bold">Tonificação (Bu)</text>
    <line x1="15" y1="30" x2="205" y2="30" stroke="#e63946" stroke-width="0.5" opacity="0.3"/>
    <text x="15" y="48" fill="#ccc" font-size="10">• Inserção lenta (seguindo o qi)</text>
    <text x="15" y="64" fill="#ccc" font-size="10">• Rotação horária suave</text>
    <text x="15" y="80" fill="#ccc" font-size="10">• Retirada rápida</text>
    <text x="15" y="96" fill="#ccc" font-size="10">• Técnica: pouca manipulação</text>
    
    <!-- Técnica 2 -->
    <rect x="240" y="0" width="220" height="110" rx="10" fill="#16213e" stroke="#53a8b6" stroke-width="1"/>
    <text x="15" y="22" fill="#53a8b6" font-size="12" font-weight="bold">Sedação (Xie)</text>
    <line x1="15" y1="30" x2="205" y2="30" stroke="#53a8b6" stroke-width="0.5" opacity="0.3"/>
    <text x="15" y="48" fill="#ccc" font-size="10">• Inserção rápida</text>
    <text x="15" y="64" fill="#ccc" font-size="10">• Rotação anti-horária forte</text>
    <text x="15" y="80" fill="#ccc" font-size="10">• Retirada lenta</text>
    <text x="15" y="96" fill="#ccc" font-size="10">• Técnica: manipulação intensa</text>
    
    <!-- Técnica 3 -->
    <rect x="480" y="0" width="220" height="110" rx="10" fill="#16213e" stroke="#52b788" stroke-width="1"/>
    <text x="15" y="22" fill="#52b788" font-size="12" font-weight="bold">Método Sobre o Fogo</text>
    <line x1="15" y1="30" x2="205" y2="30" stroke="#52b788" stroke-width="0.5" opacity="0.3"/>
    <text x="15" y="48" fill="#ccc" font-size="10">• Para síndromes de Frio</text>
    <text x="15" y="64" fill="#ccc" font-size="10">• Agulha aquecida</text>
    <text x="15" y="80" fill="#ccc" font-size="10">• Moxa na base da agulha</text>
    <text x="15" y="96" fill="#ccc" font-size="10">• Calor conduzido ao ponto</text>
    
    <!-- Técnica 4 -->
    <rect x="0" y="130" width="220" height="110" rx="10" fill="#16213e" stroke="#e6a817" stroke-width="1"/>
    <text x="15" y="152" fill="#e6a817" font-size="12" font-weight="bold">Sangria (Ci Xue)</text>
    <line x1="15" y1="160" x2="205" y2="160" stroke="#e6a817" stroke-width="0.5" opacity="0.3"/>
    <text x="15" y="178" fill="#ccc" font-size="10">• Punção de vasos superficiais</text>
    <text x="15" y="194" fill="#ccc" font-size="10">• Para Calor e Estagnação</text>
    <text x="15" y="210" fill="#ccc" font-size="10">• Pontos: LR3, GV20, ouvido</text>
    <text x="15" y="226" fill="#ccc" font-size="10">• 3-5 gotas de sangue</text>
    
    <!-- Técnica 5 -->
    <rect x="240" y="130" width="220" height="110" rx="10" fill="#16213e" stroke="#0f3460" stroke-width="1"/>
    <text x="15" y="152" fill="#0f3460" font-size="12" font-weight="bold">Eletroacupuntura</text>
    <line x1="15" y1="160" x2="205" y2="160" stroke="#0f3460" stroke-width="0.5" opacity="0.3"/>
    <text x="15" y="178" fill="#ccc" font-size="10">• Estimulação elétrica</text>
    <text x="15" y="194" fill="#ccc" font-size="10">• Frequência baixa (2Hz):</text>
    <text x="15" y="210" fill="#ccc" font-size="10">  liberação de endorfina</text>
    <text x="15" y="226" fill="#ccc" font-size="10">• Frequência alta (80Hz):</text>
    <text x="15" y="242" fill="#ccc" font-size="10">  liberação de serotonina</text>
    
    <!-- Técnica 6 -->
    <rect x="480" y="130" width="220" height="110" rx="10" fill="#16213e" stroke="#2d6a4f" stroke-width="1"/>
    <text x="15" y="152" fill="#2d6a4f" font-size="12" font-weight="bold">Auriculoterapia</text>
    <line x1="15" y1="160" x2="205" y2="160" stroke="#2d6a4f" stroke-width="0.5" opacity="0.3"/>
    <text x="15" y="178" fill="#ccc" font-size="10">• Microssistema da orelha</text>
    <text x="15" y="194" fill="#ccc" font-size="10">• Agulhas semi-permanentes</text>
    <text x="15" y="210" fill="#ccc" font-size="10">  ou sementes de mostarda</text>
    <text x="15" y="226" fill="#ccc" font-size="10">• Para: ansiedade, peso,</text>
    <text x="15" y="242" fill="#ccc" font-size="10">  dores crônicas, adicção</text>
  </g>
</svg>'''
        return self._salvar_svg(path, svg, "Técnicas de Agulhamento")
    
    def _qigong_svg(self, base: Path) -> dict:
        path = base / "mtc_qigong.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">QIGONG E TAI CHI</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Práticas corporais para cultivo do Qi · Movimento e meditação</text>
  <text x="400" y="85" text-anchor="middle" fill="#666" font-family="sans-serif" font-size="10">Ilustração conceitual — consulte um profissional para a prática</text>
</svg>'''
        return self._salvar_svg(path, svg, "Qigong e Tai Chi")
    
    def _clinica_medica_svg(self, base: Path) -> dict:
        path = base / "mtc_clinica_medica.svg"
        svg = '''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="100%" height="100%">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a1a2e"/><stop offset="1" stop-color="#16213e"/></linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(#bg)" rx="16"/>
  <text x="400" y="45" text-anchor="middle" fill="#e0e0e0" font-family="serif" font-size="22" font-weight="bold">CLÍNICA MÉDICA</text>
  <text x="400" y="65" text-anchor="middle" fill="#888" font-family="sans-serif" font-size="11">Medicina Baseada em Evidências · Integração MTC-Ocidental</text>
</svg>'''
        return self._salvar_svg(path, svg, "Clínica Médica")

# ═══════════════════════════════════════════════════════════════
# ORQUESTRADOR PRINCIPAL
# ═══════════════════════════════════════════════════════════════

class OrquestradorExtracao:
    """Orquestrador principal que coordena todos os agentes.
    
    Cargo: Arquiteto-Chefe de Extração de Conhecimento
    Função: Coordena o pipeline completo de extração, desde o
    catálogo até a geração de SVGs e pacotes para curadoria.
    Delegua tarefas para cada agente especializado.
    """
    
    def __init__(self, tools_dir: Path = Path("tools"), output_dir: Path = Path("packs")):
        self.dir = tools_dir
        self.output = output_dir
        self.output.mkdir(parents=True, exist_ok=True)
        self.cache = self.output / ".cache"
        
        # Inicializa agentes
        self.catalogador = AgenteCatalogador(tools_dir)
        self.baixador = AgenteDownloader(self.cache)
        self.extraidor = AgenteExtraidor()
        self.validador = AgenteValidador()
        self.ilustrador = AgenteIlustrador()
        
        self.pdftotext = shutil.which("pdftotext") or ""
    
    def processar(self, fonte: str, max_items: int = 0, gerar_svgs: bool = False) -> Path:
        """Processa uma fonte completa: catálogo → download → extração → validação → pacote."""
        
        log.info(f"═" * 60)
        log.info(f"PROCESSANDO FONTE: {fonte}")
        log.info(f"═" * 60)
        
        # 1. Catalogar
        log.info(f"\n📋 [CATALOGADOR] Catalogando fontes...")
        entries = self.catalogador.catalogar(fonte)
        if max_items > 0:
            entries = entries[:max_items]
        log.info(f"   {len(entries)} entradas catalogadas")
        
        if not entries:
            return self._gerar_relatorio_vazio(fonte)
        
        # 2. Baixar PDFs
        log.info(f"\n⬇️  [DOWNLOADER] Baixando PDFs...")
        baixados = []
        falhas = 0
        for entry in entries:
            result = self.baixador.baixar(entry)
            if result.download_ok:
                baixados.append(result)
            else:
                falhas += 1
        log.info(f"   {len(baixados)} baixados, {falhas} falhas")
        
        # 3. Extrair seções
        log.info(f"\n🔍 [EXTRAIDOR] Extraindo seções dos PDFs...")
        todas_secoes = []
        for entry in baixados:
            secoes = self.extraidor.extrair(entry, self.pdftotext)
            todas_secoes.extend(secoes)
        log.info(f"   {len(todas_secoes)} seções extraídas")
        
        # 4. Validar (filtro de ruído)
        log.info(f"\n🔬 [VALIDADOR] Aplicando filtro de ruído e sinal clínico...")
        rejeitados = {"sem_sinal": 0, "vetado": 0, "curto": 0, "estrutura": 0}
        aceitos = []
        for secao in todas_secoes:
            secao = self.validador.validar(secao)
            if secao.accepted:
                aceitos.append(secao)
            elif "sinal" in secao.reject_reason:
                rejeitados["sem_sinal"] += 1
            elif secao.reject_reason in ("capa/portaria", "termo_consentimento", "expediente", "referencias"):
                rejeitados["vetado"] += 1
            elif "curto" in secao.reject_reason:
                rejeitados["curto"] += 1
            else:
                rejeitados["estrutura"] += 1
        
        log.info(f"   ✅ {len(aceitos)} aceitos | ❌ {sum(rejeitados.values())} rejeitados")
        log.info(f"      (sem sinal: {rejeitados['sem_sinal']}, vetado: {rejeitados['vetado']}, "
                  f"curto: {rejeitados['curto']}, estrutura: {rejeitados['estrutura']})")
        
        # 5. Gerar pacote
        log.info(f"\n📦 [EMPACOTADOR] Gerando pacote para curadoria...")
        pack = self._gerar_pack(fonte, entries, aceitos)
        pack_path = self.output / f"pack_{fonte}.json"
        pack_path.write_text(json.dumps(pack, ensure_ascii=False, indent=1), encoding="utf-8")
        log.info(f"   Pacote salvo: {pack_path}")
        
        # 6. Gerar SVGs (opcional)
        if gerar_svgs:
            log.info(f"\n🎨 [ILUSTRADOR] Gerando SVGs...")
            svg_dir = self.output / "svgs"
            svg_dir.mkdir(parents=True, exist_ok=True)
            svgs = self.ilustrador.gerar_todas(svg_dir)
            log.info(f"   {len(svgs)} SVGs gerados")
        
        # 7. Relatório
        log.info(f"\n{'=' * 60}")
        log.info(f"RESUMO: {fonte}")
        log.info(f"  Catálogo:      {len(entries)} entradas")
        log.info(f"  Downloads:     {len(baixados)} ok, {falhas} falhas")
        log.info(f"  Seções:        {len(todas_secoes)} extraídas")
        log.info(f"  Aceitas:       {len(aceitos)} (passaram no filtro)")
        log.info(f"  Rejeitadas:    {sum(rejeitados.values())}")
        log.info(f"  Pacote:        {pack_path}")
        log.info(f"{'=' * 60}")
        
        return pack_path
    
    def processar_todas(self, max_items: int = 5, gerar_svgs: bool = True) -> list[Path]:
        """Processa todas as fontes disponíveis."""
        resultados = []
        for fonte in sorted(self.catalogador.FONTES.keys()):
            try:
                path = self.processar(fonte, max_items=max_items, gerar_svgs=gerar_svgs)
                resultados.append(path)
            except Exception as e:
                log.error(f"Falha ao processar {fonte}: {e}")
        return resultados
    
    def _gerar_pack(self, fonte: str, entries: list, sections: list[ExtractedSection]) -> list[dict]:
        """Gera um ou mais pacotes no formato esperado pela curadoria."""
        packs = []
        items_por_pack = 100
        for i in range(0, len(sections), items_por_pack):
            batch = sections[i:i + items_por_pack]
            packs.append({
                "source": f"Knowledge Extractor: {fonte}",
                "items": [
                    {
                        "id": sec.id,
                        "title": sec.title,
                        "category": sec.category,
                        "summary": sec.summary,
                        "content": sec.content,
                        "tags": sec.tags,
                        "citation": sec.citation,
                        "sourceUrl": sec.sourceUrl,
                        "sourceRef": sec.sourceRef,
                    }
                    for sec in batch
                ],
            })
        return packs
    
    def _gerar_relatorio_vazio(self, fonte: str) -> Path:
        path = self.output / f"pack_{fonte}.json"
        path.write_text("[]", encoding="utf-8")
        return path

    def gerar_paginas_conteudo(self) -> list[dict]:
        """Gera conteúdo completo para cada categoria MTC."""
        paginas = []
        paginas.append({
            "category": "MERIDIANOS",
            "title": "Meridianos Principais — Os 12 Canais Regulares",
            "description": "Os 12 meridianos principais (Jing Mai) formam a rede energética que conecta todos os órgãos e tecidos. Cada meridiano tem um trajeto específico, horário de pico e funções associadas.",
            "svg": "mtc_meridianos.svg",
            "subtopics": [
                {"name": "Meridiano do Pulmão", "time": "3-5h", "element": "Metal", "points": 11},
                {"name": "Meridiano do Intestino Grosso", "time": "5-7h", "element": "Metal", "points": 20},
                {"name": "Meridiano do Estômago", "time": "7-9h", "element": "Terra", "points": 45},
                {"name": "Meridiano do Baço", "time": "9-11h", "element": "Terra", "points": 21},
                {"name": "Meridiano do Coração", "time": "11-13h", "element": "Fogo", "points": 9},
                {"name": "Meridiano do Intestino Delgado", "time": "13-15h", "element": "Fogo", "points": 19},
                {"name": "Meridiano da Bexiga", "time": "15-17h", "element": "Água", "points": 67},
                {"name": "Meridiano do Rim", "time": "17-19h", "element": "Água", "points": 27},
                {"name": "Meridiano do Pericárdio", "time": "19-21h", "element": "Fogo", "points": 9},
                {"name": "Triplo Aquecedor (San Jiao)", "time": "21-23h", "element": "Fogo", "points": 23},
                {"name": "Meridiano da Vesícula Biliar", "time": "23-1h", "element": "Madeira", "points": 44},
                {"name": "Meridiano do Fígado", "time": "1-3h", "element": "Madeira", "points": 14},
            ],
            "references": [
                "Maciocia 2015, cap. 3 — Os Meridianos",
                "Deadman 2017, cap. 2 — Guia dos Meridianos",
                "WHO — Standard Acupuncture Nomenclature",
            ],
            "links": ["https://www.who.int/publications/i/item/9789240068076"],
        })
        return paginas


def main():
    import argparse
    parser = argparse.ArgumentParser(description="Knowledge Extractor — Sistema multi-agente de extração")
    parser.add_argument("--fonte", "-f", default="todas", help="Fonte a processar (nice/who/kdigo/esc/aha/pcdt/todas)")
    parser.add_argument("--output", "-o", default="packs", help="Diretório de saída")
    parser.add_argument("--max", "-m", type=int, default=0, help="Máximo de itens por fonte (0 = todos)")
    parser.add_argument("--svgs", action="store_true", help="Gerar SVGs ilustrativos")
    parser.add_argument("--listar", action="store_true", help="Listar fontes disponíveis")
    parser.add_argument("--paginas", action="store_true", help="Gerar conteúdo completo das páginas de assunto")
    
    args = parser.parse_args()
    
    ext = OrquestradorExtracao(output_dir=Path(args.output))
    
    if args.listar:
        print(ext.catalogador.listar_disponiveis())
        return
    
    if args.paginas:
        paginas = ext.gerar_paginas_conteudo()
        out = Path(args.output) / "paginas_conteudo.json"
        out.write_text(json.dumps(paginas, ensure_ascii=False, indent=1), encoding="utf-8")
        print(f"{len(paginas)} páginas geradas em {out}")
        return
    
    if args.fonte == "todas":
        ext.processar_todas(max_items=args.max, gerar_svgs=args.svgs)
    else:
        ext.processar(args.fonte, max_items=args.max, gerar_svgs=args.svgs)


if __name__ == "__main__":
    main()
