import { prisma } from "../../../prismaClient";
import { FinanceDTO } from "../types/finance.types";

export class FinanceRepository {
  async findAll() {
    return await prisma.finance.findMany({
      orderBy: { date: 'desc' },
      include: { patient: { select: { name: true } } }
    });
  }

  async findById(id: string) {
    return await prisma.finance.findUnique({
      where: { id },
      include: { patient: true }
    });
  }

  async create(data: FinanceDTO) {
    return await prisma.finance.create({
      data: {
        ...data,
        date: new Date(data.date)
      } as any
    });
  }

  async update(id: string, data: Partial<FinanceDTO>) {
    const updateData = { ...data };
    if (updateData.date) updateData.date = new Date(updateData.date);
    return await prisma.finance.update({
      where: { id },
      data: updateData as any
    });
  }

  async delete(id: string) {
    return await prisma.finance.delete({ where: { id } });
  }

  async getSummary() {
    const incomes = await prisma.finance.aggregate({
      where: { type: 'INCOME' },
      _sum: { amount: true }
    });
    const expenses = await prisma.finance.aggregate({
      where: { type: 'EXPENSE' },
      _sum: { amount: true }
    });
    return {
      totalIncome: incomes._sum.amount || 0,
      totalExpense: expenses._sum.amount || 0,
      balance: (incomes._sum.amount || 0) - (expenses._sum.amount || 0)
    };
  }
}
