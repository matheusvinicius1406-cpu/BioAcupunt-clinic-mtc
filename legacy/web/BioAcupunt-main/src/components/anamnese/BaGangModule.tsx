import { useAnamneseStore } from "../../store/anamneseStore";

export const BaGangModule = () => {
    const anamnese = useAnamneseStore(state => state.anamneseData);
    const updateField = useAnamneseStore(state => state.updateField);

    const categories = [
        { 
            id: 'termorregulacao', 
            name: 'Termorregulação', 
            options: ['Aversão ao Frio', 'Aversão ao Calor', 'Ondas de Calor', 'Febrícula vespertina', 'Frio nas extremidades'] 
        },
        { 
            id: 'transpiracao', 
            name: 'Transpiração', 
            options: ['Diurna espontânea', 'Noturna (Yin Xu)', 'Sem suor', 'Sudorese localizada', 'Odor forte'] 
        },
        { 
            id: 'sono', 
            name: 'Sono', 
            options: ['Insônia inicial', 'Interrompido', 'Sonhos intensos', 'Sonolência excessiva', 'Normal'] 
        },
        { 
            id: 'digestao', 
            name: 'Digestão', 
            options: ['Distensão pós-prandial', 'Náusea', 'Refluxo', 'Fome excessiva', 'Sem apetite'] 
        },
        { 
            id: 'fezes', 
            name: 'Fezes', 
            options: ['Pastosas', 'Constipação', 'Urgência', 'Fezes ressecadas', 'Alternado'] 
        },
        { 
            id: 'urina', 
            name: 'Urina', 
            options: ['Escassa/Escura', 'Abundante/Clara', 'Noctúria', 'Ardor', 'Incontinência'] 
        },
        { 
            id: 'emocional', 
            name: 'Emocional', 
            options: ['Irritabilidade', 'Ansiedade', 'Tristeza', 'Medo', 'Preocupação'] 
        },
        { 
            id: 'energia', 
            name: 'Energia Geral', 
            options: ['Fadiga matinal', 'Fadiga vespertina', 'Fadiga constante', 'Boa disposição'] 
        },
    ];

    const toggleOption = (categoryId: string, option: string) => {
        const currentOptions = anamnese.baGang[categoryId as keyof typeof anamnese.baGang] as string[] || [];
        const newOptions = currentOptions.includes(option)
            ? currentOptions.filter(o => o !== option)
            : [...currentOptions, option];
        updateField('baGang', categoryId, newOptions);
    };

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pb-20">
            {categories.map(cat => (
                <div key={cat.id} className="bg-[#0D1117] p-6 rounded-xl border border-[#30363D] hover:border-[#00C896]/30 transition-all">
                    <h4 className="text-sm font-semibold text-[#00C896] mb-4 flex items-center justify-between">
                        {cat.name}
                        <span className="text-[10px] text-[#8B949E] uppercase font-bold tracking-widest">
                            {(anamnese.baGang[cat.id as keyof typeof anamnese.baGang] as string[])?.length || 0} selecionados
                        </span>
                    </h4>
                    <div className="grid grid-cols-2 gap-2">
                        {cat.options.map(opt => {
                            const isSelected = (anamnese.baGang[cat.id as keyof typeof anamnese.baGang] as string[])?.includes(opt);
                            return (
                                <button
                                    key={opt}
                                    onClick={() => toggleOption(cat.id, opt)}
                                    className={`text-left px-3 py-2 rounded-lg text-[11px] transition-all border ${
                                        isSelected 
                                            ? 'bg-[#00C896]/10 border-[#00C896] text-[#00C896]' 
                                            : 'bg-[#161B22] border-[#30363D] text-[#E6EDF3] hover:border-[#8B949E]'
                                    }`}
                                >
                                    {opt}
                                </button>
                            );
                        })}
                    </div>
                </div>
            ))}
            
            <div className="col-span-full">
                <label className="block text-xs uppercase text-[#8B949E] mb-2 font-bold tracking-widest">Notas do Interrogatório</label>
                <textarea 
                    className="w-full h-24 bg-[#0D1117] border border-[#30363D] rounded-xl p-4 text-[#E6EDF3] focus:border-[#00C896] text-sm"
                    placeholder="Outros sinais e sintomas observados..."
                    value={anamnese.baGang.notas}
                    onChange={(e) => updateField('baGang', 'notas', e.target.value)}
                />
            </div>
        </div>
    );
};
