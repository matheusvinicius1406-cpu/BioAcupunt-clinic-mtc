import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAnamneseStore } from "../store/anamneseStore";
import { ClinicalHeader } from "../components/anamnese/ClinicalHeader";
import { ChevronRight, ChevronLeft, Activity, FileText, Brain, CheckCircle2, Save, Loader2, Zap, History, Calendar } from "lucide-react";
import { QueixaModule } from "../components/anamnese/QueixaModule";
import { BaGangModule } from "../components/anamnese/BaGangModule";
import { SemiologiaModule } from "../components/anamnese/SemiologiaModule";
import { DiagnosticoModule } from "../components/anamnese/DiagnosticoModule";
import { ZangFuModule } from "../components/anamnese/ZangFuModule";
import { api } from "../services/api";

export default function AnamneseScreen() {
    const { patientId } = useParams();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState(0);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [patient, setPatient] = useState<any>(null);

    const anamnese = useAnamneseStore(state => state.anamneseData);
    const clinicalHistory = useAnamneseStore(state => state.clinicalHistory);
    const updateField = useAnamneseStore(state => state.updateField);
    const setCurrentDiagnosis = useAnamneseStore(state => state.setCurrentDiagnosis);
    const setAnamnesis = useAnamneseStore(state => state.setAnamnesis);
    const setHistory = useAnamneseStore(state => state.setHistory);

    useEffect(() => {
        const loadData = async () => {
            if (!patientId) return;
            try {
                const patientData = await api.getPatient(patientId);
                setPatient(patientData);
                
                if (patientData.clinicalRecords && patientData.clinicalRecords.length > 0) {
                    setHistory(patientData.clinicalRecords);
                    // Load the most recent record by default if no ID is set in store
                    if (!anamnese.id) {
                        setAnamnesis(patientData.clinicalRecords[0]);
                        if (patientData.clinicalRecords[0].diagnostico) {
                            setCurrentDiagnosis(patientData.clinicalRecords[0].diagnostico);
                        }
                    }
                }
            } catch (err) {
                console.error("Erro ao carregar prontuário:", err);
            } finally {
                setLoading(false);
            }
        };
        loadData();
    }, [patientId]);

    const handleSave = async () => {
        if (!patientId) return;
        setSaving(true);
        try {
            const saved = await api.updateAnamnese(patientId, anamnese);
            setAnamnesis({ ...anamnese, id: saved.id });

            // Generate AI diagnosis after save in the diagnosis tab
            if (activeTab === 4) {
                 const diag = await api.diagnose(saved.id);
                 setCurrentDiagnosis(diag);
            }
        } catch (err) {
            console.error("Erro ao salvar:", err);
        } finally {
            setSaving(false);
        }
    };

    const tabs = [
        { label: 'Queixa & Dor', icon: FileText, component: <QueixaModule /> },
        { label: 'Interrogatório (Ba Gang)', icon: Brain, component: <BaGangModule /> },
        { label: 'Zang Fu & 5 Elementos', icon: Zap, component: <ZangFuModule /> },
        { label: 'Semiologia (Língua/Pulso)', icon: Activity, component: <SemiologiaModule /> },
        { label: 'Diagnóstico & Plano', icon: CheckCircle2, component: <DiagnosticoModule /> }
    ];

    if (loading) return (
        <div className="min-h-screen bg-[#0D1117] flex items-center justify-center">
            <Loader2 className="animate-spin text-[#00C896]" size={32} />
        </div>
    );

    return (
        <div className="min-h-screen bg-[#0D1117] text-[#E6EDF3] font-sans">
            <ClinicalHeader patient={patient || {name: 'Paciente não identificado'}} />
            
            <main className="max-w-7xl mx-auto p-6 grid grid-cols-1 lg:grid-cols-12 gap-8">
                 {/* Navigation & History Sidebar */}
                 <aside className="lg:col-span-3 space-y-6">
                    <div className="bg-[#161B22] p-4 rounded-2xl border border-[#30363D] shadow-sm">
                        <h3 className="text-[10px] font-bold text-[#8B949E] uppercase tracking-widest mb-3 px-2">Menu Clínico</h3>
                        {tabs.map((tab, idx) => (
                            <button key={tab.label} onClick={() => { handleSave(); setActiveTab(idx); }} 
                                className={`w-full text-left p-3 rounded-xl flex items-center gap-3 transition-all text-sm ${activeTab === idx ? 'bg-[#30363D] text-[#00C896] border border-[#00C896]/20' : 'text-[#8B949E] hover:bg-[#0D1117]'}`}>
                                <tab.icon size={16} /> {tab.label}
                            </button>
                        ))}
                    </div>

                    <div className="bg-[#161B22] p-5 rounded-2xl border border-[#30363D]">
                        <h3 className="text-[10px] font-bold text-[#8B949E] uppercase tracking-widest mb-4 flex items-center gap-2">
                            <History size={12} className="text-[#00C896]"/> Timeline Clínica
                        </h3>
                        <div className="space-y-2 max-h-[300px] overflow-y-auto pr-1">
                            {clinicalHistory.map((rec) => (
                                <button 
                                    key={rec.id}
                                    onClick={() => setAnamnesis(rec)}
                                    className={`w-full p-3 rounded-lg border text-left transition-all ${anamnese.id === rec.id ? 'bg-[#00C896]/10 border-[#00C896]/50' : 'bg-[#0D1117] border-[#30363D] hover:border-[#8B949E]'}`}
                                >
                                    <div className="flex items-center gap-2 text-xs font-bold text-[#E6EDF3]">
                                        <Calendar size={12} className="text-[#00C896]" />
                                        {new Date(rec.date).toLocaleDateString()}
                                    </div>
                                    <p className="text-[10px] text-[#8B949E] mt-1 line-clamp-1">{rec.queixa?.principal || "Consulta"}</p>
                                </button>
                            ))}
                        </div>
                        <button 
                            onClick={() => {
                                setAnamnesis({ ...anamnese, id: '', date: new Date().toISOString() });
                                setActiveTab(0);
                            }}
                            className="w-full mt-4 py-2 border border-dashed border-[#30363D] rounded-lg text-[10px] font-bold text-[#8B949E] hover:border-[#00C896] hover:text-[#00C896] transition-all"
                        >
                            + Novo Atendimento
                        </button>
                    </div>
                 </aside>

                 {/* Content Modules */}
                 <section className="lg:col-span-9 bg-[#161B22] p-10 rounded-2xl border border-[#30363D] min-h-[700px] shadow-2xl relative">
                    <div className="flex justify-between items-center mb-8">
                        <h2 className="text-2xl font-display text-[#E6EDF3] font-bold">
                            {tabs[activeTab].label}
                        </h2>
                        {saving && (
                            <div className="flex items-center gap-2 px-3 py-1 bg-[#00C896]/10 border border-[#00C896]/30 rounded-full">
                                <Loader2 size={12} className="animate-spin text-[#00C896]" />
                                <span className="text-[10px] text-[#00C896] font-bold uppercase tracking-widest">Calculando...</span>
                            </div>
                        )}
                    </div>

                    {tabs[activeTab].component}
                 </section>

                 <footer className="col-span-full flex justify-between items-center pt-8 border-t border-[#30363D]">
                    <button onClick={() => setActiveTab(t => Math.max(0, t-1))} className="text-[#8B949E] flex items-center gap-2 hover:text-white transition-all"><ChevronLeft size={16}/> Anterior</button>
                    <div className="flex gap-4">
                        <button onClick={handleSave} className="px-6 py-3 border border-[#30363D] rounded-xl text-[#8B949E] font-bold hover:bg-[#30363D] transition-all">Salvar Rascunho</button>
                        <button onClick={async () => { await handleSave(); if (activeTab < 3) setActiveTab(t => t + 1); else navigate('/dashboard'); }} 
                            className="bg-[#00C896] px-8 py-3 rounded-xl text-[#0D1117] font-bold flex items-center gap-2 hover:scale-[1.02] active:scale-[0.98] transition-all">
                            {activeTab === 3 ? 'Finalizar Atendimento' : 'Próximo Passo'} <ChevronRight size={16}/>
                        </button>
                    </div>
                 </footer>
            </main>
        </div>
    );
}
