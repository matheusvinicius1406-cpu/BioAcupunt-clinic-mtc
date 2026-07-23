import { APPOINTMENT_STATUS_LABELS } from "./types";

const DATE_FMT = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

const DATETIME_FMT = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

const TIME_FMT = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
});

const CURRENCY_FMT = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

export function formatCurrency(value: number): string {
  return Number.isFinite(value) ? CURRENCY_FMT.format(value) : "—";
}

const MONTH_FMT = new Intl.DateTimeFormat("pt-BR", {
  month: "short",
  year: "numeric",
});

// "2026-07" -> "jul de 2026". Meio do mês evita deslize de fuso.
export function formatMonth(ym: string): string {
  const [y, m] = ym.split("-").map(Number);
  if (!y || !m) return ym;
  return MONTH_FMT.format(new Date(y, m - 1, 15));
}

export function formatDate(iso: string): string {
  const d = new Date(iso);
  return isNaN(d.getTime()) ? "—" : DATE_FMT.format(d);
}

export function formatDateTime(iso: string): string {
  const d = new Date(iso);
  return isNaN(d.getTime()) ? "—" : DATETIME_FMT.format(d);
}

export function formatTime(iso: string): string {
  const d = new Date(iso);
  return isNaN(d.getTime()) ? "—" : TIME_FMT.format(d);
}

export function statusLabel(status: string): string {
  return APPOINTMENT_STATUS_LABELS[status] ?? status;
}

// Mapeia status de agendamento -> classe de badge.
export function appointmentBadgeClass(status: string): string {
  switch (status) {
    case "COMPLETED":
      return "badge-ok";
    case "CONFIRMED":
      return "badge-info";
    case "SCHEDULED":
      return "badge-neutral";
    case "CANCELLED":
      return "badge-danger";
    case "NO_SHOW":
      return "badge-warn";
    default:
      return "badge-neutral";
  }
}

export function patientBadgeClass(status: string): string {
  const s = status.toUpperCase();
  if (s === "ACTIVE" || s === "ATIVO") return "badge-ok";
  if (s === "INACTIVE" || s === "INATIVO") return "badge-neutral";
  return "badge-neutral";
}
