import { Link } from "react-router-dom";
import { ChevronRight, ArrowRight, Sparkles } from "lucide-react";

export function SynergyCard({ synergy }: { synergy: any }) {
  return (
    <div className="bg-white rounded-3xl p-6 shadow-sm border border-emerald-50 hover:shadow-md transition-all group flex flex-col h-full">
      <div className="w-12 h-12 bg-emerald-100 rounded-2xl flex items-center justify-center text-emerald-600 mb-4 group-hover:scale-110 transition-transform">
        <Sparkles size={24} />
      </div>
      
      <h3 className="text-xl font-bold text-gray-900 mb-2 truncate group-hover:text-emerald-700 transition-colors">
        {synergy.title}
      </h3>
      
      <p className="text-gray-500 text-sm line-clamp-3 mb-6 flex-1">
        {synergy.description}
      </p>
      
      <div className="pt-4 border-t border-gray-50 mt-auto">
        <Link 
          to={`/sinergia/${synergy.id}`}
          className="flex items-center justify-between font-bold text-emerald-600 group-hover:text-emerald-700 text-sm"
        >
          Explorar Protocolo
          <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform" />
        </Link>
      </div>
    </div>
  );
}
