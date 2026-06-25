import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api } from '../services/api';
import { ArrowLeft, Share2, Bookmark, Clock, Tag } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { motion } from 'motion/react';

export default function KnowledgeDetailScreen() {
  const { id } = useParams();
  const [item, setItem] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (id) {
      api.getKnowledgeById(id).then((data) => {
        setItem(data);
        setLoading(false);
      });
    }
  }, [id]);

  if (loading) return <div className="p-10 text-center animate-pulse">Carregando conteúdo...</div>;
  if (!item) return <div className="p-10 text-center">Conteúdo não encontrado.</div>;

  return (
    <motion.div 
      initial={{ opacity: 0 }} 
      animate={{ opacity: 1 }}
      className="max-w-3xl mx-auto bg-white min-h-screen shadow-xl"
    >
      <div className="sticky top-0 bg-white/80 backdrop-blur-md z-10 border-b border-gray-100 p-4 flex justify-between items-center">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
          <ArrowLeft size={24} />
        </button>
        <div className="flex gap-2">
          <button className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
            <Bookmark size={20} />
          </button>
          <button className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
            <Share2 size={20} />
          </button>
        </div>
      </div>

      <div className="p-6 md:p-10">
        <div className="flex items-center gap-2 mb-4">
          <span className="px-2 py-1 bg-emerald-100 text-emerald-700 text-xs font-bold rounded uppercase">
            {item.category}
          </span>
          <span className="text-gray-400 flex items-center gap-1 text-xs">
            <Clock size={12} /> 5 min leitura
          </span>
        </div>

        <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-6 leading-tight">
          {item.title}
        </h1>

        {item.image && (
          <img 
            src={item.image} 
            alt={item.title} 
            className="w-full rounded-2xl mb-8 object-cover max-h-80 shadow-md"
            referrerPolicy="no-referrer"
          />
        )}

        <div className="prose prose-emerald max-w-none">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {item.content}
          </ReactMarkdown>
        </div>

        <div className="mt-12 pt-8 border-t border-gray-100">
          <h4 className="text-sm font-bold text-gray-400 uppercase mb-4 flex items-center gap-2">
            <Tag size={14} /> Tags relacionadas
          </h4>
          <div className="flex flex-wrap gap-2">
            {item.tags?.map((tag: string) => (
              <Link 
                key={tag} 
                to={`/conhecimento/busca?q=${tag}`}
                className="px-3 py-1 bg-gray-50 hover:bg-emerald-50 text-emerald-700 rounded-full text-sm border border-gray-200 hover:border-emerald-200 transition-colors"
              >
                #{tag}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </motion.div>
  );
}
