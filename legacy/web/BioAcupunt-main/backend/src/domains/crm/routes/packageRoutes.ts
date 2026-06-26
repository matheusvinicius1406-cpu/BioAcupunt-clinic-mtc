import { Router } from "express";
import { PackageController } from "../controllers/PackageController";
import { authMiddleware, UserRole } from "../../../middleware/auth.middleware";
import { checkRole } from "../../../middleware/rbac.middleware";

const router = Router();

router.use(authMiddleware);

router.get("/", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PackageController.list);
router.post("/", checkRole([UserRole.ADMIN]), PackageController.create);
router.post("/sell", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PackageController.sell);
router.get("/patient/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PackageController.listByPatient);
router.post("/session/:sessionId/use", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PackageController.useSession);
router.patch("/session/:sessionId/use", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PackageController.useSession);
router.get("/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), PackageController.show);
router.put("/:id", checkRole([UserRole.ADMIN]), PackageController.update);
router.delete("/:id", checkRole([UserRole.ADMIN]), PackageController.delete);

export default router;
