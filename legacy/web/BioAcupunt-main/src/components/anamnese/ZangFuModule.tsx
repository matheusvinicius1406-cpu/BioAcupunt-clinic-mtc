import { useAnamneseStore } from "../../store/anamneseStore";
import { Circle, Box, Wind, Droplet, Flame } from "lucide-react";

export const ZangFuModule = () => {
    const anamnese = useAnamneseStore(state => state.anamneseData);
    const updateField = useAnamneseStore(state => state.updateField);

    const elements = [
        { name: 'Fogo', icon: Flame, color: 'text-red-500', organs: ['Coração', 'Intestino Delgado'] },
        { name: 'Terra', icon: Box, color: 'text-yellow-500', organs: ['Baço', 'Estômago'] },
        { name: 'Metal', icon: Wind, color: 'text-gray-300', organs: ['Pulmão', 'Intestino Grosso'] },
        { name: 'Água', icon: Droplet, color: 'text-blue-500', organs: ['Rim', 'Bexiga'] },
        { name: 'Madeira', icon: Circle, color: 'text-green-500', organs: ['Fígado', 'Vesícula Biliar'] },
    ];

    return (
        <div className="space-y-8 pb-20">
            {/* 5 ELEMENTOS VISUALIZER */}
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                {elements.map(el => (
                    <div key={el.name} className="bg-[#0D1117] p-4 rounded-xl border border-[#30363D] flex flex-col items-center gap-3 group hover:border-[#00C896]/50 transition-all">
                        <el.icon size={24} className={el.color} />
                        <span className="text-[10px] uppercase font-bold tracking-widest text-[#8B949E]">{el.name}</span>
                        <div className="flex flex-col items-center gap-1">
                            {el.organs.map(org => (
                                <span key={org} className="text-[9px] text-[#E6EDF3] opacity-60">{org}</span>
                            ))}
                        </div>
                    </div>
                ))}
            </div>

            {/* ZANG FU DETAILS */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-[#0D1117] p-6 rounded-xl border border-[#30363D]">
                    <h4 className="text-sm font-semibold text-[#00C896] mb-4">Sinais de Zang (Órgãos)</h4>
                    <div className="space-y-4">
                        {['Coração (Shen, Sangue)', 'Baço (Qi, Umidade)', 'Rim (Essência, Yang)'].map(z => (
                            <div key={z}>
                                <label className="text-[10px] text-[#8B949E] font-bold mb-2 block uppercase">{z}</label>
                                <textarea 
                                    className="w-full bg-[#161B22] border border-[#30363D] rounded-lg p-2 text-[11px] text-[#E6EDF3]"
                                    placeholder="Observações..."
                                />
                            </div>
                        ))}
                    </div>
                </div>
                <div className="bg-[#0D1117] p-6 rounded-xl border border-[#30363D]">
                    <h4 className="text-sm font-semibold text-[#00C896] mb-4">Sinais de Fu (Vísceras)</h4>
                    <div className="space-y-4">
                        {['Estômago/Intestinos', 'Bexiga', 'Vesícula Biliar'].map(f => (
                            <div key={f}>
                                <label className="text-[10px] text-[#8B949E] font-bold mb-2 block uppercase">{f}</label>
                                <textarea 
                                    className="w-full bg-[#161B22] border border-[#30363D] rounded-lg p-2 text-[11px] text-[#E6EDF3]"
                                    placeholder="Observações..."
                                />
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};
