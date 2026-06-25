import crypto from "crypto";

/**
 * Simple hashing utility for strings
 */
export function hashString(input: string): string {
  return crypto.createHash('sha256').update(input).digest('hex');
}
