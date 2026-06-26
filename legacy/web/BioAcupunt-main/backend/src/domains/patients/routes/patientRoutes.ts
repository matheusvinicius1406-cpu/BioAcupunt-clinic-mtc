import { Router } from "express";
import { PatientController } from "../controllers/PatientController";
import { authMiddleware, UserRole } from "../../../middleware/auth.middleware";
import { checkRole } from "../../../middleware/rbac.middleware";

const router = Router();

router.use(authMiddleware);

router.get("/", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PatientController.list);
router.get("/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PatientController.show);
router.post("/", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PatientController.create);
router.put("/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN]), PatientController.update);

export default router;
