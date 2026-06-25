import { useState, useEffect } from "react";
import { api } from "../services/api";
import { 
  TrendingUp, 
  Plus, 
  Calendar, 
  Activity, 
  FileText, 
  Heart, 
  CheckCircle,
  Eye,
  Trash2,
  Smile,
  AlertCircle
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";

// High-fidelity pre-seed evolution records for demonstration
const initialEvolutions: Record<string, any[]> = {
  "p1": [
    { id: "e1_1", date: "2026-05-10", eva: 8, pulse: "Tenso (Xian) e rápido", tongue: "Vermelha nas bordas, saburra amarela e fina", notes: "Queixa de dores de cabeça constantes e insônia severa. Ponto F3 e IG4 aplicados.", practitioner: "Dra. Camila Silva" },
    { id: "e1_2", date: "2026-05-17", eva: 6, pulse: "Tenso levemente áspero", tongue: "Menos vermelha, saburra amarela fina", notes: "Insônia apresentou melhora parcial; relatou 4 horas de sono contínuo.", practitioner: "Dra. Camila Silva" },
    { id: "e1_3", date: "2026-06-01", eva: 3, pulse: "Harmônico, levemente tenso", tongue: "Rósea, saburra branca fina", notes: "Cefaleia reduziu significativamente em intensidade e frequência.", practitioner: "Dra. Camila Silva" },
    { id: "e1_4", date: "2026-06-18", eva: 1, pulse: "Livre e moderado (Normal)", tongue: "Rósea, normal", notes: "Sem queixas de cefaleia nos últimos 10 dias. Sono restaurado a 7h por noite.", practitioner: "Dra. Camila Silva" }
  ],
  "p2": [
    { id: "e2_1", date: "2026-05-15", eva: 9, pulse: "Fraco (Ruo) e profundo (Chen)", tongue: "Pálida, marcas dentárias", notes: "Lombalgia crônica incapacitante que irradia sob frio. Moxa aplicada no canal B.", practitioner: "Dra. Camila Silva" },
    { id: "e2_2", date: "2026-05-29", eva: 7, pulse: "Profundo, porém mais firme", tongue: "Pálida com menos umidade", notes: "Melhora da aversão ao frio. Dor lombar menos aguda após moxabustão no B23.", practitioner: "Dra. Camila Silva" },
    { id: "e2_3", date: "2026-06-19", eva: 5, pulse: "Firme na posição Chi", tongue: "Moderadamente pálida", notes: "Sentindo maior vigor e pernas mais quentes. Dor lombar suportável.", practitioner: "Dra. Camila Silva" }
  ],
  "p3": [
    { id: "e3_1", date: "2026-06-01", eva: 7, pulse: "Fino (Xi) e fraco (Ruo)", tongue: "Pálida, saburra fina", notes: "Ansiedade severa com palpitações esporádicas. Ponto C7 e Yintang agulhados.", practitioner: "Dra. Camila Silva" },
    { id: "e3_2", date: "2026-06-10", eva: 4, pulse: "Fino e suave", tongue: "Levemente rosada, saburra fina", notes: "Palpitações desapareceram. Relata maior estabilidade emocional.", practitioner: "Dra. Camila Silva" },
    { id: "e3_3", date: "2026-06-20", eva: 2, pulse: "Suave, ritmo regular", tongue: "Rósea saudável", notes: "Fadiga física residual menor. Dormindo tranquilamente sem pesadelos.", practitioner: "Dra. Camila Silva" }
  ]
};

export default function EvolutionScreen() {
  const [patients, setPatients] = useState<any[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState("");
  const [evolutions, setEvolutions] = useState<Record<string, any[]>>(() => {
    const saved = localStorage.getItem("bio_evolutions");
    return saved ? JSON.parse(saved) : initialEvolutions;
  });

  const [showAddForm, setShowAddForm] = useState(false);
  const [newRecord, setNewRecord] = useState({
    date: new Date().toISOString().split("T")[0],
    eva: 5,
    pulse: "",
    tongue: "",
    notes: ""
  });

  useEffect(() => {
    api.getPatients().then(data => {
      setPatients(data);
      if (data.length > 0) {
        setSelectedPatientId(data[0].id);
      }
    });
  }, []);

  useEffect(() => {
    localStorage.setItem("bio_evolutions", JSON.stringify(evolutions));
  }, [evolutions]);

  const currentPatientEvolutions = evolutions[selectedPatientId] || [];

  const handleAddEvolution = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPatientId) return;

    const record = {
      id: `e_new_${Math.random().toString(36).substring(2, 9)}`,
      ...newRecord,
      practitioner: "Dra. Camila Silva"
    };

    setEvolutions(prev => ({
      ...prev,
      [selectedPatientId]: [...(prev[selectedPatientId] || []), record].sort((a,b) => a.date.localeCompare(b.date))
    }));

    // Reset Form
    setNewRecord({
      date: new Date().toISOString().split("T")[0],
      eva: 5,
      pulse: "",
      tongue: "",
      notes: ""
    });
    setShowAddForm(false);
  };

  const handleDeleteEvolution = (id: string) => {
    if (!window.confirm("Deseja realmente remover este registro de evolução?")) return;
    setEvolutions(prev => ({
      ...prev,
      [selectedPatientId]: (prev[selectedPatientId] || []).filter(item => item.id !== id)
    }));
  };

  // Prepares data for chart. Standardizes dates
  const chartData = currentPatientEvolutions.map(item => ({
    Data: new Date(item.date).toLocaleDateString("pt-BR", { day: '2-digit', month: '2-digit' }),
    "Nível de Dor (EVA)": item.eva
  }));

  const selectedPatientName = patients.find(p => p.id === selectedPatientId)?.name || "Paciente Selecionado";

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      
      {/* Upper description container from PDF page 5 */}
      <div className="bg-white p-8 rounded-3xl border border-[#E3ECE0] shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-[#F4F9F2] rounded-full -mr-10 -mt-10 opacity-60 pointer-events-none" />
        <div className="space-y-2 flex-1">
          <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <TrendingUp size={24} className="text-[#557A46]" />
            Linha do Tempo de Evolução Clínica
          </h2>
          <p className="text-gray-500 text-sm leading-relaxed max-w-2xl">
            Anote de forma simplificada o progresso álgico, batimentos do pulso e saburra da língua do paciente a cada nova sessão, gerando curvas de recuperação clínicas reais.
          </p>
        </div>
        <div>
          <button 
            onClick={() => setShowAddForm(true)}
            className="flex items-center gap-2 px-6 py-3.5 bg-[#E28A2B] hover:bg-[#c97820] text-white rounded-xl font-bold shadow-lg shadow-[#E28A2B]/10 transition-colors"
          >
            <Plus size={20} />
            Adicionar Registro de Evolução
          </button>
        </div>
      </div>

      {/* Select Box as shown in PDF page 5 */}
      <div className="bg-white p-6 rounded-2xl border border-[#E3ECE0] shadow-sm flex flex-col sm:flex-row sm:items-center gap-4">
        <span className="text-xs font-black tracking-widest text-gray-400 uppercase flex items-center gap-2">
          <Eye size={14} className="text-[#557A46]" />
          Visualizar Linha de Tempo De:
        </span>
        <select 
          value={selectedPatientId}
          onChange={(e) => setSelectedPatientId(e.target.value)}
          className="flex-1 max-w-md p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#557A46]"
        >
          {patients.map(p => (
            <option key={p.id} value={p.id}>{p.name} ({p.profession || "Paciente"})</option>
          ))}
        </select>
      </div>

      {/* Main interactive grid content */}
      {selectedPatientId ? (
        currentPatientEvolutions.length === 0 ? (
          <div className="bg-white rounded-3xl border border-dashed border-gray-200 p-16 text-center shadow-sm">
            <div className="w-16 h-16 bg-[#F4F9F2] rounded-full flex items-center justify-center text-[#557A46] mx-auto mb-4">
              <Activity size={32} />
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-1">Nenhuma evolução registrada para este paciente</h3>
            <p className="text-gray-500 text-sm max-w-md mx-auto leading-relaxed">
              Comece a preencher o diário quinzenal ou semanal de evolução clicando no botão "Adicionar Registro de Evolução" no topo.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            
            {/* Left Graph: Curve of pain reduction */}
            <div className="lg:col-span-7 bg-white p-6 rounded-3xl border border-[#E3ECE0] shadow-sm">
              <div className="flex justify-between items-center mb-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900">Curva de Recuperação Clínica</h3>
                  <p className="text-xs text-gray-400">Progresso do Nível de Dor (EVA) ao longo das sessões</p>
                </div>
                <div className="px-3 py-1 bg-[#EEF5EC] border border-[#D5E6CF] text-[#4E7A40] text-xs font-semibold rounded-full items-center gap-1 flex">
                  <Smile size={12} strokeWidth={2.5} />
                  <span>Dor decrescente</span>
                </div>
              </div>

              {/* Chart container */}
              <div className="h-[280px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData} margin={{ top: 10, right: 15, left: -25, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                    <XAxis dataKey="Data" tickLine={false} style={{ fontSize: '11px', fontWeight: 'bold', fill: '#94A3B8' }} />
                    <YAxis domain={[0, 10]} tickCount={6} tickLine={false} style={{ fontSize: '11px', fontWeight: 'bold', fill: '#94A3B8' }} />
                    <Tooltip contentStyle={{ borderRadius: '12px', border: '1px solid #E3ECE0', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)' }} />
                    <Line 
                      type="monotone" 
                      dataKey="Nível de Dor (EVA)" 
                      stroke="#557A46" 
                      strokeWidth={3} 
                      activeDot={{ r: 8 }} 
                      dot={{ r: 5, fill: "#FFFFFF", stroke: "#557A46", strokeWidth: 3 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Right: History Feed list */}
            <div className="lg:col-span-5 space-y-4">
              <h3 className="text-sm font-black tracking-widest text-[#557A46] uppercase mb-1 flex items-center gap-1.5 ml-1">
                <Activity size={15} />
                Histórico de Milestones de MTC
              </h3>

              <div className="max-h-[350px] overflow-y-auto space-y-3.5 pr-1.5">
                {[...currentPatientEvolutions].reverse().map((item) => (
                  <div key={item.id} className="bg-white p-5 rounded-2xl border border-[#E3ECE0] shadow-sm relative group hover:border-[#C6DAC1] transition-colors">
                    <button 
                      onClick={() => handleDeleteEvolution(item.id)}
                      className="absolute top-4 right-4 text-gray-300 hover:text-rose-500 opacity-0 group-hover:opacity-100 transition-opacity"
                      title="Deletar registro"
                    >
                      <Trash2 size={16} />
                    </button>

                    <div className="flex items-center gap-2 mb-3">
                      <span className="p-1 px-2.5 bg-[#F4F9F2] text-[#4E7A40] text-[10px] font-bold rounded-lg uppercase flex items-center gap-1">
                        <Calendar size={11} />
                        {new Date(item.date).toLocaleDateString("pt-BR", { day: '2-digit', month: 'short', year: 'numeric' })}
                      </span>
                      <span className={`p-1 px-2.5 text-[10px] font-black rounded-lg ${item.eva > 6 ? 'bg-red-50 text-red-600' : item.eva > 3 ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'}`}>
                        EVA: {item.eva}/10
                      </span>
                    </div>

                    <p className="text-sm text-gray-700 leading-relaxed font-semibold">{item.notes}</p>

                    <div className="grid grid-cols-2 gap-3 mt-4 pt-3 border-t border-gray-100 text-[11px] text-gray-500">
                      <div>
                        <span className="font-black text-gray-400 block uppercase">Pulsologia</span>
                        <span className="font-medium text-gray-700">{item.pulse || "Não registrado"}</span>
                      </div>
                      <div>
                        <span className="font-black text-gray-400 block uppercase">Inspeção Língua</span>
                        <span className="font-medium text-gray-700">{item.tongue || "Não registrado"}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

          </div>
        )
      ) : (
        <div className="bg-white p-12 text-center rounded-3xl border border-dashed border-gray-200 shadow-sm text-gray-400">
          Carregando pacientes...
        </div>
      )}

      {/* Add Modal For New Record */}
      <AnimatePresence>
        {showAddForm && (
          <div className="fixed inset-0 bg-black/55 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl max-w-xl w-full p-6 md:p-8 shadow-2xl border border-[#E3ECE0]"
            >
              <div className="flex justify-between items-center mb-6">
                <div>
                  <span className="text-[10px] font-black tracking-widest text-[#B5914A] uppercase block">Dra. Camila Silva</span>
                  <h3 className="text-xl font-bold text-gray-900">Novo Registro para: {selectedPatientName}</h3>
                </div>
                <button 
                  onClick={() => setShowAddForm(false)}
                  className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center text-gray-400 hover:text-gray-600 font-bold"
                >
                  ✕
                </button>
              </div>

              <form onSubmit={handleAddEvolution} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase mb-1">Data da Sessão</label>
                    <input 
                      type="date" 
                      required
                      value={newRecord.date}
                      onChange={(e) => setNewRecord({...newRecord, date: e.target.value})}
                      className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-sm font-bold focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase mb-1">Nível de Dor (EVA): <span className="text-[#557A46] font-black">{newRecord.eva}/10</span></label>
                    <input 
                      type="range" 
                      min="0" 
                      max="10" 
                      value={newRecord.eva}
                      onChange={(e) => setNewRecord({...newRecord, eva: parseInt(e.target.value, 10)})}
                      className="w-full mt-2 accent-[#557A46]"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase mb-1">Observações de Pulsologia (Cun, Guan, Chi)</label>
                    <input 
                      type="text" 
                      placeholder="Ex: Rápido e tenso (Xian)..."
                      value={newRecord.pulse}
                      onChange={(e) => setNewRecord({...newRecord, pulse: e.target.value})}
                      className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase mb-1">Aspectos da Língua & Saburra</label>
                    <input 
                      type="text" 
                      placeholder="Ex: Corp vermelha, saburra amarela fina..."
                      value={newRecord.tongue}
                      onChange={(e) => setNewRecord({...newRecord, tongue: e.target.value})}
                      className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-400 uppercase mb-1">Progresso Clínico / Evolução</label>
                  <textarea 
                    required
                    placeholder="Descreva a evolução do quadro álgico, humor e feedback de agulhamento do paciente..."
                    rows={3}
                    value={newRecord.notes}
                    onChange={(e) => setNewRecord({...newRecord, notes: e.target.value})}
                    className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                  />
                </div>

                <div className="pt-3 flex justify-end gap-3 border-t border-gray-100">
                  <button 
                    type="button" 
                    onClick={() => setShowAddForm(false)}
                    className="px-5 py-2.5 bg-gray-50 hover:bg-gray-100 text-gray-500 rounded-xl text-sm font-bold border border-gray-200 transition-colors"
                  >
                    Cancelar
                  </button>
                  <button 
                    type="submit"
                    className="px-6 py-2.5 bg-[#557A46] hover:bg-[#436136] text-white rounded-xl text-sm font-bold shadow-md shadow-[#557A46]/10 transition-colors"
                  >
                    Salvar Evolução
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
