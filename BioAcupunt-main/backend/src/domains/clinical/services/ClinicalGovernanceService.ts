
import { prisma } from "../../../prismaClient";
import { logger } from "../../../utils/logger";

export enum ClinicalHealthStatus {
  HEALTHY = "HEALTHY",
  INCONSISTENT = "INCONSISTENT", // Missing relationships
  STALE = "STALE", // Clinical data changed but diagnosis not re-run
  ORPHANED = "ORPHANED",
}

export class ClinicalGovernanceService {
  /**
   * Validates the integrity of a complete clinical record
   */
  async validateRecordIntegrity(anamnesisId: string): Promise<{ status: ClinicalHealthStatus; issues: string[] }> {
    const issues: string[] = [];
    
    const record = await prisma.anamnesis.findUnique({
      where: { id: anamnesisId },
      include: {
        queixa: true,
        diagnostico: true,
        treatmentPlan: true,
        patient: true,
      }
    });

    if (!record) {
      return { status: ClinicalHealthStatus.ORPHANED, issues: ["Anamnesis record does not exist"] };
    }

    if (!record.patient) issues.push("Patient reference missing");
    if (!record.queixa) issues.push("Chief complaint (Queixa) module missing");
    
    // Check if diagnosis exists for a treatment plan
    if (record.treatmentPlan && !record.diagnostico) {
      issues.push("Treatment plan exists without an accompanying Diagnosis record (Logical Inconsistency)");
    }

    // Check for staleness (simple check: if anamnesis was updated after diagnosis)
    if (record.diagnostico && record.updatedAt > record.diagnostico.createdAt) {
      return { status: ClinicalHealthStatus.STALE, issues: ["Clinical data updated post-diagnosis"] };
    }

    const status = issues.length > 0 ? ClinicalHealthStatus.INCONSISTENT : ClinicalHealthStatus.HEALTHY;
    
    return { status, issues };
  }

  /**
   * Scans the database for clinical inconsistencies
   */
  async runGlobalConsistencyAudit() {
    logger.info("Starting Global Clinical Consistency Audit", { domain: 'clinical', action: 'AUDIT_START' });
    
    const orphans = await prisma.diagnosisRecord.findMany({
      where: {
        anamnesis: null
      }
    });

    if (orphans.length > 0) {
      logger.warn(`Found ${orphans.length} orphaned DiagnosisRecords`, { 
        domain: 'clinical', 
        action: 'AUDIT_ISSUE', 
        data: { orphans: orphans.map(o => o.id) } 
      });
    }

    // More complex scans would go here
    
    logger.info("Global Consistency Audit Finished", { domain: 'clinical', action: 'AUDIT_FINISH' });
  }

  /**
   * Reconciles Audit Logs with Database State
   */
  async reconcileAuditLogs() {
    const start = Date.now();
    logger.info("Starting Audit Reconciliation Pipeline", { domain: 'clinical', action: 'RECONCILE_START' });

    // 1. Find all audits for "DIAGNOSIS_GENERATED"
    const diagnosisAudits = await prisma.clinicalAudit.findMany({
      where: { action: 'DIAGNOSIS_GENERATED' },
      orderBy: { createdAt: 'desc' },
      take: 100
    });

    let mismatches = 0;
    for (const audit of diagnosisAudits) {
      const data = audit.data ? JSON.parse(audit.data) : {};
      const { anamnesisId } = data;

      if (anamnesisId) {
        // Verify if record exists in DB
        const record = await prisma.diagnosisRecord.findUnique({ where: { anamnesisId } });
        if (!record) {
          mismatches++;
          logger.warn(`Audit mismatch found: Diagnosis recorded in audit but missing in DB for anamnesis ${anamnesisId}`, {
            traceId: audit.traceId,
            domain: 'clinical',
            action: 'RECONCILE_MISMATCH'
          });
        }
      }
    }

    logger.info("Audit Reconciliation Pipeline Finished", { 
      domain: 'clinical', 
      action: 'RECONCILE_FINISH',
      latencyMs: Date.now() - start,
      data: { checked: diagnosisAudits.length, mismatches }
    });

    return { checked: diagnosisAudits.length, mismatches };
  }
}
