import { Router } from "express";

const router = Router();

router.get("/", (req, res) => {
  res.json({ status: "ok", mode: process.env.DATABASE_URL ? "live" : "offline-memory" });
});

export default router;
