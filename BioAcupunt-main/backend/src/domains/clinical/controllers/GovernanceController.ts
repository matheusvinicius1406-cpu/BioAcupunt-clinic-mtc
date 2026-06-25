
import { Request, Response } from "express";
import { ClinicalGovernanceService } from "../services/ClinicalGovernanceService";
import { SystemHealthService } from "../services/SystemHealthService";
import { logger } from "../../../utils/logger";

const governance = new ClinicalGovernanceService();

export const GovernanceController = {
  checkHealth: async (req: Request, res: Response) => {
    try {
      const records = await governance.reconcileAuditLogs();
      res.json({
        status: SystemHealthService.inSafeMode() ? "SAFE_MODE" : "HEALTHY",
        reason: SystemHealthService.getSafeModeReason(),
        reconciliation: records
      });
    } catch (e) {
      res.status(500).json({ error: (e as Error).message });
    }
  },

  toggleSafeMode: async (req: Request, res: Response) => {
    const { active, reason } = req.body;
    if (active) {
      SystemHealthService.activateSafeMode(reason || "Manual Activation via Governance API");
    } else {
      SystemHealthService.deactivateSafeMode();
    }
    res.json({ active: SystemHealthService.inSafeMode(), reason: SystemHealthService.getSafeModeReason() });
  },

  validateRecord: async (req: Request, res: Response) => {
    const { id } = req.params;
    const result = await governance.validateRecordIntegrity(id);
    res.json(result);
  }
};
