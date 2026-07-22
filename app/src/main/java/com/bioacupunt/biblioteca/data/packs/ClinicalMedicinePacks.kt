package com.bioacupunt.biblioteca.data.packs

import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentItem
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack

/**
 * PACOTES DE CLÍNICA MÉDICA — conteúdo fornecido pela médica para consulta rápida.
 *
 * Organizado por especialidade. Cada item é um tópico clínico objetivo, com
 * citação de diretrizes e referências médicas consolidadas. Nada gerado por IA.
 *
 * Categoria usada: [CLINICA_MEDICA].
 * Para diferenciar especialidades, use as tags (ex.: "cardiologia", "pneumologia").
 */
object ClinicalMedicinePacks {

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 1: CARDIOLOGIA — 35 tópicos
    // ═══════════════════════════════════════════════════════════════

    private val cardiologiaPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Diretrizes ACC/AHA, ESC, SBCC — Medicina Baseada em Evidências",
        items = listOf(
            LibraryContentItem(
                id = "cardio_001", title = "Critério de Killip para insuficiência cardíaca no IAM",
                category = "CLINICA_MEDICA", summary = "Classes I a IV para estratificação de risco na IC por IAM.",
                content = "Classificação de Killip para gravidade da insuficiência cardíaca no IAM: Classe I — sem sinais de IC (mortalidade ~6%). Classe II — B3 ou estertores até metade dos campos pulmonares (~17%). Classe III — edema agudo de pulmão franco (~38%). Classe IV — choque cardiogênico (~67%). Útil para prognóstico imediato e decisão terapêutica no IAM com supradesnivelamento de ST.",
                tags = listOf("cardiologia", "killip", "iam", "ic", "classificação"), citation = "Diretriz ACC/AHA 2023 - IAM com supradesnível ST"
            ),
            LibraryContentItem(
                id = "cardio_002", title = "Escore CHA₂DS₂-VASc para AVC em fibrilação atrial",
                category = "CLINICA_MEDICA", summary = "Cálculo do risco de AVC em FA para decidir anticoagulação.",
                content = "CHA₂DS₂-VASc: C (IC 1pt), H (hipertensão 1pt), A₂ (idade ≥75 = 2pts), D (diabetes 1pt), S₂ (AVC/AIT/TE prévio = 2pts), V (doença vascular 1pt), A (65-74 anos 1pt), Sc (sexo feminino 1pt). Escore ≥2 em homens ou ≥3 em mulheres → anticoagulação oral indicada. Escore 1 em homens ou 2 em mulheres → considerar anticoagulação (benefício geralmente supera risco).",
                tags = listOf("cardiologia", "cha2ds2-vasc", "avc", "fa", "anticoagulação"), citation = "Diretriz ESC 2020 - Fibrilação Atrial"
            ),
            LibraryContentItem(
                id = "cardio_003", title = "Escore HAS-BLED para risco de sangramento em anticoagulados",
                category = "CLINICA_MEDICA", summary = "Avaliação do risco hemorrágico antes de anticoagular.",
                content = "HAS-BLED: H (hipertensão 1pt), A (função renal/hepática alterada 1pt cada = 2pts), S (AVC prévio 1pt), B (sangramento prévio 1pt), L (INR lábil 1pt), E (idoso >65 1pt), D (drogas/álcool 1pt cada = 2pts). Máximo 9 pontos. Escore ≥3 indica alto risco de sangramento — não contraindica anticoagulação mas sinaliza necessidade de acompanhamento rigoroso e controle de fatores modificáveis.",
                tags = listOf("cardiologia", "has-bled", "sangramento", "anticoagulação"), citation = "Diretriz ESC 2020 - Fibrilação Atrial"
            ),
            LibraryContentItem(
                id = "cardio_004", title = "Fármaco de escolha para crise hipertensiva",
                category = "CLINICA_MEDICA", summary = "Nitroprussiato de sódio é o padrão ouro na emergência hipertensiva.",
                content = "Crise hipertensiva com emergência (lesão de órgão-alvo): nitroprussiato de sódio (Nipride) 0,25-10 mcg/kg/min IV, titulado a cada 1-2 min. Ação imediata, duração curta (1-2 min). Alternativas: labetalol (20-80 mg IV a cada 10 min), nicardipina (5-15 mg/h IV), esmolol (50-200 mcg/kg/min). Meta: redução da PAM em 25% na primeira hora, depois PA 160/100 em 2-6h. Evitar queda brusca (risco de isquemia cerebral coronariana).",
                tags = listOf("cardiologia", "crise hipertensiva", "emergência", "nitroprussiato"), citation = "Diretriz SBH 2020 - Emergência Hipertensiva"
            ),
            LibraryContentItem(
                id = "cardio_005", title = "Contraindicação absoluta do beta-bloqueador no IAM",
                category = "CLINICA_MEDICA", summary = "BAV de 2º grau é contraindicação absoluta ao BB no IAM.",
                content = "Contraindicações absolutas ao beta-bloqueador no IAM agudo: BAV de 2º grau (Mobitz II) ou 3º grau, FC <50 bpm, PAS <90 mmHg, IC descompensada com estertores até bases, broncoespasmo ativo. Nestes casos, aguardar estabilização antes de iniciar BB. Contraindicações relativas: asma leve, DPOC, doença vascular periférica grave (preferir beta-1 seletivos como metoprolol ou bisoprolol).",
                tags = listOf("cardiologia", "beta-bloqueador", "iam", "bav", "contraindicação"), citation = "Diretriz ACC/AHA 2023 - IAM"
            ),
            LibraryContentItem(
                id = "cardio_006", title = "Dose de ataque e manutenção da amiodarona na FA",
                category = "CLINICA_MEDICA", summary = "Amiodarona: ataque 5-7 mg/kg IV em 1h, manutenção 200 mg/dia.",
                content = "Amiodarona na FA: Dose de ataque: 5-7 mg/kg IV em 1 hora (tipicamente 150-300 mg), seguido de 50 mg/h nas próximas 24h. OU via oral: 600-800 mg/dia divididos por 7-10 dias. Dose de manutenção: 200 mg/dia (mínimo eficaz). Devido à meia-vida longa (26-107 dias), o efeito pleno leva semanas. Monitorizar: função tireoidiana (TSH a cada 6 meses), função hepática, Rx tórax (fibrose), ECG (QTc).",
                tags = listOf("cardiologia", "amiodarona", "fa", "dose", "eletrólitos"), citation = "Diretriz ESC 2020 - Fibrilação Atrial"
            ),
            LibraryContentItem(
                id = "cardio_007", title = "Interpretação do intervalo QT corrigido (Fórmula de Bazett)",
                category = "CLINICA_MEDICA", summary = "QTc = QT / √RR. Normal <440 ms (homens) e <460 ms (mulheres).",
                content = "Fórmula de Bazett: QTc = QT / √(intervalo RR). QTc normal: <440 ms em homens, <460 ms em mulheres. >500 ms = risco alto de Torsades de Pointes. Causas de QT longo: drogas (amiodarona, levofloxacino, haloperidol, ondansetrona), eletrólitos (hipocalemia, hipomagnesemia, hipocalcemia), bradiarritmias, MI, síndromes congênitas (LQTS). Correção de Fridericia: QTc = QT / ∛RR (mais precisa em extremos de FC).",
                tags = listOf("cardiologia", "qt", "bazett", "torsades", "eletrocardiograma"), citation = "Diretriz ACC/AHA 2023 - ECG"
            ),
            LibraryContentItem(
                id = "cardio_008", title = "Principais drogas que prolongam o intervalo QT",
                category = "CLINICA_MEDICA", summary = "Lista de fármacos que prolongam QT: antiarrítmicos, ATBs, antipsicóticos.",
                content = "Drogas que prolongam QT (lista não exaustiva): Antiarrítmicos (amiodarona, sotalol, procainamida, quinidina, flecainida), Antibióticos (macrolídeos - azitromicina, claritromicina; fluoroquinolonas - levofloxacino, moxifloxacino), Antifúngicos (fluconazol, cetoconazol), Antieméticos (ondansetrona, domperidona), Antipsicóticos (haloperidol, risperidona, quetiapina), Antidepressivos (citalopram >40mg, amitriptilina), Antimaláricos (hidroxicloroquina, cloroquina). Sempre verificar interações e eletrólitos.",
                tags = listOf("cardiologia", "qt", "drogas", "torsades", "interação"), citation = "CredibleMeds - Lista de drogas que prolongam QT"
            ),
            LibraryContentItem(
                id = "cardio_009", title = "Diagnóstico diferencial entre pericardite e IAM",
                category = "CLINICA_MEDICA", summary = "Pericardite: dor alivia com inclinação. IAM: dor de forte intensidade.",
                content = "Diagnóstico diferencial: Pericardite — dor pleurítica, melhora ao inclinar para frente, piora ao deitar, pode ter irradiação para trapézio. ECG: supradesnível difuso com infra de PR. IAM — dor constritiva, sem posição de alívio, irradiação para braços/queixo, sudorese, náusea. ECG: supradesnível localizado, ondas Q. Marcadores: troponina elevada em ambos (menos na pericardite). RX: silhueta cardíaca normal na pericardite (derrame só se grande).",
                tags = listOf("cardiologia", "pericardite", "iam", "diagnóstico diferencial"), citation = "Diretriz ESC 2022 - Doenças Pericárdicas"
            ),
            LibraryContentItem(
                id = "cardio_010", title = "Critérios de Duke para endocardite infecciosa",
                category = "CLINICA_MEDICA", summary = "Critérios maiores e menores para diagnóstico de endocardite.",
                content = "Critérios de Duke (modificados): Maiores (1) Hemocultura positiva para germe típico (Streptococcus viridans, S. aureus, Enterococcus) em 2 amostras separadas; (2) Ecocardiograma com vegetação, abscesso ou deiscência de prótese. Menores (1) febre ≥38°C; (2) predisposição (cardiopatia, droga IV); (3) fenômenos vasculares (êmbolos, hemorragia conjuntival); (4) fenômenos imunológicos (fator reumatoide, glomerulonefrite, nódulos de Osler); (5) hemocultura positiva não preenchendo critério maior. Definitivo: 2 maiores, ou 1 maior + 3 menores, ou 5 menores.",
                tags = listOf("cardiologia", "endocardite", "duke", "diagnóstico"), citation = "Diretriz ESC 2023 - Endocardite Infecciosa"
            ),
            LibraryContentItem(
                id = "cardio_011", title = "Valor normal da pressão venosa central (PVC)",
                category = "CLINICA_MEDICA", summary = "PVC normal: 5-10 cmH₂O. Reflete a pré-carga do VD.",
                content = "Pressão venosa central (PVC) normal: 5-10 cmH₂O (3-8 mmHg). Medida no terço médio do átrio direito, com o paciente em decúbito dorsal zero. PVC alta (>12): hipervolemia, IC direita, TEP, tamponamento cardíaco, hipertensão pulmonar. PVC baixa (<3): hipovolemia, choque hemorrágico, sepse. Na prática, a tendência serial é mais útil que o valor absoluto. Sempre correlacionar com PA, diurese e ecocardiograma.",
                tags = listOf("cardiologia", "pvc", "pressão venosa", "pré-carga"), citation = "ACLS 2020 - Monitorização Hemodinâmica"
            ),
            LibraryContentItem(
                id = "cardio_012", title = "Fármaco de escolha para IC com fração de ejeção reduzida",
                category = "CLINICA_MEDICA", summary = "IECA + Beta-bloqueador = base do tratamento da IC-FEr.",
                content = "IC com fração de ejeção reduzida (IC-FEr ≤40%): Quatro pilares da terapia: 1) IECA (captopril, enalapril, lisinopril) ou BRA (valsartana) — reduzem mortalidade 20%. 2) Beta-bloqueador (bisoprolol, carvedilol, succinato de metoprolol) — reduzem mortalidade 34%. 3) Antagonista MR (espironolactona ou eplerenona) — reduzem mortalidade 30% em NYHA II-IV. 4) Inibidor SGLT2 (empagliflozina, dapagliflozina) — reduzem mortalidade 25% (independente de diabetes). Todos devem ser iniciados e titulados ao máximo tolerado.",
                tags = listOf("cardiologia", "ic", "ieca", "beta-bloqueador", "sglt2"), citation = "Diretriz ACC/AHA 2022 - Insuficiência Cardíaca"
            ),
            LibraryContentItem(
                id = "cardio_013", title = "Dosagem máxima de enalapril em insuficiência renal",
                category = "CLINICA_MEDICA", summary = "Ajuste conforme clearance de creatinina. Máximo 40 mg/dia.",
                content = "Enalapril: dose inicial 2,5-5 mg 1-2x/dia, titular até 10-20 mg 2x/dia (máx 40 mg/dia). Na IR: Clearance 30-60: iniciar com 2,5 mg/dia, máximo 20 mg/dia. Clearance <30: iniciar com 2,5 mg/dia, máximo 10 mg/dia. Clearance <10: contraindicado ou usar com extrema cautela. Monitorizar Cr e K+ em 1-2 semanas após cada ajuste. Aumento da Cr até 30% é aceitável e não indica suspensão — reflete hemodinâmica glomerular.",
                tags = listOf("cardiologia", "enalapril", "ieca", "renal", "dose"), citation = "Bulas aprovadas ANVISA + KDIGO 2023"
            ),
            LibraryContentItem(
                id = "cardio_014", title = "Quando indicar o uso de digoxina na IC",
                category = "CLINICA_MEDICA", summary = "Digoxina na IC: FA com resposta ventricular rápida refratária.",
                content = "Indicação de digoxina na IC: FA com resposta ventricular rápida (FC >110 bpm) não controlada por BB isolado, ou quando BB é contraindicado. Na IC-FEr em ritmo sinusal, pode ser usada como adjuvante quando NYHA II-IV persistir apesar da terapia otimizada (IECA+BB+MR+SGLT2) — reduz hospitalizações mas não reduz mortalidade. Dose: 0,125-0,25 mg/dia. Nível sérico alvo: 0,5-0,9 ng/mL. Risco de intoxicação: Cr elevada, hipocalemia, interações (amiodarona, verapamil).",
                tags = listOf("cardiologia", "digoxina", "ic", "fa"), citation = "Diretriz ACC/AHA 2022 - Insuficiência Cardíaca"
            ),
            LibraryContentItem(
                id = "cardio_015", title = "Efeitos colaterais da amiodarona",
                category = "CLINICA_MEDICA", summary = "Fibrose pulmonar, depósitos corneanos, disfunção tireoidiana.",
                content = "Efeitos colaterais da amiodarona (muito comuns em uso crônico): Tireoidianos (até 30%): hipotireoidismo (mais comum) ou hipertireoidismo. TSH a cada 6 meses. Pulmonar (1-5%): fibrose pulmonar, pneumonite — irreversível se não diagnosticada precocemente. Rx tórax + PFTs anuais. Oculares: depósitos corneanos (90% dos pacientes) — geralmente assintomáticos. Hepáticos: elevação de transaminases (15-30%), raramente cirrose. Dermatológicos: fotossensibilidade (azul-acinzentado). Neurológicos: tremor, neuropatia. Cardíacos: bradicardia, prolongamento QT.",
                tags = listOf("cardiologia", "amiodarona", "efeitos colaterais", "tireoide", "fibrose pulmonar"), citation = "Diretriz ESC 2020 - FA"
            ),
            LibraryContentItem(
                id = "cardio_016", title = "Contraindicação do verapamil em IAM com IC",
                category = "CLINICA_MEDICA", summary = "Verapamil é contraindicado no IAM por seu efeito inotrópico negativo.",
                content = "Verapamil (bloqueador de canal de cálcio não-diidropiridínico) é contraindicado no IAM agudo com insuficiência cardíaca (FE reduzida) ou BAV. Efeito inotrópico negativo pode piorar a IC e aumentar mortalidade. Também contraindicado na IC-FEr independente de IAM. Contraindicações adicionais: BAV de 2º ou 3º grau, FA com via acessória (pré-excitação), hipotensão. Alternativas seguras na IC: BB (metoprolol, carvedilol), amiodarona.",
                tags = listOf("cardiologia", "verapamil", "iam", "contraindicação"), citation = "Diretriz ACC/AHA 2023 - IAM"
            ),
            LibraryContentItem(
                id = "cardio_017", title = "Escala de Wells para tromboembolismo pulmonar",
                category = "CLINICA_MEDICA", summary = "Escore clínico para probabilidade pré-teste de TEP.",
                content = "Escala de Wells para TEP: Critérios: AVC/TVP/TEP prévio (1,5pt), FC >100 (1,5pt), cirurgia/imobilização (1,5pt), hemoptise (1pt), câncer ativo (1pt), sinais clínicos de TVP (3pt), diagnóstico alternativo menos provável que TEP (3pt). Probabilidade: >6 pts = alta; 2-6 pts = moderada; <2 pts = baixa. Wells simplificada: >4 = provável, ≤4 = improvável. D-dímero é útil quando Wells é baixo/improvável. Wells alta → angioTC direto.",
                tags = listOf("cardiologia", "tep", "wells", "tromboembolismo"), citation = "Diretriz ESC 2019 - TEP"
            ),
            LibraryContentItem(
                id = "cardio_018", title = "Critérios de PERC para afastar TEP sem exames",
                category = "CLINICA_MEDICA", summary = "8 critérios que, se todos negativos, afastam TEP sem testar.",
                content = "Critérios PERC (Pulmonary Embolism Rule-out Criteria): idade >50 (1pt), FC >100 (1pt), satO2 <95% (1pt), TVP/TEP prévio (1pt), cirurgia com anestesia geral nos últimos 4 semanas (1pt), hemoptise (1pt), uso de estrogênio (1pt), sinais unilaterais de TVP (1pt). PERC negativo (0 pts) + probabilidade clínica baixa → TEP afastado clinicamente (VPP 98%). PERC ≥1 → solicitar D-dímero ou angioTC.",
                tags = listOf("cardiologia", "perc", "tep", "exclusão"), citation = "Diretriz ESC 2019 - TEP"
            ),
            LibraryContentItem(
                id = "cardio_019", title = "Tratamento da embolia pulmonar maciça",
                category = "CLINICA_MEDICA", summary = "TEP maciço com instabilidade: trombólise é o tratamento de escolha.",
                content = "TEP maciço (instabilidade hemodinâmica: PAS <90 mmHg por >15 min ou necessidade de aminas): Trombólise sistêmica imediata — alteplase (rt-PA) 100 mg IV em 2h, ou 50 mg/h. Reduz mortalidade de 50% para 15%. Contraindicações absolutas: AVC hemorrágico prévio, cirurgia intracraniana recente, sangramento ativo, trauma craniano recente. Após trombólise, iniciar heparina quando TTPa <2x normal. Alternativa: embolectomia cirúrgica ou cateter-directed quando trombólise contraindicada.",
                tags = listOf("cardiologia", "tep", "maciço", "trombólise", "alteplase"), citation = "Diretriz ESC 2019 - TEP"
            ),
            LibraryContentItem(
                id = "cardio_020", title = "Cálculo da pressão arterial média (PAM)",
                category = "CLINICA_MEDICA", summary = "PAM = [(2xDiastólica) + Sistólica]/3. Meta na sepse: ≥65 mmHg.",
                content = "Pressão arterial média (PAM) = [(2 × PAS) + PAS] / 3 = pressão diastólica + 1/3 da pressão de pulso. Exemplo: PA 120/80 → PAM = (2×80 + 120)/3 = 93 mmHg. Importância clínica: PAM ≥65 mmHg é a meta na sepse (Surviving Sepsis Campaign). PAM <60 → risco de hipoperfusão renal e cerebral. A PAM é o principal determinante da perfusão tecidual. Dispositivos: medida invasiva (artéria) é o padrão ouro na UTI.",
                tags = listOf("cardiologia", "pam", "pressão arterial", "sepse"), citation = "ACLS 2020 - Monitorização Hemodinâmica"
            ),
            LibraryContentItem(
                id = "cardio_021", title = "Classificação de hipertensão arterial pela OMS",
                category = "CLINICA_MEDICA", summary = "Estágios 1 (140-159), 2 (160-179) e 3 (≥180) mmHg.",
                content = "Classificação OMS/ISC 2023: PA Ótima: <120/<80; Normal: 120-129/80-84; Pré-hipertensão: 130-139/85-89; HAS Estágio 1: 140-159/90-99; HAS Estágio 2: 160-179/100-109; HAS Estágio 3: ≥180/≥110. A classificação orienta o tratamento: Estágio 1 sem DM/DRC → mudança de estilo de vida por 3-6 meses. Estágio 1 com DM/DRC ou Estágio 2/3 → farmacoterapia imediata. Meta PA: <140/90 (geral), <130/80 (DM/DRC/idosos), <120/80 (alto risco CV).",
                tags = listOf("cardiologia", "has", "classificação", "oms", "estágios"), citation = "Diretriz OMS/ISC 2023 - Hipertensão Arterial"
            ),
            LibraryContentItem(
                id = "cardio_022", title = "Principais causas de hipertensão secundária",
                category = "CLINICA_MEDICA", summary = "Feocromocitoma, coarctação da aorta, doença renal, estenose de artéria renal.",
                content = "Causas de HAS secundária: Renais: doença renal parenquimatosa (glomerulonefrite, nefropatia diabética), estenose de artéria renal (fibrodisplasia em jovens, aterosclerose em idosos). Endócrinas: feocromocitoma, hiperaldosteronismo primário (Con), síndrome de Cushing, hipertireoidismo. Cardiovasculares: coarctação da aorta. Drogas: AINEs, corticoides, descongestionantes nasais, anticoncepcionais orais, eritropoetina. Apneia do sono. Suspeitar se: HAS resistente, início <30 ou >55 anos sem fatores de risco, PA >180/110, hipocalemia espontânea.",
                tags = listOf("cardiologia", "has secundária", "feocromocitoma", "estenose renal"), citation = "Diretriz SBH 2020 - HAS Secundária"
            ),
            LibraryContentItem(
                id = "cardio_023", title = "Fármaco de escolha para emergência hipertensiva na gestação",
                category = "CLINICA_MEDICA", summary = "Hidralazina IV ou labetalol IV são as drogas de escolha na gestante.",
                content = "Emergência hipertensiva na gestação (PA ≥160/110 com pré-eclâmpsia/eclâmpsia): Hidralazina 5 mg IV ou labetalol 20 mg IV são primeira linha. Sulfato de magnésio 4-6 g IV para prevenção de convulsões na eclâmpsia. Contraindicados: IECA, BRA (risco fetal), nitroprussiato (risco de toxicidade por cianeto fetal). Nifedipina oral também é opção (10 mg a cada 30 min). Parto é o tratamento definitivo. Manter PA ≤160/105 durante o trabalho de parto.",
                tags = listOf("cardiologia", "gestação", "emergência hipertensiva", "pré-eclâmpsia"), citation = "Diretriz FIGO 2023 - Pré-eclâmpsia"
            ),
            LibraryContentItem(
                id = "cardio_024", title = "Dosagem de estreptoquinase no IAM",
                category = "CLINICA_MEDICA", summary = "1,5 milhão UI IV em 1 hora na ausência de angioplastia primária.",
                content = "Estreptoquinase (SK) no IAM com supradesnível de ST quando angioplastia primária não disponível em ≤120 min: 1,5 milhão de UI diluído em 100 mL de SF, IV em 30-60 min. Efeitos colaterais: hipotensão (infundir mais lentamente), reações alérgicas (rash, anafilaxia), hemorragia. Contraindicações: AVC recente (<6 meses), cirurgia ou trauma recente (<3 semanas), sangramento ativo, diátese hemorrágica, alergia à SK (não repetir entre 5 dias e 2 anos após uso prévio). Hoje a alteplase (tPA) é preferida (mais cara, menos efeitos adversos).",
                tags = listOf("cardiologia", "estreptoquinase", "iam", "trombólise"), citation = "Diretriz ACC/AHA 2023 - IAM com supradesnível ST"
            ),
            LibraryContentItem(
                id = "cardio_025", title = "Quando suspender o AAS em paciente com AVC hemorrágico",
                category = "CLINICA_MEDICA", summary = "Suspender AAS permanentemente após AVC hemorrágico. Reintroduzir se benefício CV superar risco.",
                content = "Suspensão do AAS no AVC hemorrágico: AAS deve ser suspenso imediatamente na admissão por hemorragia intracerebral. Se o paciente usava AAS para prevenção primária (sem doença CV manifesta), suspender permanentemente. Se para prevenção secundária (IAM prévio, stent coronariano, AVC isquêmico prévio), avaliar risco-benefício: reintroduzir geralmente após 4-8 semanas se a hemorragia foi controlada e não há alto risco de ressangramento. Na dúvida, neurologia deve decidir. O risco de ressangramento com AAS é maior que o benefício na maioria dos casos.",
                tags = listOf("cardiologia", "avc hemorrágico", "aas", "suspensão", "hemorragia"), citation = "Diretriz AHA/ASA 2022 - AVC Hemorrágico"
            ),
            LibraryContentItem(
                id = "cardio_026", title = "Escala de Hunt-Hess para hemorragia subaracnoidea",
                category = "CLINICA_MEDICA", summary = "Graus I a V para estratificação de gravidade da HSA por aneurisma.",
                content = "Escala de Hunt-Hess para HSA por aneurisma: Grau I — assintomático ou cefaleia leve. Grau II — cefaleia moderada/grave, rigidez de nuca, sem déficit focal. Grau III — sonolência, confusão, déficit focal leve. Grau IV — estupor, hemiparesia moderada/grave. Grau V — coma, postura de decorticação/descerebração. Utilidade: orienta prognóstico e decisão cirúrgica. Graus I-II: cirurgia precoce (clip ou embolização) geralmente indicada. Graus III-IV: estabilizar antes da cirurgia. Grau V: prognóstico muito reservado. WFNS scale é alternativa.",
                tags = listOf("cardiologia", "hunt-hess", "hsa", "aneurisma", "neurologia"), citation = "Diretriz AHA/ASA 2023 - HSA"
            ),
            LibraryContentItem(
                id = "cardio_027", title = "Tratamento da dissecção de aorta tipo A",
                category = "CLINICA_MEDICA", summary = "Dissecção tipo A (ascendente) = cirurgia emergencial. Tipo B = tratamento clínico.",
                content = "Dissecção de aorta: Tipo A (Stanford A = DeBakey I e II) — envolve aorta ascendente. Tratamento: cirúrgico emergencial (substituição da aorta ascendente + possível reparo da válvula aórtica). Mortalidade sem cirurgia: 1-2% por hora nas primeiras 48h. Cirurgia reduz mortalidade para 10-30%. Tipo B (Stanford B) — não envolve aorta ascendente. Tratamento clínico: betabloqueador (esmolol ou labetalol) para FC 60 bpm e PAS 100-120 mmHg, depois vasodilatador (nitroprussiato). Cirurgia endovascular (TEVAR) se complicações (ruptura, má perfusão).",
                tags = listOf("cardiologia", "dissecção aorta", "tipo a", "emergência"), citation = "Diretriz ACC/AHA 2022 - Doença Aórtica"
            ),
            LibraryContentItem(
                id = "cardio_028", title = "Manobra de Valsalva e massagem do seio carotídeo",
                category = "CLINICA_MEDICA", summary = "Manobras vagais para terminar TVP ou FA paroxística.",
                content = "Manobras vagais para terminar taquicardia paroxística supraventricular (TPSV): Manobra de Valsalva — paciente expira contra glote fechada (ou sopra o êmbolo de uma seringa de 10 mL) por 15 segundos na posição semi-sentada, depois deita-se passivamente (modificação de 'Valsalva deitado semiativa'). Massagem do seio carotídeo — paciente deitado, pescoço estendido. Massagear o seio carotídeo (nível da cartilagem tireoide, na bifurcação da carótida) por 5-10 segundos de cada vez, unilateral. Auscultar carótidas antes (excluir sopro/estenose). Contraindicada se AVC/AIT recente ou estenose carotídea conhecida.",
                tags = listOf("cardiologia", "valsalva", "massagem carotídea", "tpsv", "vagal"), citation = "Diretriz ACC/AHA 2023 - Taquicardias Supraventriculares"
            ),
            LibraryContentItem(
                id = "cardio_029", title = "Classificação de Fontaine e Rutherford para isquemia crônica de membros",
                category = "CLINICA_MEDICA", summary = "Estadiamento da doença arterial periférica para guiar tratamento.",
                content = "Classificação de Fontaine: Estágio I — assintomático. IIa — claudicação >200m. IIb — claudicação <200m. III — dor isquêmica em repouso. IV — úlcera/gangrena. Classificação de Rutherford: 0 (assintomático), 1 (claudicação leve), 2 (moderada), 3 (grave), 4 (dor repouso), 5 (perda tecidual menor), 6 (perda maior). Ambas orientam o tratamento: Fontaine I-II = tratamento clínico + exercício. Fontaine III-IV = revascularização (cirúrgica ou endovascular). Doppler de MMII com ITB <0,9 confirma diagnóstico.",
                tags = listOf("cardiologia", "fontaine", "rutherford", "dap", "claudicação"), citation = "Diretriz ESC/ESVS 2022 - Doença Arterial Periférica"
            ),
            LibraryContentItem(
                id = "cardio_030", title = "Fármaco de escolha para claudicação intermitente",
                category = "CLINICA_MEDICA", summary = "Cilostazol ou pentoxifilina para claudicação por DAP.",
                content = "Claudicação intermitente por DAP: Cilostazol 100 mg 2x/dia (padrão ouro) — melhora distância de claudicação em 40-60%. Efeitos colaterais: cefaleia, diarreia, palpitações. Contraindicado na IC (aumenta mortalidade). Pentoxifilina 400 mg 3x/dia — alternativa menos potente, melhora ~20%. Ambos devem ser associados a: exercício supervisionado (caminhada 30-60 min/dia ≥3x/semana), AAS 100 mg, estatinas (atorvastatina 40-80 mg), controle rigoroso de HAS e DM, cessação do tabagismo (a intervenção mais eficaz). Revascularização se falha clínica.",
                tags = listOf("cardiologia", "claudicação", "cilostazol", "pentoxifilina", "dap"), citation = "Diretriz ESC/ESVS 2022 - Doença Arterial Periférica"
            ),
            LibraryContentItem(
                id = "cardio_031", title = "Diagnóstico laboratorial da insuficiência cardíaca",
                category = "CLINICA_MEDICA", summary = "BNP >100 pg/mL ou NT-proBNP >300 pg/mL (agudo) / >125 (crônico).",
                content = "Diagnóstico de IC: BNP ou NT-proBNP são os marcadores de escolha. Na IC aguda: BNP >100 pg/mL ou NT-proBNP >300 pg/mL (sensibilidade 95%). Na IC crônica: NT-proBNP >125 pg/mL (exclui IC). Interpretação: valores elevados indicam estresse miocárdico (sobrecarga de volume/pressão) mas não são específicos — podem estar elevados na FA, IRC, idade avançada, TEP, sepse. Valores normais: IC improvável (VPN >95%). Útil para diagnóstico diferencial de dispneia no PS. BNP guia prognóstico e resposta ao tratamento.",
                tags = listOf("cardiologia", "bnp", "nt-probnp", "ic", "biomarcador"), citation = "Diretriz ACC/AHA 2022 - Insuficiência Cardíaca"
            ),
            LibraryContentItem(
                id = "cardio_032", title = "Dosagem de heparina não fracionada no TEP",
                category = "CLINICA_MEDICA", summary = "Bolus 60-80 UI/kg + infusão 12-18 UI/kg/h. Alvo TTPa 1,5-2,5x controle.",
                content = "Heparina não fracionada (HNF) no TEP: Bolus: 60-80 UI/kg IV (máx 5.000 UI). Infusão contínua: 12-18 UI/kg/h (máx 1.000 UI/h). Ajuste: monitorizar TTPa a cada 6h até atingir alvo (1,5-2,5x o controle do laboratório). Protocolo de ajuste: TTPa <35s → bolus 80 UI/kg + ↑4 UI/kg/h; 35-45s → bolus 40 UI/kg + ↑2 UI/kg/h; 46-70s (alvo) → manter; 71-90s → ↓2 UI/kg/h; >90s → parar por 1h depois ↓3 UI/kg/h. Alternativa HBPM (enoxaparina 1 mg/kg 12/12h SC) — mais prática, sem monitorização. Transição para anticoagulante oral após estabilização.",
                tags = listOf("cardiologia", "heparina", "tep", "tpta", "anticoagulação"), citation = "Diretriz ESC 2019 - TEP"
            ),
            LibraryContentItem(
                id = "cardio_033", title = "Monitorização do INR para varfarina",
                category = "CLINICA_MEDICA", summary = "Alvo INR 2-3 para maioria, 3-4 para próteses mecânicas.",
                content = "Varfarina: alvo de INR (Razão Normalizada Internacional). Alvo 2,0-3,0: FA, TEP, TVP, prótese aórtica biológica. Alvo 2,5-3,5 (ou 3,0-4,0): prótese mecânica mitral, prótese mecânica aórtica com fatores de risco (FA, FE baixa, dupla prótese). Monitorização: INR diário até estabilizar, depois semanal (2-4 semanas), depois mensal se estável (>70% do tempo na faixa terapêutica). Ajustes: se INR abaixo do alvo → ↑ dose 5-15%; se acima → ↓ dose 10-20% ou pular 1 dose. Interações principais: antibióticos (↑INR), AINEs (↑risco de sangramento), álcool (↑INR crônico, ↓INR agudo), alimentos ricos em vitamina K (↓INR).",
                tags = listOf("cardiologia", "varfarina", "inr", "anticoagulação", "monitorização"), citation = "Diretriz ACCP 2021 - Terapia Anticoagulante"
            ),
            LibraryContentItem(
                id = "cardio_034", title = "Interação da varfarina com antibióticos",
                category = "CLINICA_MEDICA", summary = "Metronidazol e trimetoprima aumentam INR (inibem metabolismo da varfarina).",
                content = "Interações da varfarina com ATBs: Aumentam INR (risco de sangramento): metronidazol (↑ INR 50-100%), trimetoprima/sulfametoxazol (↑ INR 30-50%), ciprofloxacino, levofloxacino, moxifloxacino, fluconazol (↑ INR significativamente). Mecanismo: inibição do CYP2C9 (metabolizador da varfarina). Diminuem INR (risco de trombose): rifampicina (↓↓INR — indução enzimática potente), griseofulvina. Conduta: ao prescrever qualquer ATB a paciente em uso de varfarina, monitorizar INR a cada 2-3 dias e ajustar dose. Anticoagulantes DOAC (apixabana, rivaroxabana) têm menos interações medicamentosas.",
                tags = listOf("cardiologia", "varfarina", "interação", "antibiótico", "inr"), citation = "Diretriz ACCP 2021 - Terapia Anticoagulante"
            ),
            LibraryContentItem(
                id = "cardio_035", title = "Conduta no choque cardiogênico refratário",
                category = "CLINICA_MEDICA", summary = "Balão intra-aórtico ou ECMO como ponte para revascularização ou recuperação.",
                content = "Choque cardiogênico refratário (PAS <90 apesar de aminas vasoativas): Balão intra-aórtico (BIA/Balloon Pump) — contrapulsação que ↑fluxo coronariano e ↓pós-carga. ECMO (Extracorporeal Membrane Oxygenation) veno-arterial — oxigenação de membrana extracorpórea, suporte circulatório completo. Outros dispositivos: Impella (bomba axial percutânea), TandemHeart (bypass esquerdo percutâneo). Meta: ponte para revascularização (angioplastia de emergência ou cirurgia de revascularização). Se irreversível, considerar ponte para transplante ou dispositivo de assistência ventricular de longa duração (LVAD). Mortalidade ainda ~40-50% mesmo com suporte.",
                tags = listOf("cardiologia", "choque cardiogênico", "ecmo", "bia", "impella"), citation = "Diretriz ACC/AHA 2022 - Choque Cardiogênico"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 2: PNEUMOLOGIA — 30 tópicos
    // ═══════════════════════════════════════════════════════════════

    private val pneumologiaPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Diretrizes ATS/IDSA, SBPT, GOLD, GINA — Medicina Baseada em Evidências",
        items = listOf(
            LibraryContentItem(
                id = "pneumo_001", title = "Critérios de CURB-65 para pneumonia comunitária",
                category = "CLINICA_MEDICA", summary = "5 critérios de gravidade: Confusão, Ureia, FR, PA, idade ≥65.",
                content = "CURB-65 para pneumonia adquirida na comunidade (PAC): C (confusão mental — 1pt), U (ureia >50 mg/dL — 1pt), R (frequência respiratória ≥30/min — 1pt), B (PAS <90 ou PAD ≤60 mmHg — 1pt), 65 (idade ≥65 anos — 1pt). Risco: 0-1: baixo (mortalidade <3%) — tratar em casa; 2: moderado (9%) — considerar internação; ≥3: alto (15-40%) — internação + UTI se 4-5. Alternativa: PSI/PORT Score (mais completo, 20 critérios). CURB-65 é mais rápido para decisão no PS.",
                tags = listOf("pneumologia", "curb-65", "pneumonia", "gravidade"), citation = "Diretriz IDSA/ATS 2019 - Pneumonia Comunitária"
            ),
            LibraryContentItem(
                id = "pneumo_002", title = "Escore PSI (Pneumonia Severity Index)",
                category = "CLINICA_MEDICA", summary = "20 critérios que estratificam risco de morte em PAC. Alternativa ao CURB-65.",
                content = "PSI/PORT Score (mais sensível que CURB-65): Calcula-se por idade + comorbidades (neoplasia, DRC, ICC, DM, hepatopatia, DRC) + achados físicos (FR≥30, FC≥125, PA<90, T<35 ou ≥40, confusão) + laboratoriais (pH<7,35, Na<130, glicose>250, Ht<30%, PaO2<60) + radiológicos (derrame). Classe I: <50pts (mortalidade 0,1%) — ambulatorial. II: ≤70pts (0,6%) — ambulatorial. III: 71-90pts (2,8%) — internação breve. IV: 91-130pts (8,2%) — internação. V: >130pts (29,2%) — UTI. Mais preciso que CURB-65 para identificar baixo risco.",
                tags = listOf("pneumologia", "psi", "pneumonia", "gravidade"), citation = "Diretriz IDSA/ATS 2019 - Pneumonia Comunitária"
            ),
            LibraryContentItem(
                id = "pneumo_003", title = "Antibiótico de escolha na pneumonia comunitária ambulatorial",
                category = "CLINICA_MEDICA", summary = "Macrolídeo (azitromicina) ou doxiciclina para PAC sem comorbidades.",
                content = "PAC ambulatorial sem comorbidades nem uso recente de ATB: monoterapia com macrolídeo (azitromicina 500 mg 1x/dia por 5 dias OU claritromicina 500 mg 2x/dia por 7 dias) OU doxiciclina 100 mg 2x/dia. Com comorbidades (ICC, DM, DRC, hepatopatia, asplenia, imunossupressão, uso de ATB nos últimos 3 meses): amoxicilina 500 mg 3x/dia + macrolídeo OU amoxicilina-clavulanato 875/125 mg 2x/dia + macrolídeo. Duração total: 5-7 dias (prolongar apenas se má resposta clínica). Atentar para resistência pneumocócica ao macrolídeo (~30% no Brasil).",
                tags = listOf("pneumologia", "pac", "antibiótico", "azitromicina"), citation = "Diretriz IDSA/ATS 2019 - Pneumonia Comunitária"
            ),
            LibraryContentItem(
                id = "pneumo_004", title = "Esquema empírico para pneumonia hospitalar",
                category = "CLINICA_MEDICA", summary = "Cefalosporina 3ª geração + vancomicina para pneumonia hospitalar.",
                content = "Pneumonia hospitalar (PH) e pneumonia associada à ventilação mecânica (PAV): Empírico: cefalosporina antipseudomonas (ceftazidima 2g 8/8h ou cefepima 2g 8/8h) ou carbapenêmico (meropenem 1g 8/8h) + vancomicina 15-20 mg/kg 12/12h (se MRSA suspeito) + ± aminoglicosídeo (amicacina 15-20 mg/kg 1x/dia) se alto risco de Gram-negativos resistentes. Duração: 7 dias (descalonar assim que possível com base em culturas). Resistência: pensar em Pseudomonas, Acinetobacter, MRSA, Klebsiella ESBL. Coletar culturas (lavado broncoalveolar ou aspirado traqueal) ANTES do antibiótico.",
                tags = listOf("pneumologia", "pneumonia hospitalar", "cefalosporina", "vancomicina"), citation = "Diretriz IDSA/ATS 2019 - Pneumonia Hospitalar"
            ),
            LibraryContentItem(
                id = "pneumo_005", title = "Critérios de ATS/IDSA para pneumonia grave",
                category = "CLINICA_MEDICA", summary = "Pneumonia grave = necessidade de ventilação mecânica ou sepse com disfunção orgânica.",
                content = "Critérios de pneumonia grave (ATS/IDSA) — indicam UTI: Critérios maiores: necessidade de ventilação mecânica, choque séptico (vasopressores). Critérios menores (≥3 = UTI): FR ≥30 (1pt), PaO2/FiO2 ≤250 (1pt), infiltrados multilobares (1pt), confusão (1pt), ureia >20 mg/dL (1pt), leucopenia <4.000 (1pt), trombocitopenia <100.000 (1pt), hipotermia <36°C (1pt), PAS <90 (1pt). Um critério maior OU ≥3 menores = admissão em UTI. Mortalidade: 25-50%.",
                tags = listOf("pneumologia", "pneumonia grave", "uti", "ats/idisa"), citation = "Diretriz IDSA/ATS 2019 - Pneumonia Comunitária"
            ),
            LibraryContentItem(
                id = "pneumo_006", title = "Fatores de risco para pneumonia por MRSA",
                category = "CLINICA_MEDICA", summary = "Internação recente, uso de vancomicina/ATB prévio, colonização conhecida.",
                content = "Fatores de risco para pneumonia por S. aureus resistente à meticilina (MRSA): Hospitalização >2 dias nos últimos 90 dias. Uso de vancomicina, cefalosporinas ou fluoroquinolonas nos últimos 30 dias. Colonização conhecida por MRSA. Doença pulmonar estrutural grave (fibrose cística). Hemodiálise. Diabetes mellitus. HIV/AIDS. Usuários de drogas IV. Se ≥1 fator de risco, adicionar vancomicina 15-20 mg/kg 12/12h (alvo trough 15-20 mcg/mL) ou linezolida 600 mg 12/12h ao esquema empírico.",
                tags = listOf("pneumologia", "mrsa", "fatores de risco", "vancomicina"), citation = "Diretriz IDSA/ATS 2019 - Pneumonia Hospitalar"
            ),
            LibraryContentItem(
                id = "pneumo_007", title = "Dosagem de corticoide na exacerbação aguda da DPOC",
                category = "CLINICA_MEDICA", summary = "Prednisona 40 mg/dia por 5-7 dias na exacerbação da DPOC.",
                content = "Exacerbação aguda da DPOC: Corticosteroide sistêmico: prednisona 40 mg VO 1x/dia ou metilprednisolona 60 mg IV 1x/dia por 5-7 dias (sem necessidade de desmame). Reduz tempo de recuperação, melhora função pulmonar (VEF1) e reduz risco de falha de tratamento. Pode-se usar budesonida nebulizada (2 mg 6/6h) como alternativa menos sistêmica. Associar sempre broncodilatador: beta-2 agonista (salbutamol 5 mg neb ou 100-200 mcg spray) + anticolinérgico (ipratrópio 0,5 mg neb) 4-6/6h. Antibiótico se 3 sinais de Anthonisen (aumento dispneia, aumento volume escarro, aparecimento pus) ou VM.",
                tags = listOf("pneumologia", "dpoc", "corticoide", "prednisona", "exacerbação"), citation = "Diretriz GOLD 2024 - DPOC"
            ),
            LibraryContentItem(
                id = "pneumo_008", title = "Fármaco de manutenção na DPOC",
                category = "CLINICA_MEDICA", summary = "LAMA + LABA (ex.: tiotrópio + formoterol) é a terapia base.",
                content = "Manutenção da DPOC estável: LAMA (Long-Acting Muscarinic Antagonist) + LABA (Long-Acting Beta Agonist) é a combinação de primeira linha para a maioria dos pacientes. Exemplos: tiotrópio (Spiriva) 18 mcg inalação 1x/dia + formoterol (12 mcg 2x/dia) ou indacaterol (Onbrez) 150 mcg 1x/dia. Combinações fixas: umedilidínio/vilanterol (Anoro), tiotrópio/olodaterol (Spiolto), glicopirrônio/indacaterol (Ultibro). GOLD Grupo A: broncodilatador de curta. B: LAMA + LABA. E: LAMA + LABA + corticoide inalatório se eosinófilos ≥300. Todas reduzem exacerbações e melhoram qualidade de vida.",
                tags = listOf("pneumologia", "dpoc", "lama", "laba", "broncodilatador"), citation = "Diretriz GOLD 2024 - DPOC"
            ),
            LibraryContentItem(
                id = "pneumo_009", title = "Classificação GOLD da DPOC",
                category = "CLINICA_MEDICA", summary = "Graus 1 a 4 baseado no VEF1 pós-broncodilatador.",
                content = "Classificação GOLD da limitação ao fluxo aéreo na DPOC (baseada no VEF1 pós-BD): GOLD 1 (leve): VEF1 ≥80%. GOLD 2 (moderada): VEF1 50-79%. GOLD 3 (grave): VEF1 30-49%. GOLD 4 (muito grave): VEF1 <30%. Além do grau espirométrico, o GOLD classifica o paciente em grupos A, B ou E (baseado em sintomas e histórico de exacerbações). Grupo A: mMRC 0-1 ou CAT <10, 0-1 exacerbações. Grupo B: mMRC ≥2 ou CAT ≥10, 0-1 exacerbações. Grupo E: ≥2 exacerbações ou ≥1 internação. O grupo define o tratamento.",
                tags = listOf("pneumologia", "gold", "dpoc", "vef1", "classificação"), citation = "Diretriz GOLD 2024 - DPOC"
            ),
            LibraryContentItem(
                id = "pneumo_010", title = "Diagnóstico da asma grave",
                category = "CLINICA_MEDICA", summary = "FeNO elevado (>50 ppb), eosinófilos >300 céls/µL, má resposta ao tratamento padrão.",
                content = "Asma grave (difícil controle): definida como asma que requer GINA passo 4-5 (dose alta de CI + LABA + ± LAMA) para manter controle, ou permanece não controlada apesar desta terapia. Biomarcadores de asma T2: FeNO >50 ppb (indica inflamação eosinofílica das vias aéreas), eosinófilos sanguíneos >300 céls/µL (prediz resposta a biológicos), IgE total elevada (alérgica). Tratamento específico: anti-IgE (omalizumabe), anti-IL5 (mepolizumabe, benralizumabe), anti-IL4R (dupilumabe). Excluir diagnósticos diferenciais (DPOC, disfunção de cordas vocais, bronquiectasia) antes de classificar como asma grave.",
                tags = listOf("pneumologia", "asma grave", "feno", "eosinófilos", "biológicos"), citation = "Diretriz GINA 2024 - Asma Grave"
            ),
            LibraryContentItem(
                id = "pneumo_011", title = "Fármaco de resgate na asma",
                category = "CLINICA_MEDICA", summary = "Beta-2 agonista de curta ação (SABA) — salbutamol 100-200 mcg spray.",
                content = "Resgate na asma: SABA (Short-Acting Beta Agonist) — salbutamol (Aerolin) 100-200 mcg (1-2 jatos) spray com espaçador, ou 5 mg nebulização, a cada 20 min por 3 repetições na crise. Na crise moderada-grave: associar ipratrópio (Atrovent) 0,5 mg neb. Desde 2022, o GINA não recomenda SABASE (SABA isolado) como tratamento de manutenção — o novo paradigma é CI-formoterol (formoterol + budesonida) como medicação de resgate e manutenção (MART). Isso reduz exacerbações em 30-50% comparado a SABA isolado. Nunca usar SABA >2x/semana na asma estável — indica má controle.",
                tags = listOf("pneumologia", "asma", "salbutamol", "saba", "resgate"), citation = "Diretriz GINA 2024 - Asma"
            ),
            LibraryContentItem(
                id = "pneumo_012", title = "Tratamento de manutenção da asma",
                category = "CLINICA_MEDICA", summary = "Corticoide inalatório + LABA como base do tratamento da asma persistente.",
                content = "Manutenção da asma: Corticoide inalatório (CI) + LABA é a base (GINA passo 3). Exemplos: budesonida/formoterol (Symbicort) 200/6 mcg 2x/dia, fluticasona/salmeterol (Seretide) 250/50 mcg 2x/dia. MART (Maintenance and Reliever Therapy): budesonida/formoterol 200/6 — 1 dose 2x/dia manutenção, + doses adicionais até 12x/dia para resgate. Passo 1: CI em baixa dose. Passo 2: CI baixa + SABA. Passo 3: CI/LABA baixa. Passo 4: CI/LABA média. Passo 5: CI alta + LABA + LAMA + considerarbiológicos. O objetivo é controle: sintomas diurnos ≤2x/semana, sem sintomas noturnos, uso de resgate ≤2x/semana.",
                tags = listOf("pneumologia", "asma", "ci", "laba", "manutenção"), citation = "Diretriz GINA 2024 - Asma"
            ),
            LibraryContentItem(
                id = "pneumo_013", title = "Critérios para intubação na asma grave",
                category = "CLINICA_MEDICA", summary = "PaCO₂ >45 mmHg, exaustão, rebaixamento de consciência, PCR iminente.",
                content = "Critérios de intubação na crise asmática grave: PaCO₂ >45 mmHg (ou ↑ >10/hora) — inicialmente a PaCO₂ está baixa (hipocapnia por hiperventilação), sua normalização já é sinal de gravidade exaustão muscular respiratória — tiragem, uso de acessórios, fala entrecortada. Rebaixamento de consciência — agitação, confusão, coma. Parada respiratória iminente — gasping, bradipneia, cianose. PCR. A intubação do asmático é de alto risco: usar tubo ≥8mm, indução com cetamina (broncodilatadora), ventilação com baixa FR (10-12 irpm, volume corrente 6-8 mL/kg, tempo expiratório longo) para evitar auto-PEEP (air trapping).",
                tags = listOf("pneumologia", "asma", "intubação", "uti", "ventilação"), citation = "Diretriz GINA 2024 - Asma Aguda Grave"
            ),
            LibraryContentItem(
                id = "pneumo_014", title = "Achados radiológicos da tuberculose primária",
                category = "CLINICA_MEDICA", summary = "Complexo de Ghon: foco pulmonar + linfonodo hilar ipsilateral.",
                content = "Tuberculose primária (primeiro contato): Complexo de Ghon (Ghon complex) — foco parenquimatoso (nódulo de Ghon, geralmente no lobo inferior ou médio) + linfadenopatia hilar ipsilateral. A lesão pode calcificar e tornar-se visível como nódulo de Ghon calcificado na radiografia de tórax. Diferenciação: em crianças, a linfadenopatia é mais evidente que o foco parenquimatoso. Na TB primária progressiva (maus tratos, imunossupressão), a lesão pode cavitar ou disseminar-se (TB miliar) — padrão micronodular difuso. PPD/IGRA positivo confirma infecção.",
                tags = listOf("pneumologia", "tuberculose", "ghon", "rx tórax"), citation = "Diretriz MS Brasil 2022 - Tuberculose"
            ),
            LibraryContentItem(
                id = "pneumo_015", title = "Esquema RIPE na tuberculose",
                category = "CLINICA_MEDICA", summary = "Rifampicina + Isoniazida + Pirazinamida + Etambutol por 2 meses, depois 4 meses R+I.",
                content = "Esquema RIPE (tratamento da TB sensível): Fase intensiva (2 meses): Rifampicina (R) 300 mg/dia + Isoniazida (H) 300 mg/dia + Pirazinamida (Z) 1.500-2.000 mg/dia + Etambutol (E) 1.200-1.500 mg/dia — todos em jejum, 1x/dia, dose conforme peso. Fase de manutenção (4 meses): R + H por mais 4 meses (total 6 meses). Exceção: meningite TB: manter 9-12 meses, substituir etambutol por corticosteroide (dexametasona). Doses pediátricas: ajustar por peso. Nunca usar monoterapia com R ou H (risco de resistência). Dose fixa combinada (4 em 1) facilita adesão.",
                tags = listOf("pneumologia", "ripe", "tuberculose", "rifampicina", "isoniazida"), citation = "Diretriz MS Brasil 2022 - Tuberculose"
            ),
            LibraryContentItem(
                id = "pneumo_016", title = "Efeitos colaterais da rifampicina",
                category = "CLINICA_MEDICA", summary = "Coloração laranja de secreções (urina, suor, lágrimas) + indução enzimática hepática.",
                content = "Rifampicina: efeitos colaterais e interações. Coloração laranja-avermelhada de urina, suor, lágrimas, fezes (orientar o paciente — é benigno, esperado). Cutâneas: rash, prurido, urticária. Hepáticas: hepatite (elevação de TGO/TGP), especialmente em combinação com isoniazida e álcool. Gastrintestinais: náusea, vômito, diarreia. Hematológicas: trombocitopenia (rara, dose-dependente). Indução enzimática potente do CYP3A4 — reduz eficácia de: anticoncepcionais orais (orientar método alternativo!), varfarina (↓INR), anticonvulsivantes, sulfonilureias, metadona, corticoides. Administrar 1h antes ou 2h após refeição.",
                tags = listOf("pneumologia", "rifampicina", "efeitos colaterais", "indução enzimática", "interação"), citation = "Diretriz MS Brasil 2022 - TB"
            ),
            LibraryContentItem(
                id = "pneumo_017", title = "Neuromiopatia induzida pela isoniazida",
                category = "CLINICA_MEDICA", summary = "Isoniazida causa neuropatia periférica. Prevenir com piridoxina (B6) 25-50 mg/dia.",
                content = "Neuropatia periférica por isoniazida (H): Mecanismo: inibição da vitamina B6 (piridoxina) → deficiência de B6 → neuropatia periférica (parestesias, dormência, fraqueza em extremidades). Risco aumentado em: DM, desnutrição, gestantes, etilistas, HIV+. Prevenção: administrar piridoxina (B6) 25-50 mg/dia profilaticamente com H em todos os pacientes (especialmente grupos de risco). Tratamento: se neuropatia já presente, ↑ B6 para 50-100 mg/dia. Outros efeitos: hepatite (↑ metabolismo de H no fígado, mais comum em acetiladores lentos), rash, febre. Ajuste de dose: se TGO/TGP >3-5x LSN → suspender temporariamente.",
                tags = listOf("pneumologia", "isoniazida", "neuropatia", "piridoxina", "b6"), citation = "Diretriz MS Brasil 2022 - TB"
            ),
            LibraryContentItem(
                id = "pneumo_018", title = "Interpretação do PPD (Mantoux)",
                category = "CLINICA_MEDICA", summary = "≥5mm em imunossuprimidos, ≥10mm em contatos, ≥15mm em baixo risco.",
                content = "Interpretação do PPD (Mantoux): ≥5 mm: HIV/AIDS, transplantados, imunossuprimidos (corticoide ≥15mg/dia por >30 dias), contatos de casos bacilíferos com Rx alterado, crianças <10 anos. ≥10 mm: contatos de TB (assintomáticos, Rx normal), silicose, IRC, diabetes, profissionais de saúde, presidiários, indígenas, privados de liberdade. ≥15 mm: qualquer pessoa sem fatores de risco, incluindo BCG-vacinados. O PPD é positivo quando a LEITURA (não a aplicação) atinge o diâmetro acima, medido 48-72h após aplicação. IGRA (Quantiferon) é alternativa — não cruza com BCG, não requer segunda visita.",
                tags = listOf("pneumologia", "ppd", "tuberculose", "mantoux", "igra"), citation = "Diretriz MS Brasil 2022 - Controle da TB"
            ),
            LibraryContentItem(
                id = "pneumo_019", title = "Tratamento da tuberculose multirresistente (TB-MDR)",
                category = "CLINICA_MEDICA", summary = "Esquemas de 2ª linha com fluoroquinolona + injetável + orais por 9-20 meses.",
                content = "TB-MDR (resistente a R e H): Esquemas de 2ª linha: fluoroquinolona (levofloxacino 750 mg/dia ou moxifloxacino 400 mg/dia) + injetável (amicacina 15 mg/kg/dia ou canamicina) por 4-6 meses + etionamida (750 mg/dia) + cicloserina (500-750 mg/dia) + pirazinamida + terizidona ± linezolida (600 mg/dia), bedaquilina (400 mg/dia por 2 semanas, depois 200 mg 3x/semana). Duração total: 9-12 meses para TB-MDR (esquema encurtado) ou até 20 meses para XDR. A bedaquilina revolucionou o tratamento da TB-MDR, reduzindo duração e toxicidade. Todo TB-MDR deve ter teste de sensibilidade (TS) feito.",
                tags = listOf("pneumologia", "tb-mdr", "multirresistente", "bedaquilina", "linezolida"), citation = "Diretriz MS Brasil 2022 - TB-MDR"
            ),
            LibraryContentItem(
                id = "pneumo_020", title = "Principais causas de derrame pleural exsudativo vs transudativo",
                category = "CLINICA_MEDICA", summary = "Critérios de Light para distinguir exsudato de transudato pleural.",
                content = "Critérios de Light para classificar derrame pleural: EXSUDATO se ≥1 critério: proteína líquido/proteína soro >0,5; LDH líquido/LDH soro >0,6; LDH líquido >2/3 do limite superior do normal do soro (ou >200 UI/dL). Causas de exsudato: pneumonia (parapneumônico/empiema), neoplasia (pulmão, mama, linfoma), TEP, TB pleural, colagenose (LES, AR), pancreatite. Causas de transudato: ICC (causa mais comum de derrame), cirrose (hidrotórax hepático), síndrome nefrótica, diálise peritoneal. Atenção: ICC em uso de diurético pode artificialmente elevar proteína e mimetizar exsudato — usar gradiente de albumina soro-líquido ≥1,1 g/dL corrige.",
                tags = listOf("pneumologia", "derrame pleural", "light", "exsudato", "transudato"), citation = "Diretriz ATS 2022 - Doenças Pleurais"
            ),
            LibraryContentItem(
                id = "pneumo_021", title = "Tratamento do pneumotórax hipertensivo",
                category = "CLINICA_MEDICA", summary = "Drenagem torácica IMEDIATA com agulha no 2º EIC, linha hemiclavicular.",
                content = "Pneumotórax hipertensivo (emergência): Descompressão IMEDIATA com agulha 14G (cateter) no 2º espaço intercostal (EIC), linha hemiclavicular, lado afetado. Som de saída de ar em alta pressão → confirma diagnóstico. O ar sai assobiando. Deixar o cateter aberto (válvula unidirecional com dedo de luva ou coletor. Após descompressão, instalar dreno torácico definitivo: 4º-5º EIC, linha axilar média, tubo 24-32 Fr direcionado para ápice. Confirmar com RX. causas: Trauma (mais comum), ventilação mecânica (barotrauma), iatrogênica (CVC, biópsia pleural), espontâneo.",
                tags = listOf("pneumologia", "pneumotórax", "drenagem", "emergência", "descompressão"), citation = "Diretriz BTS 2023 - Pneumotórax"
            ),
            LibraryContentItem(
                id = "pneumo_022", title = "Local correto para drenagem torácica no pneumotórax",
                category = "CLINICA_MEDICA", summary = "2º EIC, linha hemiclavicular para descompressão; 4º-5º EIC, linha axilar média para dreno.",
                content = "Localização para drenagem torácica: Descompressão de emergência: 2º espaço intercostal (EIC), linha hemiclavicular, lado afetado — no bordo superior da 3ª costela para evitar o feixe vásculo-nervoso intercostal que corre no bordo inferior de cada costela. Dreno definitivo: 4º-5º EIC, linha axilar média (triângulo de segurança) — delimitado pelo bordo lateral do peitoral maior anteriormente, bordo lateral do latíssimo do dorso posteriormente, e linha do mamilo (4º-5º EIC). Este triângulo é o local mais seguro e confortável para inserção do dreno. Tubo direcionado para o ápice (pneumotórax) ou para base (derrame).",
                tags = listOf("pneumologia", "drenagem", "pneumotórax", "triângulo de segurança"), citation = "Diretriz BTS 2023 - Procedimentos Pleurais"
            ),
            LibraryContentItem(
                id = "pneumo_023", title = "Indicação de videotoracoscopia no pneumotórax recorrente",
                category = "CLINICA_MEDICA", summary = "Pneumotórax espontâneo recidivante → videotoracoscopia com pleurodese.",
                content = "Indicações de videotoracoscopia (VATS) no pneumotórax: 1) pneumotórax espontâneo recidivante (≥2 episódios do mesmo lado); 2) pneumotórax persistente (>5 dias com dreno); 3) fístula broncopleural; 4) pneumotórax no profissional de risco (piloto, mergulhador). Procedimento: ressecção de bolhas/bulas (grapa ou sutura) + pleurodese pleural (abrasão ou pleurectomia parcial). Sucesso >95%. Alternativa: pleurodese química (talco) através do dreno — menos invasiva, mas menor sucesso a longo prazo.",
                tags = listOf("pneumologia", "pneumotórax", "videotoracoscopia", "pleurodese"), citation = "Diretriz BTS 2023 - Pneumotórax"
            ),
            LibraryContentItem(
                id = "pneumo_024", title = "Achados da sarcoidose",
                category = "CLINICA_MEDICA", summary = "Linfadenopatia hilar bilateral, uveíte, eritema nodoso, febre, fadiga.",
                content = "Sarcoidose: doença inflamatória multissistêmica de causa desconhecida com granulomas não-caseosos. Achados: RX tórax — linfadenopatia hilar bilateral (estádio I, 50%), infiltrado intersticial (estádio II), fibrose (estádio IV). Ocular: uveíte anterior (25-50% dos pacientes). Cutâneo: eritema nodoso (principalmente em mulheres — síndrome de Löfgren = linfadenopatia hilar + febre + eritema nodoso + artralgia). Outros: fadiga, febre, perda de peso, artralgia, paralisia de Bell (neuro-sarcoidose), hipercalcemia (produção de vitamina D pelos granulomas). Diagnóstico: biópsia (transbrônquica ou mediastinal) com granuloma não-caseoso + exclusão de TB e fungos.",
                tags = listOf("pneumologia", "sarcoidose", "linfadenopatia", "granuloma"), citation = "Diretriz ATS/ERS 2020 - Sarcoidose"
            ),
            LibraryContentItem(
                id = "pneumo_025", title = "Tratamento da sarcoidose",
                category = "CLINICA_MEDICA", summary = "Corticoide (prednisona 20-40 mg/dia) é a primeira linha. Metotrexato como poupador de corticoide.",
                content = "Sarcoidose: tratamento. Quando tratar: sintomas sistêmicos (febre, fadiga), doença pulmonar sintomática ou progressiva, doença ocular, cardíaca, neurológica, hipercalcemia. Primeira linha: prednisona 20-40 mg/dia por 2-4 semanas, depois desmame lento ao longo de 6-12 meses (ou mais). Poupadores de corticoide: metotrexato 10-20 mg/semana (1ª escolha), azatioprina 50-150 mg/dia, leflunomida 20 mg/dia, hidroxicloroquina (sarcoidose cutânea). Biológicos: anti-TNF (infliximabe, adalimumabe) em casos refratários. Sarcoidose assintomática estádio I: sem tratamento — 60-80% regridem espontaneamente. Prognóstico: 10-20% desenvolvem fibrose pulmonar irreversível.",
                tags = listOf("pneumologia", "sarcoidose", "corticoide", "prednisona", "metotrexato"), citation = "Diretriz ATS/ERS 2020 - Sarcoidose"
            ),
            LibraryContentItem(
                id = "pneumo_026", title = "Fibrose pulmonar idiopática — antifibróticos",
                category = "CLINICA_MEDICA", summary = "Nintedanib ou pirfenidona como terapia antifibrótica para FPI.",
                content = "Fibrose pulmonar idiopática (FPI): duas drogas antifibróticas aprovadas: Pirfenidona (Esbriet) 801 mg 3x/dia — inibe TGF-β e síntese de colágeno. Reduz declínio do VEF1 em ~50% (estudos ASCEND/CAPACITY). Efeitos colaterais: náusea, rash fototóxico, dispepsia. Nintedanib (Ofev) 150 mg 2x/dia — inibidor de tirosina quinase (VEGFR, PDGFR, FGFR). Reduz declínio do VEF1 e exacerbações agudas (INPULSIS). Efeitos colaterais: diarreia (60%). Ambos reduzem progressão mas não curam. Critérios diagnósticos: UIP (usual interstitial pneumonia) na TCAR — padrão em favo de mel + bronquiectasias de tração. Transplante pulmonar é o único tratamento curativo.",
                tags = listOf("pneumologia", "fpi", "nintedanib", "pirfenidona", "fibrose pulmonar"), citation = "Diretriz ATS/ERS 2022 - FPI"
            ),
            LibraryContentItem(
                id = "pneumo_027", title = "Estratificação de risco no TEP (PESI simplificado)",
                category = "CLINICA_MEDICA", summary = "PESI simplificado: idade >80, câncer, ICC, DPOC, FC ≥110, PAS <100, sat <90%.",
                content = "PESI simplificado (sPESI) para TEP: 6 critérios — idade >80 anos (1pt), câncer (1pt), ICC ou DPOC (1pt), FC ≥110 bpm (1pt), PAS <100 mmHg (1pt), satO2 <90% (1pt). sPESI=0: baixo risco (mortalidade 30d ~1%) — tratar em casa se condições adequadas. sPESI ≥1: alto risco — internação. PESI completo (11 critérios) é mais discriminativo mas o simplificado é mais rápido. Utilidade: decide se o paciente com TEP de baixo risco pode ser tratado ambulatorialmente (reduz custos, evita hospitalização).",
                tags = listOf("pneumologia", "tep", "pesi", "estratificação de risco"), citation = "Diretriz ESC 2019 - TEP"
            ),
            LibraryContentItem(
                id = "pneumo_028", title = "Anticoagulação no TEP de baixo risco",
                category = "CLINICA_MEDICA", summary = "Rivaroxabana ou apixabana como DOAC de escolha no TEP de baixo risco.",
                content = "TEP de baixo risco (sPESI=0, sem disfunção de VD, troponina normal): DOACs (anticoagulantes orais diretos) são a primeira escolha. Rivaroxabana (Xarelto) 15 mg 2x/dia por 21 dias, depois 20 mg 1x/dia. Apixabana (Eliquis) 10 mg 2x/dia por 7 dias, depois 5 mg 2x/dia. Vantagens dos DOACs sobre varfarina: dose fixa, sem monitorização do INR, menos interações alimentares, menos sangramento intracraniano. Duração: mínimo 3-6 meses. Se fator de risco removível (cirurgia, trauma), suspender após 3 meses. Se não provocado, considerar anticoagulação prolongada (avaliar risco de recorrência vs sangramento).",
                tags = listOf("pneumologia", "tep", "doac", "rivaroxabana", "apixabana"), citation = "Diretriz ESC 2019 - TEP"
            ),
            LibraryContentItem(
                id = "pneumo_029", title = "Síndrome da apneia obstrutiva do sono (SAOS)",
                category = "CLINICA_MEDICA", summary = "IAH >30 = grave. CPAP é o tratamento padrão ouro.",
                content = "SAOS: Índice de Apneia-Hipopneia (IAH) classifica gravidade: IAH 5-15: leve; 15-30: moderada; >30: grave. Sintomas: ronco, apneia presenciada, sono não reparador, sonolência diurna excessiva (Epworth ≥10), cefaleia matinal, noctúria, boca seca. Consequências: HAS resistente, FA, IAM, AVC, morte súbita (pico 2-6h). Diagnóstico: polissonografia noturna (padrão ouro). Tratamento: CPAP (Continuous Positive Airway Pressure) — pressão positiva contínua. Para apneia de 5-20 cmH2O. Reduz IAH, sonolência, PA, risco CV. Alternativas: APAP (pressão automática), BiPAP (se hipoventilação associada). APM (aparelho de avanço mandibular) em casos leves a moderados. Perda de peso (essencial) — pode curar a SAOS.",
                tags = listOf("pneumologia", "saos", "apneia", "cpap", "iah"), citation = "Diretriz AASM 2023 - SAOS"
            ),
            LibraryContentItem(
                id = "pneumo_030", title = "Tratamento da SAOS com CPAP",
                category = "CLINICA_MEDICA", summary = "CPAP é o tratamento padrão ouro para SAOS moderada a grave.",
                content = "CPAP na SAOS: Padrão ouro para SAOS moderada a grave (IAH ≥15) e SAOS leve com comorbidades. Configuração: pressão fixa (CPAP) 4-20 cmH2O, determinada por polissonografia de titulação. Alternativa moderna: APAP (auto-CPAP) — ajusta pressão automaticamente entre 4-20 cmH2O baseado na detecção de apneia e ronco. Adesão: meta ≥4h/noite em ≥70% noites (dados de compliance). Fatores que melhoram adesão: máscara confortável (nasal, facial, pillows), umidificador aquecido, rampa de pressão, educação do paciente. Intervenções não-CPAP: APM (avanço mandibular), cirurgia de vias aéreas superiores (uvulopalatofaringoplastia), estimulador do nervo hipoglosso (Inspire), traqueostomia (último recurso).",
                tags = listOf("pneumologia", "cpap", "saos", "apneia", "apap"), citation = "Diretriz AASM 2023 - SAOS"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 3: GASTROENTEROLOGIA E HEPATOLOGIA — 30 tópicos
    // ═══════════════════════════════════════════════════════════════

    private val gastroPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Diretrizes ACG, EASL, AASLD, SBH — Medicina Baseada em Evidências",
        items = listOf(
            LibraryContentItem(
                id = "gastro_001", title = "Critérios de Rockall para hemorragia digestiva alta",
                category = "CLINICA_MEDICA", summary = "Escore de Rockall para risco de ressangramento e morte na HDA.",
                content = "Critérios de Rockall (HDA): Idade (<60=0, 60-79=1, ≥80=2), choque (sem choque=0, FC>100+PAS>100=1, PAS<100=2), comorbidade (nenhuma=0, ICC/DAC/outra maior=2, renal/hepática/neoplasia=3), diagnóstico endoscópico (Mallory-Weiss/sem lesão=0, outras=1, CA digestivo alto=2), estigma de sangramento recente (sem=0, coágulo/hematina=1, vaso visível/sangramento ativo=2). Score <3: baixo risco (mortalidade <5%). Score ≥5: alto risco. Rockall completo (com endoscopia) é mais preditivo que o pré-endoscópico (sem endoscopia).",
                tags = listOf("gastro", "rockall", "hda", "sangramento"), citation = "Diretriz ACG 2021 - HDA"
            ),
            LibraryContentItem(
                id = "gastro_002", title = "Escala de Glasgow-Blatchford para sangramento digestivo",
                category = "CLINICA_MEDICA", summary = "Escala pré-endoscópica que indica necessidade de intervenção urgente.",
                content = "Glasgow-Blatchford (GBS) para HDA: ureia (25-30=2, 20-25=3, 25-30=4, ≥30=6), hemoglobina (homens 12-13=1, 10-12=3, <10=6; mulheres 10-12=1, <10=6), PAS (100-109=1, 90-99=2, <90=3), FC ≥100=1, melena=1, síncope=2, hepatopatia=2, ICC=2. Score 0: baixa probabilidade de intervenção (endoscopia pode ser eletiva). Score ≥1: endoscopia precoce indicada. A principal utilidade: identificar pacientes que NÃO precisam de endoscopia urgente (score 0). Também prediz necessidade de transfusão e mortalidade. Mais sensível que Rockall para identificar baixo risco.",
                tags = listOf("gastro", "glasgow-blatchford", "hda", "sangramento"), citation = "Diretriz ACG 2021 - HDA"
            ),
            LibraryContentItem(
                id = "gastro_003", title = "Conduta na hemorragia varicosa",
                category = "CLINICA_MEDICA", summary = "Terlipressina + antibioticoprofilaxia + bandagem elástica endoscópica.",
                content = "Hemorragia varicosa (HV) — emergência: 1) Terlipressina 1-2 mg IV 4/4h por 2-5 dias (reduz sangramento ativo). Alternativa: somatostatina 250 mcg/h IV ou octreotida 50 mcg bolus + 50 mcg/h. 2) Antibioticoprofilaxia: ceftriaxona 1g IV/dia por 5-7 dias (reduz infecções e mortalidade). Alternativa: norfloxacina 400 mg VO 12/12h. 3) Endoscopia de urgência (<12h): bandagem elástica → agulha de esclerose). 4) Se ressangramento ou falha: balão de Sengstaken-Blakemore ou TIPS (shunt portossistêmico transjugular intra-hepático) de resgate. Prevenir recidiva: BB não seletivo (propranolol 40-80 mg 2x/dia) + bandagem elétrica seriada.",
                tags = listOf("gastro", "varizes esofágicas", "terlipressina", "bandagem", "tipss"), citation = "Diretriz EASL 2021 - Hipertensão Portal"
            ),
            LibraryContentItem(
                id = "gastro_004", title = "Tamponamento com sonda de Sengstaken-Blakemore",
                category = "CLINICA_MEDICA", summary = "Indicado na falha endoscópica. Balão gástrico + esofágico. Máximo 24h.",
                content = "Sonda de Sengstaken-Blakemore (balão triplo-lúmen): indicação — hemorragia varicosa ativa não controlada por endoscopia e drogas, como ponte para TIPS. Técnica: insuflar balão gástrico (250-300 mL de ar), tracionar suavemente para impactar na cárdia, insuflar balão esofágico (30-45 mmHg medidos no manômetro, máximo 100 mmHg). Fixar com tensão moderada na bochecha (ou polia com 500g de peso). Complicações: aspiração (1º de todas), necrose de esôfago, rotura esofágica (perfuração), obstrução de vias aéreas. Limite de uso: 24h no máximo (risco de necrose). Monitorar com RX para verificar posição. Alternativas modernas: Stent esofágico autoexpansível (SX-ELLA) — menos complicado.",
                tags = listOf("gastro", "sengstaken", "blakemore", "varizes", "tamponamento"), citation = "Diretriz EASL 2021 - HV"
            ),
            LibraryContentItem(
                id = "gastro_005", title = "Tratamento da úlcera péptica por H. pylori",
                category = "CLINICA_MEDICA", summary = "Terapia tripla: IBP + claritromicina + amoxicilina (ou metronidazol se alérgico).",
                content = "H. pylori: terapia tripla padrão: IBP (omeprazol 20 mg, pantoprazol 40 mg, esomeprazol 40 mg) 2x/dia + claritromicina 500 mg 2x/dia + amoxicilina 1g 2x/dia por 14 dias. Se alérgico à penicilina: IBP + claritromicina 500 mg 2x/dia + metronidazol 400 mg 2x/dia. Onde a resistência à claritromicina é >15%, usar terapia quádrupla (bismuto + IBP + tetraciclina + metronidazol). Teste de cura: teste respiratório da ureia com C13, ou antígeno fecal, ou endoscopia (com urease), mínimo 4 semanas após o término do antibiótico. Erradicação bem-sucedida → cicatrização de úlcera duodenal e gástrica. Úlcera refratária → pensar em AINEs, Zollinger-Ellison, Má-absorção, resistência.",
                tags = listOf("gastro", "h. pylori", "úlcera", "terapia tripla"), citation = "Diretriz ACG 2022 - H. pylori"
            ),
            LibraryContentItem(
                id = "gastro_006", title = "Testes para H. pylori",
                category = "CLINICA_MEDICA", summary = "Teste da urease na endoscopia é o padrão ouro. Antígeno fecal para controle de cura.",
                content = "Diagnóstico de H. pylori: Teste da urease na endoscopia (padrão ouro no momento da endoscopia) — biópsia + colocação em meio contendo ureia. Se positivo, muda de cor (vermelho/rosa) em minutos. Sensibilidade 95%. Histologia (biópsia + coloração Hematoxilina/Eosina ou Giemsa) — permite avaliar gastrite. Teste respiratório da ureia (C13 ou C14) — não invasivo, melhor para controle de cura (sensibilidade 96%). Antígeno fecal (HpSA) — útil para crianças e controle de cura. Sorologia (IgG) — útil se atrofia gástrica grave ou sangramento (falso negativo na endoscopia), mas NÃO serve para controle de cura (IgG permanece positivo anos após erradicação).",
                tags = listOf("gastro", "h. pylori", "urease", "diagnóstico"), citation = "Diretriz ACG 2022 - H. pylori"
            ),
            LibraryContentItem(
                id = "gastro_007", title = "Critérios de Child-Pugh para cirrose",
                category = "CLINICA_MEDICA", summary = "Bilirrubina, albumina, TAP, ascite e encefalopatia. Classe A (5-6), B (7-9), C (10-15).",
                content = "Child-Pugh para cirrose: 5 parâmetros — bilirrubina (mg/dL): <2=1pt, 2-3=2pt, >3=3pt; albumina (g/dL): >3,5=1pt, 2,8-3,5=2pt, <2,8=3pt; TAP (INR): <1,7=1pt, 1,7-2,3=2pt, >2,3=3pt; ascite: ausente=1, leve/moderada=2, grave=3; encefalopatia: ausente=1, grau I-II=2, III-IV=3. Classe A (5-6 pontos): bom prognóstico, mortalidade cirúrgica ~10%. Classe B (7-9): risco moderado. Classe C (10-15): alto risco, mortalidade perioperatória ~50%. Útil para prognóstico de hepatopatia crônica e risco cirúrgico. MELD (model for end-stage liver disease) é mais preciso para fila de transplante.",
                tags = listOf("gastro", "child-pugh", "cirrose", "prognóstico"), citation = "Diretriz AASLD 2022 - Cirrose"
            ),
            LibraryContentItem(
                id = "gastro_008", title = "Pontuação MELD para transplante hepático",
                category = "CLINICA_MEDICA", summary = "MELD >15 indica necessidade de transplante. Escore baseado em Cr, bilirrubina e INR.",
                content = "MELD (Model for End-Stage Liver Disease): fórmula — MELD = 3,78 × ln(bilirrubina mg/dL) + 11,2 × ln(INR) + 9,57 × ln(creatinina mg/dL) + 6,43 (× 1,0 para cirrose). Valores: <10: baixa mortalidade. 10-20: risco intermediário. >20: alto. MELD >15: considerar transplante. >30: urgência. Utilidade: priorização na fila de transplante hepático nos EUA (e muitos centros no Brasil). MELD-Na: inclui sódio → melhor preditor de mortalidade (hiponatremia é fator de mau prognóstico). MELD 3.0: versão mais recente (inclui sexo feminino como fator). MELD <15 descompensado: pode indicar transplante se complicações recorrentes (ascite refratária, PBE, encefalopatia).",
                tags = listOf("gastro", "meld", "transplante", "fígado", "cirrose"), citation = "Diretriz AASLD 2022 - Transplante Hepático"
            ),
            LibraryContentItem(
                id = "gastro_009", title = "Tratamento da encefalopatia hepática",
                category = "CLINICA_MEDICA", summary = "Lactulose + rifaximina como primeira linha para EH.",
                content = "Encefalopatia hepática (EH): Lactulose 30-60 mL VO 2-4x/dia, ajustada para 2-3 evacuações moles/dia (efeito laxante reduz absorção de amônia). Pode ser administrada por SNE ou enema. Rifaximina 550 mg 2x/dia — antibiótico não-absorvível, reduz bactérias produtoras de amônia no cólon. Rifaximina + lactulose é superior à lactulose isolada (↓ recorrência em 50%). Critérios de West Haven: grau 0 (normal), grau I (inversão do sono, déficit de atenção), grau II (letargia, desorientação, asterixe), grau III (confusão, sonolência), grau IV (coma). Graves: considerar suspensão de diuréticos, corrigir distúrbios eletrolíticos, excluir hemorragia digestiva (precipita EH por sangramento).",
                tags = listOf("gastro", "encefalopatia", "lactulose", "rifaximina", "hepatopatia"), citation = "Diretriz EASL 2022 - Encefalopatia Hepática"
            ),
            LibraryContentItem(
                id = "gastro_010", title = "Diurético de escolha para ascite cirrótica",
                category = "CLINICA_MEDICA", summary = "Espironolactona 100 mg/dia (primeira linha) associado à restrição de sódio.",
                content = "Ascite cirrótica: Diurético de escolha: espironolactona 50-100 mg 1x/dia (antagonista de aldosterona — perde potássio). Se resposta inadequada, associar furosemida 40 mg/dia (relação ideal: espironolactona 100 mg + furosemida 40 mg). Titular conforme diurese e natriurese (meta: perda de peso 0,5-1 kg/dia). Efeitos colaterais: hipercalemia (espironolactona) e hipocalemia (furosemida) — monitorizar eletrólitos no início e a cada ajuste. Ginecomastia dolorosa com espironolactona (trocar por amilorida 10-20 mg/dia). Refratária: considerar paracentese de alívio (>5L), TIPS, ou transplante. Nunca usar IECA/BRA na ascite (reduzem PA e pioram função renal).",
                tags = listOf("gastro", "ascite", "espironolactona", "diurético"), citation = "Diretriz EASL 2021 - Ascite"
            ),
            LibraryContentItem(
                id = "gastro_011", title = "Restrição hídrica e sódica na ascite",
                category = "CLINICA_MEDICA", summary = "Restrição de sódio <2 g/dia é a base. Restrição hídrica só se hiponatremia <125 mEq/L.",
                content = "Restrição na ascite cirrótica: Sódio: ≤2 g/dia (88 mmol/dia) — a restrição de sódio é a base do tratamento. A restrição hídrica NÃO é automática — só indicada se sódio sérico <125 mEq/L (1.000-1.500 mL/dia). Sódio sérico >130: não restringir água. Meta: balanço hídrico negativo de 500-1.000 mL/dia (perda de peso ideal 0,5-1 kg/dia). Avaliar resposta pela natriurese de 24h (se sódio urinário >78 mEq/dia apesar de restrição → não responsivo a diurético → precisa de nova estratégia). Orientar nutricional: dietas hipossódicas com 1-2g de sódio/dia. Ascite refratária: descontinuar diuréticos, paracentese de alívio seriada.",
                tags = listOf("gastro", "ascite", "restrição de sódio", "hiponatremia"), citation = "Diretriz EASL 2021 - Ascite"
            ),
            LibraryContentItem(
                id = "gastro_012", title = "Paracentese de alívio na ascite refratária",
                category = "CLINICA_MEDICA", summary = "Indicada se >5L de ascite ou sinais de desconforto não responsivos a diuréticos.",
                content = "Paracentese de alívio: Indicação: ascite refratária (não responsiva a doses máximas de diuréticos), ascite tensa (distensão abdominal grave, dispneia). Técnica: punção no quadrante inferior esquerdo (ou linha média inferior, 2 cm abaixo da cicatriz umbilical), agulha 22G, retirada lenta. Drenagem de >5L: administrar albumina 8 g/L retirado (ex.: 5L → 40g de albumina) para prevenir disfunção circulatória pós-paracentese (reduz mortalidade). Hemorragia: cuidado se plaquetas <50.000 ou INR >2 — coágulos geralmente contidos pela pressão intra-abdominal elevada. Análise do líquido: determinar se PBE, contagem de neutrófilos, cultura. Não precisa de permissão de rotina para punção local.",
                tags = listOf("gastro", "paracentese", "ascite", "albumina"), citation = "Diretriz EASL 2021 - Ascite"
            ),
            LibraryContentItem(
                id = "gastro_013", title = "Diagnóstico da peritonite bacteriana espontânea",
                category = "CLINICA_MEDICA", summary = "Líquido ascítico com >250 PMN/mm³. Cultura + sangue antes de antibiótico.",
                content = "Peritonite Bacteriana Espontânea (PBE) em cirróticos: Diagnóstico: paracentese — neutrófilos (PMN) no líquido ascítico >250 células/mm³. Cultura do líquido ascítico (inocular em frascos de hemocultura na beira do leito — aumenta positividade em 50%). ANTES de iniciar antibiótico. Apresentação clínica: febre (50%), dor abdominal difusa, reversão da encefalopatia, íleo, hipotensão. Muitos são assintomáticos. Fatores de risco: ascite grave (Child C), bilirrubina >3,2, história prévia de PBE. Profilaxia primária: norfloxacina 400 mg/dia se cirrótico com ascite e albumina <1,5g/dL ou Child ≥9. Profilaxia secundária: norfloxacina 400 mg/dia por tempo indefinido.",
                tags = listOf("gastro", "pbe", "peritonite", "ascite", "neutrófilos"), citation = "Diretriz EASL 2021 - PBE"
            ),
            LibraryContentItem(
                id = "gastro_014", title = "Antibiótico na peritonite bacteriana espontânea",
                category = "CLINICA_MEDICA", summary = "Cefotaxima 2g IV 8/8h ou ceftriaxona 2g IV 12/12h por 5-7 dias.",
                content = "PBE: Tratamento antibiótico: Cefalosporina de 3ª geração — cefotaxima 2g IV 8/8h ou ceftriaxona 2g IV 12/12h por 5-7 dias. Alternativa em alérgicos: Levofloxacino 500 mg 12/12h + vancomicina se suspeita de MRSA intra-hospitalar. Albumina: 1,5 g/kg no 1º dia + 1 g/kg no 3º dia, especialmente se Cr >1 mg/dL ou ureia >30 mg/dL (↓ insuficiência renal e mortalidade). Repetir paracentese em 48h: se PMN reduzir <25% → trocar antibiótico (pensar em Candida, enterococo, peritonite secundária). Profilaxia: norfloxacina 400 mg/dia após o episódio.",
                tags = listOf("gastro", "pbe", "cefotaxima", "ceftriaxona", "albumina"), citation = "Diretriz EASL 2021 - PBE"
            ),
            LibraryContentItem(
                id = "gastro_015", title = "Tratamento da hepatite B crônica",
                category = "CLINICA_MEDICA", summary = "Entecavir ou tenofovir como primeira linha para hepatite B crônica.",
                content = "Hepatite B crônica: Primeira linha: entecavir (Baraclude) 0,5 mg/dia (ou 1 mg se resistente a lamivudina) ou tenofovir (Viread) 300 mg/dia (ou TAF 25 mg — tenofovir alafenamida, melhor perfil renal). Duração indefinida (não há cura total — suprime replicação viral). Cura funcional: HBsAg negativo + anti-HBs positivo (raro). Indicações de tratamento: DNA-HBV >2.000 UI/mL + ALT elevada (>LSN), ou cirrose independente de ALT/DNA. Avaliação: HBeAg (soroconversão), anti-HBe, DNA-HBV, ALT, USG para HCC (a cada 6 meses). Resistência entecavir: <1,2% em 5 anos; tenofovir: nenhum caso documentado. Profilaxia: vacina para todos os contatos. Mãe HBsAg+: tenofovir no 3º trimestre + imunoglobulina + vacina no RN.",
                tags = listOf("gastro", "hepatite b", "entecavir", "tenofovir"), citation = "Diretriz EASL 2022 - Hepatite B"
            ),
            LibraryContentItem(
                id = "gastro_016", title = "Tratamento da hepatite C com antivirais de ação direta",
                category = "CLINICA_MEDICA", summary = "Sofosbuvir + daclatasvir por 8-12 semanas, independente de genótipo.",
                content = "Hepatite C: tratamento com antivirais de ação direta (DAA). Esquema pangenotípico padrão: sofosbuvir 400 mg/dia + daclatasvir 60 mg/dia por 12 semanas (sem cirrose) ou 24 semanas (cirrose). Alternativa: glecaprevir/pibrentasvir (Mavyret) 3 comprimidos 1x/dia — 8 semanas (sem cirrose), 12 semanas (cirrose). Taxa de cura (RVS12) >95% para todos os genótipos. Pré-tratamento: genotipagem, carga viral (HCV-RNA), fibrose (FibroScan ou APRI/FIB-4). Hemograma e função hepática. Durantes: monitorizar efeitos colaterais. Pós-tratamento: PCR-RNA 12 e 24 semanas após (RVS = RNA indetectável). HCC: continua rastreio mesmo após RVS (risco persistente se cirrose).",
                tags = listOf("gastro", "hepatite c", "sofosbuvir", "daa"), citation = "Diretriz EASL 2022 - Hepatite C"
            ),
            LibraryContentItem(
                id = "gastro_017", title = "Carga viral e genotipagem da hepatite C",
                category = "CLINICA_MEDICA", summary = "Genotipagem define duração do tratamento. Atualmente 8-12 semanas para todos.",
                content = "Hepatite C: genotipagem e carga viral. Genótipos: 1 (mais comum no Brasil e EUA — 46%), 2 (10%), 3 (30%), 4, 5, 6. Antes, o genótipo (e fibrose) determinava duração do tratamento (12 semanas para GT1 e 24 para alguns). Com pangenotípicos, a duração é fixa: geralmente 8-12 semanas. Carga viral (HCV RNA) confirma infecção ativa. O tratamento é indicado para todos os pacientes com RNA-HCV detectável, independente de fibrose (exceto expectativa de vida <12 meses). Carga viral: monitoriza resposta. RVS (resposta virológica sustentada) = RNA indetectável 12-24 semanas após o término. Genótipos 3: requerem maior atenção (resistência, resposta menor a alguns esquemas).",
                tags = listOf("gastro", "genótipo", "hepatite c", "rna"), citation = "Diretriz EASL 2022 - Hepatite C"
            ),
            LibraryContentItem(
                id = "gastro_018", title = "Contraindicação do ácido valproico em hepatopatas",
                category = "CLINICA_MEDICA", summary = "Valproico é contraindicado em hepatopatas pelo risco de hiperamonemia e coma.",
                content = "Ácido valproico (valproato de sódio): Contraindicado em hepatopatia ativa e doença metabólica hepática (distúrbio do ciclo da ureia). Mecanismo: inibe ureagênese (aumenta amônia) e metabolizado no fígado. Pode causar hepatotoxicidade (↑TGO/TGP, hiperamonemia) e encefalopatia hepática (confusão, coma). Risco maior em <2 anos, politerapia, doenças metabólicas. Alternativas seguras em hepatopatas: levetiracetam (Keppra — 90% excretado inalterado pelo rim, sem metabolismo hepático significativo). Topiramato, lamotrigina (com cautela). Se precisar usar valproico: monitorizar amônia sérica regularmente, suspender se ↑amônia ou TGO/TGP >3x LSN.",
                tags = listOf("gastro", "valproico", "hepatopatia", "hiperamonemia"), citation = "Bulas aprovadas ANVISA + Diretriz AASLD"
            ),
            LibraryContentItem(
                id = "gastro_019", title = "Doença de Wilson — diagnóstico",
                category = "CLINICA_MEDICA", summary = "Anel de Kayser-Fleischer + cobre urinário >100 µg/24h + ceruloplasmina baixa.",
                content = "Doença de Wilson (degeneração hepatolenticular): acúmulo de cobre (mutação ATP7B). Diagnóstico: Achado clássico: Anel de Kayser-Fleischer (depósito de cobre na córnea — visível na lâmpada de fenda). Cobre urinário 24h >100 µg (normal <40). Ceruloplasmina sérica <20 mg/dL (baixa em 95% dos homozigotos). Cobre livre sérico elevado (>15 µg/dL). Biópsia hepática: cobre hepático >250 µg/g. Escore de Leipzig comanda diagnóstico. Suspeitar em <40 anos com hepatopatia de causa indeterminada, sintomas neurológicos (tremor, disartria, distonia) ou psiquiátricos. Não tratar é fatal (cirrose, dano neurológico irreversível). Rastreio familiar: teste genético (ATP7B) + cobre urinário + ceruloplasmina.",
                tags = listOf("gastro", "wilson", "cobre", "kayser-fleischer"), citation = "Diretriz EASL 2022 - Doença de Wilson"
            ),
            LibraryContentItem(
                id = "gastro_020", title = "Tratamento da doença de Wilson",
                category = "CLINICA_MEDICA", summary = "D-penicilamina ou zinco como tratamento de primeira linha.",
                content = "Doença de Wilson: D-penicilamina (Cuprimine) 1-1,5 g/dia dividida em 2-4 tomadas — quelante de cobre mais potente (primeira linha). Efeitos colaterais: alergia, febre, rash, linfadenopatia, proteinúria (~30%). Pode piorar sintomas neurológicos inicialmente. Zinco (acetato de zinco 50 mg 3x/dia) — inibe absorção intestinal de cobre. Tratamento de manutenção (após descomissionar) ou primeira linha se assintomático. Trientina (Syprine) 1-1,5 g/dia — alternativa à D-penicilamina, menos efeitos colaterais. Piridoxina (B6) 25-50 mg/dia junto com D-penicilamina (evita deficiência). Meta: cobre urinário >200 µg/24h (na fase de descomissionamento) e <100 (manutenção). Tratamento por toda a vida.",
                tags = listOf("gastro", "wilson", "d-penicilamina", "zinco", "cobre"), citation = "Diretriz EASL 2022 - Doença de Wilson"
            ),
            LibraryContentItem(
                id = "gastro_021", title = "Diagnóstico da hemocromatose hereditária",
                category = "CLINICA_MEDICA", summary = "Saturação de transferrina >45% + ferritina >300 (homens) / >200 (mulheres).",
                content = "Hemocromatose hereditária (HH): acúmulo de ferro. Diagnóstico: Saturação de transferrina >45% (jejum) — é o teste de rastreio. Ferritina >300 ng/mL em homens e >200 ng/mL em mulheres (elevada indica sobrecarga de ferro). Confirmação: teste genético para C282Y e H63D (gene HFE). C282Y homozigoto = 90% dos casos. Biópsia hepática: índice hepático de ferro >1,9 (se genótipo duvidoso ou doença avançada). RM hepática (FerriScan) quantifica ferro não-invasivamente. Consequências: cirrose, DM, cardiomiopatia, artralgia, hipogonadismo, hiperpigmentação (pele bronzeada), HCC. Todo paciente deve ter avaliação hepática + função cardíaca (ecocardiograma). Rastreio familiar: ferro + transferrina + HFE.",
                tags = listOf("gastro", "hemocromatose", "ferritina", "transferrina"), citation = "Diretriz EASL 2022 - Hemocromatose"
            ),
            LibraryContentItem(
                id = "gastro_022", title = "Tratamento da hemocromatose",
                category = "CLINICA_MEDICA", summary = "Flebotomia — remover 1 unidade (450-500 mL) de sangue por semana.",
                content = "Hemocromatose: Flebotomia (tratamento padrão): remover 450-500 mL de sangue/semana (1 unidade = ~250 mg de ferro) enquanto ferritina >300 µg/L. Meta: ferritina 50-100 µg/L, saturação de transferrina <50%. A frequência é reduzida para cada 1-3 meses na manutenção. Melhora: fadiga (semana), DM (resiste se instalado), artralgia (em alguns), função cardíaca, enzimas hepáticas. Cirrose é irreversível. Se anemia ou contraindicação à flebotomia: quelante oral — deferasirox (Exjade) 20 mg/kg/dia. HCC: manter rastreio com USG a cada 6 meses se cirrose. Prognóstico: expectativa de vida normal se tratado antes de cirrose ou DM.",
                tags = listOf("gastro", "flebotomia", "hemocromatose", "deferasirox"), citation = "Diretriz EASL 2022 - Hemocromatose"
            ),
            LibraryContentItem(
                id = "gastro_023", title = "Diagnóstico diferencial entre Crohn e Retocolite Ulcerativa",
                category = "CLINICA_MEDICA", summary = "Crohn: lesões descontínuas, fístulas; Retocolite: contínua, reto sempre acometido.",
                content = "Doença de Crohn vs Retocolite Ulcerativa (RCU): Crohn: inflamação transmural (pode fistulizar), descontínua (em ilhas — poupa reto em 50%), pode afetar todo o TGI (do esôfago ao ânus), granuloma na histologia (não-caseoso), comum: fístulas, abscessos, estenoses. RCU: inflamação da mucosa e submucosa, contínua e ascendente do reto em direção proximal, afeta só o cólon, não fistuliza, lesões: pseudopólipos, mucosa granular. Histologia: criptite, abscessos de criptas. Clínica: Crohn (dor abdominal em QID, diarreia, fadiga, perda de peso); RCU (diarreia sanguinolenta, urgência, tenesmo). PSC (colangite esclerosante) só na RCU. Fatores: tabagismo piora Crohn, melhora RCU (paradoxo).",
                tags = listOf("gastro", "crohn", "retocolite", "dii"), citation = "Diretriz ECCO 2023 - DII"
            ),
            LibraryContentItem(
                id = "gastro_024", title = "Tratamento de manutenção na DII",
                category = "CLINICA_MEDICA", summary = "RCU: mesalazina. Crohn: azatioprina ou biológicos (infliximabe, adalimumabe, vedolizumabe).",
                content = "Manutenção na DII: RCU (leve-moderada): mesalazina oral 2,4-4,8 g/dia + mesalazina tópica (supositório ou enema) para doença distal. Alternativa: sulfassalazina 3-6 g/dia. São poupadores de corticoides e reduzem recorrência. Crohn: Doença luminal ativa: corticoide sistêmico (prednisona 40-60 mg/dia) por ponte até início de imunomodulador. Manutenção: Azatioprina (2-2,5 mg/kg/dia) ou 6-Mercaptopurina (1,5 mg/kg/dia). Biológicos: infliximabe (Remicade) 5 mg/kg IV — semanas 0, 2, 6, depois 8/8semanas; adalimumabe (Humira) 160/80/40mg SC; vedolizumabe (Entyvio) 300 mg IV — para DII; ustekinumabe (Stelara) IV → SC a cada 8 semanas. Metotrexato 25 mg/semana SC (Crohn apenas).",
                tags = listOf("gastro", "dii", "mesalazina", "biológicos", "azatioprina"), citation = "Diretriz ECCO 2023 - DII"
            ),
            LibraryContentItem(
                id = "gastro_025", title = "Fármacos biológicos na DII",
                category = "CLINICA_MEDICA", summary = "Infliximabe, adalimumabe, vedolizumabe para DII moderada a grave refratária.",
                content = "Biológicos na DII: Anti-TNF: infliximabe (primeira linha para DII moderada-grave), adalimumabe, certolizumabe pegol. Indicados em DII não controlada por corticoides/imunomoduladores (Crohn e RCU). Efeitos colaterais: infecções (TB, fungos), reações infusão (infliximabe — pré-medicar), reação local (adalimumabe), indução de anticorpos (formação de anticorpos contra o medicamento). Anti-integrina: vedolizumabe (α4β7) — específico do intestino, menor risco de infecções sistêmicas. Anti-IL12/23: ustekinumabe — eficaz no Crohn. Anti-IL23: risankizumabe, mirikizumabe (emergentes, com boa eficácia). Janus kinase (JAK) inhibidores: tofacitinibe, upadacitinibe — via oral, eficazes mas com risco de TEV e herpes zoster.",
                tags = listOf("gastro", "infliximabe", "adalimumabe", "vedolizumabe", "biológicos"), citation = "Diretriz ECCO 2023 - DII"
            ),
            LibraryContentItem(
                id = "gastro_026", title = "Pancreatite aguda — critérios de Ranson",
                category = "CLINICA_MEDICA", summary = "11 critérios (5 na admissão, 6 em 48h) para predizer mortalidade.",
                content = "Critérios de Ranson para pancreatite aguda: Na admissão (5): idade >55 anos (1pt), leucócitos >16.000 (1pt), glicose >200 mg/dL (1pt), LDH >350 UI/dL (1pt), AST >250 UI/dL (1pt). Em 48h (6): queda do Ht >10% (1pt), ureia >5 mg/dL (1pt), cálcio <8 mg/dL (1pt), PaO2 <60 mmHg (1pt), déficit de base >4 mEq/L (1pt), sequestro de fluidos >6L (1pt). Risco: <3 = leve (mortalidade ~1%); 3-5 = moderada (10-15%); ≥6 = grave (>50%). Desvantagens: demora 48h para ficar completo, específico para pancreatite alcoólica e biliar. Alternativa: BISAP (mais simples, pode ser calculado em 24h), APACHE-II.",
                tags = listOf("gastro", "ranson", "pancreatite", "critérios"), citation = "Diretriz IAP/APA 2020 - Pancreatite"
            ),
            LibraryContentItem(
                id = "gastro_027", title = "Escala de BISAP para pancreatite",
                category = "CLINICA_MEDICA", summary = "BISAP: 5 critérios (BUN, IR, >2, SIRS, idade >60, derrame pleural).",
                content = "BISAP (Bedside Index for Severity in Acute Pancreatitis): B = BUN >25 mg/dL (1pt), I = comprometimento do estado mental (1pt), S = SIRS (≥2 critérios: FC >90, FR >20 ou PaCO2 <32, T <36 ou >38°C, leucócitos <4.000 ou >12.000 ou bastões >10%) — 1pt, A = idade >60 anos (1pt), P = derrame pleural na imagem (1pt). Escore 0-1: mortalidade <1%. 2: ~2-5%. 3: ~10%. 4: ~15-20%. 5: >20%. Vantagens: pode ser calculado em 24h (ao contrário de Ranson), tão preditivo quanto Ranson e APACHE-II. Útil no PS para decisão de internação (≥3 = UTI).",
                tags = listOf("gastro", "bisap", "pancreatite", "gravidade", "mortalidade"), citation = "Diretriz IAP/APA 2020 - Pancreatite"
            ),
            LibraryContentItem(
                id = "gastro_028", title = "Tratamento da pancreatite aguda",
                category = "CLINICA_MEDICA", summary = "Jejum + hidratação vigorosa + analgesia (morfina não é contraindicada).",
                content = "Pancreatite aguda: Jejum (suspender dieta via oral): manter NPO até melhora da dor, tolerância oral, não usar sonda nasogástrica de rotina. Hidratação vigorosa: Ringer Lactato 250-500 mL/h nas primeiras 12-24h (5-10 mL/kg/h). Meta: FC <120, diurese >0,5 mL/kg/h, PVC 5-10. Evitar hidratação excessiva (>4L/dia). Analgesia: morfina NÃO é contraindicada (mito desmentido por meta-análises). Pode usar tramadol, dipirona, morfina, fentanil, cetamina. Antibiótico: NÃO usar profilaticamente. Indicado se infecção comprovada ou suspeita (necrose infectada). Suporte nutricional: se jejum >5-7 dias, nutrição enteral por SNE (não parenteral). CPRE de urgência: se colangite ou obstrução biliar persistente.",
                tags = listOf("gastro", "pancreatite", "hidratação", "analgesia"), citation = "Diretriz IAP/APA 2020 - Pancreatite"
            ),
            LibraryContentItem(
                id = "gastro_029", title = "Cálculo de lipase e amilase na pancreatite",
                category = "CLINICA_MEDICA", summary = "Lipase >3x LSN é mais específica que amilase. Lipase persiste elevada por mais tempo.",
                content = "Enzimas na pancreatite: Lipase: mais específica que amilase (95% vs 85%). Eleva-se 4-8h após início, pico em 24h, normaliza em 7-14 dias. Sensibilidade 90-95%. Amilase: eleva 2-12h, normaliza 3-5 dias, menos específica (pode ↑ em: parotidite, IRC, doença biliar, macroamilasemia, cetoacidose diabética). Lipase >3x LSN confirma pancreatite. Nível de elevação não se correlaciona com gravidade. Hipertrigliceridemia: amilase pode ser falsamente baixa, lipase é mais confiável. Pseudo-cisto: enzimas podem permanecer elevadas por semanas. Diagnóstico: ↑ enzimas + dor abdominal consistente + imagem (TC ou USG). TC sem contraste na admissão para excluir complicações.",
                tags = listOf("gastro", "lipase", "amilase", "pancreatite", "enzimas"), citation = "Diretriz IAP/APA 2020 - Pancreatite"
            ),
            LibraryContentItem(
                id = "gastro_030", title = "Indicação de colecistectomia na colelitíase",
                category = "CLINICA_MEDICA", summary = "Indicação se colelitíase sintomática (cólica biliar) ou complicações (pancreatite, colecistite).",
                content = "Colecistectomia: Indicação: 1) Cólica biliar (sintomática): dor em hipocôndrio direito, após refeição gordurosa. Se episódios recorrentes → indicado. 2) Colecistite aguda: dor + febre + Murphy + ↑amilase+ ↑leuc. → colecistectomia de urgência (idealmente <72h do início). Se instável, drenagem percutânea + colecistectomia tardia. 3) Pancreatite biliar: após resolução, fazer colecistectomia antes da alta (↓ risco de recorrência). 4) Câncer de vesícula: raro, incidental na histologia. Colelitíase Assintomática: sem indicação (exceto vesícula em porcelana, pólipo >10mm, >1,5cm de cálculo, ausência de contratilidade, fimose). Rastreio: USG abdome. Risco operatório: Child-Pugh A é aceitável para laparoscopia.",
                tags = listOf("gastro", "colecistectomia", "colelitíase", "vesícula"), citation = "Diretriz ASGE 2022 - Colelitíase"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 4: NEFROLOGIA E UROLOGIA — 25 tópicos
    // ═══════════════════════════════════════════════════════════════

    private val nefrologiaPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Diretrizes KDIGO, ISN, AUA — Medicina Baseada em Evidências",
        items = listOf(
            LibraryContentItem(
                id = "nefro_001", title = "Classificação KDIGO da DRC",
                category = "CLINICA_MEDICA", summary = "Estágios G1 a G5 baseado na TFG. Albuminúria adicional (A1-A3).",
                content = "DRC (Doença Renal Crônica): Classificação por TFG: G1: TFG ≥90 mL/min. G2: 60-89 (leve redução). G3a: 45-59 (leve-moderada). G3b: 30-44 (moderada-grave). G4: 15-29 (graves). G5: <15 (insuficiência renal terminal — IRT). + Categoria de albuminúria: A1 (<30 mg/g normal), A2 (30-300 — microalbuminúria), A3 (>300 — macroalbuminúria). Risco de progressão: quanto menor TFG + maior albuminúria → maior risco. Definição: TFG <60 por ≥3 meses, ou TFG ≥60 com marcadores de dano renal (albuminúria, hematúria, alterações imagem, biópsia). DRC é fator de risco CV independente. Rastreio: Cr sérica + EAS + albuminúria (spot) anualmente em DM, HAS, >60 anos.",
                tags = listOf("nefrologia", "drc", "kdigo", "tfg", "albumina"), citation = "Diretriz KDIGO 2023 - DRC"
            ),
            LibraryContentItem(
                id = "nefro_002", title = "Cálculo do Clearance de Creatinina (Cockcroft-Gault)",
                category = "CLINICA_MEDICA", summary = "ClCr = [(140-idade) × peso (kg)] / (72 × Cr) × 0,85 (se mulher).",
                content = "Clearance de creatinina (ClCr) pela fórmula de Cockcroft-Gault: ClCr = [(140 - idade) × peso (kg) em kg] / [72 × creatinina sérica (mg/dL)] × fator de sexo (1 para homens, 0,85 para mulheres). Limitações: superestima TFG em obesos e edemaciados (peso ideal), subestima em idosos e desnutridos, depende da massa muscular. CKD-EPI é mais precisa atualmente. Cockcroft-Gault ainda é usada para ajuste de dose de medicamentos (especialmente em bulas de quimioterápicos). Doses de ATB, anticoagulantes (DOACs), antidiabéticos (metformina, SGLT2, GLP-1): ajustar por TFG-CKD-EPI.",
                tags = listOf("nefrologia", "cockcroft-gault", "clearance", "creatinina"), citation = "Diretriz KDIGO 2023 - DRC"
            ),
            LibraryContentItem(
                id = "nefro_003", title = "Fórmula MDRD e CKD-EPI para TFG estimada",
                category = "CLINICA_MEDICA", summary = "CKD-EPI mais precisa em idosos e em TFG >60. MDRD era padrão anterior.",
                content = "MDRD (Modification of Diet in Renal Disease): TFGe = 175 × Cr^-1,154 × idade^-0,203 × (0,742 se mulher) × (1,212 se negro). Limitada: imprecisa para TFG >60 e em idosos. CKD-EPI (Chronic Kidney Disease Epidemiology Collaboration): TFGe = (Cr/κ)^α × min(Cr/κ,1)^-1,209 × 0,993^idade × (1,018 se mulher) × (1,159 se negro). κ = 0,7 (mulheres) ou 0,9 (homens); α = -0,329 (mulheres) ou -0,411 (homens). CKD-EPI é a recomendação atual (KDIGO): mais precisa que MDRD especialmente em TFG >60 (↓ falso ++ de DRC). CKD-EPI sem correção para raça: nova versão (2021) removeu o fator raça (desigualdade).",
                tags = listOf("nefrologia", "mdrd", "ckd-epi", "tfg"), citation = "Diretriz KDIGO 2023 - DRC"
            ),
            LibraryContentItem(
                id = "nefro_004", title = "Critérios AKIN e RIFLE para lesão renal aguda",
                category = "CLINICA_MEDICA", summary = "RIFLE: Risk (↑Cr 1,5x), Injury (↑2x), Failure (↑3x), Loss (>4 semanas), ESRD (>3 meses).",
                content = "LRA (Lesão Renal Aguda): Classificação RIFLE: R (Risk): Cr ↑1,5x ou ↓TFG >25% ou diurese <0,5 mL/kg/h por 6h. I (Injúria): Cr ↑2x ou ↓TFG >50% ou diurese <0,5 mL/kg/h por 12h. F (Falha): Cr ↑3x ou Cr ≥4 mg/dL com ↑agudo >0,5 ou ↓TFG >75% ou anúria >12h. L (Loss): perda de função por >4 semanas. E (ESRD): necessidade de diálise >3 meses. AKIN: similar ao RIFLE mas com tempo mais curto (48h). KDIGO 2012: Cr ↑≥0,3 mg/dL em 48h ou ↑≥1,5x basal em 7 dias. Estadiamento KDIGO: 1 (↑0,3 ou 1,5-1,9x), 2 (2-2,9x), 3 (3x ou Cr ≥4 ou diálise).",
                tags = listOf("nefrologia", "rifle", "akin", "lra", "kdigo"), citation = "Diretriz KDIGO 2022 - LRA"
            ),
            LibraryContentItem(
                id = "nefro_005", title = "Principais causas de IRA pré-renal",
                category = "CLINICA_MEDICA", summary = "Hipovolemia, choque, uso de AINEs, ICC, cirrose (síndrome hepatorrenal).",
                content = "IRA pré-renal (hipoperfusão renal): Causas: Hipovolemia (hemorragia, desidratação, vômitos, diarreia, queimaduras). Choque (cardiogênico, distributiva — sepse, anafilaxia). Hipotensão (AINEs, IECA, BRA — particularmente se estenose de artéria renal ou IC). ICC: baixo débito, congestão hepática. Cirrose: síndrome hepatorrenal (vasoconstrição renal por vasodilatação esplâncnica). Diagnósticos: FENa <1% (fração de excreção de sódio), FeUreia <35%, densidade urinária >1.020, Na urinário <20 mEq/L, relação Cr urinária/Cr sérica >40. Sinais clínicos: turgor cutâneo diminuído, mucosas secas, hipotensão ortostática, taquicardia. Reversível se corrigida a causa (volume, suspensão de droga).",
                tags = listOf("nefrologia", "ira", "pré-renal", "hipovolemia"), citation = "Diretriz KDIGO 2022 - LRA"
            ),
            LibraryContentItem(
                id = "nefro_006", title = "Fração de excreção de sódio (FENa) na IRA",
                category = "CLINICA_MEDICA", summary = "FENa <1% = pré-renal. FENa >2% = intrínseca (necrose tubular aguda).",
                content = "FENa = (Na urinário / Na sérico) / (Cr urinária / Cr sérica) × 100. Interpretação: FENa < 1%: pré-renal (rins intactos, retendo sódio). FENa > 2%: intrínseca (NTA — necrose tubular aguda). Zona cinzenta: 1-2% no início da NTA. Limitações: FENa pode ser <1% na NTA por contraste, rabdomiólise, glomerulonefrite aguda, e em DRC avançada. FENa pode ser >1% na pré-renal se paciente em uso de diuréticos. Alternativa: FeUreia (fração de excreção de ureia): FeUreia <35% = pré-renal (não é afetada por diuréticos). Na urinário <20: pré-renal; >40: NTA. Sempre interpretar FENa no contexto clínico.",
                tags = listOf("nefrologia", "fena", "ira", "pré-renal", "nta"), citation = "Diretriz KDIGO 2022 - LRA"
            ),
            LibraryContentItem(
                id = "nefro_007", title = "Índice de insuficiência renal (FeUreia)",
                category = "CLINICA_MEDICA", summary = "FeUreia <35% = pré-renal. Útil quando FENa é inconclusiva (ex.: diuréticos).",
                content = "FeUreia (fração excretada de ureia) = [Ureia urinária (mg/dL) × Cr sérica (mg/dL)] × 100 / [Ureia sérica (mg/dL) × Cr urinária (mg/dL)]. Interpretação: FeUreia <35% — pré-renal (reto de ureia). FeUreia >35% — NTA intrínseca. Vantagem sobre FENa: não é afetada por diuréticos de alça, sendo mais confiável em pacientes que usaram furosemida. Limitação: estados de baixo fluxo urinário, septicemia. Usar em conjunto com outros marcadores: osmolalidade urinária >500 mOsm/kg (pré-renal), <350 (intrínseca). Relação Cr urinária/Cr sérica: >40 pré-renal; <20 intrínseca. Relação Na urinário/Cr urinária: <1% pré-renal.",
                tags = listOf("nefrologia", "feureia", "ira", "pré-renal"), citation = "Diretriz KDIGO 2022 - LRA"
            ),
            LibraryContentItem(
                id = "nefro_008", title = "Acidose tubular renal — tipos 1, 2 e 4",
                category = "CLINICA_MEDICA", summary = "Tipo 1 (distal): hipocalemia. Tipo 2 (proximal): hipofosfatemia. Tipo 4: hipercalemia.",
                content = "Acidose Tubular Renal (ATR): ATR1 (distal): defeito na secreção de H+ no túbulo coletor. Laboratório: acidose metabólica hiperclorêmica, K+ baixo, Ca urinário alto (nefrolitíase), pH urinário >5,5. Tratamento: bicarbonato de sódio 1-2 mEq/kg/dia. ATR2 (proximal): defeito na reabsorção de HCO3−. Laboratório: acidose metabólica, K+ baixo, fósforo baixo (osteomalácia), glicosúria, aminoacidúria (síndrome de Fanconi). pH urinário pode ser <5,5 (quando HCO3 baixo). Tratamento: bicarbonato 5-10 mEq/kg/dia. ATR4 (hipercalêmica): deficiência de aldosterona ou resistência tubular. K+ alto, acidose leve. Tratamento: fludrocortisona (se aldosterona baixa) ou suspender IECA/BRA (causa comum).",
                tags = listOf("nefrologia", "atr", "acidose tubular", "potássio"), citation = "Diretriz KDIGO 2022 - Distúrbios Ácido-Base"
            ),
            LibraryContentItem(
                id = "nefro_009", title = "Tratamento da acidose tubular tipo 4",
                category = "CLINICA_MEDICA", summary = "Hipercalemia por deficiência de aldosterona. Fludrocortisona ou suspensão de IECA.",
                content = "ATR4 (hipercalêmica): Causa mais comum: diabetes mellitus (hiporreninemia → hipoaldosteronismo). Drogas: IECA, BRA, espironolactona, AINEs, heparina, trimetoprima, ciclosporina, tacrolimo. Insuficiência adrenal primária. Diagnóstico: hipercalemia com acidose metabólica hiperclorêmica leve + relação K+ urinário/Cr <20 (sugere aldosterona baixa). Tratamento: 1) corrigir hipercalemia (dieta, suspender drogas, resina de K+); 2) fludrocortisona 0,1-0,2 mg/dia (se aldosterona baixa); 3) suspender IECA/BRA se causados. Diurético de alça (furosemida) se hipervolemia. Bicarbonato se acidose grave. Não precisa de bicarbonato se pH >7,2.",
                tags = listOf("nefrologia", "atr4", "hipercalemia", "fludrocortisona", "ieca"), citation = "Diretriz KDIGO 2022 - Distúrbios Ácido-Base"
            ),
            LibraryContentItem(
                id = "nefro_010", title = "Síndrome nefrótica — critérios diagnósticos",
                category = "CLINICA_MEDICA", summary = "Proteinúria >3,5g/24h, hipoalbuminemia <3g/dL, edema. Pode ter hiperlipidemia.",
                content = "Síndrome nefrótica: Proteinúria >3,5g/24h (ou relação proteína/Cr urinária >3.500 mg/g). Hipoalbuminemia <3 g/dL (resultado da perda renal). Edema (hipoalbuminemia → ↓pressão oncótica → extravasamento). Hiperlipidemia (↓colesterol e TG) — LDL e VLDL aumentados. Lipidúria (gordura na urina). Causas: Primárias (doenças glomerulares): doença de lesões mínimas (crianças), glomeruloesclerose segmentar focal (GESF), glomerulonefrite membranosa (adulto), nefropatia membranoproliferativa. Secundárias: diabetes, lúpus, amiloidose, pré-eclâmpsia, drogas (AINEs, lítio, heroína), infecções (HIV, hepatite B/C, sífilis), neoplasias (mieloma, CA pulmão, CA cólon). Complicações: infecções (perda de IgG), trombose (perda de antitrombina III), IRC. Tratamento: corticoide (primeira linha na maioria), IECA/BRA (↓proteinúria), restrição de sódio, diuréticos, estatina.",
                tags = listOf("nefrologia", "nefrótica", "proteinúria", "hipoalbuminemia"), citation = "Diretriz KDIGO 2022 - Glomerulonefrites"
            ),
            LibraryContentItem(
                id = "nefro_011", title = "Tratamento da glomerulonefrite membranosa",
                category = "CLINICA_MEDICA", summary = "Corticoide + ciclofosfamida ou rituximabe para GN membranosa de alto risco.",
                content = "Glomerulonefrite membranosa: Tratamento baseado em risco de progressão (baseado em proteinúria, TFG, anti-PLA2R). Baixo risco (proteinúria <4g/dia, TFG normal): observação + IECA/BRA + estatina. Risco moderado: corticoide + ciclofosfamida (ou rituximabe) por 6-12 meses. Protocolo de Ponticelli: metilprednisolona 1g IV 3 dias alternados + clorambucil oral por 6 meses. Alto risco: rituximabe 1g IV dias 0 e 15 (ou 375 mg/m2/semana × 4). Ciclofosfamida + corticoide se rituximabe indisponível. Monitorizar com anti-PLA2R (marcador de atividade, reduz com remissão). Prognóstico: remissão espontânea em 30% (se baixa proteinúria). Progressão para IRC em 30-40%.",
                tags = listOf("nefrologia", "glomerulonefrite", "membranosa", "rituximabe"), citation = "Diretriz KDIGO 2022 - Glomerulonefrites"
            ),
            LibraryContentItem(
                id = "nefro_012", title = "Síndrome nefrítica — hematúria, proteinúria, hipertensão, oligúria",
                category = "CLINICA_MEDICA", summary = "Hematúria + proteinúria <3,5g + hipertensão + oligúria. Causa: pós-estreptocócica, IgA, lúpus.",
                content = "Síndrome nefrítica: Hematúria (glomerular — dismórfica, cilindros hemáticos) + proteinúria (geralmente <3,5g/dia) + hipertensão + oligúria + edema. Causas: Pós-estreptocócica (mais comum em crianças, 2-3 semanas após faringite ou piodermite — anti-DNAse B, ASLO). Nefropatia por IgA (Doença de Berger — hematúria recorrente associada a infecção de vias aéreas superiores, IgA alto). Glomerulonefrite lúpica (Classe III-IV) com C3 baixo e anti-dsDNA. Glomerulonefrite rapidamente progressiva (crescentes). Diagnóstico: biópsia renal (essencial). Tratamento: repouso, dieta hipossódica, diuréticos, controle da HAS. Se pós-estreptocócica: cuidados de suporte (reversível em 90% das crianças). Se IgA: IECA/BRA. Se lúpus: corticoide + micofenolato/ciclofosfamida. Se crescentes: pulsoterapia com metilprednisolona.",
                tags = listOf("nefrologia", "nefrítica", "hematúria", "iga", "lúpus"), citation = "Diretriz KDIGO 2022 - Glomerulonefrites"
            ),
            LibraryContentItem(
                id = "nefro_013", title = "Púrpura de Henoch-Schönlein (nefrite por IgA)",
                category = "CLINICA_MEDICA", summary = "Vasculite IgA. Rash purpúrico, artralgia, dor abdominal, nefrite. Corticoide se grave.",
                content = "Púrpura de Henoch-Schönlein (PHS) / Vasculite IgA: Clínica: Rash purpúrico palpável (em membros inferiores), artralgias/artrite, dor abdominal (cólica, náusea, vômito), doença renal (hematúria, proteinúria). Acomete mais crianças (3-10 anos). Causa: depósito de IgA na pequenos vasos + glomérulo. Pós-infecciosa (Estreptococo). Nefrite (30-50%): pode progredir para IRC se não tratada. Biópsia renal se proteinúria >1g/dia ou TFG ↓. Tratamento: suporte (prednisona 1-2 mg/kg/dia se dor abdominal ou nefrite). Se nefrite moderada-grave (proliferação na biópsia): corticoide por 6 meses + ciclofosfamida ou micofenolato, IECA/BRA se proteinúria persistente. Prognóstico: maioria das crianças recupera totalmente. Pior se nefrite grave ou início em adultos.",
                tags = listOf("nefrologia", "henoch-schönlein", "púrpura", "iga", "vasculite"), citation = "Diretriz KDIGO 2022 - Glomerulonefrites"
            ),
            LibraryContentItem(
                id = "nefro_014", title = "Litíase renal de cálcio — tratamento",
                category = "CLINICA_MEDICA", summary = "Hipercalciúria idiopática: tiazídicos + restrição de sódio reduzem novos cálculos.",
                content = "Litíase renal de cálcio (hipercalciúria idiopática): Diagnóstico: Ca urinário >300 mg/24h (homens) ou >250 mg (mulheres), ou >4 mg/kg/dia. Tratamento: Tiazídicos (hidroclorotiazida 25-50 mg/dia, clortalidona 25 mg/dia): reduzem excreção urinária de cálcio (↑reabsorção no túbulo proximal) e novos cálculos em 50%. Restrição de sódio (<2g/dia): reduz excreção de cálcio e potencializa efeito dos tiazídicos. Dieta: Não restringir cálcio (paradoxo — restrição aumenta absorção intestinal de oxalato e formação de cálculos). Cálcio dietético normal (1.000-1.200 mg/dia). Limitar proteína animal e oxalato (espinafre, nozes, chocolate, chá preto). Garantir ingesta hídrica adequada (urina >2-2,5L/dia). Citrato de potássio 20-60 mEq/dia se hipocitratúria (<320 mg/dia). Alopurinol se hiperuricosúria.",
                tags = listOf("nefrologia", "litíase", "cálcio", "tiazídico", "nefrolitíase"), citation = "Diretriz AUA 2022 - Litíase Renal"
            ),
            LibraryContentItem(
                id = "nefro_015", title = "Litíase por ácido úrico",
                category = "CLINICA_MEDICA", summary = "Alopurinol + citrato de potássio para alcalinizar a urina (pH >6,5).",
                content = "Litíase de ácido úrico: Radiotransparente (não aparece no RX, mas sim na TC sem contraste). Causas: pH urinário baixo (<5,5) — principal fator (obesidade, DM, gota, diarreia crônica, síndrome metabólica). Hiperuricosúria: ácido úrico >800 mg/24h (homens) ou >750 (mulheres). Desidratação. Tratamento: Alcalinizar a urina: citrato de potássio (K-citrato) 30-60 mEq/dia dividido (alvo pH urinário 6,5-7,0) — dissolução dos cálculos em semanas a meses. Alopurinol 300 mg/dia (se hiperuricosúria). Dieta: reduzir purinas (carnes vermelhas, frutos do mar, vísceras), aumentar líquidos (urina >2-2,5L/dia). Perda de peso (essencial na síndrome metabólica). Febuxostat 40-120 mg/dia (alternativa ao alopurinol). Se uricemia alta (ácido úrico sérico elevado): tratar como gota.",
                tags = listOf("nefrologia", "ácido úrico", "litíase", "alopurinol", "citrato"), citation = "Diretriz AUA 2022 - Litíase Renal"
            ),
            LibraryContentItem(
                id = "nefro_016", title = "Hiperplasia prostática benigna (HPB) — fármacos",
                category = "CLINICA_MEDICA", summary = "Bloqueadores alfa (tansulosina) relaxam a próstata e melhoram sintomas imediatamente.",
                content = "HPB: Bloqueadores alfa-1 adrenérgicos: primeira linha para sintomas urinários. Tansulosina 0,4 mg/dia (mais seletivo para próstata) — efeito em dias. Doxazosina 2-8 mg/dia. Terazosina 5-10 mg/dia. Efeitos: relaxam músculo liso da próstata e bexiga, ↑ fluxo urinário, ↓ IPSS (International Prostate Symptom Score) em 4-6 pontos. Efeitos colaterais: hipotensão ortostática (tansulosina menos que outros), ejaculação retrógrada (tansulosina ~20%). Alternativa: inibidores da 5-alfa-redutase (finasterida/dutasterida) — reduzem volume prostático em 20-30% em 6-12 meses. Combinação: tansulosina + dutasterida (Duodart) — superior na redução de retenção urinária aguda e necessidade de cirurgia. Tratamento cirúrgico: TURP (RTU) ou laser se falha clínica.",
                tags = listOf("nefrologia", "hpb", "tansulosina", "prostata"), citation = "Diretriz AUA 2022 - HPB"
            ),
            LibraryContentItem(
                id = "nefro_017", title = "Antagonistas 5-alfa-redutase na HPB",
                category = "CLINICA_MEDICA", summary = "Finasterida/dutasterida reduzem o tamanho da próstata em 6 meses.",
                content = "HPB: Inibidores da 5-alfa-redutase: Finasterida 5 mg/dia (inibe tipo II). Dutasterida 0,5 mg/dia (inibe tipos I e II). Mecanismo: bloqueiam conversão de testosterona em DHT (dihidrotestosterona), reduzindo o volume prostático (20-30% em 6-12 meses). Efeitos: redução do IPSS, melhora do fluxo urinário, ↓ retenção urinária aguda (57%), ↓ necessidade de cirurgia (67%). Indicados principalmente para próstatas >40g. Efeitos colaterais: disfunção erétil (5-8%), redução da libido (3-5%), redução do volume ejaculado, ginecomastia (<2%). Podem baixar o PSA em 50% (√ PSA × 2 para valor real — importante no rastreio de CA de próstata). Início: 6 meses para efeito máximo. Contrainidicados se CA de próstata.",
                tags = listOf("nefrologia", "finasterida", "dutasterida", "hpb"), citation = "Diretriz AUA 2022 - HPB"
            ),
            LibraryContentItem(
                id = "nefro_018", title = "Rastreio de câncer de próstata",
                category = "CLINICA_MEDICA", summary = "PSA >4 ng/mL + toque retal alterado indica biópsia.",
                content = "Rastreio CA de próstata: PSA (Antígeno Prostático Específico) + Toque retal (TR). Início: 45 anos para homens de alto risco (negros, história familiar de CA de próstata) e 50 anos para os demais. Frequência: a cada 2 anos se PSA <2,5; anual se >2,5. PSA: <4 ng/mL = baixo risco; 4-10 = intermediário (biópsia se TR+ ou relação livre/total <0,15); >10 = alto risco. TR: nódulo endurecido, assimetria, perda do sulco mediano. Limitações do PSA: falso + (HPB, prostatite, ejaculação recente, sonda vesical, DRE). Falso - AINEs, finasterida, dutasterida. Novos marcadores: PCA3 (urina), TMPRSS2-ERG, 4Kscore, MRI multiparamétrica da próstata (antes da biópsia reduz biópsias desnecessárias). Decisão compartilhada: explicar riscos e benefícios (sobrediagnóstico, sobretratamento).",
                tags = listOf("nefrologia", "psa", "câncer de próstata", "rastreio"), citation = "Diretriz AUA 2023 - CA de Próstata"
            ),
            LibraryContentItem(
                id = "nefro_019", title = "Indicação de biópsia de próstata",
                category = "CLINICA_MEDICA", summary = "PSA >10 ng/mL ou relação PSA livre/total <0,15 com PSA 4-10.",
                content = "Biópsia prostática: Indicações: 1) PSA >10 ng/mL independente de TR. 2) PSA 4-10 com TOQUE RETAL ALTERADO (nódulo). 3) PSA 4-10 com relação PSA livre/total <0,15 (sugere maior risco de malignidade). 4) Aumento significativo do PSA em curto período (PSA velocity >0,75 ng/mL/ano). 5) RM de próstata com PIRADS (Prostate Imaging Reporting and Data System) ≥4 — lesão suspeita. Biópsia: guiada por USG transretal (10-12 fragmentos) ou transperineal (menor risco de infecção). Pré-biópsia: profilaxia antibiótica (ciprofloxacino ou ceftriaxona). Complicações: hematúria, hematoespermia, prostatite aguda (<5%). Histologia: Gleason score (<6: baixo risco, 7: intermediário, 8-10: alto).",
                tags = listOf("nefrologia", "biópsia", "próstata", "psa livre"), citation = "Diretriz AUA 2023 - CA de Próstata"
            ),
            LibraryContentItem(
                id = "nefro_020", title = "Tratamento de infecção urinária recorrente",
                category = "CLINICA_MEDICA", summary = "Profilaxia com nitrofurantoína ou fosfomicina para ITU recorrente.",
                content = "ITU recorrente (≥2 episódios em 6 meses ou ≥3 em 12 meses) em mulheres sem fatores de risco: Profilaxia: Nitrofurantoína 50-100 mg/dia (por 6 meses) ou Fosfomicina trometamol 3g a cada 10 dias. Alternativas: cefalexina 250 mg/dia, trimetoprima-sulfametoxazol 40/200 mg/dia (se sensível), metenamina 1g 2x/dia (em idosos). Profilaxia pós-coital: cefalexina 250 mg dose única, nitrofurantoína 50 mg dose única. Comportamentais: micção após relação sexual, boa hidratação, evitar espermicida (alteram flora vaginal). Não há benefício comprovado em: cranberry (baixa evidência), d-manose, probióticos. Diagnóstico diferencial: síndrome uretral, cistite intersticial, vaginite, DST. ITU em homens: sempre investigar (HPB, prostatite crônica).",
                tags = listOf("nefrologia", "itu", "recorrente", "nitrofurantoína", "fosfomicina"), citation = "Diretriz EAU 2023 - Infecções Urológicas"
            ),
            LibraryContentItem(
                id = "nefro_021", title = "Cistite intersticial",
                category = "CLINICA_MEDICA", summary = "Dor vesical crônica com urgência e frequência urinária. Cistoscopia com hidrodistensão ajuda.",
                content = "Cistite intersticial (CI) / Síndrome da Bexiga Dolorosa: Sintomas: dor vesical (piora com enchimento da bexiga, melhora com micção), urgência, frequência, noctúria. Diagnóstico de exclusão: excluir ITU, CA bexiga, endometriose, DST. Testes: questionário O'Leary-Sant, diário miccional, urina (EAS, cultura, citologia), cistoscopia com hidrodistensão (glomérulos, úlcera de Hunner). Tratamento: não farmacológico: dieta (evitar café, álcool, frutas cítricas, alimentos picantes), fisioterapia do assoalho pélvico, biofeedback, acupuntura. Farmacológico: amitriptilina 10-50 mg/dia (1ª linha), hidroxizina, pentosano polissulfato 100 mg 3x/dia. Instalações vesicais: DMSO, heparina, lidocaína. Neuromodulação: TENS, estimulação do nervo tibial. Cirurgia: rara. Prognóstico: crônica, tratamento sintomático.",
                tags = listOf("nefrologia", "cistite intersticial", "bexiga"), citation = "Diretriz AUA 2022 - Cistite Intersticial"
            ),
            LibraryContentItem(
                id = "nefro_022", title = "Hematúria microscópica — diagnóstico diferencial",
                category = "CLINICA_MEDICA", summary = "Causas: glomerulonefrite, litíase, neoplasia, TB renal. Investigar com USG, citologia, cistoscopia.",
                content = "Hematúria microscópica (≥3 hemácias/campo em 2 de 3 amostras): Causas: Glomerular (glomerulonefrite — hematúria dismórfica, cilindros hemáticos, proteinúria); Litíase (cálculo renal/ureteral — dor lombar, hematúria após cólica); Neoplasia (CA bexiga, CA renal, CA próstata — >35 anos, fatores de risco); TB renal (piúria estéril, cultura +); Atividade física intensa. Investigação: USG de vias urinárias (cálculos, massa renal, espessamento vesical), citologia urinária (3 amostras), creatinina + EAS + proteinúria, cistoscopia (se >40 anos ou fatores de risco, ou se não fechar diagnóstico). TC urotomografia (Uro-TC): se suspeita de litíase ou tumor urotelial. Biópsia renal: se hematúria glomerular sugestiva (dismórfica, cilindros, proteinúria).",
                tags = listOf("nefrologia", "hematúria", "diagnóstico diferencial"), citation = "Diretriz AUA 2020 - Hematúria"
            ),
            LibraryContentItem(
                id = "nefro_023", title = "Síndrome de Fanconi",
                category = "CLINICA_MEDICA", summary = "Glicosúria, aminoacidúria, perda de fosfato por disfunção do túbulo proximal.",
                content = "Síndrome de Fanconi: Disfunção tubular proximal generalizada → perda de glicose (glicosúria normoglicêmica), aminoácidos (aminoacidúria), fosfato (hipofosfatemia → osteomalácia/raquitismo), ácido úrico, bicarbonato (acidose tubular proximal). Causas: Primária (crianças — mutação, idiopática). Adquirida: drogas (tenofovir — em HIV, ifosfamida, cisplatina, valproato), mieloma múltiplo (cadeia leve κ), doença de Wilson, cistinose, metais pesados (chumbo, mercúrio), amiloidose, transplante renal. Laboratório: glicosúria com glicemia normal, hipofosfatemia, hipouricemia, pH urinário ácido + acidose. Tratamento: corrigir o déficit (fosfato oral + bicarbonato + vitamina D). Se tenofovir: substituir outro antirretroviral. Se ifosfamida: suspender droga. Se mieloma: tratar a doença de base.",
                tags = listOf("nefrologia", "fanconi", "glicosúria", "fosfato", "tubular proximal"), citation = "Diretriz KDIGO 2022 - Distúrbios Tubulares"
            ),
            LibraryContentItem(
                id = "nefro_024", title = "Diabetes insipidus — teste da privação hídrica",
                category = "CLINICA_MEDICA", summary = "Teste de privação hídrica diferencia diabetes insipidus central de nefrogênico.",
                content = "Diabetes insipidus (DI): Poliúria (>50 mL/kg/dia), polidipsia, noctúria. Teste da privação hídrica (DDAVP test): suspender água por 8-12h (monitorar peso e desidratação). Medir: osmolalidade urinária (OsmU) e sérica (OsmS), peso, Na sérico. Normal: restrição → ↑ OsmU >800. DI central: restrição não concentra urina (OsmU <300) + DDAVP (desmopressina 10-20 mcg intranasal/SC) → ↑ OsmU >50% (rim responsivo à ADH). DI nefrogênico (resistência renal à ADH): restrição não concentra + DDAVP não responde (↑ OsmU <10%). Causas: Central: trauma, tumor hipotálamo/hipófise, neurocirurgia, histiocitose. Nefrogênico: lítio (causa mais comum), hipercalcemia, hipocalemia, doença renal policística. Tratamento central: DDAVP. Nefrogênico: suspender causa, tiazídicos + AINEs (paradoxal), dieta hipossódica + hipoproteica.",
                tags = listOf("nefrologia", "diabetes insipidus", "poliúria", "ddavp"), citation = "Diretriz KDIGO 2022 - Distúrbios Hidroeletrolíticos"
            ),
            LibraryContentItem(
                id = "nefro_025", title = "Tratamento da diabetes insipidus central",
                category = "CLINICA_MEDICA", summary = "Desmopressina (DDAVP) intranasal ou oral para DI central.",
                content = "Diabetes insipidus central: Tratamento de primeira linha: Desmopressina (DDAVP) — análogo sintético da ADH. Apresentações: Intranasal (10-20 mcg por spray, 1-2x/dia). Oral (Minirin® 0,1-0,2 mg, 1-2x/dia). Subcutâneo/IV (em hospital). Dose: titular para controlar noctúria e evitar hiponatremia. O risco principal: hiponatremia (uso excessivo). Orientar: não tomar se houver ingestão excessiva de água → náusea, cefaleia, convulsão (hiponatremia grave). Monitorizar: peso, sódio sérico (1-2 semanas após cada ajuste), osmolalidade urinária. Formas: pós-operatório de cirurgia hipofisária: DDAVP SC ou oral. Uso crônico: via intranasal (conveniente, absorção constante). Cuidados: pacientes idosos (função renal reduzida): iniciar com dose menor. Contraindicação: DI nefrogênico.",
                tags = listOf("nefrologia", "ddavp", "desmopressina", "diabetes insipidus"), citation = "Diretriz Endocrine Society 2022 - DI"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 5: ENDOCRINOLOGIA — 30 tópicos
    // ═══════════════════════════════════════════════════════════════

    private val endocrinoPack = LibraryContentPack(
        source = "[NÃO VERIFICADO] Rascunho gerado por IA — conferir contra: Diretrizes ADA, EASD, SBEM — Medicina Baseada em Evidências",
        items = listOf(
            LibraryContentItem(
                id = "endo_001", title = "Critérios diagnósticos de diabetes mellitus",
                category = "CLINICA_MEDICA", summary = "Glicemia jejum ≥126, TOTG ≥200, HbA1c ≥6,5% ou sintomas + glicemia ≥200.",
                content = "Diagnóstico de DM: 1) Glicemia de jejum ≥126 mg/dL em 2 ocasiões. 2) TOTG (75g) — glicemia 2h ≥200 mg/dL. 3) HbA1c ≥6,5% (laboratório padronizado). 4) Glicemia aleatória ≥200 mg/dL com sintomas (poliúria, polidipsia, emagrecimento). PRÉ-DIABETES: glicemia jejum 100-125 (glicemia de jejum alterada) ou TOTG 140-199 (tolerância diminuída à glicose) ou HbA1c 5,7-6,4%. Rastreio: a cada 3 anos em todos ≥45 anos ou em qualquer idade se sobrepeso/obesidade + 1 fator de risco (hipertensão, dislipidemia, história familiar, SOP, DM gestacional prévio). TOTG também é recomendado para diagnóstico de DM gestacional (24-28 semanas).",
                tags = listOf("endocrinologia", "dm", "hba1c", "totg", "diagnóstico"), citation = "Diretriz ADA 2024 - Diabetes"
            ),
            LibraryContentItem(
                id = "endo_002", title = "Metformina — mecanismo e contraindicações",
                category = "CLINICA_MEDICA", summary = "Reduz gliconeogênese hepática. Contraindicada se TFG <30 mL/min.",
                content = "Metformina: Mecanismo — reduz produção hepática de glicose (gliconeogênese), melhora sensibilidade periférica à insulina (aumenta captação de glicose no músculo e reduz no fígado). Dose inicial: 500-850 mg 1x/dia com refeição, titular até 2.000 mg/dia (dose máxima 2.550 mg). Contraindicações: TFG <30 mL/min (risco de acidose lática). Se TFG 30-45: reduzir 50% e monitorizar. Insuficiência hepática, ICC descompensada, alcoolismo grave, hipóxia tecidual. Efeitos colaterais: TGI (náusea, diarreia — tolerância gradual, forma de liberação prolongada melhora), redução de vitamina B12 (monitorizar periodicamente). Benefício CV: reduz eventos CV e mortalidade. Associada a perda de peso modesta. Primeira escolha para DM2.",
                tags = listOf("endocrinologia", "metformina", "dm2", "gliconeogênese"), citation = "Diretriz ADA 2024 - Diabetes"
            ),
            LibraryContentItem(
                id = "endo_003", title = "Sulfonilureias — riscos e benefícios",
                category = "CLINICA_MEDICA", summary = "Glibenclamida: risco de hipoglicemia e ganho de peso. Segunda linha após metformina.",
                content = "Sulfonilureias (secretagogos): Mecanismo: fecham canais de K+ATP na célula beta pancreática → ↑ secreção de insulina. Representantes: glibenclamida (5-20 mg/dia), glipizida (2,5-20 mg/dia), gliclazida (30-120 mg/dia), glimepirida (1-6 mg/dia). Efeitos colaterais: hipoglicemia (principal — especialmente glibenclamida, maior risco se jejum prolongado, idosos, IRC), ganho de peso (5-7 kg). Posição na terapia: Segunda linha (após metformina). Custo baixo. Alternativa atual: quando possível, preferir iSGLT2 ou arGLP-1 (menor risco de hipoglicemia, benefício CV/renal). Suspender se hipoglicemia frequente. Cuidados: IRC (glipizida e gliclazida são seguras em IRC; glibenclamida é contraindicada se Cr >2 mg/dL).",
                tags = listOf("endocrinologia", "sulfonilureia", "hipoglicemia", "glibenclamida"), citation = "Diretriz ADA 2024 - Diabetes"
            ),
            LibraryContentItem(
                id = "endo_004", title = "Inibidores SGLT2 — benefício cardiovascular e renal",
                category = "CLINICA_MEDICA", summary = "Empagliflozina reduz mortalidade CV e progressão da DRC. Benefício independente de DM.",
                content = "iSGLT2 (gliflozinas): Mecanismo — inibe reabsorção de glicose no túbulo proximal → glicosúria. Exemplos: empagliflozina (10-25 mg/dia), dapagliflozina (5-10 mg/dia), canagliflozina (100-300 mg/dia). Benefícios (além do controle glicêmico): Redução de eventos CV maiores (empagliflozina — EMPA-REG: ↓ IAM 14%, ↓ mortalidade CV 38%). Redução da progressão da DRC (DAPA-CKD, CREDENCE: ↓ 40% de piora da TFG). Perda de peso (2-4 kg). Redução da PA (3-5 mmHg). Redução de hospitalização por IC (30-35%). Efeitos colaterais: ITU, candidíase genital, normoglicemia, cetoacidose euglicêmica (cuidado em jejum, cirurgia), depleção de volume. Indicações de preferência: IC (FE reduzida ou preservada), DRC, alto risco CV. Contraindicações: TFG <20-25 (alguns permitem até 30).",
                tags = listOf("endocrinologia", "sglt2", "empagliflozina", "cardio-renal"), citation = "Diretriz ADA 2024 - Diabetes"
            ),
            LibraryContentItem(
                id = "endo_005", title = "Análogos de GLP-1 — perda de peso e proteção CV",
                category = "CLINICA_MEDICA", summary = "Semaglutida (Ozempic, Wegovy), liraglutida (Victoza, Saxenda) — perda de peso + proteção CV.",
                content = "arGLP-1: Mecanismo — ligam-se ao receptor GLP-1 (incretina): ↑ secreção de insulina dependente de glicose, ↓ glucagon, retardo do esvaziamento gástrico, ↑ saciedade central. Semaglutida: SC 0,5-1 mg/semana (Ozempic) — perda de peso 5-10%; dose maior (2,4 mg/semana — Wegovy) → perda de peso 15% para obesidade. Liraglutida: SC 0,6-1,8 mg/dia (Victoza), dose de 3 mg/dia (Saxenda) para obesidade. Efeitos colaterais: TGI (náusea, vômito — titulação lenta, reduz), risco de pancreatite (raro). Benefícios CV: Liraglutida (LEADER: ↓ 13% eventos), semaglutida (SUSTAIN-6: ↓ 26% eventos). Tiroidite: contraindicação relativa (história de CA medular de tireoide, MEN2). Posição na terapia: preferir em DM2 com alto risco CV ou obesidade.",
                tags = listOf("endocrinologia", "glp-1", "semaglutida", "liraglutida", "obesidade"), citation = "Diretriz ADA 2024 - Diabetes"
            ),
            LibraryContentItem(
                id = "endo_006", title = "Insulina basal — dose inicial e tipos",
                category = "CLINICA_MEDICA", summary = "NPH ou glargina como insulina basal. Dose inicial 0,1-0,2 UI/kg/dia.",
                content = "Insulinoterapia: Insulina basal (cobre necessidades basais, meia-vida longa). Tipos: NPH (ação intermediária 12-16h) — dose 2x/dia, pico 4-8h, maior risco de hipoglicemia noturna. Glargina (Lantus, Toujeo) — ação ultra longa 20-24h, 1x/dia, sem pico. Detemir (Levemir) — ação 12-20h, 1-2x/dia. Degludeca (Tresiba) — ação >42h, 1x/dia, menos hipoglicemia. Dose inicial: 0,1-0,2 UI/kg/dia (10 UI em pacientes com DM2 mal controlado). Titulação: ajustar 2-4 UI a cada 3-7 dias até glicemia jejum alvo (80-130 mg/dL). Hipo noturna: reduzir dose 10-20%. Iniciar insulina quando HbA1c >9% ou sintomas catabólicos (perda de peso) ou falha de 2-3 orais.",
                tags = listOf("endocrinologia", "insulina", "basal", "glargina", "nph"), citation = "Diretriz ADA 2024 - Diabetes"
            ),
            LibraryContentItem(
                id = "endo_007", title = "Hipoglicemia — definição e tratamento",
                category = "CLINICA_MEDICA", summary = "Glicemia <70 mg/dL. Tratar com 15g de carboidrato simples (regra dos 15).",
                content = "Hipoglicemia: Definição: glicemia <70 mg/dL. Níveis: <54 = clinicamente significativa, <40 = grave. Sintomas adrenérgicos (sudorese, tremor, palpitações, fome) e neuroglicopênicos (confusão, sonolência, fala pastosa, convulsão, coma). Regra dos 15: ingerir 15g de carboidrato simples, reavaliar glicemia em 15 min. Opções: 150 mL de suco de laranja, 4-5 balas de glicose, 1 colher de sopa de açúcar, 3-4 comprimidos de glicose (15g), 1/2 copo de refrigerante. Se glicemia <70 em jejum ou refratário: 1 ampola de G50% IV (ou glucagon 1mg SC/IM se sem acesso IV). Após correção, fazer refeição (proteína + carboidrato complexo). Recorrente: rever regime insulínico, alimentação, exercício. Alvo de glicemia para evitar hipoglicemia: >90 mg/dL.",
                tags = listOf("endocrinologia", "hipoglicemia", "15g", "glucagon"), citation = "Diretriz ADA 2024 - Diabetes"
            ),
            LibraryContentItem(
                id = "endo_008", title = "Cetoacidose diabética — correção de potássio",
                category = "CLINICA_MEDICA", summary = "Atenção ao potássio: insulina drive K+ para dentro da célula → hipocalemia.",
                content = "Cetoacidose diabética (CAD): Manejo de potássio: CAD causa depleção total de K+ (perda urinária por glicosúria e vômitos), mas glicemia alta mantém K+ no soro normal. Ao iniciar insulina (↓glicemia) + hidratação (↑volume), o K+ sérico cai rapidamente (diluição + drive insulínico para dentro da célula). Protocolo: Se K+ <3,3 mEq/L: REPOR primeiro (20-40 mEq/L de solução IV), NÃO iniciar insulina até K+ >3,3 (risco de arritmia cardíaca fatal). Se K+ 3,3-5,2: repor 20-30 mEq/L de solução para manter K+ 4-5. Se K+ >5,2: não repor, monitorizar a cada 2h. Meta: K+ 4-5 mEq/L. Bicarbonato: controverso, só se pH <6,9. Déficit hídrico: 5-8% do peso. Restaurar com SF 0,9% (primeira hora 15-20 mL/kg). Insulina: regular IV 0,1 UI/kg bolus + 0,1 UI/kg/h.",
                tags = listOf("endocrinologia", "cad", "cetoacidose", "potássio", "insulina"), citation = "Diretriz ADA 2024 - CAD"
            ),
            LibraryContentItem(
                id = "endo_009", title = "Estado hiperglicêmico hiperosmolar (EHH)",
                category = "CLINICA_MEDICA", summary = "Hiperglicemia + desidratação + hiperosmolalidade sem cetoacidose significativa.",
                content = "EHH (Estado Hiperglicêmico Hiperosmolar): Características: glicemia >600 mg/dL (média 800-1.200), osmolalidade >320 mOsm/kg, sem cetonúria/cetonemia significativa. Desidratação grave (perda >9L), pH >7,3, HCO3 >15. Fisiopatologia: insulinopenia parcial (diferencia da CAD) → hiperglicemia intensa → diurese osmótica → desidratação → redução da TFG → hiperglicemia progressiva. Fator desencadeante: infecção, AVC, IAM, suspensão de medicação. Tratamento: hidratação é a base (SF 0,9% 15-20 mL/kg na 1ª hora). Repor 50% do déficit em 12h, restante nas 24h seguintes. Insulina: iniciar apenas depois de hidratação adequada (0,05 UI/kg/h IV). Correção de K+ e sódio (Na corrigido = Na medido + 1,6 × (glicemia-100)/100). Metas: ↓ glicemia 50-70 mg/dL/h, osmolalidade ↓ 3-5 mOsm/kg/h. Complicações: trombose, rabdomiólise.",
                tags = listOf("endocrinologia", "ehh", "hiperglicemia", "hiperosmolar"), citation = "Diretriz ADA 2024 - EHH"
            ),
            LibraryContentItem(
                id = "endo_010", title = "Síndrome metabólica — critérios diagnósticos",
                category = "CLINICA_MEDICA", summary = "≥3 critérios: CA ≥94-102cm, TG ≥150, HDL <40/50, PA ≥130/85, glicemia ≥100.",
                content = "Síndrome metabólica (IDF 2005): ≥3 dos 5: 1) Circunferência abdominal: ≥94 cm (homens) ou ≥80 cm (mulheres) — caucasianos; ≥90 cm (homens asiáticos) ou ≥80 cm (mulheres). 2) Triglicerídeos ≥150 mg/dL. 3) HDL-C <40 mg/dL (homens) ou <50 mg/dL (mulheres). 4) PA ≥130/85 mmHg. 5) Glicemia jejum ≥100 mg/dL (ou DM2 pré-existente). Fisiopatologia: resistência insulínica + inflamação crônica de baixo grau → ↑ risco de DM2 (5x), AVC (2-3x), IAM (2-4x). Tratamento: perda de peso (5-10% já melhora todos os componentes), dieta (reduzir carboidratos refinados, aumento de fibras), exercício (150 min/semana), controle individual de cada fator (fibratos para TG, estatina para LDL, IECA para PA). Rastreio: todo paciente com obesidade central ou DM.",
                tags = listOf("endocrinologia", "síndrome metabólica", "obesidade", "cintura"), citation = "Diretriz IDF 2023 - Síndrome Metabólica"
            ),
            LibraryContentItem(
                id = "endo_011", title = "Obesidade — classificação pelo IMC",
                category = "CLINICA_MEDICA", summary = "IMC 25-29,9 = sobrepeso. 30-34,9 = obesidade I. 35-39,9 = II. ≥40 = III.",
                content = "Obesidade: classificação IMC: Normal: 18,5-24,9 kg/m². Sobrepeso: 25-29,9 kg/m². Obesidade Grau I: 30-34,9. Grau II: 35-39,9. Grau III: ≥40 (obesidade mórbida). Mas IMC não é suficiente: distribuição de gordura importa (gordura visceral → maior risco CV). Circunferência abdominal >88 cm (mulheres) ou >102 cm (homens) indica risco aumentado. Tratamento: IMC ≥25 + comorbidades: mudança de estilo de vida + farmacoterapia (como orlistate, liraglutida 3 mg, semaglutida 2,4 mg, fentermina, bupropiona/naltrexona). IMC ≥35 + comorbidade (DM2, SAOS, HAS refratária): considerarcirurgia bariátrica (Bypass gástrico, Sleeve). Meta de perda: 5-10% já melhora metabólica significativa. Reganho de peso é comum (manutenção é o maior desafio).",
                tags = listOf("endocrinologia", "imc", "obesidade", "bariátrica"), citation = "Diretriz ABESO 2023 - Obesidade"
            ),
            LibraryContentItem(
                id = "endo_012", title = "Dislipidemia — classificação e metas",
                category = "CLINICA_MEDICA", summary = "LDL <100 mg/dL na prevenção primária, <70 na secundária, <50 se muito alto risco.",
                content = "Dislipidemia: Metas de LDL (Diretriz Europeia 2023): Risco muito alto (DM + lesão órgão, doença CV estabelecida, IC, FA): meta LDL <55 mg/dL (ou redução ≥50%). Alto risco (DM >40 anos, HAS + 3 FR): LDL <70. Moderado (DM <40 anos sem fatores, HAS com FR): LDL <100. Baixo (SCORE <1%): LDL <116. Medicamentos: estatina (primeira linha — atorvastatina 10-80 mg, rosuvastatina 5-40 mg) reduz LDL 30-50%. Ezetimiba 10 mg/dia: reduz LDL adicional 20%, se associada à estatina. Inibidores PCSK9 (evolocumabe, alirocumabe): redução adicional 60%. Triglicerídeos: >500 mg/dL: risco de pancreatite. Fibrato (bezafibrato, fenofibrato), ômega-3 (4g/dia). Estilo de vida: dieta mediterrânea, exercício, controle de DM, perda de peso.",
                tags = listOf("endocrinologia", "dislipidemia", "ldl", "estatina"), citation = "Diretriz ESC 2023 - Dislipidemias"
            ),
            LibraryContentItem(
                id = "endo_013", title = "Tireoidite de Hashimoto — diagnóstico e tratamento",
                category = "CLINICA_MEDICA", summary = "Hipotireoidismo autoimune. Anti-TPO elevado + TSH elevado + T4 livre baixo.",
                content = "Tireoidite de Hashimoto (doença autoimune): Causa mais comum de hipotireoidismo. Patologia: infiltração linfocítica da tireoide, anticorpos contra tireoglobulina (anti-TG) e tireoperoxidase (anti-TPO). Triagem: TSH + T4 livre + anti-TPO. Diagnóstico: TSH elevado + T4 livre baixo (hipotireoidismo primário) + anti-TPO positivo (autoimune). Clínica: fadiga, ganho de peso, constipação, frio, pele seca, queda de cabelo, mixedema, depressão, bócio (presente em 70%). Tratamento: levotiroxina (L-T4) — reposição hormonal. Dose: 1,6-1,8 mcg/kg/dia (50-200 mcg/dia). Iniciar em 25-50 mcg/dia e titular lentamente (a cada 2-3 meses). Meta: TSH 0,5-2,5 mUI/mL. Nos idosos: iniciar com 12,5-25 mcg/dia, titular lentamente. Tratamento para toda a vida. Grávidas: aumentar dose 30-50%.",
                tags = listOf("endocrinologia", "hashimoto", "hipotireoidismo", "tsh", "anti-tpo"), citation = "Diretriz SBEM 2024 - Tireoide"
            ),
            LibraryContentItem(
                id = "endo_014", title = "Hipertireoidismo — causas e tratamento",
                category = "CLINICA_MEDICA", summary = "Doença de Basedow-Graves: anti-TRAb elevado. Tratamento com tiazol ou iodo-131.",
                content = "Hipertireoidismo: Causas: Doença de Basedow-Graves (70-80% — autoimune, anti-TRAb, bócio difuso, exoftalmia, dermopatia). Bócio nodular tóxico (nódulo autônomo). Tireoidite subaguda (Dor) — fase transitória. Tireoidite linfocítica (Silenciosa). Tireoidite pós-parto. Excesso de levotiroxina (iatrogênico). Diagnóstico: TSH <0,01 + T4 livre ↑/T3 ↑. Se Basedow: anticorpo anti-TRAb positivo, cintilografia ↑captação. Tratamento: 1) Tiazóis: metimazol (Tapazol) 10-40 mg/dia ou propiltiouracil (PTU 100-300 mg/dia) — inibem a síntese de hormônio. PTU é alternativa durante gestação (1º trimestre). Efeitos colaterais: agranulocitose (febre, faringite — suspender e colher hemograma). 2) Iodo-131 (radioiodoterapia): destrói a tireoide, indicado se falha/não aderência a tiazol. 3) Tireoidectomia: bócio grande, suspeita de câncer, gestação.",
                tags = listOf("endocrinologia", "basegow", "hipertireoidismo", "metimazol"), citation = "Diretriz ATA 2022 - Hipertireoidismo"
            ),
            LibraryContentItem(
                id = "endo_015", title = "Nódulo tireoidiano — avaliação com USG e PAAF",
                category = "CLINICA_MEDICA", summary = "Classificação ACR TI-RADS guia risco de malignidade e necessidade de PAAF.",
                content = "Nódulo tireoidiano: Epidemiologia: muito comum (50% em >60 anos), 5-15% são malignos. Avaliação: USG com classificação TI-RADS (ACR). Critérios: composição (cístico 0, misto 1, sólido 2), ecogenicidade (hiper/anecoico 0, hipoecoico 1, muito hipoecoico 3), formato (mais largo que alto: 0, mais alto que largo: 3), margens (lisas 0, indefinidas 0, lobuladas/irregulares 2, extensão extra-tireoidiana 3), echogenic foci (ausentes 0, macrocalcificações 1, artifacto cauda de cometa 2, microcalcificações 3). Soma >7 = TR5 (≥50% malignidade — PAAF indicada). >4 = TR4. >1 = TR3. PAAF (biópsia por agulha fina): indicada se nódulo >1cm com TR ≥5, >1,5cm com TR ≥4, >2,5cm com TR ≥3. Bethesda classifica resultado: I (não diagnóstico), II (benigno), III (atipia), IV (suspeito de neoplasia folicular), V (suspeito de malignidade), VI (maligno).",
                tags = listOf("endocrinologia", "nódulo", "tireoide", "ti-rads", "paaf"), citation = "Diretriz ACR 2023 - TI-RADS"
            ),
            LibraryContentItem(
                id = "endo_016", title = "Síndrome de Cushing — diagnóstico diferencial",
                category = "CLINICA_MEDICA", summary = "Teste de supressão com dexametasona 1mg noturno é o rastreio inicial.",
                content = "Síndrome de Cushing: Excesso de cortisol. Rastreio: Teste de supressão com dexametasona 1 mg à meia-noite — Cortisol matinal >1,8 mcg/dL sem supressão = anormal. Cortisol livre urinário 24h (≥3 amostras). Cortisol salivar noturno (21-23h). Confirmação: após rastreios anormais → teste de supressão com 2 mg de dexametasona em 48h (Liddle). Causas: ACTH-dependente (80%): Doença de Cushing (adenoma hipofisário — 70%), Síndrome de Cushing ectópica (tumor produtor de ACTH: pulmão, pâncreas, carcinóide — 10%). ACTH-independente (20%): adenoma adrenal, carcinoma adrenal, hiperplasia adrenal bilateral. Tratamento: se adenoma hipofisário → cirurgia transesfenoidal. Se adrenal → adrenalectomia. Se ectópico → tratar tumor. Fármacos: cetoconazol, metirapona, osilodrostat (inibidores da síntese de cortisol).",
                tags = listOf("endocrinologia", "cushing", "cortisol", "dexametasona"), citation = "Diretriz Endocrine Society 2022 - Cushing"
            ),
            LibraryContentItem(
                id = "endo_017", title = "Fármaco de escolha para neuropatia diabética dolorosa",
                category = "CLINICA_MEDICA", summary = "Gabapentina, pregabalina, amitriptilina, duloxetina como primeira linha para dor neuropática.",
                content = "Neuropatia diabética dolorosa: Tratamento da dor neuropática: 1ª linha: Gabapentina 300-3.600 mg/dia (iniciar 300 mg/dia, titular a cada 3-7 dias). Pregabalina (Lyrica) 150-600 mg/dia (iniciar 75 mg 2x/dia, titular). Amitriptilina 25-100 mg/dia (noturna, cuidado em idosos — sedação, glaucoma, retenção urinária). Duloxetina (Cymbalta) 30-60 mg/dia. Venlafaxina (37,5-225 mg). 2ª linha: Tramadol 50-100 mg 4/4h, tapentadol. Objetivo: redução de 30-50% da dor. Não opioides fortes na dor neuropática. Tratamento adjuvante: controle glicêmico rigoroso (HbA1c <7%), exercício, fisioterapia. Prevenção: rastreio anual de neuropatia com monofilamento e teste de sensibilidade vibratória (diapasão 128 Hz) em diabéticos. Úlcera: exame diário dos pés, calçados adequados, equipe multidisciplinar.",
                tags = listOf("endocrinologia", "neuropatia", "gabapentina", "duloxetina", "pregabalina"), citation = "Diretriz ADA 2024 - Neuropatia Diabética"
            ),
            LibraryContentItem(
                id = "endo_018", title = "Pé diabético — classificação e tratamento",
                category = "CLINICA_MEDICA", summary = "Classificação de Wagner (0-5) para pé diabético. Úlceras infectadas requerem ATB + desbridamento.",
                content = "Pé diabético: Classificação de Wagner: Grau 0 — pele íntegra, mas deformidade/celosidade. Grau 1 — úlcera superficial. Grau 2 — úlcera profunda (até tendão/cápsula). Grau 3 — abscesso, osteomielite. Grau 4 — gangrena de parte do antepé. Grau 5 — gangrena extensa. Abordagem: úlcera superficial (Grau 1-2): desbridamento, curativo, descarga de peso (órtese, cadeira de rodas, bota de contato total), antibioticoterapia (amoxicilina-clavulanato, ciprofloxacino + clindamicina se infecção moderada), cultura de ferida. Grau 3-5: internação, ATB IV, desbridamento cirúrgico, angiografia se doença arterial obstrutiva periférica, amputação se gangrena. Doença arterial periférica: presente em 50% dos pés diabéticos. Avaliar com ITB (índice tornozelo-braço) e Doppler. Prevenção: exame diário dos pés, calçados adequados, hidratação da pele, controle glicêmico.",
                tags = listOf("endocrinologia", "pé diabético", "wagner", "úlcera", "amputação"), citation = "Diretriz IWGDF 2023 - Pé Diabético"
            ),
            LibraryContentItem(
                id = "endo_019", title = "Hipoglicemia no DM2 — prevenção e tratamento",
                category = "CLINICA_MEDICA", summary = "Evitar hipoglicemia em DM2: usar SGLT2/GLP-1 em vez de sulfonilureia. Alvo HbA1c individualizado.",
                content = "Prevenção de hipoglicemia no DM2: Fatores de risco: idosos, IRC, polifarmácia, sulfonilureias (glibenclamida), insulina, jejum prolongado, perda de peso rápida, doença hepática. Medidas de prevenção: 1) Preferir drogas com baixo risco de hipoglicemia (metformina, iSGLT2, arGLP-1, DPP4) sobre sulfonilureias e insulina em idosos. 2) Alvo de HbA1c individualizado: <7% (geral), <8% (idosos com comorbidades), <8,5% (idosos frágeis). 3) Educação do paciente, monitorização (glicemia capilar), CGM (monitor contínuo de glicose) em alto risco. 4) Suspender sulfonilureias ao iniciar insulina. 5) Ajustar dose de insulina se refeições perdidas. Tratamento: 15g carboidrato simples → reavaliar em 15 min → repetir se necessário → glucagon se hipoglicemia grave (SC, IM, nasal) → encaminhar ao PS se refratário.",
                tags = listOf("endocrinologia", "hipoglicemia", "prevenção", "dm2"), citation = "Diretriz ADA 2024 - Diabetes"
            ),
            LibraryContentItem(
                id = "endo_020", title = "Insulinoterapia intensiva no DM1",
                category = "CLINICA_MEDICA", summary = "Múltiplas doses (basal-bolus) ou bomba de insulina para DM1. Ajuste por carboidratos e glicemia.",
                content = "DM1: Insulinoterapia intensiva: Cobertura basal (40-50% da dose total): insulina de ação longa (glargina, degludeca 1x/dia) ou bomba de insulina (infusão subcutânea contínua). Bolus (50-60% da dose total): insulina de ação rápida (lispro, aspart, glulisina) ou ultrarrápida (Fiasp, Lyumjev) às refeições. Contagem de carboidratos: 1 UI para cada 10-15g de carboidrato (ajuste individual). Correção: 1 UI reduz glicemia em 30-50 mg/dL (fator de sensibilidade). Dose total diária: 0,5-0,7 UI/kg/dia. Sistemas híbridos de laço fechado: bomba + CGM (Medtronic 780G, Tandem t:slim X2 + Dexcom) — algoritmo ajusta automaticamente insulina basal. Metas: glicemia 70-180 mg/dL 70% do tempo (Time-in-Range), HbA1c <7%, <4% do tempo em hipoglicemia (<70). Monitor: CGM (Dexcom, Libre) ou glicemia capilar (≥4x/dia).",
                tags = listOf("endocrinologia", "dm1", "insulina", "bomba", "contagem de carboidratos"), citation = "Diretriz ADA 2024 - DM1"
            ),
            LibraryContentItem(
                id = "endo_021", title = "Síndrome dos ovários policísticos (SOP)",
                category = "CLINICA_MEDICA", summary = "Critérios de Rotterdam: 2 de 3: oligo/anovulação + hiperandrogenismo + ovários policísticos no USG.",
                content = "SOP (Síndrome dos Ovários Policísticos): Critérios de Rotterdam (2003): Necessário 2 de 3: 1) Oligoanovulação (ciclos >35 dias ou <8 ciclos/ano); 2) Hiperandrogenismo clínico (hirsutismo, acne, alopecia) ou laboratorial (testosterona total/livre elevada); 3) Ovários policísticos ao USG (≥20 folículos de 2-9mm por ovário OU volume ovariano >10 mL). Excluir outras causas (hiperplasia adrenal congênita, Cushing, tumor adrenal). Metabolismo: 50-70% têm resistência insulínica → ↑ risco de DM2, HAS, dislipidemia. Tratamento: 1) Perda de peso (5-10% melhora ciclos e fertilidade). 2) Anticoncepcionais orais combinados (regulam ciclo e melhoram hirsutismo). 3) Metformina 1.500-2.000 mg/dia se resistência insulínica ou anovulação. 4) Hirsutismo: espironolactona 100 mg/dia (associar ACO), laser, eflornitina tópica. 5) Infertilidade: clomifeno + metformina, ou letrozol (primeira linha para indução de ovulação). Rastreio: síndrome metabólica, DM2, HAS, dislipidemia.",
                tags = listOf("endocrinologia", "sop", "ovário policístico", "hirsutismo", "metformina"), citation = "Diretriz Endocrine Society 2024 - SOP"
            ),
            LibraryContentItem(
                id = "endo_022", title = "Osteoporose — rastreio com DXA e tratamento",
                category = "CLINICA_MEDICA", summary = "DXA com T-score ≤ -2,5 na coluna ou quadril = osteoporose. Bisfosfonatos são primeira linha.",
                content = "Osteoporose: Diagnóstico: DXA (Dual-energy X-ray Absorptiometry) na coluna lombar e quadril. T-score ≤ -2,5 = osteoporose; -1 a -2,5 = osteopenia. Rastreio: mulheres ≥65 anos, homens ≥70 anos, ou >50 anos com fatores de risco (fratura prévia, corticoide crônico, DM, HAS, IRC, tabagismo). FRAX (Fracture Risk Assessment Tool): calcula risco de fratura em 10 anos. Tratamento: Bisfosfonatos: alendronato 70 mg/semana (primeira linha), risedronato 35 mg/semana, zoledronato 5 mg IV anual. Por 3-5 anos (drug holiday após — risco de fratura atípica). Denosumabe (Prolia) 60 mg SC a cada 6 meses — inibidor RANKL. Teriparatida (Forteo) 20 mcg SC/dia — anabólico. Raloxifeno 60 mg/dia (SERM) — apenas na coluna. Cálcio (1.000-1.200 mg/dia) + Vitamina D (800-1.000 UI/dia) como adjuvantes (não previnem fratura isoladamente). Suplementação: sempre garantir, especialmente em idosos institucionalizados e pós-fratura.",
                tags = listOf("endocrinologia", "osteoporose", "dxa", "bisfosfonato", "fratura"), citation = "Diretriz Endocrine Society 2023 - Osteoporose"
            ),
            LibraryContentItem(
                id = "endo_023", title = "Hipercalcemia — causas e tratamento",
                category = "CLINICA_MEDICA", summary = "Hiperparatireoidismo primário (PTH alto) e neoplasia (PTHrp) são as principais causas.",
                content = "Hipercalcemia: Causas: Hiperparatireoidismo primário (HPTP) — PTH elevado, Ca alto, fósforo baixo (adenoma paratireoide). O mais comum ambulatorial. Neoplasia (PTHrp) — CA de pulmão, mama, mieloma, linfoma. Mais comum na emergência. Sarcoidose/granulomatoses (↑ 1,25-D). Imobilização prolongada. Drogas: lítio, tiazídicos. Síndrome leite-álcali. Imobilização. Diagnóstico: PTH (↑ = HPTP), ⟂screening: SPEP (mieloma), PTHrp (se suspeita de neoplasia), 25-OH-D e 1,25-D (sarcoidose). Tratamento: Ca <12 mg/dL assintomático: tratar causa + hidratação oral. >12 sintomático: hidratação com SF 0,9% (3-4 L/dia) + furosemida (após hidratação) + bifosfonato (pamidronato 60-90 mg IV ou zoledronato 4 mg IV) + calcitonina (4 UI/kg SC 12/12h) para efeito rápido. Se Ca >14 ou sintomática grave: diálise. Cirurgia: HPTP se Ca >1 mg/dL acima do normal, TFG <60, osteoporose, idade <50 ou sintomas.",
                tags = listOf("endocrinologia", "hipercalcemia", "pth", "bifosfonato"), citation = "Diretriz Endocrine Society 2023 - Hiperparatireoidismo"
            ),
            LibraryContentItem(
                id = "endo_024", title = "Hipocalcemia — diagnóstico e tratamento",
                category = "CLINICA_MEDICA", summary = "Sinais de Chvostek e Trousseau. Repor cálcio IV ou VO conforme gravidade.",
                content = "Hipocalcemia: Causas: Pós-tireoidectomia (paratireoidectomia iatrogênica) — causa mais comum. Hipoalbuminemia corrigir: Ca corrigido = Ca medido + 0,8 × (4 - albumina). Hipomagnesemia (↓PTH). Deficiência de Vitamina D (↑ PTH). Insuficiência renal (hiperfosfatemia → ↓ Ca). Pancreatite grave. Hipoparatireoidismo autoimune. Sinais: Chvostek (espasmo dos músculos faciais à percussão do nervo facial) e Trousseau (espasmo carpal ao esfigmo >3 min). Parestesias periorais, espasmo muscular, tetania, convulsão, arritmia (QT longo, Torsade de Pointes). Tratamento: Aguda sintomática: Ca gluconato 10% 10-20 mL IV (89-179 mg de Ca elementar) ou CaCl2. Infusão: 1-2 ampolas em 500 mL de SF a 50-100 mL/h. Crônica: Ca oral 500-1.000 mg/dia + calitriol (0,25-1 mcg/dia) + Vitamina D2. Meta: Ca sérico 7,5-8,5 mg/dL (abaixo do normal para evitar hipercalciúria). Monitorizar Ca urinário (alvo <300 mg/24h).",
                tags = listOf("endocrinologia", "hipocalcemia", "chvostek", "trousseau", "cálcio"), citation = "Diretriz Endocrine Society 2023 - Hipoparatireoidismo"
            ),
            LibraryContentItem(
                id = "endo_025", title = "Hiperaldosteronismo primário (Síndrome de Conn)",
                category = "CLINICA_MEDICA", summary = "HAS + hipocalemia + relação aldosterona/renina elevada. Causa tratável de HAS secundária.",
                content = "Hiperaldosteronismo primário (Síndrome de Conn): Excesso de aldosterona pela adrenal → ↑Na, ↓K, ↓renina. Clínica: HAS (difícil controle), hipocalemia (pode ser normocalêmica), alcalose metabólica, fraqueza muscular, palpitações, poliúria. Rastreio: relação aldosterona plasmática (PAC) / atividade de renina plasmática (PRA) ou renina direta (DRC), cortado como >20 (ng/dL)/ng/mL/h, ou >30 com aldosterona >15. Confirmatória: teste de sobrecarga salina, teste de captopril. Subtipos: Adenoma adrenal (40% — Conn) → cirurgia (adrenalectomia); Hiperplasia bilateral (60%) → tratamento clínico com antagonista de MR (espironolactona 100-400 mg/dia ou eplerenona 50-200 mg/dia). Tratamento: espironolactona (ginecomastia, disfunção erétil) ou eplerenona (menos efeitos). Controle da HAS (↓ PA, ↓ K, ↓ proteinúria). Tomografia de adrenal antes da cirurgia. Veia adrenal: cateterismo para diferenciação adenoma vs hiperplasia.",
                tags = listOf("endocrinologia", "hiperaldosteronismo", "conn", "espironolactona"), citation = "Diretriz Endocrine Society 2023 - Hiperaldosteronismo"
            ),
            LibraryContentItem(
                id = "endo_026", title = "Fármaco de escolha para tumor carcinoide",
                category = "CLINICA_MEDICA", summary = "Análogos de somatostatina (octreotida, lanreotida) para controle de sintomas carcinoide.",
                content = "Tumor carcinoide e síndrome carcinoide: Síndrome carcinoide: flush (rubor facial), diarreia, sibilância, dor abdominal, lesões valvares cardíacas direitas (endocardite fibrosa). Ocorre com metástases hepáticas (a serotonina não é inativada no fígado). Tratamento: Análogos de somatostatina: octreotida SC 100-500 mcg 3x/dia ou IM de depósito 20-30 mg a cada 4 semanas (Sandostatin LAR). Lanreotida 60-120 mg a cada 4 sem (Somatuline Autogel). Controla flush e diarreia em 70% dos pacientes. Efeitos colaterais: colelitíase (inapto de contratilidade da vesícula), hiperglicemia leve. Interferon-alfa: alternativa para refratários. Cirurgia: ressecção de tumor primário + metástases hepáticas se possível. Cuidados: evitar biópsia percutânea de metástase hepática (risco de sangramento). Evitar anestesia com adrenalina. Cintilografia com octreotida (Octreoscan) ou PET-DOTATATE para estadiamento.",
                tags = listOf("endocrinologia", "carcinoide", "octreotida", "lanreotida"), citation = "Diretriz ESMO 2023 - Tumores Neuroendócrinos"
            ),
            LibraryContentItem(
                id = "endo_027", title = "Monitorização do TSH e ajuste da levotiroxina",
                category = "CLINICA_MEDICA", summary = "Ajustar dose em 12,5-25 mcg conforme TSH. Aguardar 6-8 semanas entre ajustes.",
                content = "Levotiroxina (L-T4, Synthroid): Monitoramento: TSH 6-8 semanas após cada ajuste (meia-vida da T4 é de 7 dias; novo equilíbrio em 6 semanas). Meta: 0,5-2,5 mUI/mL (geral). <4,5 (idosos com comorbidades). <3 (gravidez — cada trimestre). Ajustes: se TSH >10: ↑25-50 mcg/dia. TSH 4,5-10: ↑12,5-25 mcg/dia. TSH 2,5-4,5: manter (geral). TSH <0,5 (supressão) com T4 normal: ↓12,5-25 mcg/dia. TSH <0,1 + T4 alta (hipertireoidismo iatrogênico): ↓25-50 mcg/dia. Administração: tomar em jejum (30-60 min antes do café), sem cálcio, ferro, soja, fibras. Interações: ferro, carbonato de cálcio, omeprazol, estrógenos, rifampicina, fenitoína. Gravidez: aumentar 30-50% na suspeita (>semanas 4-6). Pós-parto: reduzir para dose pré-gestacional. Tireoidectomia total: dose inicial 1,6-1,8 mcg/kg/dia.",
                tags = listOf("endocrinologia", "levotiroxina", "tsh", "ajuste"), citation = "Diretriz ATA 2024 - Hipotireoidismo"
            ),
            LibraryContentItem(
                id = "endo_028", title = "DM gestacional — rastreio e tratamento",
                category = "CLINICA_MEDICA", summary = "TOTG 75g entre 24-28 semanas. Glicemia jejum ≥92, 1h ≥180, 2h ≥153 mg/dL.",
                content = "DM gestacional (DMG): Rastreio: TOTG 75g entre 24-28 semanas de gestação. Critérios de Carpenter-Coustan/IADPSG: jejum ≥92 mg/dL; 1h ≥180 mg/dL; 2h ≥153 mg/dL. Precoce: se fator de risco, TOTG na primeira consulta (jejum ≥126 = DM manifesto). Tratamento: 1) Dieta (6 refeições/dia, carboidratos 35-45% do total). 2) Metformina: 500-1.500 mg/dia (categoria B na gestação — usada em 50% das mulheres nos EUA). 3) Insulina: basal (NPH) + bolus (regular, lispro, aspart) se glicemia jejum >95 ou pós-prandial 1h >140. Meta: jejum <95, 1h pós <140, 2h pós <120. Monitorizar: glicemia capilar 4-7x/dia. Parto: controle glicêmico peri-parto (infusão de insulina se >140). Pós-parto: suspender medicação, reavaliar com TOTG 75g em 6-12 semanas (70% normaliza). Risco futuro: 50% desenvolvem DM2 em 5-10 anos. Rastreio anual.",
                tags = listOf("endocrinologia", "dm gestacional", "totg", "insulina"), citation = "Diretriz ADA 2024 - DMG"
            ),
            LibraryContentItem(
                id = "endo_029", title = "Diagnóstico de DM1 vs DM2",
                category = "CLINICA_MEDICA", summary = "Autoanticorpos (GAD, IA2, ZnT8) + peptídeo C diferenciam DM1 de DM2.",
                content = "DM1 vs DM2: DM1 (autoimune): destruição de células beta (insulinopenia absoluta). Marcadores: autoanticorpos (GAD65, IA-2, ZnT8, autoanticorpo insulina), peptídeo C baixo (<0,2 nmol/L). DM2 (resistência insulínica): insulinopenia relativa, associada a obesidade, síndrome metabólica. Peptídeo C normal ou alto (>0,6). LADA (DM1 de início tardio em adultos): autoanticorpos + idade >30 + peptídeo C baixo em meses a anos. Tratamento: DM1: insulina desde o diagnóstico (déficit absoluto). DM2: mudança de estilo de vida + metformina → escalonar conforme necessidade. Cetose/ Cetoacidose: típica no DM1 ao diagnóstico, pode ocorrer no DM2 em estresse grave. Idade: DM1 típica em <30 anos, DM2 >40. MAS: 30% dos casos de DM1 são diagnosticados >30 anos (LADA). Fatores de risco: DM2 tem história familiar forte (pais + irmão = 50%).",
                tags = listOf("endocrinologia", "dm1", "dm2", "autoanticorpos", "peptídeo c"), citation = "Diretriz ADA 2024 - Diagnóstico de DM"
            ),
            LibraryContentItem(
                id = "endo_030", title = "Tratamento da DM2 com base no risco CV",
                category = "CLINICA_MEDICA", summary = "Se alto risco CV: iSGLT2 ou arGLP-1 independente de HbA1c. Metformina é 1ª linha geral.",
                content = "DM2 — individualização da terapia: Sem doença CV ou DRC: metformina (1ª linha) + mudança de estilo de vida. Com doença CV estabelecida (IAM, AVC, IC, DAP): adicionar arGLP-1 (semaglutida, liraglutida) ou iSGLT2 (empagliflozina, dapagliflozina) independente de HbA1c — reduzem eventos CV maiores. Com DRC (TFG 25-60, albuminúria): iSGLT2 reduz progressão da DRC (benefício máximo se TFG 25-45). ArGLP-1 também reduz progressão (segunda linha). Com IC (FE <40%): iSGLT2 reduz hospitalização por IC. Com obesidade (IMC >30): arGLP-1 promove perda de peso. Meta glicêmica: HbA1c <7% (geral), <8% (idosos/comorbidades), <6,5% (jovens, expectativa de vida longa, sem hipoglicemia). Se insulina necessária: metformina + iSGLT2 + GLP-1 + insulina basal. Alvo de TIR (Time-in-Range): >70% (70-180 mg/dL).",
                tags = listOf("endocrinologia", "dm2", "risco cv", "sglt2", "glp-1"), citation = "Diretriz ADA 2024 - Abordagem Farmacológica"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // LISTA DE TODOS OS PACOTES
    // ═══════════════════════════════════════════════════════════════

    /** Todos os pacotes de clínica médica disponíveis para importação. */
    val allPacks: List<LibraryContentPack> by lazy {
        listOf(
            cardiologiaPack,
            pneumologiaPack,
            gastroPack,
            nefrologiaPack,
            endocrinoPack,
        )
    }

    /** Novos pacotes de especialidades: Neurologia, Psiquiatria, Hemato/Onco, Infecto. */
    val specialtyPacks: List<LibraryContentPack> by lazy { SpecialtyMedicinePacks.allPacks }

    /** Pacotes de emergência, pediatria, ginecologia/obstetrícia. */
    val emergencyPacks: List<LibraryContentPack> by lazy { EmergencyPacks.allPacks }

    /** Pacote de farmacologia clínica. */
    val pharmacologyPacks: List<LibraryContentPack> by lazy { PharmacologyPacks.allPacks }
}
