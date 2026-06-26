
export interface QueixaDTO {
  principal: string;
  evolucao?: string;
  melhora?: string;
  piora?: string;
}

export interface BaGangDTO {
  interior?: string;
  exterior?: string;
  frio?: string;
  calor?: string;
  deficiencia?: string;
  excesso?: string;
  yin?: string;
  yang?: string;
}

export interface ZangFuDTO {
  coracao?: string;
  intestinoDelgado?: string;
  pericardio?: string;
  triploAquecedor?: string;
  figado?: string;
  vesiculaBiliar?: string;
  bacoPancreas?: string;
  estomago?: string;
  pulmao?: string;
  intestinoGrosso?: string;
  rim?: string;
  bexiga?: string;
}

export interface TongueExamDTO {
  cor?: string;
  forma?: string;
  saburra?: string;
  umidade?: string;
}

export interface PulseExamDTO {
  impressao?: string;
  posicaoCun?: string;
  posicaoGuan?: string;
  posicaoChi?: string;
}

export interface TreatmentPlanDTO {
  pontos?: string[];
  tecnicas?: string[];
  orientacoes?: string[];
  proximaSessao?: string | Date;
}

export interface ClinicalPayloadDTO {
  id?: string;
  date?: string | Date;
  queixa?: QueixaDTO;
  baGang?: BaGangDTO;
  zangFu?: ZangFuDTO;
  lingua?: TongueExamDTO;
  pulso?: PulseExamDTO;
  plano?: TreatmentPlanDTO;
  mapaDor?: { x: number; y: number; eva: number }[];
  evolution?: string;
  evaFinal?: number;
}

export interface DiagnosisResultDTO {
  patient_id: string;
  state: string;
  normalized: {
    symptom_ids: string[];
    sign_ids: string[];
    unmapped: string[];
  };
  inference: {
    patterns: { id: string; confidence: number }[];
    best_pattern: string | null;
    confidence: number;
    diagnosis: string | null;
  };
  decision: {
    next_state: string;
    allowed_transition: boolean;
    reason: string | null;
  };
  treatment: {
    generated: boolean;
    plan: {
      acupuncture_points: string[];
      herbal: string;
      priority: string;
      frequency: string;
    } | null;
  };
  validation: {
    valid: boolean;
    errors: string[];
  };
  risk: {
    flag: boolean;
    reasons: string[];
  };
  delta: any;
}
