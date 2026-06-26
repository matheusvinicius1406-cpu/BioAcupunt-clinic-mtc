
import { Request, Response, NextFunction } from 'express';
import { UserRole } from './auth.middleware';
import { logger } from '../utils/logger';

export const checkRole = (allowedRoles: UserRole[]) => {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.user) {
      return res.status(401).json({ error: 'User context missing' });
    }

    const { role } = req.user;

    if (allowedRoles.includes(role)) {
      return next();
    }

    logger.warn(`Permission denied for user ${req.user.id}`, {
      traceId: req.traceId,
      domain: req.originalUrl.split('/')[2],
      action: 'PERMISSION_CHECK',
      data: { required: allowedRoles, current: role }
    });

    return res.status(403).json({ error: 'Insufficient permissions' });
  };
};

export const isAdmin = checkRole([UserRole.ADMIN]);
export const isClinician = checkRole([UserRole.ADMIN, UserRole.CLINICIAN]);
export const isAssistant = checkRole([UserRole.ADMIN, UserRole.CLINICIAN, UserRole.ASSISTANT]);
