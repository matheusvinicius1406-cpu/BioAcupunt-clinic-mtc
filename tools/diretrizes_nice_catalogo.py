#!/usr/bin/env python3
"""
CATÁLOGO DE DIRETRIZES NICE (Reino Unido) — National Institute for Health
and Care Excellence.

Gera o arquivo de fontes consumido por ingest_guidelines.py.
URLs seguem o padrão: https://www.nice.org.uk/guidance/ngXXX

Uso: python tools/diretrizes_nice_catalogo.py > fontes_nice.json
"""
import json
import re
import unicodedata
from pathlib import Path

BASE = "https://www.nice.org.uk/guidance/"

# Lista principal de diretrizes NICE (NG = NICE Guideline)
# Organizadas por especialidade clínica
DIRETRIZES = [
    # ── Câncer ────────────────────────────────────────────────
    ("NG12", "Suspected cancer: recognition and referral", "CANCER"),
    ("NG101", "Early and locally advanced breast cancer: diagnosis and management", "CANCER"),
    ("NG143", "Ovarian cancer: recognition and management", "CANCER"),
    ("NG122", "Lung cancer: diagnosis and management", "CANCER"),
    ("NG129", "Colorectal cancer", "CANCER"),
    ("NG131", "Prostate cancer: diagnosis and management", "CANCER"),
    ("NG151", "Pancreatic cancer in adults: diagnosis and management", "CANCER"),
    ("NG241", "Stomach cancer: recognition and management", "CANCER"),
    ("NG196", "Thyroid cancer: recognition and management", "CANCER"),
    ("NG47", "Acute leukaemia in adults", "CANCER"),
    ("NG52", "Non-Hodgkin lymphoma: diagnosis and management", "CANCER"),
    ("NG44", "Hodgkin lymphoma: diagnosis and management", "CANCER"),
    ("NG170", "Endometrial cancer: diagnosis and management", "CANCER"),
    ("NG230", "Penile cancer: recognition and management", "CANCER"),
    ("NG123", "Skin cancer: recognition and management", "CANCER"),
    ("NG146", "Hepatocellular carcinoma: diagnosis and management", "CANCER"),
    ("NG97", "Brain tumours (primary) and brain metastases", "CANCER"),
    ("NG85", "Oesophageal cancer: diagnosis and management", "CANCER"),
    ("NG219", "Renal cancer: diagnosis and management", "CANCER"),
    ("NG231", "Bladder cancer: diagnosis and management", "CANCER"),

    # ── Cardiologia ────────────────────────────────────────────
    ("NG185", "Cardiovascular disease: risk assessment and reduction", "CARDIOLOGIA"),
    ("CG181", "Cardiovascular disease: risk assessment and reduction (legacy)", "CARDIOLOGIA"),
    ("NG238", "Hypertension in adults: diagnosis and management", "CARDIOLOGIA"),
    ("NG136", "Hypertension in pregnancy: diagnosis and management", "CARDIOLOGIA"),
    ("NG106", "Heart failure: diagnosis and management", "CARDIOLOGIA"),
    ("NG185", "Lipid modification: cardiovascular risk assessment", "CARDIOLOGIA"),
    ("CG172", "Myocardial infarction: cardiac rehabilitation and prevention", "CARDIOLOGIA"),
    ("NG238", "Chronic coronary syndromes", "CARDIOLOGIA"),
    ("NG221", "Acute coronary syndromes", "CARDIOLOGIA"),
    ("NG194", "Atrial fibrillation: diagnosis and management", "CARDIOLOGIA"),
    ("NG243", "Peripheral arterial disease", "CARDIOLOGIA"),
    ("NG242", "Venous thromboembolism: diagnosis and management", "CARDIOLOGIA"),
    ("NG158", "Venous thromboembolism in adults: anticoagulation", "CARDIOLOGIA"),
    ("NG207", "Pulmonary hypertension", "CARDIOLOGIA"),
    ("NG249", "Aortic valve disease", "CARDIOLOGIA"),
    ("NG250", "Mitral valve disease", "CARDIOLOGIA"),
    ("NG248", "Infective endocarditis", "CARDIOLOGIA"),
    ("NG245", "Cardiomyopathies", "CARDIOLOGIA"),
    ("NG251", "Cardiovascular disease in diabetes", "CARDIOLOGIA"),
    ("NG252", "Cardio-oncology", "CARDIOLOGIA"),
    ("NG244", "Pregnancy and cardiovascular disease", "CARDIOLOGIA"),
    ("NG247", "Myocarditis and pericarditis", "CARDIOLOGIA"),

    # ── Diabetes e Endocrinologia ──────────────────────────────
    ("NG28", "Type 2 diabetes in adults: management", "ENDOCRINOLOGIA"),
    ("NG18", "Diabetes (type 1 and type 2) in children", "ENDOCRINOLOGIA"),
    ("NG17", "Type 1 diabetes in adults: diagnosis and management", "ENDOCRINOLOGIA"),
    ("NG32", "Diabetic foot problems: prevention and management", "ENDOCRINOLOGIA"),
    ("NG79", "Diabetic retinopathy: management and monitoring", "ENDOCRINOLOGIA"),
    ("NG203", "Thyroid disease: assessment and management", "ENDOCRINOLOGIA"),
    ("NG143", "Obesity: identification and management", "ENDOCRINOLOGIA"),
    ("NG246", "Osteoporosis: diagnosis and management", "ENDOCRINOLOGIA"),
    ("NG87", "Osteoporosis: assessing risk of fragility fracture", "ENDOCRINOLOGIA"),
    ("NG235", "Polycystic ovary syndrome: diagnosis and management", "ENDOCRINOLOGIA"),

    # ── Saúde Mental ───────────────────────────────────────────
    ("NG222", "Depression in adults: treatment and management", "SAUDE_MENTAL"),
    ("NG225", "Bipolar disorder: assessment and management", "SAUDE_MENTAL"),
    ("NG178", "Schizophrenia and psychosis: recognition and management", "SAUDE_MENTAL"),
    ("NG155", "Anxiety disorders: management", "SAUDE_MENTAL"),
    ("NG189", "Eating disorders: recognition and treatment", "SAUDE_MENTAL"),
    ("NG191", "Obsessive-compulsive disorder and body dysmorphic disorder", "SAUDE_MENTAL"),
    ("NG116", "Post-traumatic stress disorder", "SAUDE_MENTAL"),
    ("NG87", "Alcohol-use disorders: diagnosis and management", "SAUDE_MENTAL"),
    ("NG215", "Drug misuse in adults: opioid detoxification", "SAUDE_MENTAL"),
    ("NG204", "Self-harm: assessment, management and prevention", "SAUDE_MENTAL"),
    ("NG223", "Suicide prevention", "SAUDE_MENTAL"),
    ("NG206", "Attention deficit hyperactivity disorder: diagnosis and management", "SAUDE_MENTAL"),
    ("NG170", "Autism spectrum disorder in adults: diagnosis and management", "SAUDE_MENTAL"),
    ("NG128", "Autism spectrum disorder in children and young people", "SAUDE_MENTAL"),
    ("NG158", "Dementia: assessment, management and support", "SAUDE_MENTAL"),
    ("NG97", "Dementia: risk reduction", "SAUDE_MENTAL"),

    # ── Pneumologia ────────────────────────────────────────────
    ("NG80", "Asthma: diagnosis and monitoring", "PNEUMOLOGIA"),
    ("NG244", "Asthma: management", "PNEUMOLOGIA"),
    ("NG115", "Chronic obstructive pulmonary disease (COPD)", "PNEUMOLOGIA"),
    ("NG191", "Pulmonary fibrosis: diagnosis and management", "PNEUMOLOGIA"),
    ("NG249", "Community-acquired pneumonia", "PNEUMOLOGIA"),
    ("NG138", "Tuberculosis: diagnosis and management", "PNEUMOLOGIA"),
    ("NG194", "Sleep apnoea syndrome", "PNEUMOLOGIA"),
    ("NG221", "Bronchiectasis", "PNEUMOLOGIA"),
    ("NG202", "Pleural disease", "PNEUMOLOGIA"),

    # ── Infectologia ───────────────────────────────────────────
    ("NG169", "HIV testing: increasing uptake", "INFECTOLOGIA"),
    ("NG233", "HIV: antiretroviral therapy", "INFECTOLOGIA"),
    ("NG225", "Hepatitis B (chronic): diagnosis and management", "INFECTOLOGIA"),
    ("NG139", "Hepatitis C (chronic): diagnosis and management", "INFECTOLOGIA"),
    ("NG221", "Sepsis: recognition, diagnosis and early management", "INFECTOLOGIA"),
    ("NG168", "Antimicrobial stewardship", "INFECTOLOGIA"),
    ("NG234", "COVID-19: management in adults", "INFECTOLOGIA"),
    ("NG240", "Influenza: prevention and treatment", "INFECTOLOGIA"),
    ("NG230", "Sexually transmitted infections: diagnosis and management", "INFECTOLOGIA"),
    ("NG251", "Meningitis and meningococcal disease", "INFECTOLOGIA"),

    # ── Nefrologia ─────────────────────────────────────────────
    ("NG203", "Chronic kidney disease: assessment and management", "NEFROLOGIA"),
    ("NG107", "Acute kidney injury: prevention and management", "NEFROLOGIA"),
    ("NG148", "Renal replacement therapy in adults", "NEFROLOGIA"),
    ("NG199", "Renal transplantation", "NEFROLOGIA"),

    # ── Gastroenterologia ──────────────────────────────────────
    ("NG56", "Gastro-oesophageal reflux disease in adults", "GASTROENTEROLOGIA"),
    ("NG129", "Coeliac disease: recognition and management", "GASTROENTEROLOGIA"),
    ("NG151", "Inflammatory bowel disease (IBD)", "GASTROENTEROLOGIA"),
    ("NG241", "Irritable bowel syndrome in adults", "GASTROENTEROLOGIA"),
    ("NG50", "Cirrhosis in adults: diagnosis and management", "GASTROENTEROLOGIA"),
    ("NG185", "Pancreatitis", "GASTROENTEROLOGIA"),
    ("NG207", "Gallstone disease", "GASTROENTEROLOGIA"),

    # ── Reumatologia ───────────────────────────────────────────
    ("NG226", "Rheumatoid arthritis in adults: management", "REUMATOLOGIA"),
    ("NG240", "Systemic lupus erythematosus", "REUMATOLOGIA"),
    ("NG232", "Psoriatic arthritis", "REUMATOLOGIA"),
    ("NG231", "Osteoarthritis: diagnosis and management", "REUMATOLOGIA"),
    ("NG250", "Gout: diagnosis and management", "REUMATOLOGIA"),
    ("NG226", "Systemic sclerosis", "REUMATOLOGIA"),
    ("NG253", "Sjögren's syndrome", "REUMATOLOGIA"),
    ("NG246", "Vasculitis", "REUMATOLOGIA"),

    # ── Neurologia ────────────────────────────────────────────
    ("NG127", "Stroke and transient ischaemic attack", "NEUROLOGIA"),
    ("NG230", "Multiple sclerosis: management", "NEUROLOGIA"),
    ("NG252", "Parkinson's disease in adults", "NEUROLOGIA"),
    ("NG217", "Epilepsies in adults: diagnosis and management", "NEUROLOGIA"),
    ("NG183", "Epilepsies in children and young people", "NEUROLOGIA"),
    ("NG220", "Migraine: diagnosis and management", "NEUROLOGIA"),
    ("NG196", "Motor neurone disease", "NEUROLOGIA"),
    ("NG247", "Myasthenia gravis", "NEUROLOGIA"),
    ("NG203", "Spinal cord injury", "NEUROLOGIA"),
    ("NG251", "Headache disorders", "NEUROLOGIA"),

    # ── Ginecologia e Obstetrícia ──────────────────────────────
    ("NG201", "Antenatal care", "GO"),
    ("NG121", "Intrapartum care for healthy women and babies", "GO"),
    ("NG235", "Postnatal care", "GO"),
    ("NG240", "Hypertension in pregnancy: diagnosis and management", "GO"),
    ("NG133", "Diabetes in pregnancy: management", "GO"),
    ("NG69", "Menstrual disorders: assessment and management", "GO"),
    ("NG238", "Menopause: diagnosis and management", "GO"),
    ("NG240", "Endometriosis: diagnosis and management", "GO"),
    ("NG244", "Uterine fibroids: diagnosis and management", "GO"),
    ("NG250", "Contraception", "GO"),
    ("NG196", "Fertility problems: assessment and treatment", "GO"),

    # ── Pediatria ──────────────────────────────────────────────
    ("NG97", "Fever in under 5s: assessment and initial management", "PEDIATRIA"),
    ("NG209", "Diarrhoea and vomiting in children", "PEDIATRIA"),
    ("NG241", "Constipation in children and young people", "PEDIATRIA"),
    ("NG234", "Bronchiolitis in children: diagnosis and management", "PEDIATRIA"),
    ("NG28", "Faltering growth: recognition and management", "PEDIATRIA"),
    ("NG212", "Sepsis in children", "PEDIATRIA"),
    ("NG243", "Asthma in children", "PEDIATRIA"),

    # ── Emergência e Cuidados Intensivos ───────────────────────
    ("NG232", "Emergency and acute medical care", "EMERGENCIA"),
    ("NG220", "Major trauma: assessment and initial management", "EMERGENCIA"),
    ("NG221", "Major trauma: service delivery", "EMERGENCIA"),
    ("NG240", "Fractures: assessment and management", "EMERGENCIA"),
    ("NG219", "Head injury: assessment and early management", "EMERGENCIA"),
    ("NG235", "Burns: assessment and management", "EMERGENCIA"),
    ("NG236", "Spinal injury: assessment and initial management", "EMERGENCIA"),
    ("NG247", "Acute poisoning", "EMERGENCIA"),
    ("NG250", "Anaphylaxis: assessment and management", "EMERGENCIA"),

    # ── Saúde Pública ──────────────────────────────────────────
    ("NG28", "Physical activity: brief advice for adults", "SAUDE_PUBLICA"),
    ("NG44", "Physical activity for children and young people", "SAUDE_PUBLICA"),
    ("NG203", "Tobacco: preventing uptake and promoting quitting", "SAUDE_PUBLICA"),
    ("NG185", "Nutrition in adults: oral nutrition support", "SAUDE_PUBLICA"),
    ("NG102", "Immunisations: reducing inequalities", "SAUDE_PUBLICA"),
    ("NG237", "Vaccine uptake in children and young people", "SAUDE_PUBLICA"),
    ("NG211", "Mental wellbeing at work", "SAUDE_PUBLICA"),
    ("NG245", "Air pollution: outdoor air quality and health", "SAUDE_PUBLICA"),
]

