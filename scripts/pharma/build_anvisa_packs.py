#!/usr/bin/env python3
"""Gera os packs JSON do catálogo ANVISA pra app/src/main/assets/packs/pharma_anvisa/.

Fonte: dados.anvisa.gov.br/dados/DADOS_ABERTOS_MEDICAMENTOS.csv — dataset público,
delimitado por ';', encoding ISO-8859-1 (latin-1), com vírgulas dentro de campos
citados (ex.: PRINCIPIO_ATIVO com múltiplos ativos) — por isso usa o módulo `csv`
real, nunca split ingênuo por ';'.

Cobre só a camada de IDENTIFICAÇÃO (nome comercial, princípio ativo, classe
terapêutica, categoria regulatória, fabricante, registro, situação). Posologia,
interação, contraindicação e composição de excipientes NÃO existem em nenhum
dataset aberto em bulk — essa camada é curada manualmente pela médica dentro do
app (ver FormularioMedicamento), nunca gerada por este script.

Uso:
    python scripts/pharma/build_anvisa_packs.py [--csv CAMINHO] [--out DIR] [--batch-size N]

Se --csv não for passado, baixa o CSV de dados.anvisa.gov.br.
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
import unicodedata
import urllib.request
from datetime import date
from pathlib import Path

ANVISA_URL = "https://dados.anvisa.gov.br/dados/DADOS_ABERTOS_MEDICAMENTOS.csv"
DEFAULT_OUT = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets" / "packs" / "pharma_anvisa"
DEFAULT_BATCH_SIZE = 1000

# Mapa de CATEGORIA_REGULATORIA (como aparece no CSV, já sem acento/maiúsculo) pro
# enum Kotlin CategoriaRegulatoria. Qualquer valor não mapeado cai em OUTRO — a
# ingestão nunca descarta uma linha só por categoria desconhecida.
CATEGORIA_MAP = {
    "GENERICO": "GENERICO",
    "SIMILAR": "SIMILAR",
    "REFERENCIA": "REFERENCIA",
    "FITOTERAPICO": "FITOTERAPICO",
    "BIOLOGICO": "BIOLOGICO",
    "NOVO": "NOVO",
    "ESPECIFICO": "ESPECIFICO",
    "DINAMIZADO": "DINAMIZADO",
}


def _strip_accents(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    return "".join(ch for ch in normalized if not unicodedata.combining(ch))


def map_categoria(raw: str) -> str:
    key = _strip_accents(raw).strip().upper()
    return CATEGORIA_MAP.get(key, "OUTRO")


def split_principios_ativos(raw: str) -> list[str]:
    return [p.strip() for p in raw.split(",") if p.strip()]


def download_csv(dest: Path) -> Path:
    print(f"Baixando {ANVISA_URL} -> {dest}", file=sys.stderr)
    with urllib.request.urlopen(ANVISA_URL, timeout=60) as response, open(dest, "wb") as f:
        f.write(response.read())
    return dest


def load_rows(csv_path: Path):
    with open(csv_path, "r", encoding="ISO-8859-1", newline="") as f:
        reader = csv.DictReader(f, delimiter=";")
        yield from reader


def build_items(csv_path: Path) -> tuple[list[dict], dict[str, int]]:
    stats = {"lidas": 0, "validas": 0, "rejeitadas_tipo": 0, "rejeitadas_inativo": 0,
              "rejeitadas_sem_id": 0, "duplicadas": 0}
    seen_ids: set[str] = set()
    items: list[dict] = []

    for row in load_rows(csv_path):
        stats["lidas"] += 1

        if (row.get("TIPO_PRODUTO") or "").strip().upper() != "MEDICAMENTO":
            stats["rejeitadas_tipo"] += 1
            continue
        if (row.get("SITUACAO_REGISTRO") or "").strip().upper() != "ATIVO":
            stats["rejeitadas_inativo"] += 1
            continue

        reg_id = (row.get("NUMERO_REGISTRO_PRODUTO") or "").strip()
        if not reg_id:
            stats["rejeitadas_sem_id"] += 1
            continue
        if reg_id in seen_ids:
            stats["duplicadas"] += 1
            continue
        seen_ids.add(reg_id)

        items.append({
            "id": reg_id,
            "nomeComercial": (row.get("NOME_PRODUTO") or "").strip(),
            "principiosAtivos": split_principios_ativos(row.get("PRINCIPIO_ATIVO") or ""),
            "classeTerapeutica": (row.get("CLASSE_TERAPEUTICA") or "").strip(),
            "categoriaRegulatoria": map_categoria(row.get("CATEGORIA_REGULATORIA") or ""),
            "empresaDetentora": (row.get("EMPRESA_DETENTORA_REGISTRO") or "").strip(),
            "situacaoAtiva": True,
        })
        stats["validas"] += 1

    return items, stats


def write_packs(items: list[dict], out_dir: Path, batch_size: int) -> int:
    out_dir.mkdir(parents=True, exist_ok=True)
    for existing in out_dir.glob("anvisa_batch_*.json"):
        existing.unlink()

    source = f"ANVISA Dados Abertos - Medicamentos ({ANVISA_URL}), gerado em {date.today().isoformat()}"
    total_batches = 0
    for start in range(0, len(items), batch_size):
        batch = items[start:start + batch_size]
        total_batches += 1
        out_path = out_dir / f"anvisa_batch_{total_batches:03d}.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump({"source": source, "items": batch}, f, ensure_ascii=False, indent=None)
    return total_batches


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--csv", type=Path, default=None, help="CSV já baixado (senão baixa de novo)")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="Diretório de saída dos packs")
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
    args = parser.parse_args()

    csv_path = args.csv
    if csv_path is None:
        csv_path = Path(__file__).resolve().parent / "_anvisa_medicamentos.csv"
        download_csv(csv_path)
    elif not csv_path.exists():
        print(f"Arquivo não encontrado: {csv_path}", file=sys.stderr)
        sys.exit(1)

    items, stats = build_items(csv_path)
    total_batches = write_packs(items, args.out, args.batch_size)

    print("--- Ingestão ANVISA concluída ---")
    for key, value in stats.items():
        print(f"  {key}: {value}")
    print(f"  packs gerados: {total_batches} em {args.out}")

    if stats["validas"] == 0:
        print("ERRO: nenhum item válido gerado.", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
