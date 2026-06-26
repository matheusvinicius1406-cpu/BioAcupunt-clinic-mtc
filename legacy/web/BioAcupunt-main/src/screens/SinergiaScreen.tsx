import { useState } from "react";
import { 
  Star, 
  AlertTriangle, 
  Zap
} from "lucide-react";
import { motion } from "motion/react";

const SINERGIAS = [
  {
    id: 1,
    title: "Microagulhamento Estético (Dermaroller)",
    intro: "Ao associar o agulhamento sistêmico em BP6 e IG11, o tempo de reparação epitelial cai de 48h para menos de 24h. O estímulo ao colágeno age em sinergia profunda.",
    warning: "Atenção: Evite passar agulha rígida sobre vasos congestionados; use pontos de canais para drenar calor e acelerar mitose."
  },
  {
    id: 2,
    title: "Toxina Botulínica (Botox)",
    intro: "O ponto Yintang e pontos locais faciais reduzem a força hiperativa transitória dos músculos faciais, fazendo os efeitos do Botox durarem mais tempo e prevenindo cefaleias tensionais reflexas.",
    warning: "Nota: Aplique acupuntura antes do botox. Nunca realize agulhamento local na face nas primeiras 72h pós aplicação para evitar migração difusiva do composto."
  },
  {
    id: 3,
    title: "Preenchedores & Fios de Sustentação de PDO",
    intro: "A inserção de fios ou ácido hialurônico causa microtraumas locais e edemas. O agulhamento contralateral em IG4 impede retenção hídrica extrema e hematomas de longa duração.",
    warning: "Nota: Higienização cirúrgica extrema da face deve ser garantida durante toda a sessão."
  }
];

const PONTOS_ESTETICOS = [
  {
    name: "Yintang (Entre as Sobrancelhas)",
    canal: "Extraordinário (Ex-HN3)",
    effect: "Diminui instantaneamente a ansiedade gerada pela agulha do preenchimento/botox. Reduz a tensão do músculo corrugador da glabela, suavizando rugas expressivas."
  },
  {
    name: "IG4 (Hegu)",
    canal: "Intestino Grosso",
    effect: "O ponto analgésico mais potente do corpo para a face. Reduz severamente a dor e estimula a drenagem linfática, minimizando edemas pós-procedimento."
  },
  {
    name: "VG20 (Baihui - Cem Reuniões)",
    canal: "Vaso Governador",
    effect: "Ponto clássico para ascender o Qi vital. Age com potente efeito lifting facial, contraindo tecidos flácidos e otimizando a firmeza muscular cutânea."
  },
  {
    name: "BP6 (Sanyinjiao)",
    canal: "Baço-Pâncreas",
    effect: "O ponto mestre de regulação do sangue, Yin e hormônios femininos. Melhora a hidratação natural da derme profunda, promovendo brilho celular espontâneo."
  }
];

