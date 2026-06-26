
import { prisma } from "../../../prismaClient";
import { DiagnosisResultDTO } from "../types/clinical.types";

export class DiagnosisRepository {
  async findByAnamnesisId(anamnesisId: string) {
    return await prisma.diagnosisRecord.findUnique({
      where: { anamnesisId }
    });
  }

  async save(anamnesisId: string, analysis: DiagnosisResultDTO) {
    const diagRecord = {
      diagnosis: analysis.inference?.diagnosis || "Undetermined",
      rationale: analysis.decision?.reason || "Deterministic output",
      syndromes: analysis.inference?.patterns?.map((p: any) => p.id) || [],
      confidenceScore: analysis.inference?.confidence || 0.0,
    };

    return await prisma.diagnosisRecord.upsert({
      where: { anamnesisId },
      create: {
        anamnesisId,
        ...diagRecord
      },
      update: {
        ...diagRecord
      }
    });
  }
}
