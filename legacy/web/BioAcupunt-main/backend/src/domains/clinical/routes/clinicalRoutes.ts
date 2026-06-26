import { Router } from "express";
import { ClinicalController } from "../controllers/ClinicalController";
import { GovernanceController } from "../controllers/GovernanceController";
import { authMiddleware, UserRole } from "../../../middleware/auth.middleware";
import { checkRole } from "../../../middleware/rbac.middleware";
import { validate } from "../../../middleware/validate.middleware";
import { ClinicalPayloadSchema } from "../schemas/clinical.schema";

const router = Router();

// Apply auth to all clinical routes
router.use(authMiddleware);

// Standard Clinical Routes
router.post("/save/:patientId", 
  checkRole([UserRole.ADMIN, UserRole.CLINICIAN]), 
  validate(ClinicalPayloadSchema),
  ClinicalController.save
);
router.post("/analyze", checkRole([UserRole.ADMIN, UserRole.CLINICIAN]), ClinicalController.analyze);
router.get("/history/:patientId", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), ClinicalController.getHistory);

// Governance & Resilience Routes
router.get("/governance/health", checkRole([UserRole.ADMIN]), GovernanceController.checkHealth);
router.post("/governance/safe-mode", checkRole([UserRole.ADMIN]), GovernanceController.toggleSafeMode);
router.get("/governance/validate/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN]), GovernanceController.validateRecord);

export default router;
