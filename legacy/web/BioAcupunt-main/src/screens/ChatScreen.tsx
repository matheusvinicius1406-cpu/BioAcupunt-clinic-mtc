import { useState, useRef, useEffect } from "react";
import { useParams } from "react-router-dom";
import { api } from "../services/api";
import { Send, Bot, User, Sparkles, Loader2 } from "lucide-react";
import { motion } from "motion/react";
import ReactMarkdown from 'react-markdown';

export default function ChatScreen() {
  const { patientId } = useParams();
  const [messages, setMessages] = useState<any[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, loading]);

  const sendMessage = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!input.trim() || loading) return;

    const userMessage = { role: "user", text: input };
    setMessages(prev => [...prev, userMessage]);
    const currentInput = input;
    setInput("");
    setLoading(true);

    try {
      const response = await api.chat(currentInput, patientId, messages);
      setMessages(prev => [...prev, { role: "assistant", text: response.response }]);
    } catch (error) {
      console.error("Chat error:", error);
      setMessages(prev => [...prev, { role: "assistant", text: "Desculpe, tive um problema ao processar sua solicitação no modelo de Inteligência." }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-170px)] max-w-4xl mx-auto space-y-4">
      {/* Search/Header Title */}
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-display font-semibold text-emerald-dark flex items-center gap-2">
            <Sparkles className="text-emerald-primary" size={22} />
            Inteligência BioAcupunt
          </h1>
          <p className="text-xs text-neutral-400 mt-0.5">Assistente biomédico & MTC treinado com base em protocolos suíços</p>
        </div>
        <span className="text-[10px] bg-neutral-900 text-white font-mono px-2.5 py-1 rounded-md tracking-wider font-semibold">
          GEMINI LLM SECURE PROXY
        </span>
      </header>

      {/* Main chat window container */}
      <div className="flex-1 overflow-hidden flex flex-col bg-white rounded-2xl border border-emerald-light/60 shadow-sm">
        
        {/* Messages feed */}
        <div ref={scrollRef} className="flex-1 overflow-y-auto p-6 space-y-6 scroll-smooth bg-gradient-to-b from-white to-[#FBFDFB]">
          {messages.length === 0 && (
            <div className="h-full flex flex-col items-center justify-center text-center p-8 max-w-md mx-auto my-auto space-y-4">
              <div className="w-16 h-16 bg-emerald-light/40 border border-emerald-medium/20 text-emerald-primary rounded-full flex items-center justify-center shadow-2xs">
                <Bot size={28} />
              </div>
              <div>
                <h3 className="text-base font-display font-semibold text-emerald-dark">Como posso auxiliar seu diagnóstico hoje?</h3>
                <p className="text-xs text-neutral-400 mt-1 leading-relaxed">
                  Consulte sobre localização anatômica de meridianos, sinergias de óleos essenciais, fitoterapia, ou peça ajuda para estruturar o prontuário deste paciente.
                </p>
              </div>
              <div className="grid grid-cols-1 gap-2 w-full pt-2">
                <button 
                  onClick={() => setInput("Quais são as principais sinergias para dor lombar crônica?")}
                  className="p-2.5 text-[11px] text-neutral-500 bg-neutral-50 hover:bg-emerald-light/30 border border-neutral-200/50 rounded-xl text-left transition-colors cursor-pointer"
                >
                  ⚡ Sinergias para dor lombar crônica
                </button>
                <button 
                  onClick={() => setInput("Dicas de pontos de acupuntura para ansiedade na odontologia integrativa")}
                  className="p-2.5 text-[11px] text-neutral-500 bg-neutral-50 hover:bg-emerald-light/30 border border-neutral-200/50 rounded-xl text-left transition-colors cursor-pointer"
                >
                  ⚡ Pontos de auriculoterapia para ansiedade
                </button>
              </div>
            </div>
          )}

          {messages.map((m, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}
            >
              <div className={`flex gap-3 max-w-[85%] ${m.role === "user" ? "flex-row-reverse" : "flex-row"}`}>
                {/* Avatar bubble */}
                <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 font-bold border ${
                  m.role === "user" 
                    ? "bg-neutral-100 border-neutral-250 text-neutral-600" 
                    : "bg-emerald-light border-emerald-medium/10 text-emerald-primary"
                }`}>
                  {m.role === "user" ? <User size={14} /> : <Bot size={14} />}
                </div>
                
                {/* Text box bubble */}
                <div className={`p-4 rounded-2xl ${
                  m.role === "user" 
                    ? "bg-gradient-to-r from-emerald-primary to-emerald-dark text-white rounded-tr-none shadow-md shadow-emerald-primary/5" 
                    : "bg-[#FAF7F2] text-neutral-800 rounded-tl-none border border-gold-lux/20 shadow-3xs"
                }`}>
                  <div className={`prose prose-sm max-w-none leading-relaxed ${m.role === "user" ? "prose-invert" : ""}`}>
                    <ReactMarkdown>{m.text}</ReactMarkdown>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}

          {loading && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex justify-start">
              <div className="flex gap-3 max-w-[85%]">
                <div className="w-8 h-8 rounded-full bg-emerald-light text-emerald-primary flex items-center justify-center flex-shrink-0">
                  <Bot size={14} />
                </div>
                <div className="p-4 bg-[#FAF7F2] rounded-2xl rounded-tl-none border border-gold-lux/20 flex items-center gap-2">
                  <Loader2 className="animate-spin text-emerald-primary" size={15} />
                  <span className="text-xs text-neutral-400 font-medium">Buscando na base médica biomédica...</span>
                </div>
              </div>
            </motion.div>
          )}
        </div>

        {/* Input Form area */}
        <div className="p-4 bg-neutral-50/80 border-t border-emerald-light/40">
          <form onSubmit={sendMessage} className="relative">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Pergunte ao consultor biomédico de MTC..."
              className="w-full pl-5 pr-14 py-4 rounded-xl bg-white border border-emerald-light shadow-2xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:border-transparent text-sm transition-all text-neutral-700"
            />
            <button
              type="submit"
              disabled={loading || !input.trim()}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 p-2.5 bg-gradient-to-r from-emerald-primary to-emerald-dark text-white rounded-lg hover:opacity-95 disabled:opacity-30 transition-all cursor-pointer"
            >
              <Send size={15} />
            </button>
          </form>
          
          <div className="flex items-center justify-between text-[10px] text-neutral-400 mt-2.5 px-1 font-mono">
            <span>🛡️ Criptografia de ponta a ponta</span>
            <span>A Inteligência Artificial deve atuar como suporte consultivo</span>
          </div>
        </div>
      </div>
    </div>
  );
}
