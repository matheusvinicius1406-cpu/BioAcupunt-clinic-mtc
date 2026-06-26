import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { ArrowUpCircle, ArrowDownCircle, MoreHorizontal } from "lucide-react";

export function TransactionList({ transactions = [] }: { transactions: any[] }) {
  if (transactions.length === 0) {
    return (
      <div className="text-center py-12 bg-gray-50 rounded-3xl border border-dashed border-gray-200">
        <p className="text-gray-500">Nenhum lançamento recente.</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-3xl overflow-hidden border border-gray-100 shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="bg-gray-50/50 border-b border-gray-100">
              <th className="px-6 py-4 text-xs font-bold text-gray-400 uppercase">Data</th>
              <th className="px-6 py-4 text-xs font-bold text-gray-400 uppercase">Descrição</th>
              <th className="px-6 py-4 text-xs font-bold text-gray-400 uppercase">Tipo</th>
              <th className="px-6 py-4 text-xs font-bold text-gray-400 uppercase text-right">Valor</th>
              <th className="px-6 py-4 text-xs font-bold text-gray-400 uppercase text-right">Ação</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {transactions.map((t) => (
              <tr key={t.id} className="hover:bg-gray-50/50 transition-colors">
                <td className="px-6 py-4 text-sm text-gray-600">
                  {format(new Date(t.date), 'dd/MM/yy', { locale: ptBR })}
                </td>
                <td className="px-6 py-4">
                  <p className="font-bold text-gray-900">{t.description}</p>
                  <p className="text-xs text-gray-400 capitalize">{t.category}</p>
                </td>
                <td className="px-6 py-4">
                  <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold ${
                    t.type === 'RECEITA' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
                  }`}>
                    {t.type === 'RECEITA' ? <ArrowUpCircle size={12} /> : <ArrowDownCircle size={12} />}
                    {t.type === 'RECEITA' ? 'Crédito' : 'Débito'}
                  </span>
                </td>
                <td className={`px-6 py-4 text-sm font-black text-right ${
                  t.type === 'RECEITA' ? 'text-emerald-600' : 'text-rose-600'
                }`}>
                  {t.type === 'RECEITA' ? '+' : '-'} R$ {(t.amount || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                </td>
                <td className="px-6 py-4 text-right">
                  <button className="text-gray-300 hover:text-gray-600 transition-colors">
                    <MoreHorizontal size={20} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
