#!/usr/bin/env python3
"""
INGESTOR DE DIRETRIZES PÚBLICAS → pacote JSON para a fila de curadoria.

Isto é ferramenta de build, não código do app. Ele NÃO escreve conteúdo clínico:
baixa um documento público, extrai o texto que já está lá e recorta em seções,
carregando o número da página de cada trecho.

Por que a página importa (R4): a citação "Diretriz ACC/AHA 2023" não permite
conferência — nomeia um documento sem apontar para nada dentro dele. Já
"PCDT Asma, p. 26" a médica abre e compara em segundos. É a diferença entre
revisar e conferir, e é ela que torna uma fila de centenas de itens viável.

Uso:
    python tools/ingest_guidelines.py fontes.json > pack.json

Formato de fontes.json:
    [{"url": "...", "titulo": "PCDT Asma", "categoria": "CLINICA_MEDICA",
      "fonte": "Ministério da Saúde — PCDT", "prefixo_id": "pcdt_asma"}]

Requer: pdftotext (poppler). Sem ele, aborta — nunca inventa o texto.
"""
from __future__ import annotations

import json
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, asdict
from pathlib import Path

# Um trecho curto demais não sustenta uma resposta; longo demais deixa de ser
# "a seção que responde" e vira despejo de página.
MIN_CHARS = 320
MAX_CHARS = 2400

UA = "Mozilla/5.0 (compatible; BioAcupunt-ingest/1.0)"


@dataclass
class Item:
    id: str
    title: str
    category: str
    summary: str
    content: str
    tags: list[str]
    citation: str
    sourceUrl: str
    sourceRef: str


def require_pdftotext() -> str:
    exe = shutil.which("pdftotext")
    if not exe:
        sys.exit(
            "ERRO: pdftotext não encontrado (instale poppler-utils).\n"
            "Abortando: sem extração real não há ingestão — este script não\n"
            "gera conteúdo clínico, só transporta o que está no documento."
        )
    return exe


def download_url(page_url: str) -> str:
    """Deriva a URL do PDF a partir da página de listagem do gov.br."""
    base = page_url.rstrip("/")
    if base.endswith("/view"):
        base = base[: -len("/view")]
    return base + "/@@download/file"


def download(url: str, dest: Path, tentativas: int = 3) -> None:
    # O gov.br corta conexão em PDFs grandes (IncompleteRead). Repetir resolve;
    # aceitar o download parcial produziria um PDF truncado, e daí um acervo com
    # metade de um protocolo — pior que fonte ausente, porque parece completo.
    ultimo: Exception | None = None
    for n in range(tentativas):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=180) as r:
                data = r.read()
            if not data.startswith(b"%PDF"):
                raise ValueError(f"não é PDF (talvez landing page): {url}")
            dest.write_bytes(data)
            return
        except Exception as e:
            ultimo = e
            if n < tentativas - 1:
                time.sleep(2 * (n + 1))
    raise ultimo  # type: ignore[misc]


def pages_of(pdf: Path, exe: str) -> list[str]:
    """Texto por página. O índice+1 é o número da página impressa no PDF."""
    out = pdf.with_suffix(".txt")
    subprocess.run(
        [exe, "-layout", "-enc", "UTF-8", str(pdf), str(out)],
        check=True, capture_output=True,
    )
    return out.read_text(encoding="utf-8", errors="replace").split("\f")


# Cabeçalho numerado ("7.2 Tratamento") ou linha inteira em caixa alta.
HEADING = re.compile(r"^\s{0,8}((?:\d+\.){0,3}\d+\s+[A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\n]{4,80}|[A-ZÁÉÍÓÚÂÊÔÃÕÇ][A-ZÁÉÍÓÚÂÊÔÃÕÇ \-/,]{8,70})\s*$")


def sections(page: str) -> list[tuple[str, str]]:
    """(título, corpo) por cabeçalho. Sem cabeçalho, a página inteira vira um bloco."""
    lines = page.splitlines()
    marks = [i for i, l in enumerate(lines) if HEADING.match(l)]
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


# ── Filtro de excelência ───────────────────────────────────────────────────
# Uma seção extraída não é automaticamente um item de biblioteca.
#
# A versão anterior deste filtro rejeitava ruído conhecido e mantinha o resto —
# e passava 15% de lixo, porque "resto" inclui tudo que ainda não foi catalogado
# como ruído. A lógica está invertida aqui: um item só entra na fila se
# apresentar **sinal clínico positivo**. O ônus da prova é do texto.
#
# Consequência assumida: paráfrase de fisiopatologia bem escrita, sem dose nem
# conduta, é descartada. É perda real. Mas a fila existe para a médica decidir o
# que o app pode responder à beira do leito, e nesse contexto um parágrafo que
# não conclui nada ocupa o lugar de um que conclui.

