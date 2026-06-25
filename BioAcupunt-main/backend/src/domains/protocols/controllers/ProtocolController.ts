import { Request, Response } from "express";
import { ProtocolRepository } from "../repositories/ProtocolRepository";

const repository = new ProtocolRepository();

export const ProtocolController = {
  listSynergies: async (req: Request, res: Response) => {
    try {
      res.json(await repository.findAllSynergies());
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  listSynergiesByProcedure: async (req: Request, res: Response) => {
    try {
      res.json(await repository.findSynergiesByProcedure(req.params.procedure));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  }
};
