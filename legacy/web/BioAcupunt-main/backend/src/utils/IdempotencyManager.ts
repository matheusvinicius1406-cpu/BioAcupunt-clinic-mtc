
import { logger } from "./logger";

/**
 * Production-grade Idempotency Manager
 * Prevents duplicate clinical writes within a short temporal window
 */
class IdempotencyManager {
  private cache = new Map<string, { result: any, timestamp: number }>();
  private TTL = 5000; // 5 seconds window for deduplication

  /**
   * Generates a unique key based on the request context or takes an explicit requestId
   */
  generateKey(userId: string, action: string, payload: any, explicitRequestId?: string): string {
    if (explicitRequestId) return `req:${explicitRequestId}`;
    
    const bodyString = JSON.stringify(payload);
    return `${userId}:${action}:${Buffer.from(bodyString).toString('base64').substring(0, 32)}`;
  }

  /**
   * Checks if a request is a duplicate and returns the previous result if available
   */
  get(key: string): any | null {
    const cached = this.cache.get(key);
    if (cached && (Date.now() - cached.timestamp < this.TTL)) {
      logger.info('Idempotency hit! Reusing previous result.', { domain: 'system', action: 'IDEMPOTENCY_HIT', data: { key } });
      return cached.result;
    }
    return null;
  }

  /**
   * Stores the result of a successful operation
   */
  set(key: string, result: any) {
    this.cache.set(key, { result, timestamp: Date.now() });
    
    // Self-cleaning
    setTimeout(() => this.cache.delete(key), this.TTL + 1000);
  }
}

export const idempotency = new IdempotencyManager();
