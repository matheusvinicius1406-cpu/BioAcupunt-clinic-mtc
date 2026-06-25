import { Router } from "express";
import { ProtocolController } from "../controllers/ProtocolController";
import { authMiddleware, UserRole } from "../../../middleware/auth.middleware";
import { checkRole } from "../../../middleware/rbac.middleware";

const router = Router();

router.use(authMiddleware);

router.get("/synergies", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), ProtocolController.listSynergies);
router.get("/synergies/procedure/:procedure", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), ProtocolController.listSynergiesByProcedure);

export default router;
