import { Request, Response } from "express";
import { PackageRepository } from "../repositories/PackageRepository";

const repository = new PackageRepository();

export const PackageController = {
  list: async (req: Request, res: Response) => {
    try {
      res.json(await repository.findAll());
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  create: async (req: Request, res: Response) => {
    try {
      res.status(201).json(await repository.create(req.body));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  show: async (req: Request, res: Response) => {
    try {
      const item = await repository.findById(req.params.id);
      if (!item) return res.status(404).json({ error: "Package not found" });
      res.json(item);
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  update: async (req: Request, res: Response) => {
    try {
      res.json(await repository.update(req.params.id, req.body));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  delete: async (req: Request, res: Response) => {
    try {
      await repository.delete(req.params.id);
      res.status(204).send();
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  sell: async (req: Request, res: Response) => {
    try {
      res.status(201).json(await repository.sell(req.body));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  listByPatient: async (req: Request, res: Response) => {
    try {
      res.json(await repository.findSessionsByPatient(req.params.id));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  useSession: async (req: Request, res: Response) => {
    try {
      res.json(await repository.useSession(req.params.sessionId));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  }
};
