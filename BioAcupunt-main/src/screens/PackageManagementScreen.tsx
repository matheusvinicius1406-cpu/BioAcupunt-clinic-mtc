import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { api } from "../services/api";
import { Plus, ShoppingBag, Trash2, CheckCircle, Ticket } from "lucide-react";
import { motion } from "motion/react";

export default function PackageManagementScreen() {
  const [packages, setPackages] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getPackages().then(data => {
      setPackages(data);
      setLoading(false);
    });
  }, []);

  const formatCurrency = (val: number) => val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

  return (
    <div className="max-w-5xl mx-auto">
      <header className="flex items-center justify-between mb-10">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Gestão de Pacotes</h1>
          <p className="text-gray-500">Configure planos de sessões recorrentes</p>
        </div>
        <button className="flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white rounded-xl font-bold hover:bg-emerald-700 transition-all shadow-lg">
          <Plus size={20} /> Novo Pacote
        </button>
      </header>

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1,2,3].map(i => <div key={i} className="h-64 bg-white rounded-3xl animate-pulse" />)}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {packages.map((pkg) => (
            <motion.div 
              key={pkg.id}
              className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 flex flex-col"
            >
              <div className="mb-6">
                <span className="px-3 py-1 bg-emerald-100 text-emerald-700 text-xs font-bold rounded-full uppercase tracking-wider">
                  {pkg.totalSessions} SESSÕES
                </span>
                <h3 className="text-2xl font-bold text-gray-900 mt-4">{pkg.name}</h3>
                <p className="text-gray-500 text-sm mt-2">{pkg.description}</p>
              </div>

              <div className="flex-1 space-y-4 mb-8">
                <div className="flex items-center gap-2 text-sm text-emerald-600 font-medium">
                  <CheckCircle size={16} /> Suporte Prioritário
                </div>
                <div className="flex items-center gap-2 text-sm text-emerald-600 font-medium">
                  <CheckCircle size={16} /> Validade {pkg.validityDays} dias
                </div>
              </div>

              <div className="pt-6 border-t border-gray-50 flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold text-gray-400">VALOR TOTAL</p>
                  <p className="text-xl font-black text-emerald-700">{formatCurrency(pkg.price)}</p>
                </div>
                <Link 
                  to={`/financeiro/pacotes/vender?packageId=${pkg.id}`}
                  className="p-3 bg-emerald-50 text-emerald-600 rounded-xl hover:bg-emerald-100"
                  title="Vender para paciente"
                >
                  <Ticket size={24} />
                </Link>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
