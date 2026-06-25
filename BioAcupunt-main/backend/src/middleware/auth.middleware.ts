
import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { logger } from '../utils/logger';

export enum UserRole {
  ADMIN = 'ADMIN',
  CLINICIAN = 'CLINICIAN',
  ASSISTANT = 'ASSISTANT',
  READ_ONLY = 'READ_ONLY',
}

export interface AuthUser {
  id: string;
  role: UserRole;
  email: string;
}

const JWT_SECRET = process.env.JWT_SECRET || 'secret-dev-key';

export const authMiddleware = (req: Request, res: Response, next: NextFunction) => {
  const authHeader = req.headers.authorization;

  // FOR DEMO/DEVELOPMENT: If no token provided and not in production, we provide a default CLINICIAN user
  // In real production, this would strictly fail.
  if (!authHeader) {
    if (process.env.NODE_ENV !== 'production') {
      req.user = { id: 'dev-user-id', role: UserRole.ADMIN, email: 'admin@bioacupunt.com' };
      return next();
    }
    return res.status(401).json({ error: 'Missing authentication token' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET) as AuthUser;
    req.user = decoded;
    next();
  } catch (error) {
    logger.error('Authentication failed', error, { traceId: req.traceId });
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
};
