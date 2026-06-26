import { AlertTriangle, Clock, Mic, FileStack, User } from 'lucide-react';

export const ClinicalHeader = ({ patient }: { patient: any }) => {
  return (
    <header className="sticky top-0 bg-[#0D1117] border-b border-[#30363D] p-4 flex items-center justify-between z-50">
      <div className="flex items-center gap-4">
        <div className="w-10 h-10 rounded-full bg-[#30363D] flex items-center justify-center text-[#E6EDF3]">
            <User size={20} />
        </div>
        <div>
            <h1 className="text-lg font-display text-[#E6EDF3] font-semibold">{patient?.name || "Paciente Selecionado"}</h1>
            <p className="text-xs text-[#8B949E]">Sessão atual: #12 • Idade: 45 anos</p>
        </div>
      </div>
      
      <div className="flex items-center gap-4">
        <div className="w-24 h-8 bg-[#161B22] rounded-md border border-[#30363D]"></div> {/* Sparkline mock */}
        <button className="p-2 bg-[#161B22] rounded-full text-[#00C896]"><Mic size={18}/></button>
        <button className="p-2 bg-[#161B22] rounded-full text-[#E6EDF3]"><FileStack size={18}/></button>
      </div>
    </header>
  );
};
