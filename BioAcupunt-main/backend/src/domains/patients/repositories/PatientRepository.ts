import { prisma } from "../../../prismaClient";
import { CreatePatientDTO, UpdatePatientDTO } from "../types/patient.types";

export class PatientRepository {
  async findAll() {
    return await prisma.patient.findMany({ 
      where: { status: 'ACTIVE' },
      orderBy: { createdAt: 'desc' },
      include: {
        clinicalRecords: {
          orderBy: { date: 'desc' },
          take: 1,
          include: { queixa: true }
        }
      }
    });
  }

  async findById(id: string) {
    return await prisma.patient.findUnique({
      where: { id },
      include: {
        clinicalRecords: {
          orderBy: { date: 'desc' },
          include: {
            queixa: true,
            baGang: true,
            zangFu: true,
            lingua: true,
            pulso: true,
            diagnostico: true,
            treatmentPlan: true
          }
        },
        appointments: {
          take: 5,
          orderBy: { date: 'desc' }
        },
        packages: true
      }
    });
  }

  async create(data: CreatePatientDTO) {
    return await prisma.patient.create({ data: data as any });
  }

  async update(id: string, data: UpdatePatientDTO) {
    return await prisma.patient.update({ where: { id }, data: data as any });
  }

  async softDelete(id: string) {
    return await prisma.patient.update({ 
      where: { id }, 
      data: { status: 'INACTIVE' } 
    });
  }
}
