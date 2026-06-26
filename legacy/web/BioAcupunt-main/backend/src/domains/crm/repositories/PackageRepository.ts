import { prisma } from "../../../prismaClient";
import { PackageDTO, PackageSessionDTO } from "../types/package.types";

export class PackageRepository {
  async findAll() {
    return await prisma.package.findMany();
  }

  async findById(id: string) {
    return await prisma.package.findUnique({ where: { id } });
  }

  async create(data: PackageDTO) {
    return await prisma.package.create({ data: data as any });
  }

  async update(id: string, data: Partial<PackageDTO>) {
    return await prisma.package.update({ where: { id }, data: data as any });
  }

  async delete(id: string) {
    return await prisma.package.delete({ where: { id } });
  }

  async sell(data: { patientId: string; packageId: string }) {
    const pkg = await this.findById(data.packageId);
    if (!pkg) throw new Error("Package not found");

    // Create session records for the entire package
    const sessions = [];
    for (let i = 1; i <= pkg.totalSessions; i++) {
      sessions.push(
        prisma.packageSession.create({
          data: {
            patientId: data.patientId,
            packageId: data.packageId,
            sessionNumber: i,
            used: false
          }
        })
      );
    }

    return await prisma.$transaction(sessions);
  }

  async findSessionsByPatient(patientId: string) {
    return await prisma.packageSession.findMany({
      where: { patientId },
      include: { package: true },
      orderBy: [{ packageId: 'asc' }, { sessionNumber: 'asc' }]
    });
  }

  async useSession(sessionId: string) {
    return await prisma.packageSession.update({
      where: { id: sessionId },
      data: {
        used: true,
        usedAt: new Date()
      }
    });
  }
}
