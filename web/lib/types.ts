// Tipos espelhando os schemas Pydantic reais do backend
// (backend/app/schemas/*). Nao invente campos aqui — se um campo nao existe no
// schema do FastAPI, ele nao entra.

export interface TokenPair {
  access_token: string;
  refresh_token: string;
  token_type: string;
}

// backend/app/schemas/auth.py :: UserResponse
export interface User {
  id: number;
  clinic_id: number;
  email: string;
  full_name: string;
  role: string; // "admin" | "professional" | "staff"
}

// backend/app/schemas/patient.py :: PatientResponse
// (document NAO retorna — e armazenado so como hash HMAC no backend)
export interface Patient {
  id: number;
  clinic_id: number;
  name: string;
  status: string;
  created_at: string;
  updated_at: string;
}

// backend/app/schemas/appointment.py :: AppointmentResponse
export interface Appointment {
  id: number;
  clinic_id: number;
  patient_id: number;
  professional_id: number | null;
  scheduled_at: string;
  status: string;
  notes: string;
  created_at: string;
}

// backend/app/models/appointment.py :: AppointmentStatus
export const APPOINTMENT_STATUS_LABELS: Record<string, string> = {
  SCHEDULED: "Agendado",
  CONFIRMED: "Confirmado",
  CANCELLED: "Cancelado",
  COMPLETED: "Concluído",
  NO_SHOW: "Faltou",
};

export const ROLE_LABELS: Record<string, string> = {
  admin: "Administradora",
  professional: "Profissional",
  staff: "Recepção",
};

// backend/app/schemas/transaction.py :: TransactionResponse
export interface Transaction {
  id: number;
  clinic_id: number;
  patient_id: number | null;
  appointment_id: number | null;
  amount_brl: number;
  occurred_on: string; // YYYY-MM-DD
  type: string;
  method: string;
  category: string;
  status: string;
  notes: string;
  created_at: string;
}

// backend/app/schemas/transaction.py :: FinancialSummary
export interface FinancialSummary {
  start: string | null;
  end: string | null;
  net_revenue_brl: number;
  payments_brl: number;
  refunds_brl: number;
  pending_brl: number;
  expenses_brl: number;
  transaction_count: number;
}

// backend/app/schemas/report.py :: ReportsOverview
export interface ReportsOverview {
  patients_total: number;
  appointments_total: number;
  appointments_by_status: Record<string, number>;
  finance: FinancialSummary;
}

// backend/app/models/transaction.py :: TransactionType / TransactionStatus
export const TRANSACTION_TYPE_LABELS: Record<string, string> = {
  PAGAMENTO: "Pagamento",
  REEMBOLSO: "Reembolso",
  DESPESA: "Despesa",
};

export const TRANSACTION_STATUS_LABELS: Record<string, string> = {
  PAGO: "Pago",
  PENDENTE: "Pendente",
  REEMBOLSADO: "Reembolsado",
};

export function transactionBadgeClass(status: string): string {
  switch (status) {
    case "PAGO":
      return "badge-ok";
    case "PENDENTE":
      return "badge-warn";
    case "REEMBOLSADO":
      return "badge-danger";
    default:
      return "badge-neutral";
  }
}

// backend/app/schemas/crm.py :: CrmPatientResponse
export interface CrmPatient {
  id: number;
  clinic_id: number;
  name: string;
  phone: string;
  email: string;
  birth_date: string;
  stage: string;
  total_sessions: number;
  total_revenue_brl: number;
  last_visit: string;
  next_appointment: string;
  tags: string;
  notes: string;
  referral_source: string;
  nps_score: number | null;
  health_insurance: string;
  main_complaint: string;
  created_at: string;
  updated_at: string;
}

// backend/app/schemas/crm.py :: CrmPipelineSummary
export interface CrmPipelineSummary {
  total: number;
  by_stage: Record<string, number>;
  total_revenue_brl: number;
  average_nps: number | null;
}

// app crm/domain/model/PatientStage.kt — ordem do funil preservada.
export const CRM_STAGES: string[] = [
  "FIRST_CONTACT",
  "LEAD",
  "ACTIVE",
  "TREATMENT",
  "MAINTENANCE",
  "INACTIVE",
  "CHURNED",
];

export const CRM_STAGE_LABELS: Record<string, string> = {
  FIRST_CONTACT: "Primeiro contato",
  LEAD: "Lead",
  ACTIVE: "Ativo",
  TREATMENT: "Em tratamento",
  MAINTENANCE: "Manutenção",
  INACTIVE: "Inativo",
  CHURNED: "Churn",
};

export function crmStageBadgeClass(stage: string): string {
  switch (stage) {
    case "ACTIVE":
    case "TREATMENT":
      return "badge-ok";
    case "MAINTENANCE":
      return "badge-info";
    case "FIRST_CONTACT":
    case "LEAD":
      return "badge-neutral";
    case "INACTIVE":
      return "badge-warn";
    case "CHURNED":
      return "badge-danger";
    default:
      return "badge-neutral";
  }
}