export default function SinergiaScreen() {
  const [selectedPonto, setSelectedPonto] = useState<any>(PONTOS_ESTETICOS[0]);

  return (
    <div className="max-w-7xl mx-auto space-y-8">
      
      {/* Black/Jade luxury green banner */}
      <div className="bg-gradient-to-r from-emerald-dark to-[#041F17] p-8 md:p-10 rounded-2xl text-white shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-80 h-80 bg-emerald-primary/10 -mr-20 -mt-20 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-gold-lux/5 -ml-20 -mb-20 rounded-full blur-2xl" />
        
        <div className="space-y-3.5 max-w-4xl relative z-10">
          <span className="p-1 px-3 bg-gradient-to-r from-[#C9A96E] to-[#FAF7F2]/20 border border-[#C9A96E]/30 text-white text-[9px] font-black tracking-widest rounded-full uppercase">
            BIOMEDICINA ESTÉTICA & ACUPUNTURA INTEGRATIVA
          </span>
          <h2 className="text-2xl md:text-4xl font-display font-medium leading-tight tracking-tight mt-2">
            Guia de Sinergia Integrada & Harmonização Facial MTC
          </h2>
          <p className="text-neutral-300 text-xs leading-relaxed">
            Consulte soluções clínicas que interconectam técnicas de volumização, clareamento, suspensão de PDO e modulação de colágeno às correntes de energia biomédica integrativas.
          </p>
        </div>
      </div>

      {/* Main double column split layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        
        {/* Left Column: Sinergia nos Procedimentos */}
        <div className="lg:col-span-7 bg-white p-6 md:p-8 rounded-2xl border border-emerald-light/60 shadow-sm space-y-6">
          <div>
            <h3 className="text-lg font-display font-semibold text-emerald-dark flex items-center gap-1.5">
              <Zap size={16} className="text-emerald-primary" />
              Sincronização nos Procedimentos Estéticos
            </h3>
            <p className="text-xs text-neutral-400 mt-0.5">Sintonia milenar integrada à dermatofuncional.</p>
          </div>

          <div className="space-y-5">
            {SINERGIAS.map((sin, idx) => (
              <div 
                key={sin.id} 
                className="bg-[#FBFDFB] border border-emerald-light/40 rounded-xl p-5 space-y-3.5 hover:border-emerald-primary/30 transition-all shadow-3xs"
              >
                <div className="flex items-center gap-3">
                  <span className="w-6 h-6 rounded-full bg-gradient-to-tr from-emerald-primary to-emerald-dark text-white font-bold text-xs flex items-center justify-center shadow-sm">
                    {idx + 1}
                  </span>
                  <h4 className="text-xs font-bold text-neutral-800 uppercase tracking-wider">{sin.title}</h4>
                </div>

                <p className="text-xs text-neutral-600 leading-relaxed font-medium pl-1">
                  {sin.intro}
                </p>

                {/* Warning note bar with gold border as requested by user */}
                <div className="bg-[#FAF7F2] border border-gold-lux/20 rounded-lg p-3 flex items-start gap-2.5">
                  <AlertTriangle className="text-[#A6884E] flex-shrink-0 mt-0.5" size={13} />
                  <p className="text-[10.5px] text-[#A6884E] font-bold leading-relaxed">
                    {sin.warning}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Column: Pontos de Acupuntura */}
        <div className="lg:col-span-5 bg-white p-6 md:p-8 rounded-2xl border border-emerald-light/60 shadow-sm space-y-6">
          <div>
            <h3 className="text-lg font-display font-semibold text-emerald-dark flex items-center gap-1.5">
              <Star size={16} className="text-[#C9A96E]" fill="#C9A96E" />
              Pontos Estéticos Selecionados (MTC)
            </h3>
            <p className="text-xs text-neutral-400 mt-0.5">Clique para ampliar os benefícios anatômicos.</p>
          </div>

          {/* List of points */}
          <div className="space-y-3">
            {PONTOS_ESTETICOS.map((ponto) => {
              const isActive = selectedPonto.name === ponto.name;
              return (
                <button
                  key={ponto.name}
                  onClick={() => setSelectedPonto(ponto)}
                  className={`w-full text-left p-4.5 rounded-xl border transition-all flex items-start gap-3 cursor-pointer ${
                    isActive 
                      ? 'bg-emerald-light/30 border-emerald-medium/30 shadow-3xs' 
                      : 'bg-transparent border-neutral-100 hover:bg-neutral-50/50'
                  }`}
                >
                  <div className={`w-2 h-2 rounded-full mt-1.5 flex-shrink-0 ${isActive ? 'bg-emerald-primary scale-125 shadow-sm' : 'bg-neutral-300'}`} />
                  <div className="min-w-0 flex-1">
                    <h4 className="text-xs font-bold text-neutral-800 leading-tight">{ponto.name}</h4>
                    <p className="text-[9px] text-[#A6884E] font-bold uppercase tracking-wider mt-1 font-mono">Canal: {ponto.canal}</p>
                    {isActive && (
                      <p className="text-[11px] text-neutral-500 mt-2.5 leading-relaxed bg-white p-3.5 rounded-lg border border-emerald-light/50 shadow-3xs">
                        {ponto.effect}
                      </p>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </div>

      </div>

    </div>
  );
}
