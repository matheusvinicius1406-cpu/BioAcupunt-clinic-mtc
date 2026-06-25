import { TrendingUp, TrendingDown, DollarSign } from "lucide-react";

export function FinanceSummaryCard({ title, value, type = 'income' }: { title: string, value: string, type?: 'income' | 'expense' | 'balance' }) {
  const isIncome = type === 'income';
  const isBalance = type === 'balance';

  return (
    <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex items-center gap-4">
      <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${
        isBalance ? 'bg-blue-100 text-blue-600' : isIncome ? 'bg-emerald-100 text-emerald-600' : 'bg-rose-100 text-rose-600'
      }`}>
        {isBalance ? <DollarSign size={24} /> : isIncome ? <TrendingUp size={24} /> : <TrendingDown size={24} />}
      </div>
      <div>
        <p className="text-xs font-bold text-gray-400 uppercase tracking-wider">{title}</p>
        <p className="text-xl font-black text-gray-900 mt-0.5">{value}</p>
      </div>
    </div>
  );
}
