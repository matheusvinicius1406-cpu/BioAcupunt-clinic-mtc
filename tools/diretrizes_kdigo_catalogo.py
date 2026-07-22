#!/usr/bin/env python3
"""
CATÁLOGO DE DIRETRIZES KDIGO — Kidney Disease: Improving Global Outcomes.

Gera o arquivo de fontes consumido por ingest_guidelines.py.
KDIGO publica diretrizes completas em PDF com capítulos numerados.

Uso: python tools/diretrizes_kdigo_catalogo.py > fontes_kdigo.json
"""
import json
import re
import unicodedata
from pathlib import Path

BASE = "https://kdigo.org/guidelines/"

# Diretrizes KDIGO — cada uma com seu slug e ano de publicação
DIRETRIZES = [
    ("acute-kidney-injury", "Acute Kidney Injury (AKI) and Acute Kidney Disease (AKD)", "2024"),
    ("anemia-in-ckd", "Anemia in Chronic Kidney Disease", "2024"),
    ("anca-vasculitis", "Antineutrophilic Cytoplasmic Antibody (ANCA)-Associated Vasculitis", "2023"),
    ("adpkd", "Autosomal Dominant Polycystic Kidney Disease (ADPKD)", "2023"),
    ("blood-pressure-in-ckd", "Blood Pressure in Chronic Kidney Disease", "2021"),
    ("ckd-evaluation-and-management", "CKD Evaluation and Management", "2024"),
    ("ckd-mbd", "CKD-Mineral and Bone Disorder (CKD-MBD)", "2017"),
    ("diabetes-in-ckd", "Diabetes and Chronic Kidney Disease", "2022"),
    ("glomerular-diseases", "Glomerular Diseases", "2021"),
    ("heart-failure-in-ckd", "Heart Failure in Chronic Kidney Disease", "2024"),
    ("hepatitis-c-in-ckd", "Hepatitis C in Chronic Kidney Disease", "2022"),
    ("igan-igav", "IgA Nephropathy (IgAN) / IgA Vasculitis (IgAV)", "2024"),
    ("lipids-in-ckd", "Lipids in Chronic Kidney Disease", "2023"),
    ("living-kidney-donor", "Living Kidney Donor", "2023"),
    ("lupus-nephritis", "Lupus Nephritis", "2024"),
    ("nephrotic-syndrome-in-children", "Nephrotic Syndrome in Children", "2023"),
    ("transplant-candidate", "Transplant Candidate", "2020"),
    ("transplant-recipient", "Transplant Recipient", "2020"),
]

def slug(titulo: str) -> str:
    s = unicodedata.normalize("NFD", titulo).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "_", s).strip("_")

def main() -> None:
    dest = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("fontes_kdigo.json")
    fontes = []
    for slug_path, titulo, ano in DIRETRIZES:
        short = slug(titulo)[:60]
        fonte = {
            "url": f"{BASE}{slug_path}/",
            "url_pagina": f"{BASE}{slug_path}/",
            "titulo": f"KDIGO {ano} — {titulo}",
            "categoria": "CLINICA_MEDICA",
            "fonte": f"KDIGO (Kidney Disease: Improving Global Outcomes) — Clinical Practice Guideline {ano}",
            "prefixo_id": "kdigo_" + short,
            "tags": ["kdigo", "nefrologia", "drc", slug_path],
        }
        fontes.append(fonte)
    dest.write_text(json.dumps(fontes, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(fontes)} fontes KDIGO gravadas em {dest}", file=sys.stderr)

if __name__ == "__main__":
    import sys
    main()
