
import { prisma } from "../../../prismaClient";
import { logger } from "../../../utils/logger";

export class AuditService {
  static async logAction(traceId: string, domain: string, action: string, data: any) {
    // Non-blocking background execution
    setImmediate(async () => {
      try {
        await prisma.clinicalAudit.create({
          data: {
            traceId,
            domain,
            action,
            data: data ? JSON.stringify(data) : null,
          }
        });
      } catch (error) {
        // We only log to console if audit fails to avoid infinite loops
        console.error('CRITICAL: Audit trail persistence failed', error);
      }
    });
  }
}
