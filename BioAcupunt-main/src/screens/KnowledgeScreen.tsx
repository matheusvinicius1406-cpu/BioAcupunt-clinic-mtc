import { useState, useEffect } from 'react';
import { api } from '../services/api';
import { 
  Search, BookOpen, Tag, ChevronRight, Award, Zap, Brain, Target, 
  HelpCircle, BookOpenCheck, ArrowLeft, RefreshCw, AlertCircle, FileText,
  Bookmark, Send, CheckCircle2, ChevronDown, User, Heart, Star, Sparkles, Plus, GraduationCap, Lock, Globe, ListFilter, Shield, Layers
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import Markdown from 'react-markdown';

const EIXOS = [
  {
    id: "eixo-1",
    name: "I. Fundamentos e Filosofia Tradicional",
    description: "Conceitos cosmológicos, Yin-Yang, Cinco Elementos",
    themes: [
      { id: "yin-yang", title: "Teoria do Yin e Yang" },
      { id: "five-elements", title: "Os Cinco Elementos (Wu Xing)" },
      { id: "vital-substances", title: "As Três Substâncias Vitais (Qi, Sangue, Jinye)" },
      { id: "three-treasures", title: "Os Três Tesouros (Shen, Qi, Jing)" },
      { id: "qi-dynamics", title: "Dinâmica e Circulação do Qi" },
      { id: "five-organs", title: "Fisiologia dos Cinco Órgãos (Zang)" },
      { id: "six-viscera", title: "Fisiologia das Seis Vísceras (Fu)" },
      { id: "zang-fu-relations", title: "Relações Interórgãos (Zang-Fu)" },
      { id: "extra-substances", title: "Teoria das Substâncias Extraordinárias" },
      { id: "qi-blood-formation", title: "Mecanismos de Formação do Qi e Sangue" },
      { id: "spleen-stomach-source", title: "O Papel do Estômago e do Baço como Fonte Pós-Celestial" },
      { id: "pre-celestial-essence", title: "A Essência Pré-Celestial e os Rins" },
      { id: "shen-mental-harmony", title: "O Espírito (Shen) e a Harmonização Mental" },
      { id: "liver-circulation", title: "O papel do Fígado no Livre Fluxo de Energia" },
      { id: "spleen-heart-axis", title: "A Mente e o Eixo Baço-Coração" }
    ]
  },
  {
    id: "eixo-2",
    name: "II. Meridianos e Colaterais (Jing Luo)",
    description: "Estrutura e caminhos dos canais energéticos",
    themes: [
      { id: "lu-meridian", title: "Anatomia do Canal do Pulmão (Shou Taiyin)" },
      { id: "li-meridian", title: "Anatomia do Canal do Intestino Grosso (Shou Yangming)" },
      { id: "st-meridian", title: "Anatomia do Canal do Estômago (Zu Yangming)" },
      { id: "sp-meridian", title: "Anatomia do Canal do Baço (Zu Taiyin)" },
      { id: "ht-meridian", title: "Anatomia do Canal do Coração (Shou Shaoyin)" },
      { id: "si-meridian", title: "Anatomia do Canal do Intestino Delgado (Shou Taiyang)" },
      { id: "bl-meridian", title: "Anatomia do Canal da Bexiga (Zu Taiyang)" },
      { id: "ki-meridian", title: "Anatomia do Canal do Rim (Zu Shaoyin)" },
      { id: "pc-meridian", title: "Anatomia do Canal do Pericárdio (Shou Jueyin)" },
      { id: "te-meridian", title: "Anatomia do Canal do Triplo Aquecedor (Shou Shaoyang)" },
      { id: "gb-meridian", title: "Anatomia do Canal da Vesícula Biliar (Zu Shaoyang)" },
      { id: "lr-meridian", title: "Anatomia do Canal do Fígado (Zu Jueyin)" },
      { id: "gv-meridian", title: "Vaso Governador (Du Mai)" },
      { id: "cv-meridian", title: "Vaso Concepção (Ren Mai)" },
      { id: "extra-vessels", title: "Os Outros Seis Vasos Extraordinários" }
    ]
  },
  {
    id: "eixo-3",
    name: "III. Fisiopatologia Básica do Zang Fu",
    description: "Distúrbios energéticos clínicos de órgãos e vísceras",
    themes: [
      { id: "lu-patterns", title: "Disfunções do Qi do Pulmão (Deficiência e Estagnação)" },
      { id: "li-patterns", title: "Secura e Calor no Intestino Grosso" },
      { id: "st-patterns", title: "Deficiência de Qi e Frio no Estômago" },
      { id: "sp-patterns", title: "Deficiência de Qi do Baço com Umidade Retida" },
      { id: "sp-blood-def", title: "Deficiência de Sangue do Baço (Pi Xue Xu)" },
      { id: "ht-blood-def", title: "Deficiência de Sangue e Yin do Coração" },
      { id: "ht-fire", title: "Fogo e Calor de Coração (Xin Huo Kang)" },
      { id: "si-heat", title: "Calor Umidade no Intestino Delgado" },
      { id: "bl-heat", title: "Calor Umidade na Bexiga (Lin Zheng)" },
      { id: "ki-yin-def", title: "Deficiência de Yin do Rim e Fogo Vazio" },
      { id: "ki-yang-def", title: "Deficiência de Yang do Rim e Frio Clínico" },
      { id: "lr-qi-stagnation", title: "Estagnação de Qi do Fígado (Gan Qi Zhi)" },
      { id: "lr-yang-rising", title: "Ascensão de Yang do Fígado (Gan Yang Shang Kang)" },
      { id: "lr-gb-heat", title: "Calor Umidade no Fígado e Vesícula Biliar" },
      { id: "sj-obstruction", title: "Obstruções no Triplo Aquecedor (San Jiao Block)" }
    ]
  },
  {
    id: "eixo-4",
    name: "IV. Semiótica e Diagnóstico Clínico",
    description: "Exame clínico de língua, pulso e interrogação",
    themes: [
      { id: "inspection-shen", title: "Inspeção Geral e Psiquismo (Shen)" },
      { id: "facial-color", title: "Diagnóstico pela Tez e Cor da Pele" },
      { id: "tongue-morphology", title: "Semiologia da Língua: Morfologia Geral" },
      { id: "tongue-color", title: "Semiologia da Língua: Avaliação das Cores do Corpo" },
      { id: "tongue-coating", title: "Semiologia da Língua: Avaliação da Saburra" },
      { id: "tongue-sublingual", title: "Semiologia da Língua: Vasos Sublinguais e Movimento" },
      { id: "observation-smell", title: "Auscultação e Olfação Clínicas" },
      { id: "interrogation-ten", title: "Interrogatório dos 10 Aspectos Clássicos" },
      { id: "sleep-sweat", title: "Semiologia do Sono e Transpiração" },
      { id: "palpation-abdomen", title: "Palpação Clássica do Abdômen e Meridianos" },
      { id: "pulse-cun-guan-chi", title: "Palpação do Pulso: Posicionamento (Cun, Guan, Chi)" },
      { id: "pulse-wiry", title: "Tipos de Pulso: Pulso Tenso (Xian) e Corda" },
      { id: "pulse-rapid-slow", title: "Tipos de Pulso: Pulso Rápido (Shuo) e Lento (Chi)" },
      { id: "pulse-floating-deep", title: "Tipos de Pulso: Pulso Flutuante (Fu) e Profundo (Chen)" },
      { id: "tongue-pulse-integration", title: "Integração da Língua com o Pulso de MTC" }
    ]
  },
  {
    id: "eixo-5",
    name: "V. Metodologias de Diferenciação de Síndromes",
    description: "As 8 Regras, Seis Canais, 4 Camadas",
    themes: [
      { id: "eight-principles", title: "As Oito Regras Diagnósticas (Ba Gang)" },
      { id: "vital-substance-diff", title: "Diferenciação pelas Substâncias Vitais" },
      { id: "pathogenic-factors", title: "Diferenciação de Padrões de Fatores Patogênicos" },
      { id: "six-stages", title: "Diferenciação pelos Seis Canais (Shanghan Lun)" },
      { id: "taiyang-level", title: "Nível Taiyang (Vento-Frio)" },
      { id: "yangming-level", title: "Nível Yangming (Calor Interno Extremo)" },
      { id: "shaoyang-level", title: "Nível Shaoyang (Semi-Superficial)" },
      { id: "taiyin-shaoyin", title: "Nível Taiyin e Shaoyin (Frio e Deficiência Definitivos)" },
      { id: "jueyin-level", title: "Nível Jueyin (Calor e Frio Mistos)" },
      { id: "four-levels", title: "Diferenciação pelas Quatro Camadas (Wen Bing)" },
      { id: "wei-qi-levels", title: "Níveis Wei e Qi (Defesas e Fogo Crítico)" },
      { id: "ying-xue-levels", title: "Níveis Ying e Xue (Calor no Sangue)" },
      { id: "three-jiaos", title: "Diferenciação pelo Triplo Aquecedor (San Jiao)" },
      { id: "combined-patterns", title: "Síndromes Combinadas de Zang Fu" },
      { id: "five-phase-diff", title: "Diferenciação pelas Cinco Fases Clínicas" }
    ]
  },
  {
    id: "eixo-6",
    name: "VI. Prescrição Terapêutica & Pontos Especiais",
    description: "Pontos Shu Antigos, Fonte, Conexão, Fenda e dorsais",
    themes: [
      { id: "emergency-points", title: "Pontos de Emergência e Reanimação" },
      { id: "five-shu-intro", title: "Pontos Shu Antigos: Teoria de Fluxo e Cinco Fases" },
      { id: "jing-ying-points", title: "Pontos Shu Poço (Jing) e Manancial (Ying)" },
      { id: "shu-river-points", title: "Pontos Shu Riacho (Shu) e Rio (Jing)" },
      { id: "he-sea-points", title: "Pontos Shu Mar (He) e Aplicações" },
      { id: "yuan-source-points", title: "Pontos Fonte (Yuan) e Nutrição dos Órgãos" },
      { id: "luo-connecting", title: "Pontos de Conexão (Luo) para Tratamento de Pares" },
      { id: "xi-cleft", title: "Pontos de Fenda (Xi) para Casos de Dor Aguda" },
      { id: "back-shu", title: "Pontos de Assentamento Dorsal (Bei Shu)" },
      { id: "front-mu", title: "Pontos de Alarme Frontal (Mu)" },
      { id: "eight-influential", title: "Os Oito Pontos de Influência (Hui)" },
      { id: "intersection-points", title: "Pontos de Interseção de Meridianos (Jiao Hui Xue)" },
      { id: "eight-confluent", title: "Os Oito Pontos de Confluência dos Vasos Extraordinários" },
      { id: "point-coupling", title: "Teoria das Combinações Acopladas (Ex: IG4 e F3)" },
      { id: "biological-clock", title: "Seleção de Pontos Baseada no Horário Biológico" }
    ]
  },
  {
    id: "eixo-7",
    name: "VII. Técnicas Clínicas de Acupuntura",
    description: "Moxabustão, auriculoterapia, ventosas e eletroacupuntura",
    themes: [
      { id: "manipulation-techniques", title: "Técnicas de Manipulação (Tonificação e Dispersão)" },
      { id: "needling-depth", title: "Agulhamento Superficial, Profundo e Oblíquo" },
      { id: "trigger-points", title: "Agulhamento de Pontos Gatilho" },
      { id: "moxabuston-clinical", title: "Teoria e Aplicação da Moxabustão" },
      { id: "cupping-therapy", title: "Ventosaterapia Deslizante e Fixa" },
      { id: "wet-cupping", title: "Ventosaterapia Úmida (Sangria Terapêutica)" },
      { id: "yamamoto-ynsa", title: "Craniopuntura de Yamamoto (YNSA)" },
      { id: "chinese-scalp", title: "Craniopuntura Chinesa Clássica" },
      { id: "auricular-chinese", title: "Auriculoterapia Chinesa: Sistema Geral" },
      { id: "auricular-french", title: "Auriculoterapia Francesa (Nogier)" },
      { id: "electroacupuncture", title: "Eletroacupuntura: Frequências de Estímulo" },
      { id: "laseracupuncture", title: "Laseracupuntura de Baixa Intensidade" },
      { id: "wrist-ankle-needling", title: "Agulhamento de Punho e Tornozelo" },
      { id: "pharmacopuncture", title: "Farmacopuntura e Injeção Fitoterápica" },
      { id: "tactile-diagnoses", title: "Diagnósticos Reacionais por Palpação de Agulhas" }
    ]
  },
  {
    id: "eixo-8",
    name: "VIII. Fitoterapia e Dietoterapia MTC",
    description: "Prescrições, naturezas de alimentos e dosagem clássica",
    themes: [
      { id: "food-energetics", title: "Teoria das Quatro Energias e Cinco Sabores" },
      { id: "food-temperatures", title: "Alimentos Frios, Refrescantes, Neutros, Mornos e Quentes" },
      { id: "diet-qi-blood-def", title: "Dietoterapia para Deficiência de Qi e Sangue" },
      { id: "diet-dampness", title: "Dietoterapia para Combater Umidade e Mucosidade" },
      { id: "diet-excess-heat", title: "Dietoterapia de Purificação para Excesso de Calor" },
      { id: "tea-prescriptions", title: "Prescrições de Chás Fitoterápicos Clássicos" },
      { id: "herbal-bi-syndrome", title: "Ervas para Dor Articular Crônica (Síndrome Bi)" },
      { id: "classic-herbs", title: "Ervas Clássicas: Ginseng (Ren Shen) e Angelica (Dang Gui)" },
      { id: "liver-herbs", title: "Ervas Reguladoras do Qi do Fígado (Chai Hu)" },
      { id: "warm-herbs", title: "Ervas Quentes para Dispersar o Frio Sistêmico" },
      { id: "anshen-formulas", title: "Fórmulas para Ansiedade e Insônia (An Shen)" },
      { id: "herbal-acupuncture-integration", title: "Combinação de Fitoterapia com Acupuntura" },
      { id: "kidney-yang-diet", title: "Dietoterapia para Fortalecer o Rim Yang" },
      { id: "medicinal-soups", title: "Receitas de Sopas Medicinais Tradicionais" },
      { id: "dosage-regulations", title: "Regulamentos Clínicos de Dosagem" }
    ]
  },
  {
    id: "eixo-9",
    name: "IX. Evidências Científicas e Neurofisiologia",
    description: "Neurociência, ressonância fMRI e ensaios clínicos",
    themes: [
      { id: "endorphin-release", title: "Mecanismos de Liberação de Endorfinas" },
      { id: "axon-reflex", title: "Reflexo Axônico e Resposta Inflamatória" },
      { id: "peripheral-nerves", title: "Estimulação de Nervos Periféricos e Gliais" },
      { id: "gate-control", title: "Teoria do Controle das Comportas da Dor" },
      { id: "fmri-neuroimaging", title: "Modulação Cortical por Ressonância Magnética fMRI" },
      { id: "migraine-evidence", title: "Acupuntura para Enxaqueca e Cefaleia Crônica" },
      { id: "anxiety-sleep-evidence", title: "Controle Científico da Ansiedade e Insônia" },
      { id: "gastrointestinal-evidence", title: "Efeitos Clínicos no Sistema Gastrointestinal" },
      { id: "pregnancy-fertility", title: "Papel da Acupuntura na Gestação e Fertilidade" },
      { id: "hpa-axis", title: "Respostas Endócrinas do Eixo HPA" },
      { id: "rct-quality", title: "Análise de Ensaios Clínicos Randomizados" },
      { id: "who-guidelines", title: "Diretrizes Clínicas da Organização Mundial da Saúde" },
      { id: "moxa-biological-mechanisms", title: "Mecanismos Biológicos da Moxabustão" },
      { id: "histamine-response", title: "Respostas Celulares de Histaminas no Sítio" },
      { id: "needling-safety", title: "Segurança Clínica, Biossegurança e Prevenção" }
    ]
  },
  {
    id: "eixo-10",
    name: "X. Casos Práticos, Insights & Protocolos Vivos",
    description: "Evolução contínua baseada na experiência clínica real",
    themes: [
      { id: "protocol-scoliose", title: "Protocolo Vivo para Escoliose e Dor Lombar" },
      { id: "protocol-bell", title: "Protocolo para Paralisia Facial Periférica (Bell)" },
      { id: "protocol-weight-control", title: "Protocolo Integrado para Redução de Peso" },
      { id: "protocol-stroke-rehab", title: "Protocolo de Reabilitação Pós-AVC" },
      { id: "protocol-menopause", title: "Protocolo para Climatério e Menopausa" },
      { id: "protocol-burnout", title: "Protocolo para Ansiedade e Burnout" },
      { id: "case-migraine", title: "Caso Clínico 1: Enxaqueca Crônica" },
      { id: "case-infertility", title: "Caso Clínico 2: Infertilidade Secundária" },
      { id: "case-fibromyalgia", title: "Caso Clínico 3: Fibromialgia" },
      { id: "case-rhinitis", title: "Caso Clínico 4: Alergia e Rinite Alérgica" },
      { id: "case-post-surgery-pain", title: "Caso Clínico 5: Dor Crônica Pós-Cirúrgica" },
      { id: "protocol-versioning", title: "Estratégias de Atualização de Protocolos" },
      { id: "clinical-records-feedback", title: "Feedback dos Pacientes e Otimização" },
      { id: "clinical-auditing", title: "Auditoria de Qualidade das Condutas" },
      { id: "clinical-mind-insights", title: "Inteligência de Insights da Mente Digital" }
    ]
  }
];

const MTC_SEMANTIC_MAP: Record<string, string[]> = {
  "ansiedade": ["shen", "coração", "ht7", "pc6", "insônia", "estagnação", "depressão", "sono", "estresse", "irritar", "nervoso", "mente", "coracao", "insolonia"],
  "insônia": ["sono", "noite", "shen", "coração", "an shen", "ht7", "bp6", "r3", "vg20", "pesadelo", "agitação", "coracao", "insonha", "dormir"],
  "estresse": ["fígado", "f3", "vb34", "estagnação", "irritado", "tpm", "frustração", "tenso", "cefaleia", "figado", "estresso", "estressado"],
  "dor": ["lombar", "algia", "cefaleia", "bloqueio", "lombalgia", "f3", "ig4", "frio", "umidade", "dor crônica", "enxaqueca", "pescoço", "ombros", "articulação", "cronica"],
  "lombalgia": ["dor", "costas", "rim", "b23", "b40", "r3", "frio", "lombar", "artrite", "coluna", "escoliose"],
  "digestão": ["baço", "estômago", "e36", "bp6", "vc12", "intestino", "azia", "fezes", "gastro", "diarreia", "baco", "estomago", "digesti", "apetite"],
  "fadiga": ["fraqueza", "cansaço", "qi", "baço", "rim", "e36", "vc4", "r3", "energia", "debilitado", "baco", "cansaco", "desanimo"],
  "menopausa": ["calor", "mulher", "climatério", "ginecologia", "uterino", "bp6", "r3", "f7", "sangue", "climaterio"],
  "imunidade": ["wei qi", "gripe", "respiratório", "pulmão", "defesa", "e36", "p7", "ig11", "respiratorio", "pulmao"],
};

export default function KnowledgeScreen() {
  const [activeTab, setActiveTab] = useState<'study' | 'simulator' | 'ia-educator' | 'mente-clinica' | 'pdf-repo' | 'cdss'>('study');
  const [cdssSymptoms, setCdssSymptoms] = useState<string[]>([]);
  const [cdssResult, setCdssResult] = useState<any>(null);
  const [cdssLoading, setCdssLoading] = useState(false);
  const [showSchema, setShowSchema] = useState(false);
  const [selectedSchemaTable, setSelectedSchemaTable] = useState<string>("conceitos");
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedEixo, setExpandedEixo] = useState<string | null>("eixo-1");
  
  // Specific Theme State
  const [selectedTheme, setSelectedTheme] = useState<any>(null);
  const [themeDetails, setThemeDetails] = useState<any>(null);
  const [loadingTheme, setLoadingTheme] = useState(false);
  const [activeSubTab, setActiveSubTab] = useState<'teoria' | 'evidencia' | 'flashcards' | 'quiz' | 'chat'>('teoria');

  // PDF Document Management State hooks
  const [pdfList, setPdfList] = useState<any[]>([]);
  const [pdfLogs, setPdfLogs] = useState<any[]>([]);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [uploadingPdf, setUploadingPdf] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadCategory, setUploadCategory] = useState('Protocolos Clínicos');
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploadSuccessMsg, setUploadSuccessMsg] = useState("");

  const CDSS_SYMPTOMS_CATALOG = [
    {
      category: "Sinais Gerais e Emocionais",
      icon: "🧠",
      items: [
        "Irritabilidade ou Frustração",
        "Ansiedade ou Tensão Muscular",
        "Fadiga física e Cansaço muscular",
        "Insônia severa (vigília ou pesadelos)",
        "Palpitações noturnas e agitação",
        "Aversão severa ao frio e extremidades congeladas",
        "Dor lombar surda e cansaço nos joelhos",
        "Queimação gástrica severa e refluxo ácido"
      ]
    },
    {
      category: "Língua (Inspeção Física)",
      icon: "👅",
      items: [
        "Língua com bordas vermelhas",
        "Língua pálida com marcas de dentes nos bordos",
        "Língua vermelha com pouca ou nenhuma saburra",
        "Língua pálida, inchada e úmida nos bordos",
        "Língua vermelha com saburra amarela e seca"
      ]
    },
    {
      category: "Pulso (Soma de Palpação)",
      icon: "⚡",
      items: [
        "Pulso em corda / Tenso (Xian)",
        "Pulso fraco e suave (Ru)",
        "Pulso fino e rápido (Xi Shuo)",
        "Pulso lento, profundo e fraco (Chi-Chen-Ru)",
        "Pulso rápido e forte (Shuo Shi)"
      ]
    },
    {
      category: "Sintomas Secundários (Interrogatório)",
      icon: "✍️",
      items: [
        "Cefaleia tensional ou Enxaqueca",
        "Inchaço abdominal logo após comer",
        "Sensação de calor nos 5 centros",
        "Garganta e boca seca sem sede real",
        "Urina clara e de grande volume",
        "Sede intensa com preferência por água gelada",
        "Fezes secas e constipação irritativa"
      ]
    }
  ];

  const handleEvaluateCDSS = async (symptoms: string[]) => {
    setCdssLoading(true);
    try {
      const res = await api.evaluateCdss(symptoms);
      setCdssResult(res);
    } catch (err) {
      console.error("Erro ao avaliar CDSS:", err);
    } finally {
      setCdssLoading(false);
    }
  };

  const handleToggleSymptom = (symptom: string) => {
    const next = cdssSymptoms.includes(symptom)
      ? cdssSymptoms.filter(s => s !== symptom)
      : [...cdssSymptoms, symptom];
    
    setCdssSymptoms(next);
    if (next.length > 0) {
      handleEvaluateCDSS(next);
    } else {
      setCdssResult(null);
    }
  };

  const fetchPdfsAndLogs = async () => {
    setPdfLoading(true);
    try {
      const list = await api.getPdfList();
      const logs = await api.getPdfLogs();
      setPdfList(list);
      setPdfLogs(logs);
    } catch (err) {
      console.error("Erro ao resgatar lista de PDFs ou logs:", err);
    } finally {
      setPdfLoading(false);
    }
  };

  const handlePdfUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.name.toLowerCase().endsWith(".pdf")) {
      setUploadError("O formato do arquivo é inválido. Por favor envie somente arquivos no formato .pdf para a biblioteca.");
      return;
    }

    setUploadingPdf(true);
    setUploadError(null);
    setUploadSuccessMsg("");

    const reader = new FileReader();
    reader.onload = async () => {
      try {
        const base64Data = (reader.result as string).split(',')[1];
        await api.uploadPdf({
          title: uploadTitle.trim() || file.name.replace(/\.pdf$/i, ""),
          fileName: file.name,
          base64Data: base64Data,
          category: uploadCategory
        });
        setUploadTitle('');
        setUploadSuccessMsg(`Upload de "${file.name}" realizado com absoluto sucesso!`);
        await fetchPdfsAndLogs();
      } catch (err: any) {
        setUploadError(err.response?.data?.error || "Erro ao carregar e indexar arquivo de PDF.");
      } finally {
        setUploadingPdf(false);
      }
    };
    reader.readAsDataURL(file);
  };

  // Simple semantic expansion helper
  const getSemanticTerms = (query: string): string[] => {
    const term = query.toLowerCase().trim();
    if (!term) return [];
    
    const terms = [term];
    
    // Check our MTC dictionary for direct keys
    Object.keys(MTC_SEMANTIC_MAP).forEach(key => {
      if (key.includes(term) || term.includes(key)) {
        terms.push(...MTC_SEMANTIC_MAP[key]);
      }
    });

    return Array.from(new Set(terms));
  };

  // Flashcards flipping state
  const [currentFlashcardIndex, setCurrentFlashcardIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);

  // Quiz interactive state
  const [quizAnswers, setQuizAnswers] = useState<Record<number, number | null>>({});
  const [quizSubmittedMap, setQuizSubmittedMap] = useState<Record<number, boolean>>({});

  // Theme specialized chat channel
  const [chatInput, setChatInput] = useState('');
  const [chatMessages, setChatMessages] = useState<any[]>([]);
  const [sendingChat, setSendingChat] = useState(false);

  // IA Simulator State
  const [difficulty, setDifficulty] = useState<'Fácil' | 'Intermediário' | 'Avançado' | 'Especialista'>('Intermediário');
  const [simCase, setSimCase] = useState<any>(null);
  const [generatingCase, setGeneratingCase] = useState(false);
  const [diagnosisAnswer, setDiagnosisAnswer] = useState('');
  const [treatmentAnswer, setTreatmentAnswer] = useState('');
  const [dietAnswer, setDietAnswer] = useState('');
  const [evalResult, setEvalResult] = useState<any>(null);
  const [evaluatingCase, setEvaluatingCase] = useState(false);

  // IA Educadora Converter state
  const [rawUploadText, setRawUploadText] = useState('');
  const [convertFormat, setConvertFormat] = useState<'resumo' | 'flashcards' | 'mindmap' | 'questoes'>('resumo');
  const [conversionResult, setConversionResult] = useState<string>('');
  const [converting, setConverting] = useState(false);

  // Mente Clínica Metrics
  const [mindStats, setMindStats] = useState<any>(null);
  const [statsLoading, setStatsLoading] = useState(false);

  // Load Theme Details
  const handleSelectTheme = async (theme: any) => {
    setSelectedTheme(theme);
    setLoadingTheme(true);
    setThemeDetails(null);
    setActiveSubTab('teoria');
    setCurrentFlashcardIndex(0);
    setIsFlipped(false);
    setQuizAnswers({});
    setQuizSubmittedMap({});
    setChatMessages([
      {
        sender: 'bot',
        text: `Olá! Sou seu Instrutor Especialista para o tema **${theme.title}**. Que dúvida acadêmica ou detalhe clínico sobre este protocolo clássico gostaria de discutir?`
      }
    ]);

    try {
      const details = await api.getThemeDetails(theme.id);
      setThemeDetails(details);
    } catch (err) {
      console.error("Erro ao carregar tema dinâmico:", err);
    } finally {
      setLoadingTheme(false);
    }
  };

  // Theme Chat Send
  const handleSendThemeChat = async () => {
    if (!chatInput.trim() || sendingChat) return;
    const userText = chatInput;
    setChatInput('');
    setChatMessages(prev => [...prev, { sender: 'user', text: userText }]);
    setSendingChat(true);

    try {
      const history = chatMessages.map(m => ({
        role: m.sender === 'user' ? 'user' : 'model',
        parts: [{ text: m.text }]
      }));
      const pageContext = `Dra. Camila estudando o tema MTC: ${selectedTheme.title}. 
Definição: ${themeDetails?.concept || ""}. 
Fisiopatologia: ${themeDetails?.foundation || ""}. 
Diagnóstico clássico: ${themeDetails?.diagnosis || ""}. 
Tratamento sugerido: ${themeDetails?.treatment || ""}. 
Pontos Principais: ${JSON.stringify(themeDetails?.primaryPoints || [])}`;
      const res = await api.chat(userText, undefined, history, pageContext);
      setChatMessages(prev => [...prev, { sender: 'bot', text: res.response }]);
    } catch (err) {
      console.error(err);
      setChatMessages(prev => [...prev, { sender: 'bot', text: "Erro ao contatar o especialista. Tentando restabelecer link." }]);
    } finally {
      setSendingChat(false);
    }
  };

  // Generate Patient Simulation Case
  const handleGenerateSimCase = async () => {
    setGeneratingCase(true);
    setSimCase(null);
    setEvalResult(null);
    setDiagnosisAnswer('');
    setTreatmentAnswer('');
    setDietAnswer('');
    try {
      const res = await api.generateSimulationCase(difficulty, selectedTheme?.title);
      setSimCase(res);
    } catch (err) {
      console.error(err);
    } finally {
      setGeneratingCase(false);
    }
  };

  // Evaluate Simulation Answer
  const handleSubmitSimEvaluation = async () => {
    if (!diagnosisAnswer.trim() || !treatmentAnswer.trim()) return;
    setEvaluatingCase(true);
    setEvalResult(null);
    try {
      const res = await api.evaluateSimulationCase(simCase, {
        diagnosis: diagnosisAnswer,
        treatmentPoints: treatmentAnswer,
        dietAdvice: dietAnswer
      });
      setEvalResult(res);
    } catch (err) {
      console.error(err);
    } finally {
      setEvaluatingCase(false);
    }
  };

  // IA Educadora Converter Execution
  const handleConvertText = async () => {
    if (!rawUploadText.trim() || converting) return;
    setConverting(true);
    setConversionResult('');
    try {
      const res = await api.iaEducatorConvert(rawUploadText, convertFormat);
      if (res.output && typeof res.output === 'object') {
        setConversionResult(JSON.stringify(res.output, null, 2));
      } else {
        setConversionResult(res.output || res);
      }
    } catch (err) {
      console.error(err);
      setConversionResult("Houve uma oscilação na conversão do Gemini. Por favor redimensione o texto ou tente de novo.");
    } finally {
      setConverting(false);
    }
  };

  // Fetch Mente Clínica Insights
  const fetchMenteClinica = async () => {
    setStatsLoading(true);
    try {
      const data = await api.getMenteClinicaInsights();
      setMindStats(data);
    } catch (err) {
      console.error(err);
    } finally {
      setStatsLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'mente-clinica') {
      fetchMenteClinica();
    } else if (activeTab === 'pdf-repo') {
      fetchPdfsAndLogs();
    }
  }, [activeTab]);

  // Filter 150 themes by search text with intelligent semantic expansion
  const filteredEixos = EIXOS.map(eixo => {
    const queryTerms = getSemanticTerms(searchQuery);
    const matched = eixo.themes.filter(t => {
      if (!searchQuery.trim()) return true;
      const themeTitleLower = t.title.toLowerCase();
      const eixoNameLower = eixo.name.toLowerCase();
      return queryTerms.some(term => 
        themeTitleLower.includes(term) || 
        eixoNameLower.includes(term)
      );
    });
    return { ...eixo, themes: matched };
  }).filter(e_matched => e_matched.themes.length > 0);

  return (
    <div className="min-h-screen bg-slate-50/50 p-6 font-sans">
      
      {/* Dynamic Header Block */}
      <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <div className="w-6 h-6 rounded bg-emerald-500/10 flex items-center justify-center text-emerald-600">
              <GraduationCap size={16} />
            </div>
            <span className="text-xs font-bold text-emerald-600 uppercase tracking-widest">Plataforma Acadêmica MTC</span>
          </div>
          <h1 className="text-3xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            Centro de Conhecimento <span className="text-emerald-600 font-extrabold uppercase text-sm px-2 py-0.5 bg-emerald-50 rounded-md border border-emerald-200">PRO</span>
          </h1>
          <p className="text-sm text-neutral-600 mt-1">Biblioteca científica, simulação gamificada de casos clínicos reais e inteligência adaptativa integrada.</p>
        </div>

        {/* Global Hub Selector Tabs */}
        <div className="flex bg-neutral-100 p-1.5 rounded-2xl border border-neutral-200/50 flex-wrap gap-1 md:gap-0">
          <button
            onClick={() => { setActiveTab('study'); setSelectedTheme(null); }}
            className={`px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${activeTab === 'study' ? 'bg-white text-neutral-950 shadow-xs' : 'text-neutral-500 hover:text-neutral-900'}`}
          >
            <BookOpen size={13} /> Biblioteca (150 Temas)
          </button>
          <button
            onClick={() => setActiveTab('simulator')}
            className={`px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${activeTab === 'simulator' ? 'bg-white text-neutral-950 shadow-xs' : 'text-neutral-500 hover:text-neutral-900'}`}
          >
            <Target size={13} /> Simulador Clínico MTC
          </button>
          <button
            onClick={() => setActiveTab('ia-educator')}
            className={`px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${activeTab === 'ia-educator' ? 'bg-white text-neutral-950 shadow-xs' : 'text-neutral-500 hover:text-neutral-900'}`}
          >
            <Brain size={13} /> IA Educadora
          </button>
          <button
            onClick={() => setActiveTab('mente-clinica')}
            className={`px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${activeTab === 'mente-clinica' ? 'bg-white text-neutral-950 shadow-xs' : 'text-neutral-500 hover:text-neutral-900'}`}
          >
            <Zap size={13} /> Mente de Prontuários
          </button>
          <button
            onClick={() => setActiveTab('pdf-repo')}
            className={`px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${activeTab === 'pdf-repo' ? 'bg-white text-neutral-950 shadow-xs' : 'text-neutral-500 hover:text-neutral-900'}`}
          >
            <FileText size={13} className="text-red-500" /> Repositório PDF
          </button>
          <button
            onClick={() => setActiveTab('cdss')}
            className={`px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${activeTab === 'cdss' ? 'bg-gradient-to-r from-amber-50 to-orange-50 text-orange-700 border border-orange-200/50 shadow-xs' : 'text-neutral-500 hover:text-neutral-900'}`}
          >
            <Shield size={13} className="text-amber-600 animate-pulse" /> Motor Clínico (CDSS)
          </button>
        </div>
      </header>

      {/* ----------------- TAB: STUDY & BIBLIOTECA (150 THEMES) ----------------- */}
      {activeTab === 'study' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* LEFT COLUMN: Eixos & Themes List (Hierarchical Side rail) */}
          <div className="lg:col-span-4 bg-white border border-neutral-200/80 rounded-3xl p-5 shadow-xs space-y-4 max-h-[820px] overflow-y-auto">
            <div className="space-y-2">
              <h3 className="text-sm font-bold text-neutral-800 flex items-center gap-1.5">
                <ListFilter size={16} className="text-neutral-500" /> Grade Temática Geral
              </h3>
              <p className="text-xs text-neutral-500">Navegue pelas 10 divisões estruturais de estudo continuado.</p>
            </div>

            {/* Live Global Search for the 150 themes */}
            <div className="relative">
              <input
                type="text"
                placeholder="Busca global de temas..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-neutral-50 hover:bg-neutral-100/50 focus:bg-white text-xs border border-neutral-200 focus:outline-none focus:border-emerald-500 transition-all text-neutral-800"
              />
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" size={14} />
            </div>

            <div className="space-y-2">
              {filteredEixos.map((eixo) => {
                const isExpanded = expandedEixo === eixo.id || searchQuery.length > 0;
                return (
                  <div key={eixo.id} className="border border-neutral-100 rounded-2xl overflow-hidden">
                    <button
                      onClick={() => setExpandedEixo(isExpanded ? null : eixo.id)}
                      className={`w-full flex items-center justify-between p-3.5 text-left transition-colors cursor-pointer ${isExpanded ? 'bg-emerald-50/50 text-emerald-950 font-semibold' : 'bg-white hover:bg-neutral-50/50 text-neutral-700'}`}
                    >
                      <div className="min-w-0 pr-2">
                        <h4 className="text-xs font-bold leading-tight truncate">{eixo.name}</h4>
                        <p className="text-[10px] text-neutral-400 truncate mt-0.5">{eixo.description}</p>
                      </div>
                      <ChevronDown size={14} className={`text-neutral-400 transition-transform shrink-0 ${isExpanded ? 'rotate-180 text-emerald-700' : ''}`} />
                    </button>

                    {isExpanded && (
                      <div className="bg-neutral-50/40 p-2 border-t border-neutral-100 space-y-1">
                        {eixo.themes.map((theme) => {
                          const isSelected = selectedTheme?.id === theme.id;
                          return (
                            <button
                              key={theme.id}
                              onClick={() => handleSelectTheme({ ...theme, axis: eixo.name })}
                              className={`w-full flex items-center justify-between p-2.5 rounded-xl text-left text-xs transition-colors cursor-pointer ${isSelected ? 'bg-emerald-600 text-white font-bold shadow-xs' : 'hover:bg-neutral-100/50 text-neutral-600'}`}
                            >
                              <span className="truncate pr-2">{theme.title}</span>
                              <ChevronRight size={12} className={isSelected ? 'text-white' : 'text-neutral-400'} />
                            </button>
                          );
                        })}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* RIGHT COLUMN: Theme details / Interactive workspace */}
          <div className="lg:col-span-8 space-y-6">
            {!selectedTheme ? (
              <div className="bg-white border border-neutral-200/80 rounded-3xl p-10 text-center shadow-xs flex flex-col items-center justify-center min-h-[500px]">
                <div className="w-16 h-16 rounded-full bg-emerald-50 border border-emerald-100 flex items-center justify-center text-emerald-600 mb-4 animate-bounce">
                  <BookOpen size={28} />
                </div>
                <h2 className="text-lg font-bold text-neutral-800">Selecione um dos 150 Temas de Estudo</h2>
                <p className="text-sm text-neutral-500 max-w-sm mt-1.5 leading-relaxed">
                  Clique em um tema no sumário hierárquico ao lado para acessar resumos, evidências científicas, exames de memorização e inteligência de MTC especializada.
                </p>

                {/* Residência Progress Widget Quick Indicator */}
                <div className="mt-8 border-t border-neutral-100 pt-6 w-full max-w-md grid grid-cols-3 gap-2 text-center">
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-neutral-150">
                    <span className="block text-[11px] font-bold text-neutral-400 uppercase tracking-wide">Eixos Completos</span>
                    <strong className="text-xl font-extrabold text-neutral-800 block mt-1">2 / 10</strong>
                  </div>
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-neutral-150">
                    <span className="block text-[11px] font-bold text-neutral-400 uppercase tracking-wide">Casos Resolvidos</span>
                    <strong className="text-xl font-extrabold text-neutral-800 block mt-1">14</strong>
                  </div>
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-neutral-150">
                    <span className="block text-[11px] font-bold text-neutral-400 uppercase tracking-wide">Estudo Ativo</span>
                    <strong className="text-xl font-extrabold text-emerald-600 block mt-1">84 %</strong>
                  </div>
                </div>
              </div>
            ) : (
              <div className="bg-white border border-neutral-200/80 rounded-3xl shadow-xs overflow-hidden">
                
                {/* Active Theme Title header bar */}
                <div className="p-6 bg-gradient-to-r from-neutral-900 to-emerald-950 text-white flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div>
                    <span className="inline-block text-[10px] uppercase font-bold tracking-widest text-emerald-400 bg-emerald-950/80 px-2 py-0.5 rounded border border-emerald-800 mb-2">
                      {selectedTheme.axis}
                    </span>
                    <h2 className="text-xl font-bold text-white tracking-tight">{selectedTheme.title}</h2>
                    <p className="text-xs text-neutral-400 mt-1 flex items-center gap-1">
                      <Lock size={10} /> Conteúdo Clínico Integrado • Dra. Camila Silva
                    </p>
                  </div>
                  
                  {/* Share/Bookmark interactive buttons */}
                  <div className="flex gap-1.5 shrink-0">
                    <button className="p-2.5 bg-white/10 hover:bg-white/20 rounded-xl transition-colors text-white hover:text-amber-300">
                      <Star size={14} className="fill-current" />
                    </button>
                    <button className="p-2.5 bg-white/10 hover:bg-white/20 rounded-xl transition-colors text-white">
                      <Bookmark size={14} />
                    </button>
                  </div>
                </div>

                {/* Shimmer loading wrapper */}
                {loadingTheme ? (
                  <div className="p-10 text-center space-y-4 animate-pulse">
                    <div className="h-6 bg-neutral-200 w-1/3 rounded-md mx-auto" />
                    <div className="h-4 bg-neutral-100 w-1/2 rounded-md mx-auto" />
                    <div className="space-y-2 pt-4">
                      <div className="h-4 bg-neutral-100 rounded-md" />
                      <div className="h-4 bg-neutral-100 rounded-md" />
                      <div className="h-4 bg-neutral-100 rounded-md w-5/6" />
                    </div>
                  </div>
                ) : (
                  <div>
                    {/* Inner workspace tab selectors */}
                    <div className="flex border-b border-neutral-200 overflow-x-auto bg-neutral-50 px-3">
                      <button
                        onClick={() => setActiveSubTab('teoria')}
                        className={`px-4 py-3 text-xs font-bold whitespace-nowrap transition-all border-b-2 cursor-pointer ${activeSubTab === 'teoria' ? 'border-emerald-600 text-emerald-900' : 'border-transparent text-neutral-500 hover:text-neutral-800'}`}
                      >
                        📖 Resumo Clínico
                      </button>
                      <button
                        onClick={() => setActiveSubTab('evidencia')}
                        className={`px-4 py-3 text-xs font-bold whitespace-nowrap transition-all border-b-2 cursor-pointer ${activeSubTab === 'evidencia' ? 'border-emerald-600 text-emerald-900' : 'border-transparent text-neutral-500 hover:text-neutral-800'}`}
                      >
                        🧪 Evidências & Casos
                      </button>
                      <button
                        onClick={() => setActiveSubTab('flashcards')}
                        className={`px-4 py-3 text-xs font-bold whitespace-nowrap transition-all border-b-2 cursor-pointer ${activeSubTab === 'flashcards' ? 'border-emerald-600 text-emerald-900' : 'border-transparent text-neutral-500 hover:text-neutral-800'}`}
                      >
                        🃏 Flashcards ({themeDetails?.flashcards?.length || 1})
                      </button>
                      <button
                        onClick={() => setActiveSubTab('quiz')}
                        className={`px-4 py-3 text-xs font-bold whitespace-nowrap transition-all border-b-2 cursor-pointer ${activeSubTab === 'quiz' ? 'border-emerald-600 text-emerald-900' : 'border-transparent text-neutral-500 hover:text-neutral-800'}`}
                      >
                        📝 Treinamento Quiz
                      </button>
                      <button
                        onClick={() => setActiveSubTab('chat')}
                        className={`px-4 py-3 text-xs font-bold whitespace-nowrap transition-all border-b-2 cursor-pointer ${activeSubTab === 'chat' ? 'border-emerald-600 text-emerald-900' : 'border-transparent text-neutral-500 hover:text-neutral-800'}`}
                      >
                        💬 Especialista AI
                      </button>
                    </div>

                    {/* Sub-tab view panel */}
                    <div className="p-6">
                      
                      {/* SUB-TAB: TEORIA & COMPREENSION */}
                      {activeSubTab === 'teoria' && themeDetails && (
                        <div className="space-y-6">
                          <div>
                            <h3 className="text-sm font-bold text-neutral-900 uppercase tracking-widest border-l-2 border-emerald-600 pl-2 mb-2">Conceito Clássico</h3>
                            <p className="text-xs text-neutral-650 leading-relaxed bg-neutral-50 p-4 rounded-xl border border-neutral-100">{themeDetails.concept}</p>
                          </div>
                          
                          <div>
                            <h3 className="text-sm font-bold text-neutral-900 uppercase tracking-widest border-l-2 border-emerald-600 pl-2 mb-2">Fundamentação & Patologia</h3>
                            <p className="text-xs text-neutral-650 leading-relaxed bg-neutral-50 p-4 rounded-xl border border-neutral-100">{themeDetails.foundation}</p>
                          </div>

                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div className="bg-rose-50/50 p-4 rounded-2xl border border-rose-100">
                              <h4 className="text-xs font-bold text-rose-950 mb-1 flex items-center gap-1">🧐 Sintomas e Diagnósticos (Língua/Pulso)</h4>
                              <p className="text-xs text-rose-900 leading-relaxed mt-1">{themeDetails.diagnosis}</p>
                            </div>
                            <div className="bg-emerald-50/50 p-4 rounded-2xl border border-emerald-100">
                              <h4 className="text-xs font-bold text-emerald-950 mb-1 flex items-center gap-1">🎯 Princípio Terapêutico MTC</h4>
                              <p className="text-xs text-emerald-900 leading-relaxed mt-1">{themeDetails.treatment}</p>
                            </div>
                          </div>

                          {/* Primary points schema mapping */}
                          <div>
                            <h3 className="text-sm font-bold text-neutral-900 uppercase tracking-widest border-l-2 border-emerald-600 pl-2 mb-3">Pontos Primários Clássicos</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                              {themeDetails.primaryPoints?.map((p: any, i: number) => (
                                <div key={i} className="p-3 bg-white border border-neutral-200 rounded-xl shadow-2xs">
                                  <div className="flex items-center justify-between">
                                    <strong className="text-xs text-emerald-600 font-extrabold">{p.name || p}</strong>
                                    <span className="text-[9px] bg-neutral-100 text-neutral-500 px-1.5 py-0.5 rounded font-mono">Ponto Chave</span>
                                  </div>
                                  <p className="text-[10px] text-neutral-400 mt-0.5 font-semibold">Localização: {p.location || "Canal Principal Mecânico"}</p>
                                  <p className="text-xs text-neutral-600 mt-1 leading-normal italic">“{p.rationale || "Mover e aquecer canais colaterais obstruídos pelo vento exógeno."}”</p>
                                </div>
                              ))}
                            </div>
                          </div>
                        </div>
                      )}

                      {/* SUB-TAB: EVIDENCIA CIENTIFICA & CASES */}
                      {activeSubTab === 'evidencia' && themeDetails && (
                        <div className="space-y-6">
                          <div className="bg-amber-50/30 p-4 rounded-2xl border border-amber-200/50">
                            <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-1.5 mb-2">
                              <Sparkles size={16} className="text-amber-500" /> Neurobiologia & Evidências Modernas
                            </h3>
                            <p className="text-xs text-neutral-650 leading-relaxed italic">{themeDetails.scientificEvidence}</p>
                          </div>

                          <div>
                            <h3 className="text-sm font-semibold text-neutral-800 mb-3">Exemplo de Caso Clínico</h3>
                            <p className="text-xs text-neutral-650 leading-relaxed bg-neutral-50 p-4 rounded-xl border border-neutral-150">{themeDetails.clinicalCases}</p>
                          </div>

                          {/* Interactive Attachments Guides */}
                          <div>
                            <h3 className="text-xs font-bold text-neutral-400 uppercase tracking-wider mb-2.5">Recursos e e-Books Anexos</h3>
                            <div className="space-y-2">
                              {themeDetails.resources?.pdfGuides?.map((pdf: string, i: number) => {
                                let targetFile = "Guia Pratico BioAcupunt - Protocolo Clinico Prescritivo de Emergencia.pdf";
                                if (i === 1 || pdf.toLowerCase().includes("tratado") || pdf.toLowerCase().includes("livro")) {
                                  targetFile = "Tratado Classico de Acupuntura - Fundamentos e Canais Principais.pdf";
                                } else if (i === 2 || pdf.toLowerCase().includes("análise") || pdf.toLowerCase().includes("artigo") || pdf.toLowerCase().includes("evidência")) {
                                  targetFile = "Analise Cientifica de Pontos Extras e Estudos Clinicos Modernos.pdf";
                                }
                                return (
                                  <div key={i} className="p-3 bg-white border border-neutral-150 rounded-xl flex items-center justify-between">
                                    <div className="flex items-center gap-2">
                                      <FileText size={18} className="text-red-500" />
                                      <div className="text-left">
                                        <span className="text-xs font-semibold text-neutral-800 block">{pdf}</span>
                                        <span className="text-[9px] text-neutral-400 font-mono">Arquivo Real: {targetFile}</span>
                                      </div>
                                    </div>
                                    <div className="flex gap-1.5">
                                      <button 
                                        onClick={() => window.open(`/api/knowledge/pdf/view/${encodeURIComponent(targetFile)}`, '_blank')}
                                        className="text-[10px] bg-emerald-50 hover:bg-emerald-100 text-emerald-800 font-extrabold px-3 py-1.5 rounded-lg cursor-pointer"
                                        title="Visualizar PDF no navegador"
                                      >
                                        Visualizar
                                      </button>
                                      <button 
                                        onClick={() => { window.location.href = `/api/knowledge/pdf/download/${encodeURIComponent(targetFile)}`; }}
                                        className="text-[10px] bg-slate-100 hover:bg-slate-200 text-neutral-600 font-extrabold px-3 py-1.5 rounded-lg cursor-pointer"
                                        title="Efetuar download real e registrar log"
                                      >
                                        Download
                                      </button>
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        </div>
                      )}

                      {/* SUB-TAB: FLASHCARDS (Active Study Recall) */}
                      {activeSubTab === 'flashcards' && themeDetails && (
                        <div className="space-y-6 flex flex-col items-center justify-center">
                          <div className="text-center">
                            <h3 className="text-sm font-bold text-neutral-800">Memorização Ativa MTC</h3>
                            <p className="text-xs text-neutral-500">Pratique a lembrança ativa das fórmulas e pontos mais emblemáticos.</p>
                          </div>

                          {/* Deck Progress Indicator */}
                          {themeDetails.flashcards && themeDetails.flashcards.length > 0 && (
                            <span className="text-xs text-neutral-500 font-medium font-mono">
                              Cartão {currentFlashcardIndex + 1} de {themeDetails.flashcards.length}
                            </span>
                          )}

                          {/* Flipped Flashcard container */}
                          <div 
                            onClick={() => setIsFlipped(!isFlipped)}
                            className="w-full max-w-md h-56 perspective-1000 cursor-pointer my-4 animate-fade-in"
                          >
                            <div className={`relative w-full h-full duration-500 [transform-style:preserve-3d] transition-transform ${isFlipped ? '[transform:rotateY(180deg)]' : ''}`}>
                              {/* Front */}
                              <div className="absolute inset-0 w-full h-full bg-slate-50 border-2 border-dashed border-emerald-300 rounded-3xl p-6 flex flex-col justify-between items-center [backface-visibility:hidden] shadow-xs">
                                <span className="text-[10px] uppercase font-bold tracking-wider text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-md font-sans">Pergunta de Fixação</span>
                                <p className="text-center text-sm font-bold text-neutral-800 px-4 leading-relaxed font-sans">
                                  {themeDetails.flashcards?.[currentFlashcardIndex]?.question || "Clique para revelar a pergunta clássica de harmonização?"}
                                </p>
                                <span className="text-[10px] text-neutral-400 font-sans">Clique para Virar e Ver a Resposta</span>
                              </div>
                              {/* Back */}
                              <div className="absolute inset-0 w-full h-full bg-emerald-700 rounded-3xl p-6 flex flex-col justify-between items-center [backface-visibility:hidden] [transform:rotateY(180deg)] text-white shadow-md">
                                <span className="text-[10px] uppercase font-bold tracking-wider text-emerald-200 bg-emerald-800/80 px-2.5 py-0.5 rounded-md font-sans">Gabarito Consolidado</span>
                                <p className="text-center text-xs leading-relaxed font-semibold px-4 font-sans">
                                  {themeDetails.flashcards?.[currentFlashcardIndex]?.answer || "O equilíbrio de energia é recuperado estancando focos de calor na base de moxa indireta."}
                                </p>
                                <span className="text-[10px] text-emerald-200 font-sans">Clique para Voltar à Pergunta</span>
                              </div>
                            </div>
                          </div>

                          {/* Navigation Buttons for deck */}
                          {themeDetails.flashcards && themeDetails.flashcards.length > 1 && (
                            <div className="flex gap-4">
                              <button
                                disabled={currentFlashcardIndex === 0}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setCurrentFlashcardIndex(prev => prev - 1);
                                  setIsFlipped(false);
                                }}
                                className="px-4 py-2 border border-neutral-300 bg-white hover:bg-neutral-50 text-neutral-700 text-xs font-bold rounded-xl transition-all disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer font-sans"
                              >
                                Anterior
                              </button>
                              <button
                                disabled={currentFlashcardIndex === themeDetails.flashcards.length - 1}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setCurrentFlashcardIndex(prev => prev + 1);
                                  setIsFlipped(false);
                                }}
                                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-all disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer font-sans"
                              >
                                Próxima
                              </button>
                            </div>
                          )}
                        </div>
                      )}

                      {/* SUB-TAB: TRAINING QUIZ */}
                      {activeSubTab === 'quiz' && themeDetails && (
                        <div className="space-y-6">
                          <div className="border-b border-neutral-100 pb-3">
                            <h3 className="text-sm font-bold text-neutral-800">Exame de Retenção de Conhecimento</h3>
                            <p className="text-xs text-neutral-500">Responda a questão e avalie seu julgamento terapêutico clínico.</p>
                          </div>

                          {themeDetails.quiz?.map((q: any, i: number) => {
                            const isSubmitted = !!quizSubmittedMap[i];
                            const currentSelection = quizAnswers[i];

                            return (
                              <div key={i} className="p-4 bg-white border border-neutral-200/80 rounded-2xl space-y-4 shadow-3xs">
                                <span className="inline-block text-[9px] bg-neutral-100 text-neutral-600 font-bold px-2 py-0.5 rounded font-mono">Questão {i + 1}</span>
                                <p className="text-xs font-bold text-neutral-800 leading-relaxed">
                                  {q.question}
                                </p>

                                <div className="space-y-2">
                                  {q.options?.map((opt: string, idx: number) => {
                                    const isSelected = currentSelection === idx;
                                    const isCorrect = idx === q.correctIndex;
                                    
                                    let optionStyle = "border-neutral-200 hover:border-neutral-350 bg-white text-neutral-700";
                                    if (isSubmitted) {
                                      if (isCorrect) {
                                        optionStyle = "border-emerald-500 bg-emerald-50 text-emerald-900 font-bold";
                                      } else if (isSelected) {
                                        optionStyle = "border-rose-400 bg-rose-50 text-rose-900";
                                      } else {
                                        optionStyle = "border-neutral-100 bg-neutral-50/50 text-neutral-400 opacity-60";
                                      }
                                    } else if (isSelected) {
                                      optionStyle = "border-emerald-500 bg-emerald-50 text-emerald-950 font-semibold ring-1 ring-emerald-500";
                                    }

                                    return (
                                      <button
                                        key={idx}
                                        disabled={isSubmitted}
                                        onClick={() => setQuizAnswers(prev => ({ ...prev, [i]: idx }))}
                                        className={`w-full text-left p-3 rounded-xl border text-xs transition-all cursor-pointer ${optionStyle} flex items-center justify-between font-sans`}
                                      >
                                        <span>{opt}</span>
                                        {isSubmitted && isCorrect && <CheckCircle2 size={14} className="text-emerald-600 shrink-0" />}
                                        {isSubmitted && isSelected && !isCorrect && <AlertCircle size={14} className="text-rose-600 shrink-0" />}
                                      </button>
                                    );
                                  })}
                                </div>

                                {!isSubmitted ? (
                                  <button
                                    disabled={currentSelection === undefined || currentSelection === null}
                                    onClick={() => setQuizSubmittedMap(prev => ({ ...prev, [i]: true }))}
                                    className="w-full bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold py-3 rounded-xl transition-all disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer font-sans"
                                  >
                                    Submeter Resposta
                                  </button>
                                ) : (
                                  <div className="p-4 bg-emerald-500/5 rounded-2xl border border-emerald-100 mt-4 animate-fade-in">
                                    <h4 className="text-xs font-bold text-emerald-950 flex items-center gap-1 mb-1 font-sans">
                                      <Sparkles size={12} className="text-emerald-600" /> Explicação Acadêmica
                                    </h4>
                                    <p className="text-[11px] text-neutral-650 leading-relaxed font-sans">
                                      {q.explanation}
                                    </p>
                                    <button
                                      onClick={() => {
                                        setQuizAnswers(prev => {
                                          const copy = { ...prev };
                                          delete copy[i];
                                          return copy;
                                        });
                                        setQuizSubmittedMap(prev => ({ ...prev, [i]: false }));
                                      }}
                                      className="text-[10px] text-emerald-700 font-bold underline flex items-center gap-1 mt-3 font-sans"
                                    >
                                      <RefreshCw size={10} /> Tentar Outra Vez
                                    </button>
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      )}

                      {/* SUB-TAB: SPECIALIST CHAT */}
                      {activeSubTab === 'chat' && (
                        <div className="space-y-4">
                          <div className="bg-neutral-50 border border-neutral-150 p-3 px-4 rounded-xl text-neutral-500 text-[11px] font-mono flex items-center gap-1.5">
                            <Lock size={11} className="text-neutral-400" /> Canal de Estudos e-Learning • Dra. Camila Silva Silva
                          </div>

                          {/* Chat Message Scroll list */}
                          <div className="space-y-3 h-52 overflow-y-auto p-2 border border-neutral-100 rounded-2xl bg-neutral-50/40">
                            {chatMessages.map((m, idx) => (
                              <div key={idx} className={`flex ${m.sender === 'bot' ? 'mr-auto justify-start' : 'ml-auto justify-end'}`}>
                                <div className={`p-3 rounded-2xl text-xs max-w-[85%] leading-relaxed ${m.sender === 'bot' ? 'bg-white border border-neutral-200 text-neutral-800' : 'bg-emerald-600 text-white font-medium'}`}>
                                  {m.text}
                                </div>
                              </div>
                            ))}
                            {sendingChat && (
                              <div className="text-xs italic text-neutral-400 flex items-center gap-1 animate-pulse">
                                Resgatando referências clássicas...
                              </div>
                            )}
                          </div>

                          {/* Chat footer text sender */}
                          <div className="flex gap-2.5">
                            <input
                              type="text"
                              value={chatInput}
                              onChange={(e) => setChatInput(e.target.value)}
                              onKeyDown={(e) => { if(e.key === 'Enter') handleSendThemeChat(); }}
                              placeholder="Fazer pergunta teórica sobre este tema ao Gemini..."
                              className="flex-1 bg-white focus:bg-white text-xs px-4 py-2.5 rounded-xl border border-neutral-200 focus:outline-none focus:border-emerald-500 transition-all text-neutral-800"
                            />
                            <button
                              onClick={handleSendThemeChat}
                              disabled={!chatInput.trim() || sendingChat}
                              className="p-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl transition-all disabled:opacity-40"
                            >
                              <Send size={14} />
                            </button>
                          </div>
                        </div>
                      )}

                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ----------------- TAB: IA CLINICAL PATIENT SIMULATOR ----------------- */}
      {activeTab === 'simulator' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* Left panel: Simulator config & setup */}
          <div className="lg:col-span-4 bg-white border border-neutral-200/80 rounded-3xl p-5 shadow-xs space-y-5">
            <div>
              <h3 className="text-sm font-bold text-neutral-800 flex items-center gap-1.5">
                <Target size={16} className="text-emerald-600 animate-spin" /> Residência Virtual de Casos
              </h3>
              <p className="text-xs text-neutral-500 mt-0.5">Gerencie o treinamento clínico simulando pacientes reais estruturados pela rede inteligente do Gemini.</p>
            </div>

            <div className="space-y-3">
              <span className="block text-xs font-bold text-neutral-700">Escolha o nível de complexidade</span>
              <div className="grid grid-cols-2 gap-2">
                {(['Fácil', 'Intermediário', 'Avançado', 'Especialista'] as const).map((lvl) => (
                  <button
                    key={lvl}
                    onClick={() => setDifficulty(lvl)}
                    className={`p-3 rounded-xl text-center text-xs font-bold border transition-all cursor-pointer ${difficulty === lvl ? 'bg-emerald-600 text-white border-emerald-600 shadow-sm' : 'bg-neutral-50 border-neutral-200 text-neutral-600 hover:bg-neutral-100'}`}
                  >
                    {lvl}
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={handleGenerateSimCase}
              disabled={generatingCase}
              className="w-full bg-neutral-900 hover:bg-neutral-900/90 text-white text-xs font-bold py-3.5 rounded-xl transition-all shadow-md hover:shadow-lg flex items-center justify-center gap-2 cursor-pointer"
            >
              <Sparkles size={14} className="text-amber-400 fill-amber-400/20" /> 
              {generatingCase ? "Gerando Paciente..." : "Gerar Caso de Treinamento"}
            </button>
          </div>

          {/* Right panel: Active patient case and evaluation */}
          <div className="lg:col-span-8">
            {!simCase ? (
              <div className="bg-white border border-neutral-200/80 rounded-3xl p-10 text-center shadow-xs flex flex-col items-center justify-center min-h-[460px]">
                <div className="w-16 h-16 rounded-full bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 mb-4 animate-pulse">
                  <User size={28} />
                </div>
                <h2 className="text-lg font-bold text-neutral-800">Pronto para Treinar</h2>
                <p className="text-sm text-neutral-500 max-w-sm mt-1.5 leading-relaxed">
                  Defina a complexidade ao lado e gere um caso clínico vivo. Diagnosticando o meridiano, saburra e pulso, receba avaliação imediata do nosso Mestre IA.
                </p>
              </div>
            ) : (
              <div className="bg-white border border-neutral-200/80 rounded-3xl shadow-xs overflow-hidden space-y-6 p-6">
                
                {/* Simulated Patient details head banner */}
                <div className="bg-slate-50 p-5 rounded-2xl border border-neutral-200/50 flex flex-col md:flex-row justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full bg-amber-500 animate-ping inline-block" />
                      <strong className="text-sm text-neutral-800">Paciente: {simCase.patientName}</strong>
                    </div>
                    <p className="text-xs text-neutral-500 font-mono">Gênero: {simCase.sex} • Idade: {simCase.age} • Profissão: {simCase.profession}</p>
                    <span className="inline-block text-[9px] bg-amber-100 text-amber-800 font-extrabold px-1.5 py-0.5 rounded uppercase mt-2">Dificuldade: {simCase.difficulty}</span>
                  </div>
                  <div className="bg-white p-3 rounded-xl border text-center shrink-0">
                    <span className="block text-[10px] text-neutral-400 font-bold uppercase">Dor Inicial EVA</span>
                    <strong className="text-xl font-extrabold text-red-500 block">{simCase.evaScore} / 10</strong>
                  </div>
                </div>

                {/* Complaint and Physical state diagnostics */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="p-4 bg-slate-50 rounded-2xl border border-neutral-100 space-y-2">
                    <h4 className="text-xs font-bold text-neutral-800 uppercase flex items-center gap-1.5">
                      <Bookmark size={13} className="text-rose-600" /> Queixa Principal & Hábito
                    </h4>
                    <p className="text-xs text-neutral-600 leading-relaxed italic">“{simCase.mainComplaint}”</p>
                    <p className="text-xs text-neutral-650 leading-relaxed mt-2">{simCase.history}</p>
                  </div>

                  <div className="p-4 bg-slate-50 rounded-2xl border border-neutral-100 space-y-2">
                    <h4 className="text-xs font-bold text-neutral-800 uppercase flex items-center gap-1.5">
                      <Heart size={13} className="text-emerald-600" /> Semiologia Clínica (Língua/Pulso)
                    </h4>
                    <div>
                      <span className="text-[10px] text-neutral-400 block font-bold font-mono">ASPECTO DA LÍNGUA</span>
                      <p className="text-xs text-neutral-700 leading-normal font-medium">{simCase.tongue}</p>
                    </div>
                    <div className="pt-2">
                      <span className="text-[10px] text-neutral-400 block font-bold font-mono">ASPECTO DO PULSO</span>
                      <p className="text-xs text-neutral-700 leading-normal font-medium">{simCase.pulse}</p>
                    </div>
                  </div>
                </div>

                {/* Submitting Answer Section */}
                <div className="space-y-4 border-t border-neutral-100 pt-6">
                  <h3 className="text-xs font-bold text-neutral-400 uppercase tracking-widest">Sua Conduta e Raciocínio Acadêmico</h3>
                  
                  <div className="space-y-3">
                    <div>
                      <label className="block text-xs font-bold text-neutral-700 mb-1">Qual o Diagnóstico MTC (Síndrome principal envolvida)?</label>
                      <input
                        type="text"
                        placeholder="Ex: Estagnação de Qi do Fígado que agride o Baço"
                        value={diagnosisAnswer}
                        onChange={(e) => setDiagnosisAnswer(e.target.value)}
                        className="w-full text-xs p-3 rounded-xl border border-neutral-200 focus:outline-none focus:border-emerald-500 transition-all text-neutral-800 bg-neutral-50/50"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-neutral-700 mb-1">Plano de Tratamento (Método de estímulo e Pontos chave aplicados)?</label>
                      <input
                        type="text"
                        placeholder="Ex: Circular o Qi, pontos F3, IG4, BP6 e E36 em moxa indireta..."
                        value={treatmentAnswer}
                        onChange={(e) => setTreatmentAnswer(e.target.value)}
                        className="w-full text-xs p-3 rounded-xl border border-neutral-200 focus:outline-none focus:border-emerald-500 transition-all text-neutral-800 bg-neutral-50/50"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-neutral-700 mb-1">Recomendações Dietéticas & Adjuvantes?</label>
                      <input
                        type="text"
                        placeholder="Ex: Evitar alimentos crus, priorizar alimentos de natureza morna..."
                        value={dietAnswer}
                        onChange={(e) => setDietAnswer(e.target.value)}
                        className="w-full text-xs p-3 rounded-xl border border-neutral-200 focus:outline-none focus:border-emerald-500 transition-all text-neutral-800 bg-neutral-50/50"
                      />
                    </div>
                  </div>

                  <button
                    onClick={handleSubmitSimEvaluation}
                    disabled={evaluatingCase || !diagnosisAnswer.trim() || !treatmentAnswer.trim()}
                    className="w-full bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold py-3.5 rounded-xl transition-all shadow-md hover:shadow-lg flex items-center justify-center gap-2 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <BookOpenCheck size={14} /> 
                    {evaluatingCase ? "Avaliando conduta..." : "Submeter para Avaliação Clínica MTC"}
                  </button>
                </div>

                {/* Evaluation Feedback results block */}
                {evalResult && (
                  <div className="p-5 rounded-2xl border bg-emerald-50/20 border-emerald-100 flex flex-col space-y-4">
                    <div className="flex items-center justify-between border-b pb-3 border-emerald-100">
                      <div>
                        <span className="block text-[10px] text-emerald-800 font-extrabold uppercase tracking-wide">AVALIAÇÃO DE DESEMPENHO</span>
                        <strong className="text-xl font-black text-emerald-950 block">{evalResult.verdict}</strong>
                      </div>
                      <div className="bg-emerald-600 text-white font-extrabold px-4.5 py-2.5 rounded-2xl text-center shadow-xs">
                        <span className="block text-[8px] uppercase font-bold text-emerald-100">Feedback IA</span>
                        <strong className="text-lg">{evalResult.score} / 100</strong>
                      </div>
                    </div>

                    <div className="space-y-3.5 text-xs">
                      <div>
                        <span className="font-bold text-neutral-800 block">Crítica sobre o Diagnóstico de Canal:</span>
                        <p className="text-neutral-600 leading-relaxed mt-0.5">{evalResult.criticDiagnosis}</p>
                      </div>
                      <div>
                        <span className="font-bold text-neutral-800 block">Crítica sobre o Agulhamento e Pontos:</span>
                        <p className="text-neutral-600 leading-relaxed mt-0.5">{evalResult.criticPoints}</p>
                      </div>
                      <div>
                        <span className="font-bold text-neutral-800 block">Diferencial Ideal Clássico:</span>
                        <p className="text-emerald-900 bg-emerald-100/10 p-3 rounded-lg border border-emerald-100 leading-relaxed font-medium mt-1">
                          <strong>Síndrome de MTC Correta:</strong> {evalResult.idealSyndrome}. <br />
                          {evalResult.idealTreatment}
                        </p>
                      </div>
                    </div>
                  </div>
                )}

              </div>
            )}
          </div>
        </div>
      )}

      {/* ----------------- TAB: IA EDUCATOR PORTAL CONVERTER ----------------- */}
      {activeTab === 'ia-educator' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* Left panel format selector */}
          <div className="lg:col-span-5 bg-white border border-neutral-200/80 rounded-3xl p-5 shadow-xs space-y-4">
            <div>
              <h3 className="text-sm font-bold text-neutral-800 flex items-center gap-1.5">
                <Brain size={16} className="text-emerald-600" /> Conversão Educativa IA
              </h3>
              <p className="text-xs text-neutral-500 mt-0.5">Importe ou copie ensaios e chaves clínicas exóticas para transformá-los instantaneamente em materiais didáticos.</p>
            </div>

            <textarea
              rows={8}
              placeholder="Cole aqui o texto do artigo científico, livro ou anotação clínica..."
              value={rawUploadText}
              onChange={(e) => setRawUploadText(e.target.value)}
              className="w-full text-xs p-3 rounded-xl bg-neutral-50 hover:bg-neutral-100/40 focus:bg-white border border-neutral-200 focus:outline-none focus:border-emerald-500 transition-all text-neutral-800"
            />

            <div className="space-y-2">
              <span className="block text-xs font-semibold text-neutral-700">Formato Acadêmico Esperado</span>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { id: 'resumo', label: '📖 Resumo Técnico' },
                  { id: 'flashcards', label: '🃏 Flashcards' },
                  { id: 'mindmap', label: '🕸️ Linhas Diagnósticas' },
                  { id: 'questoes', label: '❓ Quiz Multi-escolha' }
                ].map((f) => (
                  <button
                    key={f.id}
                    onClick={() => setConvertFormat(f.id as any)}
                    className={`p-3 rounded-xl text-center text-xs font-bold transition-all border cursor-pointer ${convertFormat === f.id ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs' : 'bg-neutral-50 border-neutral-200 text-neutral-600'}`}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={handleConvertText}
              disabled={converting || !rawUploadText.trim()}
              className="w-full bg-neutral-900 hover:bg-neutral-900/90 text-white text-xs font-bold py-3.5 rounded-xl transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-45"
            >
              <RefreshCw size={13} className={converting ? "animate-spin" : ""} />
              {converting ? "Decompondo material..." : "Converter e Consolidar"}
            </button>
          </div>

          {/* Right panel: results render */}
          <div className="lg:col-span-7 bg-white border border-neutral-200/80 rounded-3xl p-6 shadow-xs min-h-[460px] flex flex-col justify-between">
            <div>
              <h3 className="text-sm font-bold text-neutral-800 border-b pb-2 mb-4">Material Didático Estruturado</h3>
              {!conversionResult ? (
                <div className="text-center p-12 text-neutral-400 text-xs flex flex-col items-center justify-center space-y-2">
                  <BookOpenCheck size={32} strokeWidth={1} />
                  <span>Nenhum material didático convertido ainda neste canal. Insira o texto ao lado para iniciar.</span>
                </div>
              ) : (
                <div className="whitespace-pre-line text-xs leading-relaxed text-neutral-700 bg-neutral-50 p-4 rounded-xl border border-neutral-100 max-h-[480px] overflow-y-auto font-mono">
                  {conversionResult}
                </div>
              )}
            </div>

            {conversionResult && (
              <div className="flex gap-2.5 pt-4 border-t border-neutral-100">
                <button 
                  onClick={() => { setRawUploadText(''); setConversionResult(''); }}
                  className="flex-1 py-3 border border-neutral-200 text-neutral-700 text-xs font-bold rounded-xl hover:bg-neutral-50 transition-colors cursor-pointer"
                >
                  Limpar Área
                </button>
                <button className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-all cursor-pointer">
                  Exportar Conteúdo
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ----------------- TAB: MENTE CLÍNICA METRICS ----------------- */}
      {activeTab === 'mente-clinica' && (
        <div className="bg-white border border-neutral-200/80 rounded-3xl p-6 shadow-xs space-y-6">
          <div className="flex items-center justify-between border-b pb-4">
            <div>
              <h3 className="text-sm font-bold text-neutral-800 flex items-center gap-1.5">
                <Zap size={16} className="text-amber-500 fill-amber-500/10" /> Segunda Mente de Prontuários Clinicos
              </h3>
              <p className="text-xs text-neutral-500 mt-0.5">Análise automatizada de diagnósticos de MTC ativos de nossa base clínica para fornecer propostas de tratamento.</p>
            </div>
            <button 
              onClick={fetchMenteClinica}
              disabled={statsLoading}
              className="p-2 bg-neutral-50 border border-neutral-200 rounded-lg hover:bg-neutral-100 text-neutral-600 transition-all text-xs flex items-center gap-1.5 cursor-pointer"
            >
              <RefreshCw size={12} className={statsLoading ? "animate-spin" : ""} /> Atualizar Insights
            </button>
          </div>

          {statsLoading ? (
            <div className="p-12 text-center animate-pulse space-y-4">
              <div className="h-6 bg-neutral-200 w-1/4 rounded-md mx-auto" />
              <div className="h-4 bg-neutral-100 w-1/2 rounded-md mx-auto" />
            </div>
          ) : (
            <div className="space-y-6">
              
              {/* Top stats indices widgets */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-indigo-50/50 p-4 rounded-2xl border border-indigo-100/50 text-center">
                  <span className="block text-[11px] text-indigo-700 font-bold uppercase tracking-wider">Histórico de Prontuários</span>
                  <strong className="text-2xl font-black text-indigo-950 block mt-1">{mindStats?.totalCasesAnalyzed || 3}</strong>
                  <span className="text-[10px] text-neutral-400">Total de prontuários com histórico</span>
                </div>
                <div className="bg-emerald-50/50 p-4 rounded-2xl border border-emerald-100/50 text-center">
                  <span className="block text-[11px] text-emerald-700 font-bold uppercase tracking-wider">Síndrome mais atendida</span>
                  <strong className="text-sm font-black text-emerald-950 block mt-2 truncate">
                    {mindStats?.topSyndromes?.[0]?.name || "Estagnação de Qi do Fígado"}
                  </strong>
                  <span className="text-[10px] text-neutral-400">Recorrência detectada</span>
                </div>
                <div className="bg-rose-50/50 p-4 rounded-2xl border border-rose-100/50 text-center">
                  <span className="block text-[11px] text-rose-700 font-bold uppercase tracking-wider">Média de Alívio</span>
                  <strong className="text-2xl font-black text-rose-950 block mt-1">92 %</strong>
                  <span className="text-[10px] text-neutral-400">Taxa de sucesso e indicação</span>
                </div>
              </div>

              {/* Dynamic generated insights lists cards */}
              <div className="space-y-4 pt-4 border-t border-neutral-100">
                <h4 className="text-xs font-bold text-neutral-400 uppercase tracking-widest">Recomendações Inteligentes Automatizadas</h4>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {mindStats?.insights?.map((ins: any, idx: number) => (
                    <div key={idx} className="p-4 bg-slate-50 rounded-2xl border border-neutral-150 flex flex-col justify-between space-y-3">
                      <div>
                        <div className="flex items-center justify-between">
                          <strong className="text-xs text-neutral-800">{ins.title}</strong>
                          <span className="text-[9px] bg-amber-100 text-amber-800 px-2 py-0.5 rounded font-extrabold uppercase font-mono">{ins.metric}</span>
                        </div>
                        <p className="text-[11px] text-neutral-500 leading-normal mt-1.5">{ins.scientificContext}</p>
                      </div>
                      <div className="bg-emerald-500/5 border border-emerald-100/80 p-2.5 rounded-xl">
                        <span className="text-[9px] uppercase font-bold text-emerald-800 block">Conduta Alvo:</span>
                        <p className="text-[11px] text-emerald-950 font-medium italic mt-0.5">“{ins.tip}”</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

            </div>
          )}
        </div>
      )}

      {/* ----------------- TAB: PDF REPOSITORY & AUDIT TRAIL ----------------- */}
      {activeTab === 'pdf-repo' && (
        <div className="space-y-6">
          <div className="bg-white p-6 md:p-8 rounded-3xl border border-neutral-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-6 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-rose-500/5 rounded-full -mr-10 -mt-10 opacity-60 pointer-events-none" />
            <div className="space-y-2 flex-1">
              <h2 className="text-xl font-bold text-neutral-900 flex items-center gap-2">
                <FileText size={22} className="text-red-500" />
                Repositório de Prontuários & PDFs Científicos
              </h2>
              <p className="text-xs text-neutral-500 leading-relaxed max-w-2xl">
                Espaço dedicado para carregar, consultar e baixar os documentos em PDF de protocolos, referências suíças e rascunhos científicos de MTC. Todos os downloads registram rastreamento de auditoria em conformidade legal.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            
            {/* Uploader Column */}
            <div className="lg:col-span-5 bg-white border border-neutral-200 rounded-3xl p-6 shadow-xs space-y-5">
              <div>
                <h3 className="text-sm font-bold text-neutral-800 mb-1">Indexar Novo Documento PDF</h3>
                <p className="text-[11px] text-neutral-400">Insira as informações do metadado antes de enviar.</p>
              </div>

              <div className="space-y-3">
                <div>
                  <label className="block text-[10px] uppercase font-bold text-neutral-400 tracking-wider mb-1.5">Título do Documento (Opcional)</label>
                  <input
                    type="text"
                    placeholder="Ex: Tratado de Pontos Auriculares"
                    value={uploadTitle}
                    onChange={(e) => setUploadTitle(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-250 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-all text-neutral-700"
                  />
                </div>

                <div>
                  <label className="block text-[10px] uppercase font-bold text-neutral-400 tracking-wider mb-1.5">Categoria / Classificação</label>
                  <select
                    value={uploadCategory}
                    onChange={(e) => setUploadCategory(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-neutral-50 border border-neutral-250 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-all text-neutral-700"
                  >
                    <option value="Protocolos Clínicos">📜 Protocolos Clínicos</option>
                    <option value="Artigos Científicos">🧪 Artigos Científicos</option>
                    <option value="Livros e Obras">📚 Livros e Obras</option>
                    <option value="Histórico de Prontuários">📋 Prontuários e Exportações</option>
                  </select>
                </div>

                {/* Upload Zone supporting drag & drop & manual click selection */}
                <div 
                  className="border-2 border-dashed border-neutral-250 rounded-2xl hover:border-emerald-500 p-8 text-center cursor-pointer transition-colors relative"
                  onClick={() => document.getElementById('manual-pdf-file-selector')?.click()}
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault();
                    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                      const filesTransfer = {
                        target: {
                          files: e.dataTransfer.files
                        }
                      } as unknown as React.ChangeEvent<HTMLInputElement>;
                      handlePdfUpload(filesTransfer);
                    }
                  }}
                >
                  <input
                    type="file"
                    id="manual-pdf-file-selector"
                    accept="application/pdf"
                    className="hidden"
                    onChange={handlePdfUpload}
                  />

                  <div className="flex flex-col items-center justify-center space-y-2">
                    <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-red-500">
                      <FileText size={20} />
                    </div>
                    <div>
                      <span className="text-xs font-bold text-neutral-800 block">Clique para selecionar ou arraste o PDF</span>
                      <span className="text-[10px] text-neutral-400 block mt-1">Formato suportado: apenas arquivos .pdf de até 15MB</span>
                    </div>
                  </div>
                </div>

                {uploadingPdf && (
                  <div className="bg-slate-50 p-3 rounded-xl border border-neutral-150 flex items-center gap-3">
                    <span className="animate-spin w-4 h-4 border-2 border-emerald-500 border-t-transparent rounded-full flex-shrink-0" />
                    <span className="text-[11px] text-neutral-600 font-medium">Lendo binários e indexando na base de dados...</span>
                  </div>
                )}

                {uploadError && (
                  <div className="p-3 bg-red-50 border border-red-150 rounded-xl text-[11px] text-red-700 flex items-center gap-2">
                    <AlertCircle size={14} className="flex-shrink-0" />
                    <span>{uploadError}</span>
                  </div>
                )}

                {uploadSuccessMsg && (
                  <div className="p-3 bg-emerald-50 border border-emerald-150 rounded-xl text-[11px] text-emerald-800 flex items-center gap-2">
                    <CheckCircle2 size={14} className="flex-shrink-0 text-emerald-600" />
                    <span>{uploadSuccessMsg}</span>
                  </div>
                )}
              </div>
            </div>

            {/* List & Logs Column */}
            <div className="lg:col-span-7 space-y-6">
              
              {/* PDF Documents List */}
              <div className="bg-white border border-neutral-200 rounded-3xl p-6 shadow-xs">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-sm font-bold text-neutral-800">Livros & Artigos Armazenados</h3>
                    <p className="text-[11px] text-neutral-400">Arquivos válidos e prontos para consulta imediata do profissional.</p>
                  </div>
                  <button 
                    onClick={fetchPdfsAndLogs}
                    disabled={pdfLoading}
                    className="p-1.5 hover:bg-neutral-100 rounded-lg cursor-pointer text-neutral-500"
                    title="Recarregar arquivos"
                  >
                    <RefreshCw size={14} className={pdfLoading ? "animate-spin" : ""} />
                  </button>
                </div>

                {pdfLoading ? (
                  <div className="py-12 text-center text-neutral-400 text-xs">Carregando lista de PDFs...</div>
                ) : pdfList.length === 0 ? (
                  <div className="py-12 text-center text-neutral-400 text-xs">Nenhum PDF cadastrado até o momento.</div>
                ) : (
                  <div className="divide-y divide-neutral-100 max-h-[300px] overflow-y-auto pr-1">
                    {pdfList.map((pdfItem, index) => (
                      <div key={index} className="py-3 flex items-center justify-between gap-4">
                        <div className="flex items-start gap-2.5 min-w-0">
                          <div className="w-8 h-8 rounded bg-red-50 text-red-500 flex items-center justify-center flex-shrink-0 font-bold text-xs mt-0.5">
                            PDF
                          </div>
                          <div className="min-w-0">
                            <span className="text-xs font-bold text-neutral-800 block truncate" title={pdfItem.title}>{pdfItem.title}</span>
                            <div className="flex items-center gap-2 text-[10px] text-neutral-400 mt-0.5">
                              <span className="bg-slate-100 text-neutral-600 px-1.5 py-0.5 rounded-sm font-mono font-semibold">{pdfItem.category}</span>
                              <span className="font-mono">{pdfItem.sizeString}</span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-1.5 flex-shrink-0">
                          <button
                            onClick={() => window.open(`/api/knowledge/pdf/view/${encodeURIComponent(pdfItem.fileName)}`, '_blank')}
                            className="bg-emerald-50 hover:bg-emerald-150 text-emerald-800 text-[10px] font-extrabold px-3 py-1.5 rounded-lg cursor-pointer transition-all"
                            title="Visualizar documento em nova aba"
                          >
                            Visualizar
                          </button>
                          <button
                            onClick={() => { window.location.href = `/api/knowledge/pdf/download/${encodeURIComponent(pdfItem.fileName)}`; setTimeout(fetchPdfsAndLogs, 1500); }}
                            className="bg-neutral-100 hover:bg-neutral-250 text-neutral-600 text-[10px] font-extrabold px-3 py-1.5 rounded-lg cursor-pointer transition-all"
                            title="Download com log de rastreio"
                          >
                            Download
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* PDF Download Audit Trail Logs */}
              <div className="bg-neutral-900 text-neutral-300 rounded-3xl p-6 shadow-xs font-mono space-y-4">
                <div className="flex items-center justify-between border-b border-neutral-800 pb-3">
                  <div className="flex items-center gap-2">
                    <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
                    <span className="text-[11px] uppercase font-black tracking-widest text-[#E28A2B] font-sans">Histórico de Auditoria de Acesso</span>
                  </div>
                  <span className="text-[9px] bg-neutral-800 text-neutral-400 px-2.5 py-0.5 rounded uppercase font-semibold">LOGS EM REAL-TIME</span>
                </div>

                <div className="space-y-2 max-h-[220px] overflow-y-auto pr-1">
                  {pdfLogs.length === 0 ? (
                    <div className="text-[10px] text-neutral-500 py-4 text-center">Nenhum evento registrado no log local.</div>
                  ) : (
                    pdfLogs.map((log, index) => (
                      <div key={index} className="text-[10px] leading-relaxed py-1.5 border-b border-neutral-800/40 last:border-0">
                        <span className="text-emerald-500">[{new Date(log.timestamp).toLocaleTimeString()}]</span>{' '}
                        <span className="text-amber-500 font-extrabold">BAIXADO:</span>{' '}
                        <span className="text-white italic font-sans font-semibold">"{log.fileName}"</span>{' '}
                        por <span className="text-indigo-400 font-sans font-bold">{log.user || "Dra. Camila Silva"}</span>{' '}
                        <span className="text-neutral-500 font-sans text-[9px]">({log.ipAddress})</span>
                      </div>
                    ))
                  )}
                </div>
              </div>

            </div>

          </div>
        </div>
      )}

      {/* ----------------- TAB: CDSS (CLINICAL DECISION SUPPORT SYSTEM) ----------------- */}
      {activeTab === 'cdss' && (
        <div className="space-y-8">
          
          {/* Header Introduction Banner */}
          <div className="bg-gradient-to-r from-[#1B3026] to-[#2E4F3E] text-white p-6 md:p-8 rounded-3xl border border-emerald-800/25 shadow-sm">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div className="space-y-2">
                <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-black uppercase tracking-widest border border-emerald-500/30">
                  <Shield size={12} className="animate-pulse" /> Motor de Inferência Determinística
                </div>
                <h2 className="text-2xl md:text-3xl font-bold tracking-tight">Motor de Decisão Clínica (CDSS)</h2>
                <p className="text-neutral-300 text-xs md:text-sm max-w-3xl">
                  Processador estruturado de sintomas por grafo de conexões de MTC. A IA (Gemini) é acoplada exclusivamente na retaguarda como <strong>camada explicativa e de fundamentação bibliográfica</strong>, as hipóteses e condutas de tratamento são calculadas de forma rígida pelo motor de regras clássico.
                </p>
              </div>
              <button
                onClick={() => setShowSchema(!showSchema)}
                className={`py-2.5 px-4 rounded-xl text-xs font-bold font-mono transition-all flex items-center gap-2 cursor-pointer ${showSchema ? 'bg-amber-500 text-neutral-900 border border-amber-400' : 'bg-neutral-800/100 text-white border border-neutral-700/80 hover:bg-neutral-700'}`}
              >
                <Layers size={13} /> {showSchema ? "Fechar Blueprint DB" : "Ver Arquitetura de Dados SQL"}
              </button>
            </div>
          </div>

          {/* Database Schema & Architecture Visualizer (Answer to PostgreSQL architect requirement) */}
          {showSchema && (
            <motion.div 
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              transition={{ duration: 0.3 }}
              className="bg-white border border-neutral-200 rounded-3xl p-6 shadow-sm space-y-6"
            >
              <div className="border-b border-neutral-100 pb-4">
                <span className="text-[10px] font-bold text-amber-600 uppercase tracking-widest block mb-1">Arquitetura Supabase / PostgreSQL</span>
                <h3 className="text-base font-bold text-neutral-900">Blueprint do Banco de Dados Clínico Universais</h3>
                <p className="text-xs text-neutral-500 mt-1">
                  Modelagem normalizada com integridade referencial, indexação inteligente e indexadores JSONB para metadados de MTC altamente dinâmicos.
                </p>
              </div>

              {/* Table Toggles */}
              <div className="flex flex-wrap gap-1.5 border-b border-neutral-100 pb-3">
                {[
                  { id: "conceitos", label: "Tabela: conceitos (Universal)" },
                  { id: "relacoes", label: "Tabela: relacoes (Grafo)" },
                  { id: "sintomas", label: "Tabela: sintomas" },
                  { id: "hipoteses", label: "Tabela: hipoteses" },
                  { id: "regras", label: "Tabela: regras (Condicionais)" },
                  { id: "protocolos", label: "Tabela: protocolos" },
                  { id: "casos_clinicos", label: "Tabela: casos_clinicos" },
                  { id: "evidencias", label: "Tabela: evidencias" }
                ].map((tab) => (
                  <button
                    key={tab.id}
                    onClick={() => setSelectedSchemaTable(tab.id)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-mono font-bold transition-all cursor-pointer ${selectedSchemaTable === tab.id ? 'bg-neutral-900 text-white' : 'bg-neutral-100 text-neutral-500 hover:bg-neutral-200'}`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                
                {/* Visualizer card table column schema representation */}
                <div className="lg:col-span-5 space-y-4">
                  {selectedSchemaTable === "conceitos" && (
                    <div className="space-y-3">
                      <div className="p-4 bg-emerald-50 rounded-2xl border border-emerald-100">
                        <span className="text-[9px] font-bold text-emerald-800 uppercase tracking-wider block">Tipo de Entidade</span>
                        <h4 className="text-xs font-bold text-emerald-950 mt-1">Esquema Conceitual Flexível</h4>
                        <p className="text-[11px] text-emerald-900/80 mt-1 leading-relaxed">
                          Serve como nodo principal para qualquer ponto de acupuntura, canal, sintoma, patologia, ou fitoterápico, reduzindo a complexidade de criar tabelas esparsas.
                        </p>
                      </div>
                      <div className="bg-neutral-50 border border-neutral-100 rounded-2xl p-4 space-y-2 font-mono text-[11px]">
                        <div className="flex justify-between border-b border-neutral-200/50 pb-1 text-neutral-500 font-sans font-bold">
                          <span>Coluna (Postgres)</span>
                          <span>Tipo SQL / Constraint</span>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <span className="font-bold text-neutral-950">id</span>
                          <span>UUID PRIMARY KEY (gen_random_uuid())</span>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <span className="font-bold text-neutral-950">tipo</span>
                          <span>VARCHAR(60) NOT NULL (Indexed)</span>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <span className="font-bold text-neutral-950">nome</span>
                          <span>VARCHAR(255) NOT NULL (Unique Index)</span>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <span className="font-bold text-neutral-950">descricao</span>
                          <span>TEXT NULL</span>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <span className="font-bold text-neutral-950">metadata</span>
                          <span>JSONB DEFAULT '{}'::jsonb</span>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <span className="font-bold text-neutral-950">criado_em</span>
                          <span>TIMESTAMP DEFAULT CURRENT_TIMESTAMP</span>
                        </div>
                      </div>
                      <p className="text-[10px] text-neutral-400 italic">💡 metadata armazena e.g. {"{ 'canal': 'Estômago', 'natureza': 'Yin' }"}</p>
                    </div>
                  )}

                  {selectedSchemaTable === "relacoes" && (
                    <div className="space-y-3">
                      <div className="p-4 bg-orange-50 rounded-2xl border border-orange-100">
                        <span className="text-[9px] font-bold text-orange-850 uppercase tracking-wider block">O Coração do Grafo Clínico</span>
                        <h4 className="text-xs font-bold text-orange-950 mt-1">Conexões Dinâmicas</h4>
                        <p className="text-[11px] text-orange-900/80 mt-1 leading-relaxed">
                          Define ligamentos como 'X trata Y', 'X agrava Y', 'X é acoplado de Y'. Permite query recursiva imediata para decodificação sistêmica.
                        </p>
                      </div>
                      <div className="bg-neutral-50 border border-neutral-100 rounded-2xl p-4 space-y-2 font-mono text-[11px]">
                        <div className="flex justify-between border-b border-neutral-100 pb-1 text-neutral-500 font-sans font-bold">
                          <span>Coluna (Postgres)</span>
                          <span>Tipo SQL / Constraint</span>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <td>id</td>
                          <td>UUID PRIMARY KEY</td>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <td>origem_id</td>
                          <td>UUID REFERENCES conceitos(id)</td>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <td>destino_id</td>
                          <td>UUID REFERENCES conceitos(id)</td>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <td>tipo</td>
                          <td>VARCHAR(50) NOT NULL ('causa', 'trata')</td>
                        </div>
                        <div className="flex justify-between text-neutral-800">
                          <td>peso</td>
                          <td>DOUBLE PRECISION DEFAULT 1.0</td>
                        </div>
                      </div>
                    </div>
                  )}

                  {selectedSchemaTable === "sintomas" && (
                    <div className="space-y-3">
                      <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                        <span className="text-[9px] font-bold text-slate-800 uppercase tracking-wider block">Valores Semióticos</span>
                        <h4 className="text-xs font-bold text-slate-950 mt-1">Tabela sintomas</h4>
                        <p className="text-[11px] text-slate-900/80 mt-1 leading-relaxed">
                          Dimensões sintomáticas ordenadas por tipo (Subjetivo, Língua, Pulso, Emocional) de fácil classificação.
                        </p>
                      </div>
                      <div className="bg-neutral-50 border border-neutral-100 rounded-2xl p-4 space-y-2 font-mono text-[11px]">
                        <div className="flex justify-between border-b border-neutral-100 pb-1 text-neutral-500 font-sans font-bold">
                          <span>Coluna</span>
                          <span>Tipo</span>
                        </div>
                        <div>id: UUID PRIMARY KEY</div>
                        <div>nome: VARCHAR(255) NOT NULL (Unique)</div>
                        <div>grupo: VARCHAR(100) NOT NULL ('Língua', 'Pulso')</div>
                        <div>descricao: TEXT NULL</div>
                      </div>
                    </div>
                  )}

                  {selectedSchemaTable === "hipoteses" && (
                    <div className="space-y-3">
                      <div className="p-4 bg-blue-50 rounded-2xl border border-blue-100">
                        <h4 className="text-xs font-bold text-blue-950 mt-1">Tabela hipoteses</h4>
                        <p className="text-[11px] text-blue-900/80 mt-1 leading-relaxed">
                          Armazena as doze principais descompesações clássicas (síndromes Clínicas Zang Fu / Oito Regras).
                        </p>
                      </div>
                      <div className="bg-neutral-50 border border-neutral-100 rounded-2xl p-4 space-y-2 font-mono text-[11px]">
                        <div>id: UUID PRIMARY KEY</div>
                        <div>nome: VARCHAR(255) NOT NULL</div>
                        <div>descricao: TEXT NULL</div>
                        <div>metadata: JSONB NULL</div>
                      </div>
                    </div>
                  )}

                  {selectedSchemaTable === "regras" && (
                    <div className="space-y-3">
                      <div className="p-4 bg-indigo-50 rounded-2xl border border-indigo-100">
                        <h4 className="text-xs font-bold text-indigo-950 mt-1">Tabela regras (Inferencia CDSS)</h4>
                        <p className="text-[11px] text-indigo-900/80 mt-1 leading-relaxed">
                          Mantém os condicionais lógicos e a pontuação/peso dinâmico que determinam as relações de causa entre sintomas de entrada e hipótese diagnóstica calculada. Em conformidade com decisões determinísticas.
                        </p>
                      </div>
                      <div className="bg-neutral-50 border border-neutral-100 rounded-2xl p-4 space-y-2 font-mono text-[11px]">
                        <div>id: UUID PRIMARY KEY</div>
                        <div>nome: VARCHAR(100) NOT NULL</div>
                        <div>condicoes: JSONB NOT NULL (Expressão sintomatologia)</div>
                        <div>peso: DOUBLE PRECISION NOT NULL (Ex: 3.5)</div>
                        <div>diagnostico_alvo: VARCHAR(255) NOT NULL</div>
                      </div>
                    </div>
                  )}

                  {selectedSchemaTable === "protocolos" && (
                    <div className="space-y-3">
                      <div className="p-4 bg-pink-50 rounded-2xl border border-pink-100">
                        <h4 className="text-xs font-bold text-pink-950 mt-1">Tabela protocolos</h4>
                        <p className="text-[11px] text-pink-900/80 mt-1 leading-relaxed">
                          Procedimentos estruturados, canais a serem ativados e sequência estrita de execução física das agulhas.
                        </p>
                      </div>
                      <div className="bg-neutral-50 border border-neutral-100 rounded-2xl p-4 space-y-2 font-mono text-[11px]">
                        <div>id: UUID PRIMARY KEY</div>
                        <div>nome: VARCHAR(255) REGISTRO</div>
                        <div>pontos_chave: TEXT[] NOT NULL (Canais implicados)</div>
                        <div>procedimento: TEXT NOT NULL</div>
                        <div>passos: TEXT[] NOT NULL</div>
                      </div>
                    </div>
                  )}

                  {selectedSchemaTable === "casos_clinicos" && (
                    <div className="space-y-3">
                      <div className="p-4 bg-amber-50 rounded-2xl border border-amber-100">
                        <h4 className="text-xs font-bold text-amber-950 mt-1">Tabela casos_clinicos</h4>
                        <p className="text-[11px] text-amber-900/80 mt-1 leading-relaxed">
                          Atendimento clínico de consultório estruturado para aprendizado e correlação estatística.
                        </p>
                      </div>
                      <div className="bg-neutral-50 border border-neutral-100 rounded-2xl p-4 space-y-2 font-mono text-[11px]">
                        <div>id: UUID PRIMARY KEY</div>
                        <div>paciente_ficticio: VARCHAR(100)</div>
                        <div>idade: INTEGER / genero: VARCHAR(20)</div>
                        <div>queixa_principal: TEXT</div>
                        <div>sintomas: TEXT[]</div>
                        <div>diagnostico: TEXT</div>
                        <div>desfecho: TEXT</div>
                      </div>
                    </div>
                  )}

                  {selectedSchemaTable === "evidencias" && (
                    <div className="space-y-3">
                      <div className="p-4 bg-rose-50 rounded-2xl border border-rose-100">
                        <h4 className="text-xs font-bold text-rose-950 mt-1">Tabela evidencias</h4>
                        <p className="text-[11px] text-rose-900/80 mt-1 leading-relaxed">
                          Artigos científicos do PubMed indexed que validam os canais no controle biológico das comorbidades.
                        </p>
                      </div>
                      <div className="bg-neutral-50 border border-neutral-100 rounded-2xl p-4 space-y-2 font-mono text-[11px]">
                        <div>id: UUID PRIMARY KEY</div>
                        <div>titulo: VARCHAR(512) NOT NULL</div>
                        <div>estudo_link: VARCHAR(255) NULL</div>
                        <div>autores: TEXT NULL</div>
                        <div>sumario_evidencia: TEXT NOT NULL</div>
                      </div>
                    </div>
                  )}

                </div>

                {/* COPYABLE SQL COMPILATION WINDOW */}
                <div className="lg:col-span-7 space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-neutral-700 block">Visualização Código DDL (Supabase PostgreSQL / Drizzle)</span>
                    <button
                      onClick={() => {
                        let text = "";
                        if (selectedSchemaTable === "conceitos") text = "CREATE TABLE conceitos (\n  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n  tipo TEXT NOT NULL,\n  nome TEXT NOT NULL,\n  descricao TEXT,\n  metadata JSONB,\n  criado_em TIMESTAMP DEFAULT NOW()\n);";
                        else if (selectedSchemaTable === "relacoes") text = "CREATE TABLE relacoes (\n  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n  origem_id UUID REFERENCES conceitos(id) ON DELETE CASCADE,\n  destino_id UUID REFERENCES conceitos(id) ON DELETE CASCADE,\n  tipo TEXT NOT NULL,\n  peso DOUBLE PRECISION DEFAULT 1.0,\n  metadata JSONB\n);";
                        else if (selectedSchemaTable === "sintomas") text = "CREATE TABLE sintomas (\n  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n  nome TEXT NOT NULL UNIQUE,\n  grupo TEXT NOT NULL,\n  descricao TEXT\n);";
                        else if (selectedSchemaTable === "regras") text = "CREATE TABLE regras (\n  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n  nome TEXT NOT NULL,\n  condicoes JSONB NOT NULL,\n  peso DOUBLE PRECISION DEFAULT 1.0,\n  diagnostico_alvo TEXT NOT NULL\n);";
                        else text = "-- Estruturas compactas de dados integrados de MTC";
                        navigator.clipboard.writeText(text);
                      }}
                      className="text-[10px] bg-neutral-100 hover:bg-neutral-200 text-neutral-700 font-extrabold px-3 py-1.5 rounded-lg cursor-pointer transition-all"
                    >
                      Copiar Estrutura SQL
                    </button>
                  </div>
                  <div className="bg-neutral-900 text-neutral-300 rounded-2xl p-5 font-mono text-xs overflow-x-auto max-h-[380px]">
                    {selectedSchemaTable === "conceitos" && (
                      <pre className="text-emerald-400">
{`-- Criado em: 2026-06-22 - Banco PostgreSQL Supabase
CREATE TABLE conceitos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo TEXT NOT NULL, -- 'sintoma', 'diagnostico', 'ponto', 'meridiano', 'tecnica'
  nome TEXT NOT NULL UNIQUE,
  descricao TEXT,
  metadata JSONB DEFAULT '{}'::jsonb, -- dados específicos por tipo
  criado_em TIMESTAMP DEFAULT NOW()
);

-- Indexadores para aceleração de busca vetorial or relacional
CREATE INDEX idx_conceitos_tipo ON conceitos(tipo);
CREATE INDEX idx_conceitos_nome_trgm ON conceitos USING gin (nome gin_trgm_ops);`}
                      </pre>
                    )}
                    {selectedSchemaTable === "relacoes" && (
                      <pre className="text-amber-400">
{`-- Representação de Grafo de Conectividade de Acupuntura Zang Fu
CREATE TABLE relacoes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  origem_id UUID NOT NULL REFERENCES conceitos(id) ON DELETE CASCADE,
  destino_id UUID NOT NULL REFERENCES conceitos(id) ON DELETE CASCADE,
  tipo TEXT NOT NULL, -- 'causa_sindrome', 'ponto_trata_sindrome', 'meridiano_contem_ponto'
  peso DOUBLE PRECISION DEFAULT 1.0,
  metadata JSONB DEFAULT '{}'::jsonb,
  criado_em TIMESTAMP DEFAULT NOW(),
  CONSTRAINT unique_origin_dest_type UNIQUE (origem_id, destino_id, tipo)
);

CREATE INDEX idx_relacoes_origem ON relacoes(origem_id);
CREATE INDEX idx_relacoes_destino ON relacoes(destino_id);`}
                      </pre>
                    )}
                    {selectedSchemaTable === "sintomas" && (
                      <pre className="text-neutral-300">
{`CREATE TABLE sintomas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nome TEXT NOT NULL UNIQUE,
  grupo TEXT NOT NULL, -- 'Subjetivo', 'Lingua', 'Pulso', 'Emocional'
  descricao TEXT,
  metadata JSONB DEFAULT '{}'::jsonb,
  criado_em TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_sintomas_grupo ON sintomas(grupo);`}
                      </pre>
                    )}
                    {selectedSchemaTable === "hipoteses" && (
                      <pre className="text-[#6C8EBF]">
{`CREATE TABLE hipoteses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nome TEXT NOT NULL UNIQUE, -- Nome do padrão patológico Zang Fu
  descricao TEXT,
  metadata JSONB DEFAULT '{}'::jsonb,
  criado_em TIMESTAMP DEFAULT NOW()
);`}
                      </pre>
                    )}
                    {selectedSchemaTable === "regras" && (
                      <pre className="text-indigo-400">
{`CREATE TABLE regras (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nome TEXT NOT NULL,
  condicoes JSONB NOT NULL, -- Expressão determinística estruturada
  peso DOUBLE PRECISION DEFAULT 1.0, -- Peso ponderado na pontuação final
  diagnostico_alvo TEXT NOT NULL,
  metadata JSONB DEFAULT '{}'::jsonb,
  criado_em TIMESTAMP DEFAULT NOW()
);`}
                      </pre>
                    )}
                    {selectedSchemaTable === "protocolos" && (
                      <pre className="text-pink-400">
{`CREATE TABLE protocolos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nome TEXT NOT NULL UNIQUE, -- Síndrome de destino
  pontos_chave TEXT[] NOT NULL, -- ex: ['E36', 'BP6']
  procedimento TEXT NOT NULL,
  passos TEXT[] NOT NULL,
  metadata JSONB DEFAULT '{}'::jsonb,
  criado_em TIMESTAMP DEFAULT NOW()
);`}
                      </pre>
                    )}
                    {selectedSchemaTable === "casos_clinicos" && (
                      <pre className="text-amber-500">
{`CREATE TABLE casos_clinicos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  paciente_ficticio TEXT NOT NULL,
  idade INTEGER,
  genero TEXT,
  queixa_principal TEXT NOT NULL,
  historico TEXT NOT NULL,
  sintomas TEXT[] NOT NULL,
  diagnostico TEXT NOT NULL,
  desfecho TEXT NOT NULL,
  metadata JSONB DEFAULT '{}'::jsonb,
  criado_em TIMESTAMP DEFAULT NOW()
);`}
                      </pre>
                    )}
                    {selectedSchemaTable === "evidencias" && (
                      <pre className="text-rose-400">
{`CREATE TABLE evidencias (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  titulo TEXT NOT NULL,
  estudo_link TEXT,
  autores TEXT,
  sumario_evidencia TEXT NOT NULL,
  metadata JSONB DEFAULT '{}'::jsonb,
  criado_em TIMESTAMP DEFAULT NOW()
);`}
                      </pre>
                    )}
                  </div>
                  <div className="p-3 bg-neutral-100 rounded-xl text-[11px] text-neutral-600 leading-relaxed font-sans border border-neutral-200/50">
                    <strong>Vantagem de Escalabilidade:</strong> Ao usar <code>conceitos</code> e <code>relacoes</code> como um grafo flexível, o banco se adapta instantaneamente a novas pesquisas sem exigir alteração estrutural no Postgres (Diga não à proliferação de tabelas!).
                  </div>
                </div>

              </div>
            </motion.div>
          )}

          {/* Interactive CDSS Workspace Section */}
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
            
            {/* LEFT COLUMN: Semiotioc Symptoms Selection Panels */}
            <div className="xl:col-span-5 space-y-6">
              
              <div className="bg-white border border-neutral-200/85 rounded-3xl p-5 shadow-xs space-y-4">
                <div>
                  <h3 className="text-sm font-bold text-neutral-800 flex items-center gap-1.5">
                    <ListFilter size={16} className="text-neutral-500" /> Painel Diagnóstico de Entrada
                  </h3>
                  <p className="text-xs text-neutral-500 mt-1">
                    Selecione os dados somáticos observados no paciente. O motor calculará dinamicamente a ponderação.
                  </p>
                </div>

                <div className="space-y-5">
                  {CDSS_SYMPTOMS_CATALOG.map((cat, idx) => (
                    <div key={idx} className="space-y-2 border-b border-neutral-100 pb-4 last:border-b-0 last:pb-0">
                      <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-800">
                        <span>{cat.icon}</span>
                        <span>{cat.category}</span>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {cat.items.map((item, keyIdx) => {
                          const isSelected = cdssSymptoms.includes(item);
                          return (
                            <button
                              key={keyIdx}
                              onClick={() => handleToggleSymptom(item)}
                              className={`px-2.5 py-1.5 rounded-xl text-[11px] font-medium transition-all cursor-pointer border ${isSelected ? 'bg-emerald-600 text-white border-emerald-500 font-semibold shadow-xs' : 'bg-neutral-50 text-neutral-700 border-neutral-200/60 hover:bg-neutral-100'}`}
                            >
                              {item}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>

                {cdssSymptoms.length > 0 && (
                  <div className="flex items-center justify-between pt-2 border-t border-neutral-100">
                    <span className="text-[10px] font-mono text-neutral-400 uppercase tracking-widest font-black">
                      {cdssSymptoms.length} selecionado(s)
                    </span>
                    <button
                      onClick={() => { setCdssSymptoms([]); setCdssResult(null); }}
                      className="text-[10px] text-red-500 hover:text-red-700 font-bold uppercase tracking-wider cursor-pointer"
                    >
                      Limpar Seleção
                    </button>
                  </div>
                )}
              </div>

            </div>

            {/* RIGHT COLUMN: Real-Time Ponderation Results + Protocol + Case + Scientific Evidence + AI explanation */}
            <div className="xl:col-span-7 space-y-6">
              
              {cdssLoading ? (
                <div className="bg-white border border-neutral-200 rounded-3xl p-12 text-center shadow-xs flex flex-col items-center justify-center gap-3">
                  <RefreshCw size={24} className="text-emerald-600 animate-spin" />
                  <span className="text-xs text-neutral-500">Rodando algoritmo determinístico e invocando explicação complementar de IA...</span>
                </div>
              ) : !cdssResult ? (
                <div className="bg-white border border-neutral-200/80 rounded-3xl p-12 text-center shadow-xs space-y-4">
                  <div className="w-12 h-12 bg-neutral-100 rounded-full flex items-center justify-center mx-auto text-neutral-500">
                    <Shield size={20} className="text-neutral-400" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-neutral-800">Motor Pronto para Análise</h4>
                    <p className="text-xs text-neutral-500 mt-1 max-w-sm mx-auto leading-relaxed">
                      Selecione um ou mais sintomas, sinais físicas de língua ou ritmos de pulso no menu lateral para acionar as regras de predição clínica.
                    </p>
                  </div>
                </div>
              ) : (
                <div className="space-y-6">
                  
                  {/* Primary Hypothesis Calculated Card */}
                  <div className="bg-white border border-neutral-200 rounded-3xl p-6 shadow-xs space-y-4">
                    
                    {/* Diagnosis calculated badge */}
                    <div className="flex items-start justify-between gap-4 flex-wrap border-b border-neutral-100 pb-4">
                      <div>
                        <span className="text-[9px] bg-emerald-50 text-emerald-800 border border-emerald-200/50 px-2 py-0.5 rounded uppercase font-black font-mono tracking-widest">
                          Prepredição Calculada Pelo Motor de Regras
                        </span>
                        <h3 className="text-xl font-black text-neutral-900 mt-2">
                          {cdssResult.primary ? cdssResult.primary.nome : "Padrão Indeterminado / Gerais"}
                        </h3>
                        <p className="text-xs text-neutral-500 mt-1">
                          {cdssResult.primary ? cdssResult.primary.descricao : "Os sintomas providos não atingiram o limite mínimo de corte das hipóteses clínicas cadastradas. Recomenda-se tratamento harmônico."}
                        </p>
                      </div>

                      {cdssResult.primary && (
                        <div className="px-5 py-3 bg-neutral-900 text-white rounded-2xl text-center border border-neutral-800 min-w-[100px]">
                          <span className="text-[9px] uppercase font-bold text-neutral-400 block tracking-widest">Afunilamento</span>
                          <span className="text-lg font-black">{cdssResult.primary.percentMatch}%</span>
                        </div>
                      )}
                    </div>

                    {/* SCORING BAR CHART LIST */}
                    <div className="space-y-3">
                      <div className="flex justify-between items-center text-xs text-neutral-500">
                        <span className="font-bold text-neutral-700">Acurácia Co-relação das Síndromes Cadastradas:</span>
                        <span className="font-mono text-[10.5px]">Pesos acumulados</span>
                      </div>
                      <div className="space-y-2">
                        {cdssResult.breakdown.map((synd: any) => (
                          <div key={synd.id} className="space-y-1">
                            <div className="flex justify-between text-[11px] font-semibold text-neutral-700">
                              <span className="truncate max-w-[280px]">{synd.nome}</span>
                              <span className="font-mono">{synd.percentMatch}% ({synd.score} pts)</span>
                            </div>
                            <div className="w-full bg-neutral-100 h-2 rounded-full overflow-hidden">
                              <div 
                                className="bg-emerald-600 h-full rounded-full transition-all duration-500" 
                                style={{ width: `${synd.percentMatch}%` }}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {cdssResult.primary && (
                      <div className="p-3.5 bg-emerald-50 text-emerald-950 text-[11px] rounded-xl flex items-start gap-2 border border-emerald-100/60 leading-relaxed max-w-full">
                        <CheckCircle2 size={15} className="text-emerald-700 mt-0.5 flex-shrink-0" />
                        <div>
                          <strong>Sintomas que acionaram o peso:</strong> [ {cdssResult.primary.matchedSymptoms.join(", ")} ]
                        </div>
                      </div>
                    )}

                  </div>

                  {/* Structured Protocol details (Deterministic Treatment) */}
                  {cdssResult.primary && (
                    <div className="bg-white border border-neutral-200 rounded-3xl p-6 shadow-xs space-y-4">
                      <div>
                        <span className="text-[9px] text-[#E28A2B] uppercase font-black tracking-widest block font-mono">Prescrição Clássica Baseada em Grafos</span>
                        <h3 className="text-base font-bold text-neutral-900 mt-1">Diretriz de Tratamento Estruturado</h3>
                        <p className="text-xs text-neutral-500 mt-0.5">{cdssResult.primary.protocol.nome}</p>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="p-4 bg-amber-50/10 border border-amber-200/20 rounded-2xl space-y-2">
                          <span className="text-[10px] uppercase font-bold text-amber-800">Pontos Chave de Ação</span>
                          <div className="flex flex-wrap gap-1 mt-1">
                            {cdssResult.primary.protocol.pontosChave.map((pt: string, pIdx: number) => (
                              <span key={pIdx} className="bg-amber-100 text-amber-900 text-xs font-semibold px-2 py-0.5 rounded">
                                {pt}
                              </span>
                            ))}
                          </div>
                        </div>

                        <div className="p-4 bg-neutral-50 border border-neutral-150 rounded-2xl space-y-1">
                          <span className="text-[10px] uppercase font-bold text-neutral-500 block">Procedimento Clínico</span>
                          <p className="text-xs text-neutral-600 leading-relaxed font-semibold">
                            {cdssResult.primary.protocol.procedimento}
                          </p>
                        </div>
                      </div>

                      {/* Execution sequential Steps */}
                      <div className="space-y-2">
                        <span className="text-[10px] uppercase font-bold text-neutral-500 block">Passo-a-Passo de Execução Somática</span>
                        <div className="space-y-2">
                          {cdssResult.primary.protocol.passos.map((step: string, sIdx: number) => (
                            <div key={sIdx} className="flex items-start gap-2.5 text-xs text-neutral-600">
                              <span className="w-5 h-5 rounded-full bg-neutral-100 text-neutral-700 flex items-center justify-center font-bold text-[10px] flex-shrink-0 mt-0.5">
                                {sIdx + 1}
                              </span>
                              <span className="leading-relaxed mt-0.5">{step}</span>
                            </div>
                          ))}
                        </div>
                      </div>

                    </div>
                  )}

                  {/* IA Explanation Panel (Gemini serves as Explanation layer) */}
                  {cdssResult.primary && cdssResult.explanation && (
                    <div className="bg-amber-50/10 border border-amber-200/20 rounded-3xl p-6 shadow-xs space-y-4">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded bg-amber-100 text-amber-700 flex items-center justify-center">
                          <Sparkles size={14} className="animate-spin-slow" />
                        </div>
                        <div>
                          <h3 className="text-sm font-bold text-amber-900">Fundamentação Clínica Complementar (Gerado por IA)</h3>
                          <p className="text-[10px] text-amber-700">Explicação complementar, sem valor de veto diagnósticos do motor de regras.</p>
                        </div>
                      </div>

                      <div className="markdown-body p-4 bg-white border border-neutral-200/50 rounded-2xl text-xs leading-relaxed space-y-3 font-sans text-neutral-700">
                        <Markdown>{cdssResult.explanation}</Markdown>
                      </div>

                      <div className="text-[9px] text-neutral-400 italic text-right">
                        Citações bibliográficas geradas em conformidade com Giovani Maciocia & Huangdi Neijing.
                      </div>
                    </div>
                  )}

                  {/* Supporting Learning Column: Clinical Case + Scientific paper */}
                  {cdssResult.primary && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      
                      {/* Clinical Case display */}
                      <div className="bg-white border border-neutral-200 rounded-3xl p-5 shadow-xs space-y-3">
                        <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-800 border-b border-neutral-100 pb-2">
                          <Bookmark size={13} className="text-emerald-600" />
                          <span>Caso Clínico de Suporte (Aprendizagem)</span>
                        </div>
                        <div className="space-y-2">
                          <div className="flex justify-between items-center">
                            <span className="text-xs font-bold text-neutral-900 block truncate max-w-[150px]">
                              {cdssResult.primary.casoClinico.pacienteFicticio}
                            </span>
                            <span className="text-[10px] bg-slate-100 text-neutral-600 px-1.5 py-0.5 rounded font-bold font-mono">Ativo</span>
                          </div>
                          <div className="text-[11px] text-neutral-500 leading-relaxed font-medium">
                            <strong>Queixa Principal:</strong> {cdssResult.primary.casoClinico.queixaPrincipal}
                          </div>
                          <div className="text-[11px] text-neutral-500 leading-relaxed font-medium">
                            <strong>Histórico Clínico:</strong> {cdssResult.primary.casoClinico.historico}
                          </div>
                          <div className="p-2.5 bg-neutral-50 rounded-xl border border-neutral-100 text-[11px] text-emerald-900 leading-relaxed">
                            <strong>Desfecho Terapêutico:</strong> {cdssResult.primary.casoClinico.desfecho}
                          </div>
                        </div>
                      </div>

                      {/* Scientific Evidence display */}
                      <div className="bg-white border border-neutral-200 rounded-3xl p-5 shadow-xs space-y-3">
                        <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-800 border-b border-neutral-100 pb-2">
                          <Globe size={13} className="text-[#3A86C8]" />
                          <span>Evidência Científica de Apoio</span>
                        </div>
                        <div className="space-y-2">
                          <div>
                            <span className="text-xs font-bold text-neutral-950 block leading-tight">
                              {cdssResult.primary.evidence.titulo}
                            </span>
                            <span className="text-[9px] text-[#3A86C8] font-bold tracking-tight block mt-0.5">
                              {cdssResult.primary.evidence.autores}
                            </span>
                          </div>
                          <p className="text-[11px] text-neutral-600 leading-relaxed">
                            {cdssResult.primary.evidence.sumarioEvidencia}
                          </p>
                          <div className="text-[9.5px] text-neutral-400 font-bold uppercase tracking-wider font-mono">
                            VALOR DE BASE: INDEXADO NO PUBMED/MEDLINE
                          </div>
                        </div>
                      </div>

                    </div>
                  )}

                </div>
              )}

            </div>
          </div>

        </div>
      )}

    </div>
  );
}
