import { motion } from 'motion/react';

export function EVASlider({ value, onChange }: { value: number, onChange: (v: number) => void }) {
  const getLabel = (v: number) => {
    if (v === 0) return { text: 'Nenhuma dor', color: 'text-emerald-500', emoji: '😊' };
    if (v <= 3) return { text: 'Dor leve', color: 'text-emerald-400', emoji: '🙂' };
    if (v <= 6) return { text: 'Dor moderada', color: 'text-amber-500', emoji: '😐' };
    if (v <= 8) return { text: 'Dor intensa', color: 'text-orange-500', emoji: '☹️' };
    return { text: 'Dor insuportável', color: 'text-rose-500', emoji: '😫' };
  };

  const label = getLabel(value);

  return (
    <div className="p-6 bg-white rounded-2xl border border-gray-100 shadow-sm">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h3 className="font-bold text-gray-900">Escala de Dor (EVA)</h3>
          <p className="text-sm text-gray-500">Avaliação da intensidade subjetiva</p>
        </div>
        <div className="text-right">
          <span className={`text-2xl font-black ${label.color}`}>{value}</span>
          <span className="text-gray-300 ml-1">/ 10</span>
        </div>
      </div>

      <div className="relative h-12 flex items-center">
        <input 
          type="range" 
          min="0" 
          max="10" 
          step="1"
          value={value} 
          onChange={(e) => onChange(parseInt(e.target.value))} 
          className="w-full h-2 bg-gray-100 rounded-lg appearance-none cursor-pointer accent-emerald-600" 
        />
      </div>

      <motion.div 
        key={value}
        initial={{ opacity: 0, y: 5 }}
        animate={{ opacity: 1, y: 0 }}
        className="mt-4 flex items-center justify-center gap-3 py-3 bg-gray-50 rounded-xl"
      >
        <span className="text-3xl">{label.emoji}</span>
        <span className={`font-bold ${label.color}`}>{label.text}</span>
      </motion.div>

      <div className="flex justify-between mt-4 px-1">
        {[0, 2, 4, 6, 8, 10].map(n => (
          <span key={n} className="text-[10px] font-bold text-gray-300">{n}</span>
        ))}
      </div>
    </div>
  );
}
