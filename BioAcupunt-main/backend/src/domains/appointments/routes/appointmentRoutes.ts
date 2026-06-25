import { Router } from "express";
import { AppointmentController } from "../controllers/AppointmentController";
import { authMiddleware, UserRole } from "../../../middleware/auth.middleware";
import { checkRole } from "../../../middleware/rbac.middleware";

const router = Router();

router.use(authMiddleware);

router.get("/", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), AppointmentController.list);
router.post("/", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), AppointmentController.create);
router.get("/inactive", checkRole([UserRole.ADMIN, UserRole.CLINICIAN]), AppointmentController.listInactivePatients);
router.get("/patient/:patientId", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), AppointmentController.listByPatient);
router.get("/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), AppointmentController.show);
router.put("/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), AppointmentController.update);
router.delete("/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN]), AppointmentController.delete);

export default router;
