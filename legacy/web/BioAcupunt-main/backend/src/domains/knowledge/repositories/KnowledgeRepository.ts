import { prisma } from "../../../prismaClient";
import { KnowledgeDTO } from "../types/knowledge.types";

export class KnowledgeRepository {
  async findAll(filters: Partial<KnowledgeDTO>) {
    return await prisma.knowledge.findMany({ 
      where: filters as any, 
      orderBy: { createdAt: 'desc' },
      include: { category: true }
    });
  }

  async findById(id: string) {
    return await prisma.knowledge.findUnique({ 
      where: { id },
      include: { category: true }
    });
  }

  async search(query: string) {
    return await prisma.knowledge.findMany({
      where: {
        OR: [
          { title: { contains: query, mode: 'insensitive' } },
          { content: { contains: query, mode: 'insensitive' } }
        ]
      },
      include: { category: true }
    });
  }

  async getCategories() {
    return await prisma.knowledgeCategory.findMany({
      orderBy: { order: 'asc' }
    });
  }

  async create(data: KnowledgeDTO) {
    return await prisma.knowledge.create({ data: data as any });
  }

  async update(id: string, data: Partial<KnowledgeDTO>) {
    return await prisma.knowledge.update({ where: { id }, data: data as any });
  }
}
