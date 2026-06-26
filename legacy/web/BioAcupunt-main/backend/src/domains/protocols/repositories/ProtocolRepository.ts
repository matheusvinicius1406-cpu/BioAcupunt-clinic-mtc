import { prisma } from "../../../prismaClient";

export class ProtocolRepository {
  async findAllSynergies() {
    return await prisma.synergy.findMany();
  }

  async findSynergiesByProcedure(procedure: string) {
    return await prisma.synergy.findMany({ where: { procedure } });
  }

  async findSynergyById(id: string) {
    return await prisma.synergy.findUnique({ where: { id } });
  }
}
