import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../services/api";
import { ArrowLeft, Save, Plus, TrendingUp, TrendingDown, Calendar, Tag, CreditCard, User } from "lucide-react";
import { motion } from "motion/react";
import { format } from "date-fns";

export default function FinanceEntryScreen() {
  const navigate = useNavigate();
  const [patients, setPatients] = useState<any[]>([]);
  const [saving, setSaving] = useState(false);

  const [formData, setFormData] = useState({
    description: "",
    value: "",
    type: "RECEITA",
    category: "CONSULTA",
    paymentMethod: "PIX",
    date: format(new Date(), 'yyyy-MM-dd'),
    patientId: ""
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    api.getPatients().then(setPatients);
  }, []);

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.description) newErrors.description = "Descrição é obrigatória";
    if (!formData.value || isNaN(Number(formData.value))) newErrors.value = "Valor inválido";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setSaving(true);
    try {
      await api.createTransaction({
        ...formData,
        value: Number(formData.value)
      });
      navigate("/financeiro");
    } catch (error) {
      alert("Erro ao salvar lançamento.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto pb-12">
      <header className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
          <ArrowLeft size={24} />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Novo Lançamento</h1>
          <p className="text-gray-500">Registre entradas e saídas do consultório</p>
        </div>
      </header>

      <motion.form 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        onSubmit={handleSubmit} 
        className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 space-y-6"
      >
        <div className="grid grid-cols-2 gap-4">
          <button
            type="button"
            onClick={() => setFormData({...formData, type: 'RECEITA'})}
            className={`py-4 rounded-2xl flex items-center justify-center gap-2 font-bold transition-all ${
              formData.type === 'RECEITA' ? 'bg-emerald-600 text-white shadow-lg' : 'bg-gray-100 text-gray-500'
            }`}
          >
            <TrendingUp size={20} /> Receita
          </button>
          <button
            type="button"
            onClick={() => setFormData({...formData, type: 'DESPESA'})}
            className={`py-4 rounded-2xl flex items-center justify-center gap-2 font-bold transition-all ${
              formData.type === 'DESPESA' ? 'bg-rose-600 text-white shadow-lg' : 'bg-gray-100 text-gray-500'
            }`}
          >
            <TrendingDown size={20} /> Despesa
          </button>
        </div>

        <div>
          <label className="block text-sm font-bold text-gray-700 mb-2">Descrição *</label>
          <input
            type="text"
            className={`w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 ${errors.description ? 'ring-2 ring-rose-500' : ''}`}
            placeholder="Ex: Pagamento Consulta João"
            value={formData.description}
            onChange={(e) => setFormData({...formData, description: e.target.value})}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2">Valor (R$) *</label>
            <input
              type="number"
              step="0.01"
              className={`w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 ${errors.value ? 'ring-2 ring-rose-500' : ''}`}
              placeholder="0,00"
              value={formData.value}
              onChange={(e) => setFormData({...formData, value: e.target.value})}
            />
          </div>
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <Calendar size={16} className="text-emerald-500" /> Data
            </label>
            <input
              type="date"
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              value={formData.date}
              onChange={(e) => setFormData({...formData, date: e.target.value})}
            />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <Tag size={16} className="text-emerald-500" /> Categoria
            </label>
            <select
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              value={formData.category}
              onChange={(e) => setFormData({...formData, category: e.target.value})}
            >
              <option value="CONSULTA">Consulta</option>
              <option value="PACOTE">Venda de Pacote</option>
              <option value="MATERIAL">Material Clínico</option>
              <option value="ALUGUEL">Aluguel / Fixos</option>
              <option value="OUTRO">Outros</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <CreditCard size={16} className="text-emerald-500" /> Forma de Pagamento
            </label>
            <select
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              value={formData.paymentMethod}
              onChange={(e) => setFormData({...formData, paymentMethod: e.target.value})}
            >
              <option value="PIX">Pix</option>
              <option value="CARTAO_CREDITO">Cartão de Crédito</option>
              <option value="CARTAO_DEBITO">Cartão de Débito</option>
              <option value="DINHEIRO">Dinheiro</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
            <User size={16} className="text-emerald-500" /> Paciente Relacionado (opcional)
          </label>
          <select
            className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
            value={formData.patientId}
            onChange={(e) => setFormData({...formData, patientId: e.target.value})}
          >
            <option value="">Não relacionado</option>
            {patients.map(p => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
        </div>

        <button
          type="submit"
          disabled={saving}
          className="w-full py-4 bg-emerald-600 text-white rounded-2xl font-bold hover:bg-emerald-700 disabled:opacity-50 shadow-lg shadow-emerald-100 flex items-center justify-center gap-2"
        >
          {saving ? "Salvando..." : <><Save size={20} /> Salvar Lançamento</>}
        </button>
      </motion.form>
    </div>
  );
}
