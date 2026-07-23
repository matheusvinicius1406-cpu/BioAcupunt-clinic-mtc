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
