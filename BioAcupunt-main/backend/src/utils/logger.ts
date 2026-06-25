
import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';

export enum LogLevel {
  INFO = 'info',
  WARN = 'warn',
  ERROR = 'error',
}

interface LogEntry {
  timestamp: string;
  level: LogLevel;
  traceId?: string;
  requestId?: string;
  domain?: string;
  action?: string;
  message: string;
  data?: any;
  latencyMs?: number;
}

class Logger {
  info(message: string, context?: Partial<LogEntry>) {
    this.log(LogLevel.INFO, message, context);
  }

  warn(message: string, context?: Partial<LogEntry>) {
    this.log(LogLevel.WARN, message, context);
  }

  error(message: string, error?: any, context?: Partial<LogEntry>) {
    this.log(LogLevel.ERROR, message, {
      ...context,
      data: error instanceof Error ? { name: error.name, message: error.message, stack: error.stack } : error,
    });
  }

  private log(level: LogLevel, message: string, context: Partial<LogEntry> = {}) {
    const entry: LogEntry = {
      timestamp: new Date().toISOString(),
      level,
      message,
      ...context,
    };

    if (process.env.NODE_ENV === 'production') {
      console.log(JSON.stringify(entry));
    } else {
      const color = level === LogLevel.ERROR ? '\x1b[31m' : level === LogLevel.WARN ? '\x1b[33m' : '\x1b[32m';
      console.log(`${entry.timestamp} [${entry.traceId || 'NO-TRACE'}] ${color}${level.toUpperCase()}\x1b[0m ${entry.domain ? `(\x1b[36m${entry.domain}\x1b[0m)` : ''} ${message}`, entry.data || '');
    }
  }
}

export const logger = new Logger();

export const requestTraceMiddleware = (req: Request, res: Response, next: NextFunction) => {
  const traceId = (req.headers['x-trace-id'] as string) || uuidv4();
  req['traceId'] = traceId;
  res.setHeader('x-trace-id', traceId);

  const start = Date.now();

  res.on('finish', () => {
    const latencyMs = Date.now() - start;
    logger.info(`${req.method} ${req.originalUrl} - ${res.statusCode}`, {
      traceId,
      domain: req.originalUrl.split('/')[2] || 'system',
      action: 'HTTP_REQUEST',
      latencyMs,
      data: {
        method: req.method,
        url: req.originalUrl,
        statusCode: res.statusCode,
        ip: req.ip,
      },
    });
  });

  next();
};

declare global {
  namespace Express {
    interface Request {
      traceId?: string;
      user?: any;
    }
  }
}
