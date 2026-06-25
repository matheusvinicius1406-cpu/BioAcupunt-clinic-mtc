import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { ArrowLeft, ChevronRight, Book } from 'lucide-react';

export default function KnowledgeCategoryScreen() {
  const { category } = useParams();
  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    api.getKnowledge({ category }).then((data) => {
      setItems(data);
      setLoading(false);
    });
  }, [category]);

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <header className="mb-8 flex items-center gap-4">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700 transition-colors">
          <ArrowLeft size={24} />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-emerald-900">{category}</h1>
          <p className="text-emerald-700 text-sm">{items.length} artigos encontrados</p>
        </div>
      </header>

      <div className="space-y-4">
        {loading ? (
          <div className="text-center p-10 text-emerald-300">Carregando itens...</div>
        ) : items.length === 0 ? (
          <div className="p-12 bg-white rounded-2xl text-center">
            <Book size={48} className="mx-auto text-emerald-100 mb-4" />
            <p className="text-gray-500">Nenhum conteúdo encontrado nesta categoria.</p>
          </div>
        ) : (
          items.map((item) => (
            <Link
              key={item.id}
              to={`/conhecimento/${item.id}`}
              className="block bg-white p-5 rounded-2xl shadow-sm border border-emerald-50 hover:border-emerald-200 hover:shadow-md transition-all group"
            >
              <div className="flex justify-between items-center">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 group-hover:text-emerald-700 transition-colors">
                    {item.title}
                  </h3>
                  <p className="text-gray-500 text-sm line-clamp-1 mt-1">
                    {item.summary || item.content?.substring(0, 80)}...
                  </p>
                </div>
                <ChevronRight size={20} className="text-emerald-300 group-hover:text-emerald-500 transition-colors" />
              </div>
            </Link>
          ))
        )}
      </div>
    </div>
  );
}
