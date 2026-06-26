import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { api } from "../services/api";
import { ArrowLeft, Sparkles, BookOpen } from "lucide-react";
import { motion } from "motion/react";

export default function SinergiaDetailScreen() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [synergy, setSynergy] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (id) {
      api.getSynergyById(id).then(data => {
        setSynergy(data);
        setLoading(false);
      });
    }
  }, [id]);

  if (loading) return <div className="p-10 text-center animate-pulse">Carregando protocolo...</div>;
  if (!synergy) return <div className="p-10 text-center">Protocolo não encontrado.</div>;

  return (
    <div className="max-w-4xl mx-auto">
      <header className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
          <ArrowLeft size={24} />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{synergy.title}</h1>
          <p className="text-emerald-700 font-medium flex items-center gap-1">
            <Sparkles size={14} /> Protocolo de Sinergia Facial
          </p>
        </div>
      </header>

      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white rounded-3xl shadow-xl overflow-hidden border border-gray-100"
      >
        <div className="p-8 md:p-12">
          <section className="mb-10">
            <h2 className="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
              <BookOpen size={20} className="text-emerald-600" /> Descrição e Objetivos
            </h2>
            <p className="text-gray-600 leading-relaxed text-lg">
              {synergy.description}
            </p>
          </section>

          <section className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="bg-emerald-50 p-6 rounded-2xl">
              <h3 className="font-bold text-emerald-800 mb-2">Pontos Recomendados</h3>
              <ul className="list-disc list-inside text-emerald-700 space-y-1 text-sm">
                {synergy.points?.map((p: string) => <li key={p}>{p}</li>)}
                {!synergy.points && <li>Consultar mapa de pontos faciais</li>}
              </ul>
            </div>
            <div className="bg-blue-50 p-6 rounded-2xl">
              <h3 className="font-bold text-blue-800 mb-2">Óleos e Essências</h3>
              <ul className="list-disc list-inside text-blue-700 space-y-1 text-sm">
                {synergy.essences?.map((e: string) => <li key={e}>{e}</li>)}
                {!synergy.essences && <li>Lavanda, Hortelã, Gerânio</li>}
              </ul>
            </div>
          </section>
        </div>
      </motion.div>
    </div>
  );
}

