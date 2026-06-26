import { Request, Response } from "express";
import { AppointmentRepository } from "../repositories/AppointmentRepository";

const repository = new AppointmentRepository();

export const AppointmentController = {
  list: async (req: Request, res: Response) => {
    try {
      const filters: any = {};
      if (req.query.date) {
        const date = new Date(req.query.date as string);
        filters.date = {
          gte: new Date(date.setHours(0, 0, 0, 0)),
          lte: new Date(date.setHours(23, 59, 59, 999))
        };
      }
      res.json(await repository.findAll(filters));
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
      if (!item) return res.status(404).json({ error: "Appointment not found" });
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
  listByPatient: async (req: Request, res: Response) => {
    try {
      res.json(await repository.findByPatient(req.params.patientId));
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  },
  listInactivePatients: async (req: Request, res: Response) => {
    try {
      res.json(await repository.getInactivePatients());
    } catch (e: unknown) {
      res.status(500).json({ error: (e as Error).message });
    }
  }
};
