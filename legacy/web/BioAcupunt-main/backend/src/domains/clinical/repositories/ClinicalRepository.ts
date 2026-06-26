import { prisma } from "../../../prismaClient";
import { ClinicalPayloadDTO } from "../types/clinical.types";
import aiInferenceService from "../../ai/services/AIInferenceService";

export class ClinicalRepository {
  async saveAnamnesis(patientId: string, data: ClinicalPayloadDTO) {
    const anamnesisId = data.id || undefined;

    const result = await prisma.$transaction(async (tx) => {
      // 1. Upsert base Anamnesis record
      const anamnesis = await tx.anamnesis.upsert({
        where: { id: anamnesisId || 'new-id' },
        create: { 
          patientId,
          date: data.date ? new Date(data.date) : new Date(),
          evolution: data.evolution,
          evaInitial: data.mapaDor?.[0]?.eva,
          evaFinal: data.evaFinal,
        },
        update: {
          date: data.date ? new Date(data.date) : undefined,
          evolution: data.evolution,
          evaFinal: data.evaFinal,
        }
      });

      const id = anamnesis.id;

      // 2. Cascade Upserts for Domain Modules
      if (data.queixa) {
        await tx.queixa.upsert({
          where: { anamnesisId: id },
          create: { anamnesisId: id, ...data.queixa, mapaDor: data.mapaDor as any },
          update: { ...data.queixa, mapaDor: data.mapaDor as any }
        });
      }

      if (data.baGang) {
        await tx.baGangAssessment.upsert({
          where: { anamnesisId: id },
          create: { anamnesisId: id, ...data.baGang },
          update: { ...data.baGang }
        });
      }

      if (data.zangFu) {
        await tx.zangFuAssessment.upsert({
          where: { anamnesisId: id },
          create: { anamnesisId: id, ...data.zangFu },
          update: { ...data.zangFu }
        });
      }

      if (data.lingua) {
        await tx.tongueExam.upsert({
          where: { anamnesisId: id },
          create: { anamnesisId: id, ...data.lingua },
          update: { ...data.lingua }
        });
      }

      if (data.pulso) {
        await tx.pulseExam.upsert({
          where: { anamnesisId: id },
          create: { anamnesisId: id, ...data.pulso },
          update: { ...data.pulso }
        });
      }

      if (data.plano) {
        const { orientacoes, ...planoMeta } = data.plano;
        const orientacaoStr = Array.isArray(orientacoes) ? orientacoes.join("\n") : orientacoes;

        await tx.treatmentPlan.upsert({
          where: { anamnesisId: id },
          create: { 
            anamnesisId: id, 
            ...planoMeta, 
            orientacoes: orientacaoStr 
          },
          update: { 
            ...planoMeta, 
            orientacoes: orientacaoStr 
          }
        });
      }

      return anamnesis;
    });

    // Invalidate AI cache for this record to ensure medically accurate next analysis
    aiInferenceService.invalidateCache(result.id);

    return result;
  }

  async getHistoryByPatient(patientId: string) {
    return await prisma.anamnesis.findMany({
      where: { patientId },
      orderBy: { date: 'desc' },
      include: {
        queixa: true,
        baGang: true,
        zangFu: true,
        lingua: true,
        pulso: true,
        diagnostico: true,
        treatmentPlan: true
      }
    });
  }

  async findAnamnesisWithContext(id: string) {
    return await prisma.anamnesis.findUnique({
      where: { id },
      include: {
        patient: true,
        queixa: true,
        baGang: true,
        zangFu: true,
        lingua: true,
        pulso: true,
        diagnostico: true
      }
    });
  }
}
