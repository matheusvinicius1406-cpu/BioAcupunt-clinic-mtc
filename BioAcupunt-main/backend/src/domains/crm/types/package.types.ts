
export interface PackageDTO {
  id?: string;
  name: string;
  description?: string;
  price: number;
  totalSessions: number;
  validityDays?: number;
}

export interface PackageSessionDTO {
  id?: string;
  patientId: string;
  packageId: string;
  totalSessions: number;
  usedSessions: number;
  status: 'ACTIVE' | 'COMPLETED' | 'EXPIRED';
  expiryDate?: string | Date;
}
