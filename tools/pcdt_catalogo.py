#!/usr/bin/env python3
"""
Catálogo dos PCDT do Ministério da Saúde — levantado do índice A-Z de
https://www.gov.br/saude/pt-br/assuntos/pcdt

Gera o arquivo de fontes consumido por ingest_guidelines.py.
As letras Q, W, X, Y e Z não existem no índice; J e K estão vazias.

Uso:  python tools/pcdt_catalogo.py fontes_pcdt.json
"""
import json
import re
import sys
import unicodedata
from pathlib import Path

BASE = "https://www.gov.br/saude/pt-br/assuntos/pcdt/"

# (letra, título, caminho-na-página)
CATALOGO = [
    ("a", "Acidente Vascular Cerebral Isquêmico Agudo", "acidente-vascular-cerebral-isquemico-agudo/view"),
    ("a", "Acidentes Escorpiônicos", "acidentes-escorpionicos/view"),
    ("a", "Acidentes Ofídicos", "acidentes-ofidicos"),
    ("a", "Acromegalia", "acromegalia.pdf/view"),
    ("a", "Adenocarcinoma de Cólon e Reto", "adenocarcinoma-de-colon-e-de-reto/view"),
    ("a", "Adenocarcinoma de Estômago", "adenocarcinoma-de-estomago.pdf/view"),
    ("a", "Adenocarcinoma de Próstata", "adenocarcinoma-prostata.pdf/view"),
    ("a", "Amiloidoses Associadas à Transtirretina", "amiloidoses-associadas-a-transtirretina.pdf/view"),
    ("a", "Anemia por Deficiência de Ferro", "anemia-deficiencia-de-ferro/view"),
    ("a", "Anemia Hemolítica Autoimune", "anemia-hemolitica-autoimune.pdf/view"),
    ("a", "Aneurisma de Aorta Abdominal", "aneurisma-aorta-abdominal/view"),
    ("a", "Angioedema por Deficiência de C1 Esterase", "angioedema-deficiencia-c1-esterase/view"),
    ("a", "Artrite Idiopática Juvenil", "artrite-idiopatica-juvenil/view"),
    ("a", "Artrite Psoriásica", "artrite-psoriasica/view"),
    ("a", "Artrite Reativa", "artrite-reativa/view"),
    ("a", "Artrite Reumatoide", "artrite-reumatoide/view"),
    ("a", "Asma", "asma-portaria-conjunta-saes-sctie-no-43/view"),
    ("a", "Assistência ao Parto Normal", "assistencia-ao-parto-normal-diretriz-nacional/view"),
    ("a", "Atenção à Gestante e Operação Cesariana", "atencao-a-gestante-a-operacao-cesariana-diretriz/view"),
    ("a", "Doenças Raras — Linha de Cuidado", "atencao-integral-as-pessoas-com-doencas-raras-linha-de-cuidado/view"),
    ("a", "Infecções Sexualmente Transmissíveis", "atencao-integral-as-pessoas-com-infeccoes-sexualmente-transmissiveis/view"),
    ("a", "Atrofia Muscular Espinhal 5q", "atrofia-muscular-espinhal-5q-tipos-1-e-2/view"),
    ("b", "Blinatumomabe", "blinatumomabe/view"),
    ("b", "Brucelose Humana", "pcdt-brucelose-humana.pdf/view"),
    ("c", "Câncer de Cabeça e Pescoço", "cancer-de-cabeca-e-pescoco/view"),
    ("c", "Câncer de Mama", "cancer-de-mama/view"),
    ("c", "Câncer de Pulmão", "cancer-de-pulmao/view"),
    ("c", "Carcinoma de Células Renais", "carcinoma-de-celulas-renais/view"),
    ("c", "Carcinoma de Esôfago", "carcinoma-de-esofago/view"),
    ("c", "Carcinoma Diferenciado da Tireoide", "carcinoma-diferenciado-da-tireoide/view"),
    ("c", "Carcinoma Hepatocelular no Adulto", "carcinoma-hepatocelular-no-adulto/view"),
    ("c", "Colangite Biliar Primária", "colangite-biliar-primaria/view"),
    ("c", "Comportamento Agressivo no Autismo", "comportamento-agressivo-no-transtorno-do-espectro-do-autismo/view"),
    ("d", "Deficiência de Biotinidase", "deficiencia-de-biotinidase/view"),
    ("d", "Deficiência do Hormônio de Crescimento", "deficiencia-do-hormonio-de-crescimento-hipopituitarismo/view"),
    ("d", "Deficiência Intelectual — Diagnóstico Etiológico", "deficiencia-intelectual-protocolo-para-o-diagnostico-etiologico/view"),
    ("d", "Degeneração Macular Relacionada à Idade", "degeneracao-macular-relacionada-com-a-idade-portaria-conjunta-no-24/view"),
    ("d", "Dermatite Atópica", "dermatite-atopica/view"),
    ("d", "Diabete Melito Tipo 1", "diabete-melito-tipo-1/view"),
    ("d", "Diabete Melito Tipo 2", "diabete-melito-tipo-2.pdf/view"),
    ("d", "Diabetes Insípido", "diabete-insipido/view"),
    ("d", "Intoxicações por Agrotóxicos — Capítulo 1", "diagnostico-e-tratamento-de-intoxicacoes-por-agrotoxicos-capitulo-1/view"),
    ("d", "Intoxicações por Agrotóxicos — Capítulo 2", "diagnostico-e-tratamento-de-intoxicacoes-por-agrotoxicos-capitulo-2/view"),
    ("d", "Intoxicações por Agrotóxicos — Capítulo 3", "diagnostico-e-tratamento-de-intoxicacoes-por-agrotoxicos-capitulo-3/view"),
    ("d", "Intoxicações por Agrotóxicos — Capítulo 4", "diagnostico-e-tratamento-de-intoxicacoes-por-agrotoxicos-capitulo-4/view"),
    ("d", "Intoxicações por Agrotóxicos — Capítulo 5", "diagnostico-e-tratamento-de-intoxicacoes-por-agrotoxicos-capitulo-5/view"),
    ("d", "Dislipidemia", "dislipidemia/view"),
    ("d", "Distonias e Espasmo Hemifacial", "distonias-e-espasmo-hemifacial/view"),
    ("d", "Distúrbio Mineral Ósseo na Doença Renal Crônica", "disturbio-mineral-osseo-na-doenca-renal-cronica/view"),
    ("d", "Doença Celíaca", "doenca-celiaca/view"),
    ("d", "Doença de Alzheimer", "doenca-de-alzheimer/view"),
    ("d", "Doença de Chagas", "doenca-de-chagas/view"),
    ("d", "Doença de Crohn", "doenca-de-crohn/view"),
    ("d", "Doença de Fabry", "pcdt-doenca-de-fabry.pdf/view"),
    ("d", "Doença de Gaucher", "doenca-de-gaucher/view"),
    ("d", "Doença de Niemann-Pick Tipo C", "doenca-de-niemann-pick-tipo-c-diretriz-brasileira/view"),
    ("d", "Doença de Paget", "doenca-de-paget/view"),
    ("d", "Doença de Parkinson", "doenca-de-parkinson/view"),
    ("d", "Doença de Pompe", "doenca-de-pompe.pdf/view"),
    ("d", "Doença de Wilson", "doenca-de-wilson/view"),
    ("d", "Doença Falciforme", "doenca-falciforme/view"),
    ("d", "Doença Pulmonar Obstrutiva Crônica", "doenca-pulmonar-obstrutiva-cronica/view"),
    ("e", "Endometriose", "endometriose/view"),
    ("e", "Epidermólise Bolhosa", "epidermolise-bolhosa-diretriz-brasileira/view"),
    ("e", "Epilepsia", "epilepsia/view"),
    ("e", "Esclerose Lateral Amiotrófica", "esclerose-lateral-amiotrofica/view"),
    ("e", "Esclerose Múltipla", "esclerose-multipla/view"),
    ("e", "Esclerose Sistêmica", "esclerose-sistemica/view"),
    ("e", "Espasticidade", "espasticidade/view"),
    ("e", "Espondilite Ancilosante", "espondilite-ancilosante/view"),
    ("e", "Esquizofrenia", "esquizofrenia/view"),
    ("e", "Progressão da Doença Renal Crônica", "estrategias-para-atenuar-a-progressao-da-doenca-renal-cronica/view"),
    ("f", "Fenilcetonúria", "fenilcetonuria/view"),
    ("f", "Fibrose Cística", "fibrose-cistica/view"),
    ("f", "Fratura do Colo do Fêmur em Idosos — Linha de Cuidado", "fratura-do-colo-do-femur-em-idosos-linha-de-cuidado/view"),
    ("f", "Fratura do Colo do Fêmur em Idosos — Tratamento", "fratura-do-colo-do-femur-em-idosos-tratamento-diretrizes-brasileiras/view"),
    ("g", "Glaucoma", "glaucoma/view"),
    ("h", "Hanseníase", "hanseniase/view"),
    ("h", "Hemangioma Infantil", "hemangioma-infantil/view"),
    ("h", "Hemofilia A — Emicizumabe", "hemofilia-a-2013-uso-do-emicizumabe-protocolo-de-uso/view"),
    ("h", "Hemoglobinúria Paroxística Noturna", "hemoglobinuria-paroxistica-noturna/view"),
    ("h", "Hepatite Autoimune", "hepatite-autoimune/view"),
    ("h", "Hepatite B e Coinfecções", "hepatite-b-e-coinfeccoes/view"),
    ("h", "Hepatite C e Coinfecções", "hepatite-c-e-coinfeccoes/view"),
    ("h", "Hidradenite Supurativa", "hidradenite-supurativa/view"),
    ("h", "Hidroxocobalamina na Intoxicação por Cianeto", "hidroxocobalamina-na-intoxicacao-aguda-por-cianeto-protocolo-de-uso/view"),
    ("h", "Hiperplasia Adrenal Congênita", "hiperplasia-adrenal-congenita/view"),
    ("h", "Hiperprolactinemia", "hiperprolactinemia/view"),
    ("h", "Hipertensão Arterial Sistêmica", "hipertensao-arterial-sistemica.pdf/view"),
    ("h", "Hipertensão Pulmonar", "hipertensao-pulmonar/view"),
    ("h", "Hipoparatireoidismo", "hipoparatireoidismo/view"),
    ("h", "Hipotireoidismo Congênito", "hipotireoidismo-congenito/view"),
    ("h", "Homocistinúria Clássica", "homocistinuria-classica/view"),
    ("i", "Ictioses Hereditárias", "ictioses-hereditarias.pdf/view"),
    ("i", "Imunodeficiência Primária — Imunoglobulina Humana", "imunodeficiencia-primaria-com-predominancia-de-defeitos-de-anticorpos-imunoglobulina-humana/view"),
    ("i", "Imunossupressão no Transplante Hepático Pediátrico", "imunossupressao-no-transplante-hepatico-em-pediatria/view"),
    ("i", "Imunossupressão no Transplante Cardíaco", "imunossupressao-no-transplante-cardiaco/view"),
    ("i", "Imunossupressão no Transplante Renal", "imunossupressao-no-transplante-renal/view"),
    ("i", "Imunossupressão no Transplante Hepático em Adultos", "imunosupressao-no-transplante-hepatico-em-adultos/view"),
    ("i", "Incontinência Urinária não Neurogênica", "incontinencia-urinaria-nao-neurogenica/view"),
    ("i", "Imunotolerância em Hemofilia A com Inibidor", "inducao-de-imunotolerancia-para-individuos-com-hemofilia-a-e-inibidor/view"),
    ("i", "Insuficiência Adrenal", "insuficiencia-adrenal/view"),
    ("i", "Insuficiência Cardíaca com Fração de Ejeção Reduzida", "insuficiencia-cardiaca-com-fracao-de-ejecao-reduzida/view"),
    ("i", "Insuficiência Pancreática Exócrina", "insuficiencia-pancreatica-exocrina/view"),
    ("i", "Isotretinoína na Acne Grave", "isotretinoina-no-tratamento-da-acne-grave/view"),
    ("l", "Leiomioma de Útero", "leiomioma-de-utero/view"),
    ("l", "Leucemia Linfoblástica Aguda Ph+ do Adulto", "leucemia-linfoblastica-aguda-ph-de-adulto-com-mesilato-de-imatinibe-ddt/view"),
    ("l", "Leucemia Linfoblástica Aguda Ph+ Pediátrica", "leucemia-linfoblastica-aguda-cromossoma-philadelphia-positivo-de-criancas-e-adolescentes-ddt.pdf/view"),
    ("l", "Leucemia Mieloide Aguda Pediátrica", "leucemia-mieloide-aguda-de-criancas-e-adolescentes-diretrizes-diagnosticas-e-terapeuticas/view"),
    ("l", "Leucemia Mieloide Aguda do Adulto", "leucemia-mieloide-aguda-do-adulto-diretrizes-diagnosticas-e-terapeuticas/view"),
    ("l", "Leucemia Mieloide Crônica Pediátrica", "leucemia-mieloide-cronica-de-crianca-e-adolescente/view"),
    ("l", "Leucemia Mieloide Crônica do Adulto", "leucemia-mieloide-cronica-do-adulto/view"),
    ("l", "Linfangioleiomiomatose", "linfangioleiomiomatose/view"),
    ("l", "Linfoma de Hodgkin no Adulto", "linfoma-de-hodgkin-no-adulto/view"),
    ("l", "Linfoma Difuso de Grandes Células B", "linfoma-difuso-de-grandes-celulas-b/view"),
    ("l", "Linfoma Folicular", "linfoma-folicular/view"),
    ("l", "Lipofuscinose Ceroide Neuronal Tipo 2", "lipofuscinose-ceroide-neuronal-tipo-2/view"),
    ("l", "Lúpus Eritematoso Sistêmico", "lupus-eritematoso-sistemico/view"),
    ("m", "HIV em Crianças e Adolescentes — Módulo 1", "manejo-da-infeccao-pelo-hiv-em-criancas-e-adolescentes-modulo-1/view"),
    ("m", "HIV em Adultos — Módulo 1", "manejo-da-infeccao-pelo-hiv-em-adultos-modulo-1/view"),
    ("m", "HIV em Crianças e Adolescentes — Módulo 2", "manejo-da-infeccao-pelo-hiv-em-criancas-e-adolescentes-modulo-2/view"),
    ("m", "HIV em Adultos — Módulo 2", "manejo-da-infeccao-pelo-hiv-em-adultos-modulo-2/view"),
    ("m", "Marca-passos e Ressincronizadores", "marca-passos-cardiacos-implantaveis-e-ressincronizadores-protocolo-de-uso/view"),
    ("m", "Melanoma Cutâneo", "melanoma-cutaneo-ddt/view"),
    ("m", "Mesotelioma Pleural", "mesotelioma-pleural/view"),
    ("m", "Miastenia Gravis", "miastenia-gravis/view"),
    ("m", "Mieloma Múltiplo", "mieloma-multiplo-ddt/view"),
    ("m", "Miopatias Inflamatórias", "miopatias-inflamatorias.pdf/view"),
    ("m", "Mucopolissacaridose Tipo I", "mucopolissacaridose-do-tipo-i/view"),
    ("m", "Mucopolissacaridose Tipo II", "mucopolissacaridose-do-tipo-ii/view"),
    ("m", "Mucopolissacaridose Tipo VII", "mucopolissacaridose-do-tipo-vii/view"),
    ("m", "Mucopolissacaridose Tipo IV A", "mucopolissacaridose-tipo-iv-a/view"),
    ("m", "Mucopolissacaridose Tipo VI", "mucopolissacaridose-tipo-vi/view"),
    ("n", "Neoplasia Maligna Epitelial de Ovário", "neoplasia-maligna-epitelial-de-ovario-ddt/view"),
    ("o", "Osteogênese Imperfeita", "osteogenese-imperfeita/view"),
    ("o", "Osteoporose", "osteoporose/view"),
    ("p", "Palivizumabe — Prevenção do VSR", "palivizumabe-para-a-prevencao-da-infeccao-pelo-virus-sincicial-respiratorio-protocolo-de-uso/view"),
    ("p", "Deficiência Auditiva — Linha de Cuidado", "pessoas-com-deficiencia-auditiva-linha-de-cuidado/view"),
    ("p", "Porfirias", "porfirias/view"),
    ("p", "Transmissão Vertical de HIV, Sífilis e Hepatites", "prevencao-da-transmissao-vertical-do-hiv-sifilis-e-hepatites-virais/view"),
    ("p", "Profilaxia Pós-Exposição (PEP) ao HIV", "profilaxia-pos-exposicao-de-risco-pep-a-infeccao-pelo-hiv/view"),
    ("p", "Profilaxia Pré-Exposição (PrEP) ao HIV", "profilaxia-pre-exposicao-prep-oral-a-infeccao-pelo-hiv/view"),
    ("p", "Profilaxia Primária em Hemofilia Grave", "profilaxia-primaria-em-caso-de-hemofilia-grave-protocolo-de-uso/view"),
    ("p", "Psoríase", "psoriase/view"),
    ("p", "Puberdade Precoce Central", "puberdade-precoce-central/view"),
    ("r", "Raquitismo e Osteomalácia", "raquitismo-e-osteomalacia/view"),
    ("r", "Rastreamento do Câncer do Colo do Útero", "rastreamento-cancer-do-colo-do-utero/view"),
    ("r", "Retinopatia Diabética", "retinopatia-diabetica/view"),
    ("r", "Retocolite Ulcerativa", "retocolite-ulcerativa/view"),
    ("s", "Síndrome de Falência Medular", "sindrome-de-falencia-medular/view"),
    ("s", "Síndrome de Guillain-Barré", "sindrome-de-guillain-barre/view"),
    ("s", "Síndrome dos Ovários Policísticos", "sindrome-de-ovarios-policisticos/view"),
    ("s", "Síndrome de Turner", "sindrome-de-turner/view"),
    ("s", "Síndrome Mielodisplásica de Baixo Risco", "sindrome-mielodisplasica-de-baixo-risco/view"),
    ("s", "Síndrome Nefrótica Primária em Adultos", "sindrome-nefrotica-primaria-em-adultos/view"),
    ("s", "Síndrome Nefrótica Primária em Crianças", "sindrome-nefrotica-primaria-em-criancas-e-adolescentes/view"),
    ("s", "Sobrecarga de Ferro", "sobrecarga-de-ferro/view"),
    ("s", "Sobrepeso e Obesidade em Adultos", "sobrepeso-e-obesidade-em-adultos/view"),
    ("t", "Tabagismo", "tabagismo/view"),
    ("t", "Talidomida — DECH e Mieloma Múltiplo", "talidomida-no-tratamento-da-doenca-enxerto-contra-hospedeiro-e-do-mieloma-multiplo"),
    ("t", "Transtorno Afetivo Bipolar Tipo I", "transtorno-afetivo-bipolar-do-tipo-i/view"),
    ("t", "Transtorno do Déficit de Atenção com Hiperatividade", "transtorno-do-deficit-de-atencao-com-hiperatividade-tdah/view"),
    ("t", "Transtorno Esquizoafetivo", "transtorno-esquizoafetivo/view"),
    ("t", "Trombocitopenia Imune Primária", "trombocitopenia-imune-primaria/view"),
    ("t", "Tromboembolismo Venoso em Gestantes com Trombofilia", "tromboembolismo-venoso-em-gestantes-com-trombofilia/view"),
    ("t", "Tumor Cerebral no Adulto", "tumor-cerebral-no-adulto-ddt/view"),
    ("t", "Tumor do Estroma Gastrointestinal", "tumor-do-estroma-gastrointestinal/view"),
    ("u", "Endoprótese em Aorta Torácica Descendente", "utilizacao-de-endoprotese-em-aorta-toracica-descendente-diretrizes-brasileiras/view"),
    ("u", "Stents na Doença Coronariana Estável", "utilizacao-de-stents-em-pacientes-com-doenca-coronariana-estavel-diretrizes-brasileiras/view"),
    ("u", "Uveítes não Infecciosas", "uveites-nao-infecciosas/view"),
    ("v", "Vasculite Associada a ANCA", "vasculite.pdf/view"),
]


