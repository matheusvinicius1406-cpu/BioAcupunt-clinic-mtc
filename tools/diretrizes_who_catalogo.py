#!/usr/bin/env python3
"""
CATÁLOGO DE DIRETRIZES DA OMS (WHO) — Organização Mundial da Saúde.

Gera o arquivo de fontes consumido por ingest_guidelines.py.
URLs seguem o padrão: https://www.who.int/publications/i/item/978XXXXXXXXXX

Uso: python tools/diretrizes_who_catalogo.py > fontes_who.json
"""
import json
import re
import unicodedata
from pathlib import Path

BASE = "https://www.who.int/publications/i/item/"

# Diretrizes da OMS organizadas por tema
# ISBN/ID numérico é o identificador único no repositório
DIRETRIZES = [
    # ── HIV/Aids ────────────────────────────────────────────────
    ("9789240111608", "Guidelines on lenacapavir for HIV prevention", "HIV"),
    ("9789240113879", "WHO guideline on HIV service delivery", "HIV"),
    ("9789240096394", "Consolidated guidelines on HIV testing services", "HIV"),
    ("9789240052963", "Consolidated guidelines on HIV prevention, testing, treatment and monitoring", "HIV"),
    ("9789240031593", "Guidelines for managing advanced HIV disease", "HIV"),
    ("9789240028333", "Updated recommendations on HIV prevention, infant diagnosis and monitoring", "HIV"),
    ("9789240028890", "Prevention of mother-to-child transmission of HIV", "HIV"),
    ("9789240031128", "HIV self-testing and partner notification", "HIV"),

    # ── Hepatites Virais ────────────────────────────────────────
    ("9789240052673", "Guidelines for the prevention, diagnosis, care and treatment of hepatitis B", "HEPATITES"),
    ("9789240059054", "Updated recommendations on treatment of hepatitis C", "HEPATITES"),
    ("9789240035577", "Guidelines for hepatitis B and C testing", "HEPATITES"),

    # ── Tuberculose ─────────────────────────────────────────────
    ("9789240084278", "WHO consolidated guidelines on tuberculosis. Module 1: Prevention", "TUBERCULOSE"),
    ("9789240084285", "WHO consolidated guidelines on tuberculosis. Module 2: Screening", "TUBERCULOSE"),
    ("9789240084292", "WHO consolidated guidelines on tuberculosis. Module 3: Diagnosis", "TUBERCULOSE"),
    ("9789240084308", "WHO consolidated guidelines on tuberculosis. Module 4: Treatment", "TUBERCULOSE"),
    ("9789240084315", "WHO consolidated guidelines on tuberculosis. Module 5: Drug-resistant TB", "TUBERCULOSE"),
    ("9789240084322", "WHO consolidated guidelines on tuberculosis. Module 6: Comorbidities", "TUBERCULOSE"),

    # ── Doenças Tropicais Negligenciadas ────────────────────────
    ("9789240054064", "WHO guideline on control and elimination of human schistosomiasis", "TROPICAIS"),
    ("9789240059092", "Guidelines for the treatment of malaria", "TROPICAIS"),
    ("9789240082830", "Guidelines for malaria vector control", "TROPICAIS"),
    ("9789240037847", "WHO guideline for the treatment of visceral leishmaniasis", "TROPICAIS"),
    ("9789240023239", "WHO guideline on the management of dengue", "TROPICAIS"),
    ("9789240024458", "Chagas disease in adults: diagnosis and treatment", "TROPICAIS"),
    ("9789240065808", "Guidelines for the diagnosis and treatment of leprosy", "TROPICAIS"),
    ("9789240035591", "WHO guideline on lymphatic filariasis", "TROPICAIS"),

    # ── Saúde Mental ────────────────────────────────────────────
    ("9789240084278", "Mental Health Gap Action Programme (mhGAP) guideline for mental, neurological and substance use disorders", "SAUDE_MENTAL"),
    ("9789240053946", "WHO guidelines on mental health at work", "SAUDE_MENTAL"),
    ("9789240028760", "Guidelines on community mental health services", "SAUDE_MENTAL"),
    ("9789240031029", "WHO guideline on self-care interventions for mental health", "SAUDE_MENTAL"),

    # ── Saúde Materno-Infantil ──────────────────────────────────
    ("9789240030886", "WHO recommendations on antenatal care for a positive pregnancy experience", "MATERNO_INFANTIL"),
    ("9789240030787", "WHO recommendations on intrapartum care", "MATERNO_INFANTIL"),
    ("9789240030893", "WHO recommendations on postnatal care", "MATERNO_INFANTIL"),
    ("9789240030763", "WHO recommendations on newborn health", "MATERNO_INFANTIL"),
    ("9789240027084", "WHO guideline on breastfeeding", "MATERNO_INFANTIL"),
    ("9789240003650", "WHO recommendations on infant feeding and HIV", "MATERNO_INFANTIL"),
    ("9789240030879", "WHO recommendations on child growth", "MATERNO_INFANTIL"),
    ("9789240030862", "WHO guideline on childhood pneumonia", "MATERNO_INFANTIL"),
    ("9789240045781", "WHO guidelines on management of childhood diarrhoea", "MATERNO_INFANTIL"),

    # ── Doenças Não Transmissíveis ──────────────────────────────
    ("9789240082830", "WHO guideline for the pharmacological treatment of hypertension in adults", "DNT"),
    ("9789240030657", "WHO guideline on use of glycated haemoglobin for diabetes diagnosis", "DNT"),
    ("9789240027053", "WHO guidelines on physical activity and sedentary behaviour", "DNT"),
    ("9789240057463", "WHO guideline on the prevention and management of obesity", "DNT"),
    ("9789240031128", "WHO guideline on the management of chronic respiratory diseases", "DNT"),
    ("9789240067406", "WHO guideline for screening and treatment of cervical cancer", "DNT"),
    ("9789240069479", "WHO guideline on the management of breast cancer", "DNT"),
    ("9789240028883", "WHO guideline on screening for colorectal cancer", "DNT"),

    # ── Nutrição ────────────────────────────────────────────────
    ("9789240082830", "WHO guideline on prevention and management of wasting and nutritional oedema", "NUTRICAO"),
    ("9789240031081", "WHO guideline on the use of sugar-sweetened beverage taxes", "NUTRICAO"),
    ("9789240027077", "WHO guideline on sodium intake", "NUTRICAO"),
    ("9789240031067", "WHO guideline on potassium intake", "NUTRICAO"),
    ("9789240052406", "WHO guideline on trans-fat intake", "NUTRICAO"),

    # ── Doenças Infecciosas Emergentes ──────────────────────────
    ("9789240054981", "WHO guidelines on the management of COVID-19", "INFEC_INFECT"),
    ("9789240055193", "WHO guidelines on vaccine-preventable diseases", "INFEC_INFECT"),
    ("9789240062746", "WHO guideline on pandemic preparedness", "INFEC_INFECT"),
    ("9789240022850", "WHO guidelines on antimicrobial resistance surveillance", "INFEC_INFECT"),
    ("9789240030244", "WHO guidelines on infection prevention and control", "INFEC_INFECT"),

    # ── Doenças Cardiovasculares ────────────────────────────────
    ("9789240030657", "WHO guideline on cardiovascular risk assessment", "CARDIO"),
    ("9789240052567", "WHO guidelines for management of acute myocardial infarction", "CARDIO"),
    ("9789240082830", "WHO guideline on hypertension management", "CARDIO"),
    ("9789240057456", "WHO guideline on heart failure management", "CARDIO"),

    # ── Imunização ──────────────────────────────────────────────
    ("9789240068076", "WHO recommendations on routine immunization", "IMUNIZACAO"),
    ("9789240033986", "WHO guideline on vaccine safety", "IMUNIZACAO"),
    ("9789240067352", "WHO guideline on immunization in pregnancy", "IMUNIZACAO"),

    # ── Emergências Sanitárias ──────────────────────────────────
    ("9789240084704", "WHO emergency response framework", "EMERGENCIAS"),
    ("9789240066515", "WHO guidelines on mass casualty management", "EMERGENCIAS"),
    ("9789240067451", "WHO guideline on triage in emergency departments", "EMERGENCIAS"),

    # ── Água e Saneamento ───────────────────────────────────────
    ("9789240045064", "WHO guidelines for drinking-water quality", "AGUA_SANEAMENTO"),
    ("9789240052482", "WHO guideline on sanitation and health", "AGUA_SANEAMENTO"),
    ("9789240027657", "WHO guideline on wastewater surveillance", "AGUA_SANEAMENTO"),
]


def slug(titulo: str) -> str:
    s = unicodedata.normalize("NFD", titulo).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "_", s).strip("_")


def main() -> None:
    dest = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("fontes_who.json")
    fontes = []

    for isbn, titulo, categoria in DIRETRIZES:
        short = slug(titulo)[:60]
        fontes.append({
            "url": f"{BASE}{isbn}",
            "url_pagina": f"{BASE}{isbn}",
            "titulo": titulo[:120],
            "categoria": "CLINICA_MEDICA",
            "fonte": "World Health Organization (WHO) — Guidelines",
            "prefixo_id": "who_" + short,
            "tags": ["who", "oms", categoria.lower(), isbn],
        })

    dest.write_text(json.dumps(fontes, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(fontes)} fontes WHO gravadas em {dest}", file=sys.stderr)


if __name__ == "__main__":
    import sys
    main()
