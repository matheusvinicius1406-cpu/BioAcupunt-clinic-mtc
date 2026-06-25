
import { Request, Response } from "express";
import geminiService from "../services/geminiService";
import { prisma } from "../../../prismaClient";
import aiInferenceService from "../services/AIInferenceService";
import { logger } from "../../../utils/logger";

export const AIController = {
  chat: async (req: Request, res: Response) => {
    const traceId = req.traceId;
    const { message, patientId, history } = req.body;
    let systemInstruction = "Você é a Dra. Camila, uma assistente virtual especialista em Medicina Tradicional Chinesa e Acupuntura, auxiliando outros profissionais no BioAcupunt Supreme.";
    
    if (patientId) {
      try {
        const patient = await prisma.patient.findUnique({ 
          where: { id: patientId },
          include: { 
            clinicalRecords: { 
              orderBy: { date: 'desc' }, 
              take: 1,
              include: { queixa: true }
            } 
          }
        });
        if (patient) {
          const latestRecord = patient.clinicalRecords?.[0];
          // Mask name for Privacy
          const maskedName = patient.name.split(' ')[0] + ' ' + (patient.name.split(' ')[1]?.[0] || '') + '.';
          
          systemInstruction += `\n\nNo momento, a Dra. Camila está visualizando o prontuário do seguinte paciente:
Nome: ${maskedName}
Sexo: ${patient.sex || 'Não informado'}
Queixa Principal: ${latestRecord?.queixa?.principal || 'Não especificada'}
História Clínica: ${latestRecord?.queixa?.evolucao || 'Nenhum histórico listado'}
Evolução Recente: ${latestRecord?.evolution || 'Sem detalhes de evolução'}

Quando o usuário fizer perguntas rápidas, responda focado nestas queixas ou referencie o histórico do paciente se for aplicável para embasar clinicamente sua conduta terapêutica.`;
        }
      } catch (err) {
        logger.error("Context retrieval failed in AI Chat", err, { traceId });
      }
    }

    try {
      logger.info('Generating AI Chat response', { traceId, domain: 'ai', action: 'CHAT_REQUEST' });
      const response = await geminiService.generateChatResponse(message, systemInstruction, history || []);
      res.json({ response });
    } catch (e: unknown) {
      const error = e as Error;
      logger.error('AI Chat failed', error, { traceId, domain: 'ai' });
      res.status(500).json({ error: error.message });
    }
  },

  diagnose: async (req: Request, res: Response) => {
    const traceId = req.traceId;
    try {
      // Logic for selecting anamnesisId or falling back
      const paramId = req.params.anamneseId || req.params.patientId;
      
      // If it's a patientId, we probably want their latest anamnesis
      let targetAnamnesisId = paramId;
      const patientCheck = await prisma.patient.findUnique({ where: { id: paramId }, select: { id: true } });
      
      if (patientCheck) {
        const latest = await prisma.anamnesis.findFirst({
          where: { patientId: paramId },
          orderBy: { date: 'desc' },
          select: { id: true }
        });
        if (!latest) return res.status(404).json({ error: "No clinical records found for this patient" });
        targetAnamnesisId = latest.id;
      }

      const diagnosis = await aiInferenceService.getDiagnosis(targetAnamnesisId, traceId);
      res.json(diagnosis);
    } catch (e: unknown) {
      const error = e as Error;
      logger.error('Diagnosis generation failed', error, { traceId, domain: 'ai' });
      res.status(500).json({ error: error.message });
    }
  }
};
