import dotenv from "dotenv";
import path from "path";
// Configures dotenv with path resolving to the .env file in the current working directory
dotenv.config({ path: path.resolve(process.cwd(), ".env") });

import express from "express";
import { createServer as createViteServer } from "vite";
import cors from "cors";
import patientRoutes from "./backend/src/domains/patients/routes/patientRoutes";
import clinicalRoutes from "./backend/src/domains/clinical/routes/clinicalRoutes";
import aiRoutes from "./backend/src/domains/ai/routes/aiRoutes";
import appointmentRoutes from "./backend/src/domains/appointments/routes/appointmentRoutes";
import packageRoutes from "./backend/src/domains/crm/routes/packageRoutes";
import financeRoutes from "./backend/src/domains/finance/routes/financeRoutes";
import protocolRoutes from "./backend/src/domains/protocols/routes/protocolRoutes";
import knowledgeRoutes from "./backend/src/domains/knowledge/routes/knowledgeRoutes";
import healthRoutes from "./backend/src/routes/health";
import { requestTraceMiddleware } from "./backend/src/utils/logger";

async function startServer() {
  const app = express();
  const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;

  app.use(cors());
  app.use(express.json());
  app.use(requestTraceMiddleware);

  // API routes
  app.use("/api/patients", patientRoutes);
  app.use("/api/appointments", appointmentRoutes);
  app.use("/api/ai", aiRoutes);
  app.use("/api/clinical", clinicalRoutes);
  app.use("/api/packages", packageRoutes);
  app.use("/api/finance", financeRoutes);
  app.use("/api/protocols", protocolRoutes);
  app.use("/api/knowledge", knowledgeRoutes);
  app.use("/api/health", healthRoutes);

  // Vite middleware for development
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
