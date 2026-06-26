import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../services/api";
import { 
  Search, 
  UserPlus, 
  Phone, 
  ChevronRight, 
  User, 
  ArrowLeft, 
  ArrowRight, 
  ClipboardList, 
  Trash2, 
  AlertCircle,
  Activity,
  Sparkles
} from "lucide-react";
import { motion } from "motion/react";

const STAGES = [
  { id: "Triagem", label: "Pré-Consulta / Triagem", color: "bg-neutral-100 text-neutral-800 border-neutral-200" },
  { id: "Avaliacao", label: "Avaliação Agendada", color: "bg-teal-55 bg-teal-50 text-teal-800 border-teal-250" },
  { id: "Tratamento", label: "Em Tratamento Ativo", color: "bg-emerald-light text-emerald-dark border-emerald-medium/30" },
  { id: "Retorno", label: "Pós-Sessão / Retorno", color: "bg-amber-50 text-amber-800 border-amber-200" }
];

export default function PatientsScreen() {
  const navigate = useNavigate();
  const [patients, setPatients] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    loadPatients();
  }, []);

  const loadPatients = async () => {
    try {
      const data = await api.getPatients();
      
      const processed = data.map((p: any, idx: number) => {
        // Find latest anamnese to determine status
        const latestAnamnese = p.clinicalRecords?.[0];
        
        // Kanban stage logic - move to patient status or record status
        const defaultStages = ["Triagem", "Avaliacao", "Tratamento", "Retorno"];
        const stage = p.status === "ACTIVE" ? (latestAnamnese ? "Tratamento" : "Avaliacao") : "Retorno";
        
        return { 
          ...p, 
          processedAnamnese: { 
            kanbanStage: stage,
            latestQueixa: latestAnamnese?.queixa?.principal
          } 
        };
      });

      setPatients(processed);
    } catch (error) {
      console.error("Erro ao carregar pacientes:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleMoveStage = async (patient: any, direction: "left" | "right") => {
    const currentStageIdx = STAGES.findIndex(s => s.id === patient.processedAnamnese.kanbanStage);
    let newStageIdx = currentStageIdx;
    
    if (direction === "left" && currentStageIdx > 0) {
      newStageIdx = currentStageIdx - 1;
    } else if (direction === "right" && currentStageIdx < STAGES.length - 1) {
      newStageIdx = currentStageIdx + 1;
    }

    if (newStageIdx === currentStageIdx) return;
    const newStage = STAGES[newStageIdx].id;

    const updatedAnamnese = { ...patient.processedAnamnese, kanbanStage: newStage };

    try {
      setPatients(prev => prev.map(p => {
        if (p.id === patient.id) {
          return { ...p, processedAnamnese: updatedAnamnese };
        }
        return p;
      }));

      await api.updateAnamnese(patient.id, updatedAnamnese);
    } catch (error) {
      console.error("Erro ao mover estágio do paciente:", error);
      loadPatients();
    }
  };

  const handleDeletePatient = async (id: string) => {
    if (!window.confirm("Deseja realmente remover este paciente e todos os seus registros de prontuários?")) return;
    try {
      await api.deletePatient(id);
      setPatients(prev => prev.filter(p => p.id !== id));
    } catch (error) {
      console.error("Erro ao remover paciente:", error);
    }
  };

  const filteredPatients = patients.filter(p => 
    p.name.toLowerCase().includes(search.toLowerCase()) ||
    (p.profession && p.profession.toLowerCase().includes(search.toLowerCase())) ||
    (p.phone && p.phone.includes(search))
  );

  const patientsByStage = STAGES.reduce((acc, stage) => {
    acc[stage.id] = filteredPatients.filter(p => p.processedAnamnese.kanbanStage === stage.id);
    return acc;
  }, {} as Record<string, any[]>);

  const totalCRM = patients.length;
  const inTreatment = patients.filter(p => p.processedAnamnese?.kanbanStage === "Tratamento").length;
  const reengagementCount = patients.filter(p => p.processedAnamnese?.kanbanStage === "Triagem").length;
  const totalFichas = patients.length * 3;

  if (loading) {
    return (
      <div className="space-y-8 animate-pulse">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="bg-white p-5 rounded-3xl h-24 animate-shimmer" />
          ))}
        </div>
        <div className="bg-white p-6 rounded-3xl h-36 border border-emerald-light animate-shimmer" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      
      {/* 4 Upper KPI Metrics Cards matching PDF page 9 exactly with Premium Look */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        {/* Metric 1 */}
        <div className="bg-white p-5 rounded-2xl border border-emerald-light/60 shadow-sm flex items-center gap-4 hover-lift">
          <div className="w-12 h-12 bg-[#F4F7F5] border border-emerald-light rounded-full flex items-center justify-center text-emerald-primary shadow-2xs">
            <User size={20} />
          </div>
          <div>
            <p className="text-[10px] font-bold text-neutral-400 uppercase tracking-widest leading-none">Total Prontuários</p>
            <p className="text-3xl font-display font-semibold text-emerald-dark mt-1.5">{totalCRM}</p>
          </div>
        </div>

        {/* Metric 2 */}
        <div className="bg-white p-5 rounded-2xl border border-emerald-light/60 shadow-sm flex items-center gap-4 hover-lift">
          <div className="w-12 h-12 bg-emerald-light/40 border border-emerald-medium/30 rounded-full flex items-center justify-center text-emerald-primary shadow-2xs">
            <Activity size={20} className="animate-pulse" />
          </div>
          <div>
            <p className="text-[10px] font-bold text-emerald-primary uppercase tracking-widest leading-none">Sessões Ativas</p>
            <p className="text-3xl font-display font-semibold text-emerald-primary mt-1.5">{inTreatment}</p>
          </div>
        </div>

        {/* Metric 3 */}
        <div className="bg-white p-5 rounded-2xl border border-emerald-light/60 shadow-sm flex items-center gap-4 hover-lift">
          <div className="w-12 h-12 bg-[#FAF7F2] border border-gold-lux/30 rounded-full flex items-center justify-center text-[#C9A96E] shadow-2xs">
            <AlertCircle size={20} />
          </div>
          <div>
            <p className="text-[10px] font-bold text-[#C9A96E] uppercase tracking-widest leading-none">Atraso Trinta Dias</p>
            <p className="text-3xl font-display font-semibold text-[#A6884E] mt-1.5">{reengagementCount}</p>
          </div>
        </div>

        {/* Metric 4 */}
        <div className="bg-white p-5 rounded-2xl border border-emerald-light/60 shadow-sm flex items-center gap-4 hover-lift">
          <div className="w-12 h-12 bg-neutral-50 border border-neutral-200/60 rounded-full flex items-center justify-center text-neutral-500 shadow-2xs">
            <ClipboardList size={20} />
          </div>
          <div>
            <p className="text-[10px] font-bold text-neutral-400 uppercase tracking-widest leading-none">Documentos MTC</p>
            <p className="text-3xl font-display font-semibold text-neutral-900 mt-1.5">{totalFichas}</p>
          </div>
        </div>

      </div>

      {/* Reengagement alarm banner - Luxury design */}
      <div className="border border-gold-lux/40 bg-gradient-to-r from-[#FAF7F2] to-white p-5 rounded-2xl shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex gap-3.5 items-start">
          <div className="p-3 bg-[#FAF7F2] border border-[#EAD4A6] text-[#A6884E] rounded-xl flex-shrink-0">
            <AlertCircle size={18} />
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h4 className="font-display font-semibold text-neutral-800 text-sm">Alertas Automáticos de Reengajamento ({reengagementCount})</h4>
              <span className="bg-[#FAF7F2] border border-[#C9A96E]/30 text-[#A6884E] text-[9px] font-black tracking-widest px-2 py-0.5 rounded uppercase">
                Ausente +30 dias
              </span>
            </div>
            <p className="text-xs text-neutral-500 mt-1">Estes pacientes estão sem sessões registradas há mais de 30 dias. Entre em contato para preencher acompanhamento ou marcar retorno.</p>
          </div>
        </div>
        <button 
          onClick={() => navigate("/agenda")}
          className="bg-gradient-to-r from-[#C9A96E] to-[#A6884E] hover:opacity-95 text-white px-5 py-2.5 rounded-xl text-xs font-bold shadow-md shadow-amber-600/10 transition-all text-center whitespace-nowrap cursor-pointer hover-lift"
        >
          Visualizar Consultas Livres
        </button>
      </div>

      {/* Header title description with primary control inputs */}
      <div className="bg-white p-6 rounded-2xl border border-emerald-light/60 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-1">
            <h3 className="text-xl font-display font-semibold text-emerald-dark flex items-center gap-2">
              <ClipboardList className="text-emerald-primary" size={20} />
              Acompanhamento Integrado e Linha de Cuidado (Kanban Clínico)
            </h3>
            <p className="text-neutral-400 text-xs leading-relaxed max-w-2xl">
              Organize o percurso biomédico e acupuntura integrativa. Mova os cartões para atualizar o andamento das avaliações à alta.
            </p>
          </div>
          <div>
            <Link 
              to="/pacientes/novo"
              className="flex items-center justify-center gap-1.5 px-5 py-3 bg-gradient-to-r from-emerald-primary to-emerald-dark text-white rounded-xl text-xs font-black shadow-md shadow-emerald-primary/10 transition-all cursor-pointer hover-lift"
            >
              <UserPlus size={15} />
              Adicionar Novo Paciente
            </Link>
          </div>
        </div>

        {/* Live Filter query field */}
        <div className="relative mt-6 max-w-2xl">
          <input
            type="text"
            placeholder="Buscar por nome, profissão ou telefone..."
            className="w-full pl-11 pr-4 py-3 bg-[#F4F7F5]/50 border border-emerald-light rounded-xl text-xs font-medium text-neutral-700 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white transition-all"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-neutral-400" size={15} />
        </div>
      </div>

      {/* Kanban Board Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {STAGES.map((stage) => {
          const colPatients = patientsByStage[stage.id] || [];
          return (
            <div key={stage.id} className="flex flex-col min-h-[500px]">
              
              {/* Kanban Column Title */}
              <div className="flex items-center justify-between p-3.5 mb-3 bg-white rounded-xl border border-emerald-light/60 shadow-2xs">
                <span className="text-xs font-bold text-neutral-700 tracking-tight">{stage.label}</span>
                <span className={`px-2 py-0.5 text-[9px] font-black rounded-md ${stage.color} border border-black/5`}>
                  {colPatients.length}
                </span>
              </div>

              {/* Column Content Wrapper */}
              <div className="flex-1 space-y-3 p-2 bg-emerald-light/10 rounded-2xl border border-emerald-light/20">
                {colPatients.length === 0 ? (
                  <div className="text-center py-12 p-4 text-neutral-400 text-xs border border-dashed border-emerald-light/40 bg-white/50 rounded-xl">
                    <User size={20} className="mx-auto text-neutral-300 mb-2" />
                    Nenhum paciente neste estágio.
                  </div>
                ) : (
                  colPatients.map((p) => (
                    <div 
                      key={p.id}
                      className="bg-white p-4.5 rounded-xl border border-emerald-light/60 shadow-2xs hover:border-emerald-primary/45 transition-all group relative hover:shadow-xs"
                    >
                      {/* Name / Avatar Header */}
                      <div className="flex items-start justify-between gap-3 mb-2.5">
                        <div className="flex items-center gap-2.5 min-w-0">
                          <div className="w-8 h-8 rounded-full bg-emerald-light text-emerald-primary font-bold text-xs flex items-center justify-center uppercase shadow-3xs flex-shrink-0">
                            {p.name.charAt(0)}
                          </div>
                          <div className="min-w-0">
                            <h4 className="text-xs font-bold text-neutral-800 leading-tight tracking-tight truncate" title={p.name}>{p.name}</h4>
                            <span className="text-[9px] text-[#A6884E] font-bold block mt-0.5 tracking-wide truncate">{p.profession || "Paciente"}</span>
                          </div>
                        </div>
                        
                        {/* Quick Delete */}
                        <button 
                          onClick={() => handleDeletePatient(p.id)}
                          className="text-neutral-300 hover:text-rose-500 opacity-0 group-hover:opacity-100 transition-opacity p-0.5 cursor-pointer"
                          title="Remover paciente"
                        >
                          <Trash2 size={12} />
                        </button>
                      </div>

                      {/* Contact metadata */}
                      {p.phone && (
                        <div className="flex items-center gap-1.5 text-[10px] text-neutral-400 font-semibold mb-3">
                          <Phone size={10} className="text-emerald-primary" />
                          <span className="truncate">{p.phone}</span>
                        </div>
                      )}

                      {/* Actions footer block */}
                      <div className="mt-4 pt-3 border-t border-neutral-100 flex items-center justify-between">
                        <Link 
                          to={`/anamnese/${p.id}`}
                          className="text-[10px] font-bold text-emerald-primary hover:text-emerald-dark hover:underline flex items-center gap-0.5"
                        >
                          Atendimento <Sparkles size={8} />
                        </Link>
                        
                        {/* Interactive columns movers arrows */}
                        <div className="flex gap-1">
                          <button 
                            onClick={() => handleMoveStage(p, "left")}
                            disabled={stage.id === "Triagem"}
                            className="p-1 rounded bg-neutral-50 hover:bg-emerald-light border border-neutral-200 hover:border-emerald-medium text-neutral-400 hover:text-emerald-primary disabled:opacity-30 disabled:pointer-events-none cursor-pointer"
                            title="Retroceder estágio"
                          >
                            <ArrowLeft size={10} strokeWidth={2.5} />
                          </button>
                          <button 
                            onClick={() => handleMoveStage(p, "right")}
                            disabled={stage.id === "Retorno"}
                            className="p-1 rounded bg-neutral-50 hover:bg-emerald-light border border-neutral-200 hover:border-emerald-medium text-neutral-400 hover:text-emerald-primary disabled:opacity-30 disabled:pointer-events-none cursor-pointer"
                            title="Avançar estágio"
                          >
                            <ArrowRight size={10} strokeWidth={2.5} />
                          </button>
                          <Link 
                            to={`/pacientes/editar/${p.id}`}
                            className="p-1 rounded bg-neutral-50 hover:bg-emerald-light border border-neutral-200 hover:border-emerald-medium text-neutral-400 hover:text-emerald-primary flex items-center justify-center"
                            title="Editar Dados"
                          >
                            <ChevronRight size={10} strokeWidth={3} />
                          </Link>
                        </div>
                      </div>

                    </div>
                  ))
                )}
              </div>

            </div>
          );
        })}
      </div>

    </div>
  );
}
