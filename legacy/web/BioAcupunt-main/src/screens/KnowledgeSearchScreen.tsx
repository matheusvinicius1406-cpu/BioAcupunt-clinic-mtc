import { useState, useEffect } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { Search, ChevronRight, ArrowLeft } from 'lucide-react';

export default function KnowledgeSearchScreen() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (query) {
      setLoading(true);
      api.searchKnowledge(query).then((data) => {
        setResults(data);
        setLoading(false);
      });
    }
  }, [query]);

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <header className="mb-8 flex items-center gap-4">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
          <ArrowLeft size={24} />
        </button>
        <h1 className="text-2xl font-bold text-emerald-900 leading-tight">
          Resultados para <span className="text-emerald-600">"{query}"</span>
        </h1>
      </header>

      {loading ? (
        <div className="text-center p-10 animate-pulse text-emerald-300 font-medium">Buscando na base de conhecimento...</div>
      ) : results.length === 0 ? (
        <div className="bg-white p-12 rounded-3xl text-center shadow-sm">
          <Search size={48} className="mx-auto text-emerald-100 mb-4" />
          <h3 className="text-xl font-bold text-gray-900 mb-2">Nada por aqui</h3>
          <p className="text-gray-500">Não encontramos resultados para sua busca. Tente termos mais genéricos.</p>
          <button 
            onClick={() => navigate('/conhecimento')}
            className="mt-6 px-6 py-2 bg-emerald-600 text-white rounded-full font-bold hover:bg-emerald-700 transition-colors"
          >
            Voltar para Início
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {results.map((item) => (
            <Link
              key={item.id}
              to={`/conhecimento/${item.id}`}
              className="block bg-white p-5 rounded-2xl shadow-sm border border-transparent hover:border-emerald-100 hover:shadow-md transition-all group"
            >
              <div className="flex justify-between items-center">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                     <span className="px-2 py-0.5 bg-gray-100 text-gray-500 text-[10px] font-bold rounded uppercase">
                      {item.category}
                    </span>
                  </div>
                  <h3 className="text-lg font-bold text-gray-900 group-hover:text-emerald-700 transition-colors">
                    {item.title}
                  </h3>
                  <p className="text-gray-500 text-sm line-clamp-2 mt-1">
                    {item.summary || item.content?.substring(0, 120)}...
                  </p>
                </div>
                <ChevronRight size={20} className="text-emerald-300 group-hover:text-emerald-500 ml-4 transition-colors" />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
