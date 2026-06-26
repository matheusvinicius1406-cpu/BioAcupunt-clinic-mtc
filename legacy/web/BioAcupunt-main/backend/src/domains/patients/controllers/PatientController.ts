import { Request, Response } from "express";
import { PatientRepository } from "../repositories/PatientRepository";
import { CreatePatientDTO, UpdatePatientDTO } from "../types/patient.types";

const repository = new PatientRepository();

export const PatientController = {
  list: async (req: Request, res: Response) => {
    try {
      const patients = await repository.findAll();
      res.json(patients);
    } catch (e: unknown) {
      const error = e as Error;
      res.status(500).json({ error: error.message });
    }
  },

  show: async (req: Request, res: Response) => {
    try {
      const patient = await repository.findById(req.params.id);
      if (!patient) return res.status(404).json({ error: "Patient not found" });
      res.json(patient);
    } catch (e: unknown) {
      const error = e as Error;
      res.status(500).json({ error: error.message });
    }
  },

  create: async (req: Request, res: Response) => {
    try {
      const data: CreatePatientDTO = req.body;
      if (data.birthDate) data.birthDate = new Date(data.birthDate);
      const patient = await repository.create(data);
      res.status(201).json(patient);
    } catch (e: unknown) {
      const error = e as Error;
      res.status(500).json({ error: error.message });
    }
  },

  update: async (req: Request, res: Response) => {
    try {
      const data: UpdatePatientDTO = req.body;
      if (data.birthDate) data.birthDate = new Date(data.birthDate);
      
      // Cleanup forbidden fields
      delete data.id;
      
      const patient = await repository.update(req.params.id, data);
      res.json(patient);
    } catch (e: unknown) {
      const error = e as Error;
      res.status(500).json({ error: error.message });
    }
  }
};
