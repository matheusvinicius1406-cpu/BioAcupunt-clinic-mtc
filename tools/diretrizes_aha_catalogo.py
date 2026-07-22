#!/usr/bin/env python3
"""
CATÁLOGO DE DIRETRIZES AHA/ACC — American Heart Association / American
College of Cardiology.

Gera o arquivo de fontes consumido por ingest_guidelines.py.
As diretrizes AHA/ACC são publicadas em Circulation (AHA) e JACC (ACC),
disponíveis em PDF nos respectivos sites.

Uso: python tools/diretrizes_aha_catalogo.py > fontes_aha.json
"""
import json
import re
import unicodedata
from pathlib import Path

# URLs base para as diretrizes AHA/ACC
BASE_AHA = "https://www.ahajournals.org/"
BASE_ACC = "https://www.jacc.org/"

# Diretrizes AHA/ACC organizadas por ano
DIRETRIZES = [
    # ── 2024 ────────────────────────────────────────────────────
    ("aha", "doi/10.1161/CIR.0000000000001204", "Atrial Fibrillation", "2024"),
    ("aha", "doi/10.1161/CIR.0000000000001198", "Chronic Coronary Disease", "2024"),
    ("aha", "doi/10.1161/CIR.0000000000001184", "Valvular Heart Disease", "2024"),
    ("aha", "doi/10.1161/CIR.0000000000001195", "Myocarditis and Pericarditis", "2024"),
    ("aha", "doi/10.1161/CIR.0000000000001188", "Hypertension", "2024"),

    # ── 2023 ────────────────────────────────────────────────────
    ("aha", "doi/10.1161/CIR.0000000000001177", "Acute Coronary Syndromes (STEMI)", "2023"),
    ("aha", "doi/10.1161/CIR.0000000000001148", "Cardiomyopathies", "2023"),
    ("aha", "doi/10.1161/CIR.0000000000001120", "Heart Failure", "2023"),
    ("aha", "doi/10.1161/CIR.0000000000001119", "Cardiovascular Disease in Diabetes", "2023"),
    ("aha", "doi/10.1161/CIR.0000000000001110", "Infective Endocarditis", "2023"),
    ("aha", "doi/10.1161/CIR.0000000000001128", "Pulmonary Hypertension", "2023"),

    # ── 2022 ────────────────────────────────────────────────────
    ("aha", "doi/10.1161/CIR.0000000000001063", "Peripheral Artery Disease", "2022"),
    ("aha", "doi/10.1161/CIR.0000000000001084", "Cardio-oncology", "2022"),
    ("aha", "doi/10.1161/CIR.0000000000001066", "Aortic Disease", "2022"),
    ("aha", "doi/10.1161/CIR.0000000000001073", "Primary Prevention of CVD", "2022"),

    # ── 2021 ────────────────────────────────────────────────────
    ("aha", "doi/10.1161/CIR.0000000000001031", "Coronary Artery Revascularization", "2021"),
    ("aha", "doi/10.1161/CIR.0000000000001020", "Chest Pain", "2021"),
    ("aha", "doi/10.1161/CIR.0000000000001007", "Dyslipidemia Management", "2021"),

    # ── 2020 ────────────────────────────────────────────────────
    ("aha", "doi/10.1161/CIR.0000000000000912", "Cardiac Arrest and CPR (ACLS)", "2020"),
    ("aha", "doi/10.1161/CIR.0000000000000918", "Atrial Fibrillation (2020 update)", "2020"),
    ("aha", "doi/10.1161/CIR.0000000000000930", "Valvular Heart Disease (2020 update)", "2020"),

    # ── 2019 ────────────────────────────────────────────────────
    ("aha", "doi/10.1161/CIR.0000000000001063", "Prevention of Stroke in AF", "2019"),
    ("aha", "doi/10.1161/CIR.0000000000000678", "Venous Thromboembolism", "2019"),
    ("aha", "doi/10.1161/CIR.0000000000000705", "Cardiovascular Disease in Pregnancy", "2019"),
    ("aha", "doi/10.1161/CIR.0000000000000746", "Sleep Apnea and CVD", "2019"),

    # ── 2018 ────────────────────────────────────────────────────
    ("aha", "doi/10.1161/CIR.0000000000000558", "Cholesterol Management", "2018"),
    ("aha", "doi/10.1161/CIR.0000000000000598", "Blood Pressure Management", "2018"),

    # ── Diretrizes ACC (JACC) ───────────────────────────────────
    ("acc", "doi/10.1016/j.jacc.2022.11.001", "Acute Aortic Syndrome", "2022"),
    ("acc", "doi/10.1016/j.jacc.2021.09.005", "Heart Valve Disease (ACC/AHA)", "2021"),
    ("acc", "doi/10.1016/j.jacc.2019.03.010", "Primary Cardiovascular Disease Prevention (ACC/AHA)", "2019"),
    ("acc", "doi/10.1016/j.jacc.2019.01.004", "Stable Ischemic Heart Disease (ACC/AHA)", "2019"),
    ("acc", "doi/10.1016/j.jacc.2018.10.023", "Lipid Management (ACC/AHA)", "2018"),
    ("acc", "doi/10.1016/j.jacc.2017.11.004", "Hypertension (ACC/AHA)", "2017"),
]

def slug(titulo: str) -> str:
    s = unicodedata.normalize("NFD", titulo).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "_", s).strip("_")

def main() -> None:
    dest = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("fontes_aha.json")
    fontes = []
    for src, doi_path, titulo, ano in DIRETRIZES:
        base = BASE_AHA if src == "aha" else BASE_ACC
        doi_short = doi_path.replace("doi/", "").replace("/", "-")
        short = slug(titulo)[:60]
        fonte = {
            "url": f"{base}{doi_path}",
            "url_pagina": f"{base}{doi_path}",
            "titulo": f"AHA/ACC {ano} — {titulo}",
            "categoria": "CLINICA_MEDICA",
            "fonte": f"American Heart Association / American College of Cardiology — Clinical Guideline {ano}",
            "prefixo_id": f"{'aha' if src == 'aha' else 'acc'}_{short}",
            "tags": [src, "cardiologia", "eua", ano],
        }
        fontes.append(fonte)
    dest.write_text(json.dumps(fontes, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(fontes)} fontes AHA/ACC gravadas em {dest}", file=sys.stderr)

if __name__ == "__main__":
    import sys
    main()
