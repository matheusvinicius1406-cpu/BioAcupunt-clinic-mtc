import { prisma } from "../../../prismaClient";
import { AppointmentDTO } from "../types/appointment.types";

export class AppointmentRepository {
  async findAll(filters: any) {
    return await prisma.appointment.findMany({
      where: filters,
      orderBy: { date: 'asc' },
      include: { patient: { select: { name: true, phone: true } } }
    });
  }

  async findById(id: string) {
    return await prisma.appointment.findUnique({
      where: { id },
      include: { patient: true }
    });
  }

  async create(data: AppointmentDTO) {
    return await prisma.appointment.create({
      data: {
        ...data,
        date: new Date(data.date)
      } as any
    });
  }

  async update(id: string, data: Partial<AppointmentDTO>) {
    const updateData = { ...data };
    if (updateData.date) updateData.date = new Date(updateData.date);
    return await prisma.appointment.update({
      where: { id },
      data: updateData as any
    });
  }

  async delete(id: string) {
    return await prisma.appointment.delete({ where: { id } });
  }

  async findByPatient(patientId: string) {
    return await prisma.appointment.findMany({
      where: { patientId },
      orderBy: { date: 'desc' }
    });
  }

  async getInactivePatients() {
    const sixtyDaysAgo = new Date();
    sixtyDaysAgo.setDate(sixtyDaysAgo.getDate() - 60);

    return await prisma.patient.findMany({
      where: {
        status: 'ACTIVE',
        appointments: {
          none: {
            date: { gte: sixtyDaysAgo }
          }
        }
      },
      orderBy: { updatedAt: 'desc' }
    });
  }
}
