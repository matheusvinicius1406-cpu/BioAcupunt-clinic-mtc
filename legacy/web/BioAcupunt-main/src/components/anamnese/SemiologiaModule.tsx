import { useAnamneseStore } from "../../store/anamneseStore";

export const SemiologiaModule = () => {
    const anamnese = useAnamneseStore(state => state.anamneseData);
    const updateField = useAnamneseStore(state => state.updateField);

    const linguaOptions = {
        cor: ['Pálida', 'Rosa Normal', 'Vermelha', 'Púrpura', 'Azulada'],
        forma: ['Normal', 'Inchada', 'Fina', 'Fissurada', 'Denteada'],
        saburraCor: ['Branca', 'Amarela', 'Cinza', 'Preta', 'Sem saburra'],
        saburraEspessura: ['Fina', 'Espessa', 'Úmida', 'Seca'],
        umidade: ['Seca', 'Normal', 'Escorregadia']
    };

    const pulsePositions = [
        { id: 'esquerdocun', label: 'Esq - Cun (Coração/PC)' },
        { id: 'esquerdoguan', label: 'Esq - Guan (Fígado/VB)' },
        { id: 'esquerdochi', label: 'Esq - Chi (Rim Yin)' },
        { id: 'direitocun', label: 'Dir - Cun (Pulmão)' },
        { id: 'direitoguan', label: 'Dir - Guan (Baço/E)' },
        { id: 'direitochi', label: 'Dir - Chi (Rim Yang)' },
    ];

    const pulseQualities = ['Vazio (Xu)', 'Cheio (Shi)', 'Corda (Xian)', 'Escorregadio (Hua)', 'Fino (Xi)', 'Flutuante (Fu)', 'Profundo (Chen)'];

    const togglePulseQuality = (posId: string, quality: string) => {
        const current = anamnese.pulso[posId as keyof typeof anamnese.pulso] as string[] || [];
        const next = current.includes(quality) ? current.filter(q => q !== quality) : [...current, quality];
        updateField('pulso', posId, next);
    };

    return (
        <div className="space-y-12 pb-20">
            {/* LÍNGUA */}
            <section>
                <h3 className="text-lg font-semibold text-[#00C896] mb-6 flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-[#00C896]"></span>
                    Exame de Língua
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {Object.entries(linguaOptions).map(([key, options]) => (
                        <div key={key} className="bg-[#0D1117] p-4 rounded-xl border border-[#30363D]">
                            <label className="block text-[10px] uppercase text-[#8B949E] mb-3 font-bold">{key.replace(/([A-Z])/g, ' $1')}</label>
                            <div className="flex flex-wrap gap-2">
                                {options.map(opt => (
                                    <button
                                        key={opt}
                                        onClick={() => updateField('lingua', key, opt)}
                                        className={`px-3 py-1.5 rounded-lg text-[10px] border transition-all ${
                                            anamnese.lingua[key as keyof typeof anamnese.lingua] === opt
                                                ? 'bg-[#00C896]/20 border-[#00C896] text-[#00C896]'
                                                : 'bg-[#161B22] border-[#30363D] text-[#E6EDF3] hover:border-[#8B949E]'
                                        }`}
                                    >
                                        {opt}
                                    </button>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            </section>

            {/* PULSOS */}
            <section>
                <h3 className="text-lg font-semibold text-[#00C896] mb-6 flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-[#00C896]"></span>
                    Pulsologia (28 Qualidades)
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {pulsePositions.map(pos => (
                        <div key={pos.id} className="bg-[#0D1117] p-4 rounded-xl border border-[#30363D]">
                            <label className="block text-[10px] uppercase text-[#8B949E] mb-3 font-bold tracking-tight">{pos.label}</label>
                            <div className="flex flex-wrap gap-1.5">
                                {pulseQualities.map(q => {
                                    const isSelected = (anamnese.pulso[pos.id as keyof typeof anamnese.pulso] as string[])?.includes(q);
                                    return (
                                        <button
                                            key={q}
                                            onClick={() => togglePulseQuality(pos.id, q)}
                                            className={`px-2 py-1 rounded-md text-[9px] border transition-all ${
                                                isSelected
                                                    ? 'bg-[#00C896]/20 border-[#00C896] text-[#00C896]'
                                                    : 'bg-[#161B22] border-[#30363D] text-[#E6EDF3] hover:border-[#8B949E]'
                                            }`}
                                        >
                                            {q}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>
                
                <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <label className="block text-[10px] uppercase text-[#8B949E] mb-2 font-bold">Frequência</label>
                        <select 
                            className="w-full bg-[#0D1117] border border-[#30363D] rounded-lg p-2 text-xs text-[#E6EDF3]"
                            value={anamnese.pulso.frequencia}
                            onChange={(e) => updateField('pulso', 'frequencia', e.target.value)}
                        >
                            <option value="">Selecione...</option>
                            <option value="lento">Lento (&lt;60 bpm)</option>
                            <option value="normal">Normal (60-80 bpm)</option>
                            <option value="rapido">Rápido (&gt;90 bpm)</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-[10px] uppercase text-[#8B949E] mb-2 font-bold">Impressão Geral</label>
                        <input 
                            type="text"
                            placeholder="Ex: Pulso em corda e fino..."
                            className="w-full bg-[#0D1117] border border-[#30363D] rounded-lg p-2 text-xs text-[#E6EDF3]"
                            value={anamnese.pulso.impressao}
                            onChange={(e) => updateField('pulso', 'impressao', e.target.value)}
                        />
                    </div>
                </div>
            </section>
        </div>
    );
};
