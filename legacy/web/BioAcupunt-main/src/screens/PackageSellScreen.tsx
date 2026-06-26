import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { api } from "../services/api";
import { ArrowLeft, Check, Search, User } from "lucide-react";
import { motion } from "motion/react";

export default function PackageSellScreen() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const packageId = searchParams.get('packageId');
  
  const [patients, setPatients] = useState<any[]>([]);
  const [pkg, setPkg] = useState<any>(null);
  const [selectedPatient, setSelectedPatient] = useState<string>("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [selling, setSelling] = useState(false);

  useEffect(() => {
    async function load() {
      const [pts, pkgs] = await Promise.all([
        api.getPatients(),
        api.getPackages()
      ]);
      setPatients(pts);
      if (packageId) {
        setPkg(pkgs.find((p: any) => p.id === packageId));
      }
      setLoading(false);
    }
    load();
  }, [packageId]);

  const filteredPatients = patients.filter(p => 
    p.name.toLowerCase().includes(search.toLowerCase())
  );

  const handleSell = async () => {
    if (!selectedPatient || !packageId) return;
    setSelling(true);
    try {
      await api.sellPackage(packageId, selectedPatient);
      alert("Venda realizada com sucesso!");
      navigate('/financeiro');
    } catch (error) {
      alert("Erro ao realizar venda.");
    } finally {
      setSelling(false);
    }
  };

  if (loading) return <div className="p-10 text-center animate-pulse">Carregando dados...</div>;

  return (
    <div className="max-w-xl mx-auto">
      <header className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
          <ArrowLeft size={24} />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Vender Pacote</h1>
          <p className="text-gray-500">Pacote selecionado: <span className="text-emerald-700 font-bold">{pkg?.name}</span></p>
        </div>
      </header>

      <div className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 space-y-6">
        <div>
          <label className="block text-sm font-bold text-gray-700 mb-2">Selecione o Paciente</label>
          <div className="relative mb-4">
            <input 
              type="text"
              placeholder="Buscar por nome..."
              className="w-full pl-10 pr-4 py-3 bg-gray-50 rounded-xl border-none focus:ring-2 focus:ring-emerald-500"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          </div>

          <div className="max-h-60 overflow-y-auto border border-gray-50 rounded-xl divide-y divide-gray-50">
            {filteredPatients.map(p => (
              <button
                key={p.id}
                onClick={() => setSelectedPatient(p.id)}
                className={`w-full flex items-center justify-between p-4 transition-colors ${
                  selectedPatient === p.id ? 'bg-emerald-50' : 'hover:bg-gray-50'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${
                    selectedPatient === p.id ? 'bg-emerald-600 text-white' : 'bg-gray-100 text-gray-400'
                  }`}>
                    {p.name.charAt(0)}
                  </div>
                  <div className="text-left">
                    <p className={`font-bold text-sm ${selectedPatient === p.id ? 'text-emerald-900' : 'text-gray-700'}`}>
                      {p.name}
                    </p>
                    <p className="text-xs text-gray-400 text-left">CPF: {p.cpf || '---'}</p>
                  </div>
                </div>
                {selectedPatient === p.id && <Check className="text-emerald-600" size={20} />}
              </button>
            ))}
          </div>
        </div>

        <div className="pt-6 border-t border-gray-50 space-y-4">
          <div className="flex justify-between items-center text-sm font-medium">
            <span className="text-gray-500">Valor do Pacote:</span>
            <span className="text-gray-900 font-bold">R$ {pkg?.price.toLocaleString('pt-BR')}</span>
          </div>
          <button
            onClick={handleSell}
            disabled={!selectedPatient || selling}
            className="w-full py-4 bg-emerald-600 text-white rounded-2xl font-bold shadow-lg shadow-emerald-100 hover:bg-emerald-700 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
          >
            {selling ? "Processando..." : "Confirmar Venda"}
          </button>
        </div>
      </div>
    </div>
  );
}
