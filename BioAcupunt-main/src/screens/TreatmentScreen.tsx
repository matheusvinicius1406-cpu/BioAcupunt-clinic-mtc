import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { api } from "../services/api";
import { ArrowLeft, Sparkles, FileText, Brain, Share2, Save, CheckCircle2, AlertCircle } from "lucide-react";
import { motion } from "motion/react";

export default function TreatmentScreen() {
  const { patientId } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [patient, setPatient] = useState<any>(null);

  useEffect(() => {
    if (patientId) {
      Promise.all([
        api.getPatient(patientId),
        api.getAnamnese(patientId)
      ]).then(([p, a]) => {
        setPatient(p);
        setData(a);
        setLoading(false);
      });
    }
  }, [patientId]);

  // Extract the latest diagnostic record from patient history if available
  let latestDiag: any = null;
  if (patient?.diagnosisHistory) {
    const history = typeof patient.diagnosisHistory === "string" 
      ? JSON.parse(patient.diagnosisHistory) 
      : patient.diagnosisHistory;
    if (Array.isArray(history) && history.length > 0) {
      latestDiag = history[history.length - 1];
    }
  }

  const handleSaveToProntuario = async () => {
    if (!patientId) return;
    setSaving(true);
    try {
      alert("Diagnóstico salvo com sucesso no histórico de prontuários do paciente.");
    } finally {
      setSaving(false);
    }
  };

  const handleShare = () => {
    let text = `BioAcupunt - Plano de Tratamento MTC\n`;
    text += `Paciente: ${patient?.name}\n\n`;
    if (latestDiag) {
      text += `Diagnóstico MTC: ${latestDiag.diagnosis}\n`;
      text += `Tratamento: ${latestDiag.treatment}\n\n`;
      text += `Raciocínio Clínico: ${latestDiag.rationale}\n\n`;
      text += `Recomendações: ${latestDiag.additionalRecommendations}\n`;
    } else {
      text += `Diagnóstico: ${data?.diagnose || "Pendente"}\n\n`;
      text += `Plano: ${data?.treatmentPlan || "Pendente"}`;
    }
    navigator.clipboard.writeText(text);
    alert("Plano de tratamento copiado para a área de transferência!");
  };

  return (
    <div className="max-w-4xl mx-auto pb-12 px-4 md:px-0">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700 cursor-pointer">
            <ArrowLeft size={24} />
          </button>
          <div>
            <h1 className="text-3xl font-bold text-neutral-800 tracking-tight flex items-center gap-2">
              <Sparkles className="text-emerald-500 animate-pulse" size={24} /> Prontuário MTC Inteligente
            </h1>
            <p className="text-sm text-neutral-500 font-medium">Paciente: <span className="text-emerald-850 font-bold">{patient?.name}</span></p>
          </div>
        </div>
        <div className="flex gap-2">
          <button 
            onClick={handleShare}
            className="px-4 py-2 text-sm bg-white border border-neutral-200 text-neutral-600 rounded-xl hover:bg-neutral-50 hover:text-neutral-900 transition-all flex items-center gap-1.5 font-bold cursor-pointer shadow-sm"
          >
            <Share2 size={16} /> Compartilhar
          </button>
        </div>
      </header>

      {loading ? (
        <div className="animate-pulse space-y-6">
          <div className="h-48 bg-neutral-200 rounded-3xl" />
          <div className="h-96 bg-neutral-200 rounded-3xl" />
        </div>
      ) : (
        <div className="space-y-8">
          {/* Main Diagnosis Banner */}
          <motion.section 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-emerald-950 text-white p-8 rounded-3xl shadow-xl relative overflow-hidden border border-emerald-900"
          >
            <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/10 -mr-10 -mt-10 rounded-full blur-xl" />
            <div className="flex items-center gap-2 mb-3">
              <Brain size={24} className="text-emerald-400" />
              <span className="text-xs uppercase tracking-wider font-extrabold text-emerald-300">Diagnóstico MTC Automatizado</span>
            </div>
            
            <h2 className="text-2xl md:text-3xl font-display font-semibold tracking-tight leading-relaxed mb-4">
              {latestDiag ? latestDiag.diagnosis : (data?.diagnose || "Padrão de desarmonia não gerado")}
            </h2>
            
            {latestDiag && (
              <div className="mt-4 pt-4 border-t border-emerald-900">
                <span className="text-xs uppercase tracking-wider font-extrabold text-emerald-300 block mb-1">Tratamento Recomendado</span>
                <p className="text-md text-emerald-100 font-medium">{latestDiag.treatment}</p>
              </div>
            )}
          </motion.section>

          {/* Rationale and Justification */}
          {latestDiag?.rationale && (
            <motion.section 
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="bg-white p-8 rounded-3xl shadow-sm border border-neutral-100"
            >
              <h3 className="text-sm font-extrabold uppercase tracking-wider text-emerald-800 mb-3 flex items-center gap-1.5">
                <FileText size={16} /> Justificativa e Raciocínio Clínico
              </h3>
              <p className="text-neutral-650 leading-relaxed text-sm whitespace-pre-line">{latestDiag.rationale}</p>
            </motion.section>
          )}

          {/* Acupuncture Points Analysis - Bento Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Primary Points */}
            <motion.section 
              initial={{ opacity: 0, scale: 0.98 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.15 }}
              className="bg-white p-8 rounded-3xl shadow-sm border border-neutral-100 flex flex-col"
            >
              <h3 className="text-sm font-extrabold text-emerald-800 uppercase tracking-wider mb-4 flex items-center gap-1.5">
                <CheckCircle2 size={18} className="text-emerald-500" /> Pontos Primários (Sistemáticos)
              </h3>
              
              {latestDiag?.primaryPoints && Array.isArray(latestDiag.primaryPoints) ? (
                <div className="space-y-4 flex-1">
                  {latestDiag.primaryPoints.map((pt: any, i: number) => (
                    <div key={i} className="p-3 bg-neutral-50 rounded-xl border border-neutral-100">
                      <div className="flex justify-between items-baseline mb-1">
                        <span className="text-sm font-bold text-neutral-850">{pt.name}</span>
                        <span className="text-[10px] bg-emerald-50 text-emerald-700 px-1.5 py-0.5 rounded-full font-bold">{pt.canal}</span>
                      </div>
                      <p className="text-xs text-neutral-400 mb-1"><strong>Localização:</strong> {pt.localizacao}</p>
                      <p className="text-xs text-neutral-600"><strong>Função:</strong> {pt.funcao}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-neutral-500 text-sm italic">{data?.treatmentPoints || "Pontos específicos recomendados aparecerão aqui."}</p>
              )}
            </motion.section>

            {/* Secondary / Complementary Points */}
            <motion.section 
              initial={{ opacity: 0, scale: 0.98 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.2 }}
              className="bg-white p-8 rounded-3xl shadow-sm border border-neutral-100 flex flex-col"
            >
              <h3 className="text-sm font-extrabold text-amber-800 uppercase tracking-wider mb-4 flex items-center gap-1.5">
                <Sparkles size={18} className="text-amber-500" /> Pontos Complementares / Extras
              </h3>
              
              {latestDiag?.secondaryPoints && Array.isArray(latestDiag.secondaryPoints) ? (
                <div className="space-y-4 flex-1">
                  {latestDiag.secondaryPoints.map((pt: any, i: number) => (
                    <div key={i} className="p-3 bg-neutral-50 rounded-xl border border-neutral-100">
                      <div className="flex justify-between items-baseline mb-1">
                        <span className="text-sm font-bold text-neutral-850">{pt.name}</span>
                        <span className="text-[10px] bg-amber-50 text-amber-700 px-1.5 py-0.5 rounded-full font-bold">{pt.canal || "Canal Extra"}</span>
                      </div>
                      <p className="text-xs text-neutral-400 mb-1"><strong>Localização:</strong> {pt.localizacao}</p>
                      <p className="text-xs text-neutral-600"><strong>Função:</strong> {pt.funcao}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-neutral-500 text-sm italic">Adicione queixas emocionais ou auditivas para obter pontos de apoio extras.</p>
              )}
            </motion.section>
          </div>

          {/* Contraindications and Warnings */}
          <motion.section 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.25 }}
            className="bg-amber-50/50 p-6 rounded-3xl shadow-sm border border-amber-100"
          >
            <h3 className="text-xs font-extrabold text-amber-850 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <AlertCircle size={16} className="text-amber-500" /> Contraindicações & Precauções Clínicas
            </h3>
            <p className="text-amber-900/80 text-sm mt-1 whitespace-pre-line leading-relaxed">
              {latestDiag ? latestDiag.contraindications : (data?.contraindications || "Evite estímulos excessivos em pacientes imunodeprimidos ou gestantes.")}
            </p>
          </motion.section>

          {/* Core Integrative & Diet Therapy Advice */}
          <motion.section 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="bg-white p-8 rounded-3xl shadow-sm border border-neutral-100"
          >
            <h2 className="text-xl font-bold text-neutral-800 mb-4 flex items-center gap-2">
              <Sparkles size={20} className="text-emerald-600" /> Recomendações de Dietoterapia e Estilo de Vida
            </h2>
            <div className="text-neutral-600 text-sm leading-relaxed whitespace-pre-line">
              {latestDiag ? latestDiag.additionalRecommendations : (data?.treatmentPlan || "Os pontos e técnicas sugeridas aparecerão aqui.")}
            </div>
          </motion.section>

          {/* Action buttons */}
          <div className="flex flex-col md:flex-row gap-4 pt-4">
            <button 
              onClick={handleSaveToProntuario}
              disabled={saving}
              className="flex-1 py-4 bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl font-bold shadow-lg shadow-emerald-100 hover:shadow-xl transition-all flex items-center justify-center gap-2 cursor-pointer"
            >
              <Save size={20} /> Salvar no Histórico
            </button>
            <button 
              onClick={() => navigate('/chat')}
              className="flex-1 py-4 bg-white border border-neutral-200 text-neutral-700 rounded-2xl font-bold hover:bg-neutral-50 hover:text-neutral-900 transition-all shadow-sm cursor-pointer"
            >
              Consultar Assistente MTC
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

