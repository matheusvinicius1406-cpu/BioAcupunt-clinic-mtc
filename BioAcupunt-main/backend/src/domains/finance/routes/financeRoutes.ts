import { Router } from "express";
import { FinanceController } from "../controllers/FinanceController";
import { authMiddleware, UserRole } from "../../../middleware/auth.middleware";
import { checkRole } from "../../../middleware/rbac.middleware";

const router = Router();

router.use(authMiddleware);

router.get("/", checkRole([UserRole.ADMIN]), FinanceController.list);
router.post("/", checkRole([UserRole.ADMIN]), FinanceController.create);
router.get("/summary", checkRole([UserRole.ADMIN]), FinanceController.summary);
router.get("/:id", checkRole([UserRole.ADMIN]), FinanceController.show);
router.put("/:id", checkRole([UserRole.ADMIN]), FinanceController.update);
router.delete("/:id", checkRole([UserRole.ADMIN]), FinanceController.delete);

export default router;
