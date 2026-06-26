
import { GoogleGenAI, ThinkingLevel, Type, Schema } from "@google/genai";
import { prisma } from "../../../prismaClient";
import { DiagnosisResultDTO } from "../types/clinical.types";
import { DiagnosisRepository } from "../repositories/DiagnosisRepository";
import { logger } from "../../../utils/logger";
import { AuditService } from "./AuditService";

const diagnosisRepo = new DiagnosisRepository();

export class ClinicalEngine {
  private ai = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY || "",
    httpOptions: { headers: { 'User-Agent': 'aistudio-build' } }
  });

  // Production Standard Model
  private MODEL_NAME = "gemini-3.1-pro-preview";

  async analyzeConsultation(anamnesisId: string, traceId?: string): Promise<DiagnosisResultDTO> {
    const start = Date.now();
    try {
      const anamnesis = await prisma.anamnesis.findUnique({
        where: { id: anamnesisId },
        include: { queixa: true, baGang: true, zangFu: true, lingua: true, pulso: true }
      });

      if (!anamnesis) throw new Error("Consultation not found");

      // Deterministic Inference Execution
      const analysis = await this.runDeterministicInference(this.MODEL_NAME, anamnesis, traceId);
      
      // Atomic Save to transactional database using the inferred data
      const diagRecord = {
        diagnosis: analysis.inference?.diagnosis || "Undetermined",
        rationale: analysis.decision?.reason || "Deterministic output",
        syndromes: analysis.inference?.patterns?.map((p: any) => p.id) || [],
        confidenceScore: analysis.inference?.confidence || 0.0,
      };

      await prisma.diagnosisRecord.upsert({
        where: { anamnesisId },
        create: {
          anamnesisId,
          ...diagRecord
        },
        update: {
          ...diagRecord
        }
      });

      // Auditing
      await AuditService.logAction(traceId || 'SYSTEM', 'clinical', 'DIAGNOSIS_GENERATED', {
        anamnesisId,
        valid: analysis.validation?.valid,
        latency: Date.now() - start
      });

      logger.info(`Operation completed: DIAGNOSIS_GENERATE`, { 
        traceId, 
        data: { anamnesisId, latencyMs: Date.now() - start, status: 'SUCCESS' } 
      });

      return analysis;
    } catch (error) {
      logger.error(`Critical Clinical Engine Failure`, error, { traceId, domain: 'clinical' });
      throw error;
    }
  }

  private async runDeterministicInference(modelName: string, data: any, traceId?: string): Promise<any> {
    const config: any = {
      temperature: 0, // Deterministic Mode
      topP: 0.1,
      responseMimeType: "application/json",
      responseSchema: {
        type: Type.OBJECT,
        properties: {
          patient_id: { type: Type.STRING },
          state: { type: Type.STRING },
          normalized: {
            type: Type.OBJECT,
            properties: {
              symptom_ids: { type: Type.ARRAY, items: { type: Type.STRING } },
              sign_ids: { type: Type.ARRAY, items: { type: Type.STRING } },
              unmapped: { type: Type.ARRAY, items: { type: Type.STRING } }
            }
          },
          inference: {
            type: Type.OBJECT,
            properties: {
              patterns: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    id: { type: Type.STRING },
                    confidence: { type: Type.NUMBER }
                  }
                }
              },
              best_pattern: { type: Type.STRING, nullable: true },
              confidence: { type: Type.NUMBER },
              diagnosis: { type: Type.STRING, nullable: true }
            }
          },
          decision: {
            type: Type.OBJECT,
            properties: {
              next_state: { type: Type.STRING },
              allowed_transition: { type: Type.BOOLEAN },
              reason: { type: Type.STRING, nullable: true }
            }
          },
          treatment: {
            type: Type.OBJECT,
            properties: {
              generated: { type: Type.BOOLEAN },
              plan: {
                type: Type.OBJECT,
                nullable: true,
                properties: {
                  acupuncture_points: { type: Type.ARRAY, items: { type: Type.STRING } },
                  herbal: { type: Type.STRING },
                  priority: { type: Type.STRING },
                  frequency: { type: Type.STRING }
                }
              }
            }
          },
          validation: {
            type: Type.OBJECT,
            properties: {
              valid: { type: Type.BOOLEAN },
              errors: { type: Type.ARRAY, items: { type: Type.STRING } }
            }
          },
          risk: {
            type: Type.OBJECT,
            properties: {
              flag: { type: Type.BOOLEAN },
              reasons: { type: Type.ARRAY, items: { type: Type.STRING } }
            }
          },
          delta: { type: Type.OBJECT }
        },
        required: ["patient_id", "state", "normalized", "inference", "decision", "treatment", "validation", "risk", "delta"]
      }
    };

    const prompt = `
      Você é um Deterministic Clinical State Transformation Function
      Entrada → Transformação → Saída JSON
      Sem explicação. Sem linguagem natural. Sem interpretação livre.

      GLOBAL STATE INPUT:
      {
        "patient_id": "${data.patientId || 'TEMP_001'}",
        "timestamp": "${new Date().toISOString()}",
        "state": "ACTIVE_EVALUATION",
        "input": {
          "raw_symptoms": [${JSON.stringify(data.queixa?.principal || "")}],
          "raw_signs": {
            "pulse": ${JSON.stringify(data.pulso?.impressao || "")},
            "tongue": ${JSON.stringify(data.lingua?.cor || "")},
            "other": {}
          }
        }
      }

      1. NORMALIZATION ENGINE
      Mappings:
      headache -> S001
      nausea -> S002
      night_sweats -> S003
      fatigue -> S004
      chest_pain -> S005
      dyspnea -> S006
      deep_pulse -> SG001
      rapid_pulse -> SG002
      pale_tongue -> SG003
      red_tongue -> SG004
      (Do not invent IDs. Unmapped go to "unmapped")

      2. RISK ENGINE
      S005 or S006 => risk.flag = true

      3. PATTERN INFERENCE ENGINE
      P001 Yin Def: S003 + SG002 => 0.9 / partial 0.6
      P002 Qi Stag: S001 + S002 => 0.85 / partial 0.5
      P003 Blood Def: S004 + SG003 => 0.8 full
      Only accept >= 0.7 as valid.
      Map to Diagnosis: P001 -> D001, P002 -> D002, P003 -> D003.

      4. STATE MACHINE ENGINE
      Transitions:
      NEW -> ACTIVE_EVALUATION
      ACTIVE_EVALUATION -> DIAGNOSED (only if diagnosis != null AND conf >= 0.7)
      ACTIVE_EVALUATION -> ACTIVE_EVALUATION
      DIAGNOSED -> UNDER_TREATMENT
      UNDER_TREATMENT -> STABLE

      5. TREATMENT COMPILER
      D001 -> points: ["KD3", "SP6", "REN4"], herbal: "Liu Wei Di Huang Wan", priority: "high"
      D002 -> points: ["LR3", "PC6", "ST36"], herbal: "Xiao Yao San", priority: "medium"
      D003 -> points: ["ST36", "SP10", "REN6"], herbal: "Si Wu Tang", priority: "medium"

      6. VALIDATION GATE
      Invalid if treatment generated but diagnosis null.
      Invalid if DIAGNOSED without diagnosis.
      Invalid if risk=true and state jump not allowed.
      If invalid: valid = false, next_state = current_state.

      OUTPUT ONLY THE JSON OBJECT, NOTHING ELSE.
    `;

    const model = this.ai.models;
    const response = await this.safeGenerateContent(model, modelName, prompt, config, traceId);

    try {
      const text = response.text.replace(/```json|```/g, "").trim();
      return JSON.parse(text);
    } catch (e) {
      logger.error('AI JSON Decode Failed', e, { traceId, data: { raw: response.text } });
      throw new Error("AI output was not valid JSON");
    }
  }

  private async safeGenerateContent(models: any, modelName: string, contents: string, config: any, traceId?: string): Promise<any> {
    const MAX_RETRIES = 1;
    const TIMEOUT_MS = 25000;

    for (let i = 0; i <= MAX_RETRIES; i++) {
      try {
        const resultPromise = models.generateContent({ model: modelName, contents, config });
        const timeoutPromise = new Promise((_, reject) => 
          setTimeout(() => reject(new Error("AI_TIMEOUT")), TIMEOUT_MS)
        );

        return await Promise.race([resultPromise, timeoutPromise]);
      } catch (error) {
        if (i === MAX_RETRIES) throw error;
        logger.warn(`AI generation failed, retrying (${i + 1}/${MAX_RETRIES})`, { traceId, data: { error: (error as Error).message } });
        await new Promise(res => setTimeout(res, 1000));
      }
    }
  }
}
