
export interface AppointmentDTO {
  id?: string;
  patientId: string;
  date: string | Date;
  time: string;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
  type?: string;
  notes?: string;
}
