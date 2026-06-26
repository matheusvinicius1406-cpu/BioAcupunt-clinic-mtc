
import { logger } from "../../../utils/logger";

export class SystemHealthService {
  private static isSafeModeActive = false;
  private static lastError: string | null = null;

  static activateSafeMode(reason: string) {
    if (!this.isSafeModeActive) {
      this.isSafeModeActive = true;
      this.lastError = reason;
      logger.error(`!!! SYSTEM ENTERING CLINICAL SAFE MODE !!! Reason: ${reason}`, {
        domain: 'system',
        action: 'SAFE_MODE_ACTIVATE'
      });
    }
  }

  static deactivateSafeMode() {
    this.isSafeModeActive = false;
    this.lastError = null;
    logger.info("System exiting clinical safe mode", { domain: 'system', action: 'SAFE_MODE_DEACTIVATE' });
  }

  static inSafeMode() {
    return this.isSafeModeActive;
  }

  static getSafeModeReason() {
    return this.lastError;
  }
}