_SINAIS: dict[str, re.Pattern] = {
    # Posologia: número + unidade farmacológica. O sinal mais forte de conduta.
    "posologia": re.compile(
        r"\b\d[\d.,]*\s?(?:mg|mcg|µg|g|ml|mL|UI|mmol|mEq)\b"
        r"|\bmg\s?/\s?(?:kg|m²|m2|dia|dL)\b|\bUI\s?/\s?(?:kg|dia)\b"
    ),
    "criterio": re.compile(
        r"crit[ée]rios? de (?:diagn[óo]stic|inclus|exclus)|"
        r"define?-se|considera-se|diagn[óo]stico (?:[ée]|se|de)|confirma[çc][ãa]o diagn",
        re.I,
    ),
    "conduta": re.compile(
        r"recomenda-se|est[áa] indicad|deve-se|tratamento (?:de|da|do|com|deve)|"
        r"primeira linha|esquema terap[êe]utic|conduta|manejo",
        re.I,
    ),
    "seguranca": re.compile(
        r"contraindica[çc]|efeitos? adverso|rea[çc][õo]es? adversa|intera[çc][ãa]o "
        r"medicamentos|risco de|toxicidade|suspend|descontinua",
        re.I,
    ),
    "monitorizacao": re.compile(
        r"monitor|acompanhamento|seguimento|reavalia|exames? (?:de controle|laboratoriais)",
        re.I,
    ),
}

# VETO: uma única ocorrência já condena a seção inteira. São frases que só
# aparecem em página administrativa — capa de portaria, termo de consentimento,
# ficha de dispensação. Exigir densidade aqui foi o erro da versão anterior:
# a capa do PCDT tem UMA linha de portaria e 30 de texto, e passava.
_VETO: dict[str, re.Pattern] = {
    "capa/portaria": re.compile(
        r"portaria conjunta|aprova o protocolo cl[íi]nico|di[áa]rio oficial|"
        r"fica aprovado|esta portaria entra em vigor|revoga-se a portaria",
        re.I,
    ),
    "termo de consentimento": re.compile(
        r"termo de esclarecimento e responsabilidade|declaro que fui claramente|"
        r"assinatura do (?:paciente|respons)|ficha farmacoterap[êe]utica",
        re.I,
    ),
    "expediente/créditos": re.compile(
        r"equipe t[ée]cnica|elabora[çc][ãa]o t[ée]cnica|revis[ãa]o t[ée]cnica|"
        r"secret[áa]rio de aten[çc][ãa]o especializada",
        re.I,
    ),
}

# DENSIDADE: a frase pode aparecer legitimamente numa discussão clínica; só
# condena quando domina a seção.
_DESCARTES: dict[str, re.Pattern] = {
    "referências bibliográficas": re.compile(
        r"\bet al\b|doi:\s*10\.|\bISBN\b|dispon[íi]vel em:\s*<?https?://", re.I,
    ),
}

_REF_NUMERADA = re.compile(r"^\s*\d{1,3}[\.\)]\s+[A-ZÀ-Ú][\wÀ-ú'\-]+,?\s+[A-Z]{1,3}[\.,]", re.M)
_SUMARIO = re.compile(r"\.{4,}\s*\d+\s*$", re.M)
_CID = re.compile(r"\b[A-Z]\d{2}(?:\.\d)?\b")


def assess(title: str, body: str) -> tuple[bool, str]:
    """(aceita?, motivo). O motivo alimenta o relatório de descarte."""
    t = title.lower()
    linhas = [l for l in body.splitlines() if l.strip()]
    if not linhas:
        return False, "vazio"

    # ── 1. Descartes explícitos ────────────────────────────────────────────
    if re.match(r"^\s*(refer[êe]ncias|bibliografia|sum[áa]rio|[íi]ndice|ap[êe]ndice)", t):
        return False, "seção não-clínica (referências/sumário/apêndice)"

    for motivo, rx in _VETO.items():
        if rx.search(body) or rx.search(t):
            return False, motivo

    for motivo, rx in _DESCARTES.items():
        # Uma menção isolada não condena; densidade sim.
        hits = sum(bool(rx.search(l)) for l in linhas)
        if hits / len(linhas) > 0.25 or rx.search(t):
            return False, motivo

    if len(_SUMARIO.findall(body)) >= 3:
        return False, "sumário (pontilhado + página)"

    if len(_REF_NUMERADA.findall(body)) >= 3:
        return False, "lista de referências numeradas"

    # Lista de códigos CID sem prosa em volta.
    if len(_CID.findall(body)) >= 6 and len(body.split()) < 160:
        return False, "lista de códigos CID"

    # ── 2. Qualidade estrutural ────────────────────────────────────────────
    palavras = body.split()
    if len(palavras) < 45:
        return False, "curto demais para sustentar resposta"

    sem_espaco = re.sub(r"\s", "", body)
    if not sem_espaco:
        return False, "vazio"

    letras = sum(c.isalpha() for c in sem_espaco)
    if letras / len(sem_espaco) < 0.62:
        # Tabela que o pdftotext desmontou vira sopa de números e pontuação.
        return False, "tabela desmontada (pouca prosa)"

    alfa = [c for c in body if c.isalpha()]
    if alfa and sum(c.isupper() for c in alfa) / len(alfa) > 0.45:
        return False, "predominantemente maiúsculas (cabeçalhos)"

    # Prosa ou lista de verdade — não fragmento solto.
    if "." not in body and not re.search(r"^\s*[-•–]", body, re.M):
        return False, "sem estrutura de frase ou lista"

    # ── 3. Exigência de sinal clínico ──────────────────────────────────────
    presentes = [nome for nome, rx in _SINAIS.items() if rx.search(body)]
    if not presentes:
        return False, "sem sinal clínico (nem dose, critério, conduta, segurança ou monitorização)"

    # Um sinal só pode ser coincidência lexical ("risco de" numa introdução).
    # Dois sinais distintos indicam texto que efetivamente conclui algo.
    if len(presentes) < 2:
        return False, f"sinal clínico fraco (apenas: {presentes[0]})"

    return True, "+".join(sorted(presentes))


