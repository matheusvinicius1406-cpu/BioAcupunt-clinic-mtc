import { z } from "zod";

export const ChatSchema = z.object({
  prompt: z.string().min(1),
  patientId: z.string().optional(),
  history: z.array(z.any()).optional(),
  contextData: z.any().optional()
});
