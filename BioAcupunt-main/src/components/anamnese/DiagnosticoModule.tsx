import { useState } from "react";
import { useParams } from "react-router-dom";
import { useAnamneseStore } from "../../store/anamneseStore";
import { Sparkles, CheckCircle2, AlertTriangle, Lightbulb, BookOpen, Loader2, RefreshCcw } from "lucide-react";
import { api } from "../../services/api";

export const DiagnosticoModule = () => {
    const { patientId } = useParams();
    const anamnese = useAnamneseStore(state => state.anamneseData);
    const updateField = useAnamneseStore(state => state.updateField);
    const rawDiagnosis = anamnese.currentDiagnosis;
    const setCurrentDiagnosis = useAnamneseStore(state => state.setCurrentDiagnosis);
    const [generating, setGenerating] = useState(false);

    // Map the database shape back to the new inference shape if needed
    const currentDiagnosis = rawDiagnosis?.inference ? rawDiagnosis : (rawDiagnosis ? {
        inference: {
            diagnosis: rawDiagnosis.diagnosis,
            confidence: rawDiagnosis.confidenceScore,
            patterns: rawDiagnosis.syndromes?.map((s: string) => ({ id: s })) || []
        },
        decision: {
            reason: rawDiagnosis.rationale
        },
        treatment: {
            plan: {
                acupuncture_points: [], // Treatment data is in a separate treatmentPlan table usually, fallback to empty
                herbal: 'Consulte o plano de tratamento'
            }
        },
        risk: {
            flag: false,
            reasons: []
        }
    } : null);

    const generateNewDiagnosis = async () => {
        if (!anamnese.id) {
            // Must save first if it's a new consult
            try {
                setGenerating(true);
                const saved = await api.updateAnamnese(patientId!, anamnese);
                useAnamneseStore.getState().setAnamnesis({ ...anamnese, id: saved.id });
                const diag = await api.diagnose(saved.id);
                setCurrentDiagnosis(diag);
            } catch (err) {
                console.error("Erro ao gerar diagnóstico:", err);
            } finally {
                setGenerating(false);
            }
            return;
        }

        setGenerating(true);
        try {
            await api.updateAnamnese(patientId!, anamnese);
            const diag = await api.diagnose(anamnese.id);
            setCurrentDiagnosis(diag);
        } catch (err) {
            console.error("Erro ao gerar diagnóstico:", err);
        } finally {
            setGenerating(false);
        }
    };

    return (
        <div className="space-y-8 pb-20">
            {/* IA INSIGHTS */}
            <div className="bg-gradient-to-br from-[#0D1117] to-[#161B22] p-8 rounded-2xl border border-[#00C896]/30 relative overflow-hidden">
                <div className="absolute top-0 right-0 p-4 opacity-10">
                    <Sparkles size={120} className="text-[#00C896]" />
                </div>
                
                <div className="relative z-10">
                    <div className="flex items-center justify-between mb-6">
                        <div className="flex items-center gap-3">
                            <div className="bg-[#00C896]/20 p-2 rounded-lg">
                                <Sparkles size={20} className="text-[#00C896]" />
                            </div>
                            <h3 className="text-xl font-display font-bold text-[#E6EDF3]">Diagnóstico BioAcupunt IA</h3>
                        </div>
                        <button 
                            onClick={generateNewDiagnosis}
                            disabled={generating}
                            className="bg-[#161B22] border border-[#30363D] hover:border-[#00C896] text-[#E6EDF3] px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2"
                        >
                            {generating ? <Loader2 size={14} className="animate-spin" /> : <RefreshCcw size={14} />}
                            {currentDiagnosis ? 'Atualizar Diagnóstico' : 'Gerar Primeiro Diagnóstico'}
                        </button>
                    </div>

                    {!currentDiagnosis && !generating ? (
                        <div className="text-center py-12">
                            <p className="text-[#8B949E] text-sm italic mb-4">A inteligência artificial ainda não analisou este prontuário.</p>
                            <button 
                                onClick={generateNewDiagnosis}
                                className="bg-[#00C896] text-[#0D1117] px-8 py-3 rounded-xl font-bold text-sm hover:scale-[1.02] active:scale-[0.98] transition-all"
                            >
                                Iniciar Análise MTC Supremo
                            </button>
                        </div>
                    ) : generating ? (
                        <div className="text-center py-12 space-y-4">
                            <Loader2 size={40} className="animate-spin text-[#00C896] mx-auto" />
                            <p className="text-[#00C896] text-sm animate-pulse font-bold uppercase tracking-widest">Processando Síndromes Zang Fu...</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                            <div className="lg:col-span-2 space-y-6">
                                <div className="bg-[#1C2128] p-6 rounded-xl border border-[#30363D]">
                                    <div className="flex justify-between items-start mb-3">
                                        <label className="text-[10px] uppercase text-[#8B949E] font-bold block tracking-widest">Síndrome Diagnosticada</label>
                                        {currentDiagnosis.inference?.confidence && (
                                            <span className="text-[10px] font-black text-[#00C896] bg-[#00C896]/10 px-2 py-0.5 rounded-full border border-[#00C896]/20">
                                                CONFIANÇA: {(currentDiagnosis.inference.confidence * 100).toFixed(0)}%
                                            </span>
                                        )}
                                    </div>
                                    <p className="text-lg font-medium text-[#00C896]">{currentDiagnosis.inference?.diagnosis || 'Pendente'}</p>
                                    
                                    <div className="mt-4 p-4 bg-black/20 rounded-lg border border-[#30363D]">
                                        <label className="text-[9px] uppercase text-[#8B949E] font-bold mb-2 block">Princípio de Tratamento (Fórmula Floral/Fitoterápica)</label>
                                        <p className="text-sm text-white italic">{currentDiagnosis.treatment?.plan?.herbal || 'Nenhum'}</p>
                                    </div>

                                    <p className="text-sm text-[#8B949E] mt-4 leading-relaxed font-light">
                                        {currentDiagnosis.decision?.reason || 'Sem justificativa'}
                                    </p>
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <div className="bg-[#1C2128] p-5 rounded-xl border border-[#30363D]">
                                        <label className="text-[10px] uppercase text-[#8B949E] font-bold mb-3 block tracking-widest">Pontos de Acupuntura</label>
                                        <div className="space-y-3">
                                            {currentDiagnosis.treatment?.plan?.acupuncture_points?.map((p: any, idx: number) => (
                                                <div key={idx} className="flex flex-col">
                                                    <span className="text-sm font-medium text-white">{p}</span>
                                                    <span className="text-[10px] text-[#8B949E]">Ponto de tratamento</span>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                    <div className="bg-[#1C2128] p-5 rounded-xl border border-[#30363D]">
                                        <label className="text-[10px] uppercase text-[#8B949E] font-bold mb-3 block tracking-widest">Informações Extras</label>
                                        <div className="space-y-3">
                                            <div className="flex flex-col">
                                                <span className="text-sm font-medium text-[#8B949E]">Prioridade</span>
                                                <span className="text-[10px] text-[#484F58]">{currentDiagnosis.treatment?.plan?.priority?.toUpperCase() || 'N/A'}</span>
                                            </div>
                                            <div className="flex flex-col">
                                                <span className="text-sm font-medium text-[#8B949E]">Frequência</span>
                                                <span className="text-[10px] text-[#484F58]">{currentDiagnosis.treatment?.plan?.frequency || 'N/A'}</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div className="p-4 bg-[#0D1117] border border-[#30363D] rounded-xl">
                                        <h5 className="text-xs font-bold text-[#E6EDF3] mb-2 flex items-center gap-2">
                                            <Lightbulb size={14} className="text-yellow-500" /> Próximo Estado
                                        </h5>
                                        <p className="text-[11px] text-[#8B949E]">{currentDiagnosis.decision?.next_state || 'N/A'}</p>
                                    </div>
                                    <div className="p-4 bg-[#0D1117] border border-[#30363D] rounded-xl">
                                        <h5 className="text-xs font-bold text-[#E6EDF3] mb-2 flex items-center gap-2">
                                            <BookOpen size={14} className="text-blue-400" /> Alertas Clínicos
                                        </h5>
                                        <p className="text-[11px] text-[#8B949E]">{currentDiagnosis.risk?.flag ? currentDiagnosis.risk?.reasons?.join(', ') : 'Sem contraindicações primárias'}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="space-y-4">
                                <label className="text-[10px] uppercase text-[#8B949E] font-bold block tracking-widest">Pontos Sugeridos</label>
                                <div className="space-y-2 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                                    {currentDiagnosis.treatment?.plan?.acupuncture_points?.map((ponto: any) => (
                                        <div key={ponto} className="p-3 bg-[#0D1117] border border-[#30363D] rounded-lg group hover:border-[#00C896] transition-all">
                                            <div className="flex items-center justify-between mb-1">
                                                <span className="text-xs font-mono text-[#00C896] font-bold">{ponto}</span>
                                                <CheckCircle2 size={12} className="text-[#30363D] group-hover:text-[#00C896]" />
                                            </div>
                                        </div>
                                    ))}
                                </div>
                                <button className="w-full py-3 bg-[#00C896] text-[#0D1117] rounded-xl font-bold text-xs hover:scale-[1.02] active:scale-[0.98] transition-all shadow-lg shadow-[#00C896]/10">
                                    Aplicar Protocolo ao Plano
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* ALERTA DE SEGURANÇA */}
            <div className="bg-red-500/10 border border-red-500/20 p-4 rounded-xl flex items-start gap-4">
                 <AlertTriangle className="text-red-500 shrink-0" size={20} />
                 <div>
                    <h4 className="text-xs font-bold text-red-500 uppercase tracking-widest mb-1">Alerta de Segurança Clínica</h4>
                    <p className="text-xs text-[#E6EDF3]">
                        {currentDiagnosis?.risk?.reasons?.join(', ') || "Paciente analisado pelo sistema. Mantenha vigilância sobre vasos e nervos."}
                    </p>
                 </div>
            </div>

            {/* PLANO DE TRATAMENTO MANUAL */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                 <div>
                    <label className="block text-xs uppercase text-[#8B949E] mb-3 font-bold tracking-widest">Técnicas Selecionadas</label>
                    <div className="grid grid-cols-2 gap-2">
                        {['Acupuntura', 'Moxabustão', 'Magneto', 'Laser', 'Ventosa', 'Eletro'].map(t => (
                            <button key={t} className="px-3 py-2 bg-[#0D1117] border border-[#30363D] rounded-lg text-[11px] text-left hover:border-[#00C896] transition-all">{t}</button>
                        ))}
                    </div>
                 </div>
                 <div>
                    <label className="block text-xs uppercase text-[#8B949E] mb-3 font-bold tracking-widest">Orientações e Dietoterapia</label>
                    <textarea 
                        className="w-full h-24 bg-[#0D1117] border border-[#30363D] rounded-xl p-4 text-[#E6EDF3] focus:border-[#00C896] text-sm"
                        placeholder="Orientações de dietoterapia e hábitos..."
                        value={anamnese.plano.orientacoes}
                        onChange={(e) => updateField('plano', 'orientacoes', e.target.value)}
                    />
                 </div>
            </div>
        </div>
    );
};