def slug(titulo: str) -> str:
    """
    Identificador estável a partir do título.

    NÃO truncar. A versão anterior cortava em 38 caracteres e transformava
    "..._agrotoxicos_capitulo_1" e "..._capitulo_5" no mesmo id — os cinco
    capítulos colapsavam num só na fila de curadoria, e a médica revisaria um
    item acreditando ter visto todos. Id longo não custa nada; id colidido
    apaga conteúdo em silêncio.
    """
    s = unicodedata.normalize("NFD", titulo).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "_", s).strip("_")


def main() -> None:
    dest = Path(sys.argv[1] if len(sys.argv) > 1 else "fontes_pcdt.json")
    fontes = [
        {
            "url_pagina": BASE + letra + "/" + caminho,
            "titulo": "PCDT " + titulo,
            "categoria": "CLINICA_MEDICA",
            "fonte": "Ministério da Saúde — Protocolos Clínicos e Diretrizes Terapêuticas (PCDT)",
            "prefixo_id": "pcdt_" + slug(titulo),
            "tags": ["pcdt", "ministério da saúde", titulo.lower()],
        }
        for letra, titulo, caminho in CATALOGO
    ]
    dest.write_text(json.dumps(fontes, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(fontes)} fontes gravadas em {dest}")


if __name__ == "__main__":
    main()
