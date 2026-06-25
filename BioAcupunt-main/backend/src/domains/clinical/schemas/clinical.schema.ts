
import { z } from 'zod';

export const QueixaSchema = z.object({
  principal: z.string().min(3),
  evolucao: z.string().optional(),
  melhora: z.string().optional(),
  piora: z.string().optional(),
});

export const BaGangSchema = z.object({
  interior: z.string().optional(),
  exterior: z.string().optional(),
  frio: z.string().optional(),
  calor: z.string().optional(),
  deficiencia: z.string().optional(),
  excesso: z.string().optional(),
  yin: z.string().optional(),
  yang: z.string().optional(),
});

export const ZangFuSchema = z.object({
  coracao: z.string().optional(),
  figado: z.string().optional(),
  bacoPancreas: z.string().optional(),
  pulmao: z.string().optional(),
  rim: z.string().optional(),
  // ... other fields
}).passthrough();

export const TongueExamSchema = z.object({
  cor: z.string().optional(),
  forma: z.string().optional(),
  saburra: z.string().optional(),
  umidade: z.string().optional(),
});

export const PulseExamSchema = z.object({
  impressao: z.string().optional(),
  posicaoCun: z.string().optional(),
  posicaoGuan: z.string().optional(),
  posicaoChi: z.string().optional(),
});

export const TreatmentPlanSchema = z.object({
  pontos: z.array(z.string()).optional(),
  tecnicas: z.array(z.string()).optional(),
  orientacoes: z.string().optional(),
});

export const ClinicalPayloadSchema = z.object({
  id: z.string().uuid().optional(),
  date: z.string().or(z.date()).optional(),
  queixa: QueixaSchema.optional(),
  baGang: BaGangSchema.optional(),
  zangFu: ZangFuSchema.optional(),
  lingua: TongueExamSchema.optional(),
  pulso: PulseExamSchema.optional(),
  plano: TreatmentPlanSchema.optional(),
  evolution: z.string().optional(),
  evaFinal: z.number().optional(),
});
