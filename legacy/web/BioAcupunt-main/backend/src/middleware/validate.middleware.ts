
import { Request, Response, NextFunction } from 'express';
import { ZodSchema, ZodError } from 'zod';
import { logger } from '../utils/logger';

export const validate = (schema: ZodSchema) => {
  return async (req: Request, res: Response, next: NextFunction) => {
    try {
      await schema.parseAsync({
        body: req.body,
        query: req.query,
        params: req.params,
      });
      return next();
    } catch (error) {
      if (error instanceof ZodError) {
        logger.warn('Validation error', {
          traceId: req.traceId,
          domain: req.originalUrl.split('/')[2],
          action: 'VALIDATION',
          data: error.issues
        });
        return res.status(400).json({ 
          error: 'Validation failed', 
          details: error.issues.map(e => ({ path: e.path, message: e.message })) 
        });
      }
      return res.status(500).json({ error: 'Internal server error during validation' });
    }
  };
};