def clean(text: str) -> str:
    text = re.sub(r"[ \t]{3,}", "  ", text)
    return re.sub(r"\n{3,}", "\n\n", text).strip()


def build(src: dict, exe: str) -> list[Item]:
    pdf_url = src.get("url") or download_url(src["url_pagina"])
    with tempfile.TemporaryDirectory() as td:
        pdf = Path(td) / "doc.pdf"
        download(pdf_url, pdf)
        pages = pages_of(pdf, exe)

    items: list[Item] = []
    descartes: dict[str, int] = {}
    for pageno, page in enumerate(pages, start=1):
        for title, body in sections(page):
            body = clean(body)
            if len(body) < MIN_CHARS:
                descartes["curto demais"] = descartes.get("curto demais", 0) + 1
                continue
            ok, motivo = assess(title, body)
            if not ok:
                descartes[motivo] = descartes.get(motivo, 0) + 1
                continue
            body = body[:MAX_CHARS]
            n = len(items) + 1
            heading = title.strip() or f"{src['titulo']} — p. {pageno}"
            items.append(Item(
                id=f"{src['prefixo_id']}_{n:04d}",
                title=heading[:120],
                category=src["categoria"],
                summary=" ".join(body.split())[:240],
                content=body,
                tags=src.get("tags", []),
                # Citação com localizador: é isto que a médica confere.
                citation=f"{src['titulo']}, p. {pageno}",
                # A URL da PÁGINA (não do binário): é o que a médica abre e navega.
                sourceUrl=src.get("url_pagina") or src.get("url", ""),
                sourceRef=f"p. {pageno}",
            ))

    if descartes:
        resumo = ", ".join(f"{v} {k}" for k, v in sorted(descartes.items(), key=lambda x: -x[1]))
        print(f"      descartados: {resumo}", file=sys.stderr)
    return items


def main() -> None:
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    exe = require_pdftotext()
    srcs = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    dest = Path(sys.argv[2])

    # Paralelismo modesto e deliberado: o trabalho é I/O de rede, não CPU.
    # 6 conexões aceleram ~6x sem parecer abuso para o servidor do gov.br —
    # martelar uma fonte pública com dezenas de conexões rende bloqueio, não
    # velocidade.
    packs = []
    with ThreadPoolExecutor(max_workers=6) as pool:
        futuros = {pool.submit(build, src, exe): src for src in srcs}
        for n, fut in enumerate(as_completed(futuros), 1):
            src = futuros[fut]
            try:
                items = fut.result()
            except Exception as e:  # falha de uma fonte não derruba o lote
                print(f"[{n}/{len(srcs)}] [falhou] {src['titulo']}: {e}", file=sys.stderr)
                continue
            print(f"[{n}/{len(srcs)}] [ok] {src['titulo']}: {len(items)} itens", file=sys.stderr)
            packs.append({"source": src["fonte"], "items": [asdict(i) for i in items]})

    # Unicidade de id é invariante, não expectativa: o repositório de staging
    # deduplica por id, então dois itens com o mesmo id viram um — em silêncio.
    # Falha alto aqui, antes de o acervo chegar perto da fila de revisão.
    ids = [i["id"] for p in packs for i in p["items"]]
    if len(ids) != len(set(ids)):
        from collections import Counter
        dup = {k: v for k, v in Counter(ids).items() if v > 1}
        sys.exit(
            f"ABORTADO: {len(dup)} ids duplicados afetando {sum(dup.values())} itens.\n"
            f"Exemplos: {list(dup)[:5]}\n"
            "Id colidido apaga conteúdo silenciosamente na curadoria."
        )

    # UTF-8 explícito: o console do Windows é cp1252 e destruiria os acentos.
    dest.write_text(json.dumps(packs, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"[gravado] {dest} — {len(packs)} pacotes, {len(ids)} itens", file=sys.stderr)


if __name__ == "__main__":
    main()
