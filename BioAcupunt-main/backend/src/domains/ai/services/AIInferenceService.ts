
import { DiagnosisResultDTO } from "../../clinical/types/clinical.types";
import { ClinicalEngine } from "../../clinical/services/ClinicalEngineService";
import { logger } from "../../../utils/logger";
import { prisma } from "../../../prismaClient";
import { hashString } from "../../../utils/hash";

const engine = new ClinicalEngine();

// Simple in-memory cache for production stability
const inferenceCache = new Map<string, { result: DiagnosisResultDTO, timestamp: number }>();
const CACHE_TTL = 1000 * 60 * 15; // 15 minutes for clinical stability

// High-performance inference tracking to prevent redundant parallel executions
const activeInferences = new Map<string, Promise<DiagnosisResultDTO>>();

export class AIInferenceService {
  async getDiagnosis(anamnesisId: string, traceId?: string): Promise<DiagnosisResultDTO> {
    // 1. Generate a robust cache key based on clinical state and time window
    const cacheKey = await this.generateStableKey(anamnesisId);

    // 2. Check cache
    const cached = inferenceCache.get(cacheKey);
    if (cached && (Date.now() - cached.timestamp < CACHE_TTL)) {
      logger.info(`CACHE HIT: Returning stable diagnosis for ${anamnesisId}`, { 
        traceId, domain: 'ai', action: 'CACHE_HIT', data: { latencyMs: 0, cacheKey } 
      });
      return cached.result;
    }

    // 3. Check for an active identical inference (Parallel Coalescing)
    const active = activeInferences.get(cacheKey);
    if (active) {
      logger.info(`CONCURRENCY: Joining existing active inference for ${anamnesisId}`, { traceId, domain: 'ai', data: { cacheKey } });
      return active;
    }

    // 4. Execute new inference with protection
    const inferencePromise = engine.analyzeConsultation(anamnesisId, traceId);
    activeInferences.set(cacheKey, inferencePromise);

    try {
      const result = await inferencePromise;
      inferenceCache.set(cacheKey, { result, timestamp: Date.now() });
      return result;
    } finally {
      activeInferences.delete(cacheKey);
    }
  }

  /**
   * Generates a stable key based on symptoms + anamnesisId + 10-minute time bucket
   */
  private async generateStableKey(anamnesisId: string): Promise<string> {
    const anamnesis = await prisma.anamnesis.findUnique({
      where: { id: anamnesisId },
      include: { queixa: true }
    });
    
    const symptoms = anamnesis?.queixa?.principal || "no-symptoms";
    const timeBucket = Math.floor(Date.now() / (1000 * 60 * 10)); // 10-minute intervals
    
    return hashString(`${anamnesisId}:${symptoms}:${timeBucket}`);
  }

  invalidateCache(anamnesisId: string) {
    // Note: Since we use complex keys, simple invalidation by ID is harder.
    // In prod, we'd clear by pattern or prefix if using Redis.
    // For now, we clear the entire map or just let TTL handle it.
    inferenceCache.clear();
    logger.info(`Invalidated global inference cache`, { domain: 'ai', action: 'CACHE_INVALIDATE' });
  }
}

export default new AIInferenceService();
