import { Request, Response } from "express";
import { KnowledgeRepository } from "../repositories/KnowledgeRepository";

const repository = new KnowledgeRepository();

export const KnowledgeController = {
  list: async (req: Request, res: Response) => {
    try {
      res.json(await repository.findAll(req.query));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  show: async (req: Request, res: Response) => {
    try {
      const item = await repository.findById(req.params.id);
      if (!item) return res.status(404).json({ error: "Article not found" });
      res.json(item);
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  search: async (req: Request, res: Response) => {
    try {
      res.json(await repository.search(req.query.q as string));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  categories: async (req: Request, res: Response) => {
    try {
      res.json(await repository.getCategories());
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  }
};
