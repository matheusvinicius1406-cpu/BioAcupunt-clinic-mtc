
export interface PatientDTO {
  id?: string;
  name: string;
  email?: string;
  phone?: string;
  cpf?: string;
  birthDate?: string | Date;
  sex?: string;
  profession?: string;
  address?: string;
  status?: 'ACTIVE' | 'INACTIVE';
  balance?: number;
}

export interface CreatePatientDTO extends PatientDTO {}
export interface UpdatePatientDTO extends Partial<PatientDTO> {}
