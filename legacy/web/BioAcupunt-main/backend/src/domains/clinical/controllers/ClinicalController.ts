
import { Request, Response } from "express";
import { ClinicalRepository } from "../repositories/ClinicalRepository";
import aiInferenceService from "../../ai/services/AIInferenceService";
import { ClinicalPayloadDTO } from "../types/clinical.types";
import { logger } from "../../../utils/logger";
import { SystemHealthService } from "../services/SystemHealthService";
import { ClinicalGovernanceService, ClinicalHealthStatus } from "../services/ClinicalGovernanceService";
import { idempotency } from "../../../utils/IdempotencyManager";

const repository = new ClinicalRepository();
const governance = new ClinicalGovernanceService();

export const ClinicalController = {
  save: async (req: Request, res: Response) => {
    const traceId = req.traceId;
    const userId = (req as any).user?.id || 'anonymous';
    const requestId = req.headers['x-request-id'] as string;
    
    if (SystemHealthService.inSafeMode()) {
      return res.status(503).json({ 
        error: "System in Safe Mode", 
        message: "Operações de escrita bloqueadas temporariamente para manutenção de integridade.",
        reason: SystemHealthService.getSafeModeReason(),
        requestId
      });
    }

    // PRODUCTION SCALING: Idempotency check to prevent duplicate parallel writes
    const dedupeKey = idempotency.generateKey(userId, 'SAVE_CLINICAL', req.body, requestId);
    const existingResult = idempotency.get(dedupeKey);
    if (existingResult) return res.json(existingResult);

    try {
      const { patientId } = req.params;
      const data: ClinicalPayloadDTO = req.body;
      const start = Date.now();
      
      const result = await repository.saveAnamnesis(patientId, data);
      
      logger.info('Clinical record saved', { 
        requestId, 
        traceId, 
        data: { patientId, latencyMs: Date.now() - start, status: 200 } 
      });

      idempotency.set(dedupeKey, result);
      res.json(result);
    } catch (e: unknown) {
      const error = e as Error;
      logger.error('Save operation failed', error, { requestId, traceId, domain: 'clinical' });
      
      res.status(500).json({ 
        errorCode: "INTERNAL_SERVER_ERROR",
        message: "Erro ao salvar registro clínico. Tente novamente ou contate suporte.",
        requestId 
      });
    }
  },

  analyze: async (req: Request, res: Response) => {
    const traceId = req.traceId;
    const requestId = req.headers['x-request-id'] as string;
    const start = Date.now();

    try {
      const { anamnesisId } = req.body;
      if (!anamnesisId) return res.status(400).json({ errorCode: "MISSING_PARAM", message: "anamnesisId is required", requestId });
      
      const analysis = await aiInferenceService.getDiagnosis(anamnesisId, traceId);
      
      logger.info('Clinical analysis completed', { 
        requestId, 
        traceId, 
        data: { anamnesisId, latencyMs: Date.now() - start, status: 200 } 
      });

      res.json(analysis);
    } catch (e: unknown) {
      const error = e as Error;
      logger.error('Analysis failed', error, { requestId, traceId, domain: 'clinical' });
      res.status(500).json({ 
        errorCode: "ANALYSIS_FAILED",
        message: "O Serviço de Análise Clínica está temporariamente indisponível.",
        requestId
      });
    }
  },

  getHistory: async (req: Request, res: Response) => {
    const traceId = req.traceId;
    try {
      const { patientId } = req.params;
      const history = await repository.getHistoryByPatient(patientId);
      res.json(history);
    } catch (e: unknown) {
      const error = e as Error;
      logger.error('Error fetching clinical history', error, { traceId, domain: 'clinical' });
      res.status(500).json({ error: error.message });
    }
  }
};
