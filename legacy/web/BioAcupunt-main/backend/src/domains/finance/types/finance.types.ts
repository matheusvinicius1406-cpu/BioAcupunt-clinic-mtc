
export interface FinanceDTO {
  id?: string;
  description: string;
  amount: number;
  type: 'INCOME' | 'EXPENSE';
  category?: string;
  date: string | Date;
  patientId?: string;
  paymentMethod?: string;
  status?: string;
}
