import { Router } from "express";
import { AIController } from "../controllers/AIController";
import { authMiddleware, UserRole } from "../../../middleware/auth.middleware";
import { checkRole } from "../../../middleware/rbac.middleware";

const router = Router();

router.use(authMiddleware);

router.post("/chat", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), AIController.chat);
router.post("/diagnose/:patientId", checkRole([UserRole.ADMIN, UserRole.CLINICIAN]), AIController.diagnose);

export default router;
