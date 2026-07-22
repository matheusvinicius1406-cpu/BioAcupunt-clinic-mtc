#!/usr/bin/env python3
"""
CATÁLOGO DE DIRETRIZES ESC — European Society of Cardiology.

Gera o arquivo de fontes consumido por ingest_guidelines.py.
As diretrizes ESC são publicadas no European Heart Journal e
disponíveis em PDF no portal da ESC.

Uso: python tools/diretrizes_esc_catalogo.py > fontes_esc.json
"""
import json
import re
import unicodedata
from pathlib import Path

BASE = "https://www.escardio.org/guidelines/clinical-practice-guidelines/"

# Diretrizes ESC — slug no portal da ESC
DIRETRIZES = [
    ("cardiovascular-diseases-during-pregnancy-management-of", "Cardiovascular Disease and Pregnancy", "2025"),
    ("myocarditis-and-pericarditis", "Myocarditis and Pericarditis", "2025"),
    ("valvular-heart-disease", "Valvular Heart Disease", "2025"),
    ("atrial-fibrillation", "Atrial Fibrillation", "2024"),
    ("chronic-coronary-syndromes", "Chronic Coronary Syndromes", "2024"),
    ("hypertension", "Elevated Blood Pressure and Hypertension", "2024"),
    ("peripheral-arterial-and-aortic-diseases", "Peripheral Arterial and Aortic Diseases", "2024"),
    ("acute-coronary-syndromes", "Acute Coronary Syndromes", "2023"),
    ("cardiomyopathies", "Cardiomyopathies", "2023"),
    ("cardiovascular-disease-in-patients-with-diabetes", "Cardiovascular Disease in Diabetes", "2023"),
    ("endocarditis", "Infective Endocarditis", "2023"),
    ("cardio-oncology", "Cardio-oncology", "2022"),
    ("pulmonary-hypertension", "Pulmonary Hypertension", "2022"),
    ("acute-and-chronic-heart-failure", "Acute and Chronic Heart Failure", "2021"),
    ("cardiovascular-disease-prevention", "Cardiovascular Disease Prevention", "2021"),
    ("supraventricular-tachycardia", "Supraventricular Tachycardia", "2019"),
    ("dyslipidaemias", "Dyslipidaemias", "2019"),
    ("pulmonary-embolism", "Pulmonary Embolism", "2019"),
    ("syncope", "Syncope", "2018"),
    ("myocardial-revascularization", "Myocardial Revascularization", "2018"),
]

def slug(titulo: str) -> str:
    s = unicodedata.normalize("NFD", titulo).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "_", s).strip("_")

def main() -> None:
    dest = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("fontes_esc.json")
    fontes = []
    for slug_path, titulo, ano in DIRETRIZES:
        short = slug(titulo)[:60]
        fonte = {
            "url": f"{BASE}all-esc-practice-guidelines/{slug_path}/",
            "url_pagina": f"{BASE}all-esc-practice-guidelines/{slug_path}/",
            "titulo": f"ESC {ano} — {titulo}",
            "categoria": "CLINICA_MEDICA",
            "fonte": f"European Society of Cardiology — Clinical Practice Guidelines {ano}",
            "prefixo_id": "esc_" + short,
            "tags": ["esc", "cardiologia", "europa", ano, slug_path],
        }
        fontes.append(fonte)
    dest.write_text(json.dumps(fontes, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(fontes)} fontes ESC gravadas em {dest}", file=sys.stderr)

if __name__ == "__main__":
    import sys
    main()
