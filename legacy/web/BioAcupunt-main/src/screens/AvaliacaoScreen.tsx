import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { api } from "../services/api";
import { ArrowLeft, Save, Activity, Activity as Pulse, Zap } from "lucide-react";
import { motion } from "motion/react";

export default function AvaliacaoScreen() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState({
    queixaPrincipal: "",
    eva: 5,
    historico: "",
    pulso: "NORMAL",
    lingua: "NORMAL"
  });

  useEffect(() => {
    if (id) {
      api.getPatient(id).then(data => {
        setPatient(data);
        if (data.anamnese) {
          setForm({
            queixaPrincipal: data.anamnese.queixaPrincipal || "",
            eva: data.anamnese.eva || 5,
            historico: data.anamnese.historico || "",
            pulso: data.anamnese.pulso || "NORMAL",
            lingua: data.anamnese.lingua || "NORMAL"
          });
        }
        setLoading(false);
      });
    }
  }, [id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    setSaving(true);
    
    // Transform flat evaluation form into Clinical OS Domain Payload
    const structuredPayload = {
      queixa: {
        principal: form.queixaPrincipal,
        evolucao: form.historico,
      },
      lingua: {
        cor: form.lingua,
      },
      pulso: {
        impressao: form.pulso,
      },
      mapaDor: [{ eva: form.eva }]
    };

    try {
      await api.updateAnamnese(id, structuredPayload);
      alert("Avaliação inicial salva e vinculada ao prontuário!");
      navigate(`/agenda/novo?patientId=${id}`);
    } catch (error) {
      console.error("Clinical Save Failed:", error);
      alert("Erro ao salvar avaliação. Verifique a conexão.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="p-10 text-center animate-pulse">Carregando paciente...</div>;

  return (
    <div className="max-w-3xl mx-auto pb-12">
      <header className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
          <ArrowLeft size={24} />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Avaliação Rápida</h1>
          <p className="text-gray-500">Paciente: <span className="font-bold">{patient?.name}</span></p>
        </div>
      </header>

      <motion.form 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        onSubmit={handleSubmit}
        className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 space-y-6"
      >
        <div>
          <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
            <Zap size={16} className="text-emerald-500" /> Queixa Principal
          </label>
          <textarea
            className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 h-24"
            placeholder="O que o trouxe aqui hoje?"
            value={form.queixaPrincipal}
            onChange={(e) => setForm({ ...form, queixaPrincipal: e.target.value })}
          />
        </div>

        <div>
          <label className="block text-sm font-bold text-gray-700 mb-4 flex items-center gap-2">
            <Activity size={16} className="text-emerald-500" /> Nível de Dor (EVA): {form.eva}
          </label>
          <input
            type="range"
            min="0"
            max="10"
            className="w-full h-2 bg-emerald-100 rounded-lg appearance-none cursor-pointer accent-emerald-600"
            value={form.eva}
            onChange={(e) => setForm({ ...form, eva: parseInt(e.target.value) })}
          />
          <div className="flex justify-between text-[10px] font-bold text-gray-400 px-1 mt-1">
            <span>SEM DOR (0)</span>
            <span>INTENSA (10)</span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <Pulse size={16} className="text-emerald-500" /> Pulso
            </label>
            <select
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              value={form.pulso}
              onChange={(e) => setForm({ ...form, pulso: e.target.value })}
            >
              <option value="NORMAL">Normal</option>
              <option value="RAPIDO">Rápido (Shi)</option>
              <option value="LENTO">Lento (Chi)</option>
              <option value="FORTE">Forte (Hu)</option>
              <option value="FRACO">Fraco (Xu)</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <Pulse size={16} className="text-emerald-500" /> Língua
            </label>
            <select
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              value={form.lingua}
              onChange={(e) => setForm({ ...form, lingua: e.target.value })}
            >
              <option value="NORMAL">Normal</option>
              <option value="PALIDA">Pálida</option>
              <option value="VERMELHA">Vermelha</option>
              <option value="ROXA">Roxa (Estase)</option>
              <option value="SABURRA">Saburra Saburrosa</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-sm font-bold text-gray-700 mb-2">Breve Histórico / Notas</label>
          <textarea
            className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 h-24"
            placeholder="Antecedentes, cirurgias, medicamentos..."
            value={form.historico}
            onChange={(e) => setForm({ ...form, historico: e.target.value })}
          />
        </div>

        <div className="flex gap-4 pt-4">
          <button
            type="button"
            onClick={() => navigate('/pacientes')}
            className="flex-1 py-4 bg-gray-100 text-gray-600 rounded-2xl font-bold hover:bg-gray-200 transition-all"
          >
            Pular agora
          </button>
          <button
            type="submit"
            disabled={saving}
            className="flex-[2] py-4 bg-emerald-600 text-white rounded-2xl font-bold shadow-lg shadow-emerald-100 hover:bg-emerald-700 transition-all flex items-center justify-center gap-2"
          >
            {saving ? "Salvando..." : <><Save size={20} /> Salvar e Agendar</>}
          </button>
        </div>
      </motion.form>
    </div>
  );
}
