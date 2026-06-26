import { Router } from "express";
import { KnowledgeController } from "../controllers/KnowledgeController";
import { authMiddleware, UserRole } from "../../../middleware/auth.middleware";
import { checkRole } from "../../../middleware/rbac.middleware";

const router = Router();

router.use(authMiddleware);

router.get("/", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), KnowledgeController.list);
router.get("/categories", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), KnowledgeController.categories);
router.get("/search", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), KnowledgeController.search);
router.get("/:id", checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]), KnowledgeController.show);

export default router;
