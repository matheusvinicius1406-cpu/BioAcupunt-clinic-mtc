import { useAnamneseStore } from "../../store/anamneseStore";
import { Activity, Thermometer, Zap, Wind } from "lucide-react";

export const QueixaModule = () => {
  const anamnese = useAnamneseStore(state => state.anamneseData);
  const updateField = useAnamneseStore(state => state.updateField);

  const bodyRegions = [
    'Cabeça', 'Cervical', 'Ombros', 'Dorsal', 'Lombar', 'Sacral',
    'Braços', 'Mãos', 'Quadril', 'Coxas', 'Joelhos', 'Pés', 'Abdome'
  ];

  const toggleRegion = (region: string) => {
    const current = anamnese.mapaDor || [];
    const exists = current.find((r: any) => r.region === region);
    if (exists) {
        updateField('mapaDor', '', current.filter((r: any) => r.region !== region));
    } else {
        updateField('mapaDor', '', [...current, { region, eva: 5, type: 'Dor' }]);
    }
  };

  const updateEva = (region: string, eva: number) => {
    const next = (anamnese.mapaDor || []).map((r: any) => r.region === region ? { ...r, eva } : r);
    updateField('mapaDor', '', next);
  };

  return (
    <div className="space-y-8 pb-20">
      {/* QUEIXA TEXTUAL */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-4">
            <label className="block text-xs uppercase text-[#8B949E] font-bold tracking-widest">Queixa Principal</label>
            <textarea className="w-full h-32 bg-[#0D1117] border border-[#30363D] rounded-xl p-4 text-[#E6EDF3] focus:border-[#00C896] text-sm" 
              placeholder="Descreva detalhadamente..." 
              value={anamnese.queixa.principal} 
              onChange={(e) => updateField('queixa', 'principal', e.target.value)} />
        </div>
        <div className="space-y-4">
            <label className="block text-xs uppercase text-[#8B949E] font-bold tracking-widest">Cronologia & Início</label>
            <div className="grid grid-cols-2 gap-4">
                <select className="bg-[#0D1117] border border-[#30363D] rounded-lg p-2 text-xs text-[#E6EDF3]"
                    value={anamnese.queixa.cronologia}
                    onChange={(e) => updateField('queixa', 'cronologia', e.target.value)}>
                    <option value="">Tipo de Início...</option>
                    <option value="agudo">Agudo (&lt; 24h)</option>
                    <option value="subagudo">Subagudo (1-4 sem)</option>
                    <option value="cronico">Crônico (&gt; 3 meses)</option>
                </select>
                <input type="text" placeholder="Gatilho (ex: esforço)" className="bg-[#0D1117] border border-[#30363D] rounded-lg p-2 text-xs text-[#E6EDF3]"
                    value={anamnese.queixa.gatilho}
                    onChange={(e) => updateField('queixa', 'gatilho', e.target.value)} />
            </div>
            <textarea className="w-full h-16 bg-[#0D1117] border border-[#30363D] rounded-xl p-4 text-[#E6EDF3] focus:border-[#00C896] text-sm" 
              placeholder="Evolução desde o início..." 
              value={anamnese.queixa.evolucao} 
              onChange={(e) => updateField('queixa', 'evolucao', e.target.value)} />
        </div>
      </div>

      {/* MAPA DE DOR */}
      <div className="bg-[#0D1117] p-6 rounded-2xl border border-[#30363D]">
        <h4 className="text-sm font-semibold text-[#00C896] mb-6 flex items-center gap-2">
            <Activity size={16} /> Mapa Corporal de Desconforto
        </h4>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <div className="flex flex-wrap gap-2 content-start">
                {bodyRegions.map(region => {
                    const isSelected = anamnese.mapaDor?.find((r: any) => r.region === region);
                    return (
                        <button key={region} onClick={() => toggleRegion(region)}
                            className={`px-4 py-2 rounded-xl text-xs transition-all border ${isSelected ? 'bg-[#00C896] text-[#0D1117] font-bold' : 'bg-[#161B22] border-[#30363D] text-[#8B949E] hover:border-[#8B949E]'}`}>
                            {region}
                        </button>
                    );
                })}
            </div>
            <div className="space-y-4">
                <p className="text-[10px] uppercase text-[#8B949E] font-bold">Graduação EVA / Tipo da Dor</p>
                {anamnese.mapaDor?.length === 0 && <p className="text-xs text-[#30363D] italic">Nenhuma região selecionada...</p>}
                {anamnese.mapaDor?.map((r: any) => (
                    <div key={r.region} className="bg-[#161B22] p-4 rounded-xl border border-[#30363D] flex items-center gap-4">
                         <span className="text-xs font-bold w-20">{r.region}</span>
                         <input type="range" min="0" max="10" value={r.eva} onChange={(e) => updateEva(r.region, parseInt(e.target.value))}
                            className="flex-1 accent-[#00C896]" />
                         <span className={`text-xs font-bold w-8 text-center ${r.eva > 7 ? 'text-red-500' : 'text-[#00C896]'}`}>{r.eva}</span>
                    </div>
                ))}
            </div>
        </div>
      </div>

      {/* MODULADORES */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="p-4 bg-[#0D1117] border-l-4 border-blue-500/30 rounded-r-xl">
             <label className="text-[10px] uppercase text-[#8B949E] font-bold mb-2 block">O que MELHORA a dor?</label>
             <div className="flex flex-wrap gap-2">
                {['Calor', 'Frio', 'Movimento', 'Repouso', 'Pressão'].map(opt => (
                    <button key={opt} className={`px-2 py-1 rounded-md text-[10px] border border-[#30363D] transition-all`}>{opt}</button>
                ))}
             </div>
        </div>
        <div className="p-4 bg-[#0D1117] border-l-4 border-red-500/30 rounded-r-xl">
             <label className="text-[10px] uppercase text-[#8B949E] font-bold mb-2 block">O que PIORA a dor?</label>
             <div className="flex flex-wrap gap-2">
                {['Umidade', 'Vento', 'Noite', 'Esforço', 'Tensão'].map(opt => (
                    <button key={opt} className={`px-2 py-1 rounded-md text-[10px] border border-[#30363D] transition-all`}>{opt}</button>
                ))}
             </div>
        </div>
      </div>
    </div>
  );
};