# Legacy Clinical Guidelines (CG prefix)
CG_LEGACY = [
    ("CG174", "Intravenous fluid therapy in adults in hospital", "EMERGENCIA"),
    ("CG161", "Irritable bowel syndrome in adults", "GASTROENTEROLOGIA"),
    ("CG127", "Hypertension in adults: diagnosis and management", "CARDIOLOGIA"),
    ("CG126", "Falls in older people: assessment and prevention", "EMERGENCIA"),
    ("CG103", "Delirium: prevention, diagnosis and management", "NEUROLOGIA"),
    ("CG62", "Antenatal care (multiple pregnancy)", "GO"),
    ("CG45", "Fertility: assessment and treatment", "GO"),
    ("CG30", "Long-acting reversible contraception", "GO"),
    ("CG23", "Preterm labour and birth", "GO"),
]


def slug(titulo: str) -> str:
    s = unicodedata.normalize("NFD", titulo).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "_", s).strip("_")


def fmt_tag(code: str, cat: str) -> str:
    return f"{code.lower()}-{cat.lower()}"


def main() -> None:
    dest = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("fontes_nice.json")
    fontes = []

    for code, titulo, categoria in DIRETRIZES:
        fontes.append({
            "url": f"{BASE}{code}",
            "url_pagina": f"{BASE}{code}",
            "titulo": f"NICE {code} — {titulo}",
            "categoria": "CLINICA_MEDICA",
            "fonte": "National Institute for Health and Care Excellence (NICE)",
            "prefixo_id": "nice_" + code.lower(),
            "tags": ["nice", "uk", categoria.lower(), code.lower()],
        })

    for code, titulo, categoria in CG_LEGACY:
        fontes.append({
            "url": f"{BASE}{code}",
            "url_pagina": f"{BASE}{code}",
            "titulo": f"NICE CG{code} — {titulo} (legado)",
            "categoria": "CLINICA_MEDICA",
            "fonte": "National Institute for Health and Care Excellence (NICE) — Clinical Guideline (legado)",
            "prefixo_id": "nice_cg" + code.lower(),
            "tags": ["nice", "uk", "legado", categoria.lower(), code.lower()],
        })

    dest.write_text(json.dumps(fontes, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(fontes)} fontes NICE gravadas em {dest}", file=sys.stderr)


if __name__ == "__main__":
    import sys
    main()
