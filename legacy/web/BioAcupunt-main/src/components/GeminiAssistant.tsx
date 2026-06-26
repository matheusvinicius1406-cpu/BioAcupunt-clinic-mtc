import React, { useState, useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";
import { api } from "../services/api";
import { 
  Sparkles, 
  X, 
  Send, 
  Trash2, 
  User, 
  Bot, 
  Clipboard, 
  Minimize2,
  Lock,
  RefreshCw
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import Markdown from 'react-markdown';

interface Message {
  id: string;
  sender: "user" | "bot";
  text: string;
  timestamp: Date;
}

export function GeminiAssistant() {
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputText, setInputText] = useState("");
  const [loading, setLoading] = useState(false);
  const [activePatient, setActivePatient] = useState<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Check URL path dynamic context mapping
  const pathParts = location.pathname.split("/");
  let activePatientId = "";

  if (location.pathname.startsWith("/anamnese/") && pathParts[2]) {
    activePatientId = pathParts[2];
  } else if (location.pathname.startsWith("/tratamento/") && pathParts[2]) {
    activePatientId = pathParts[2];
  } else if (location.pathname.startsWith("/pacientes/avaliacao/") && pathParts[3]) {
    activePatientId = pathParts[3];
  } else if (location.pathname.startsWith("/pacientes/editar/") && pathParts[3]) {
    activePatientId = pathParts[3];
  }

  // Fetch patient details if viewing a patient-specific record
  useEffect(() => {
    if (activePatientId) {
      api.getPatient(activePatientId)
        .then(data => {
          setActivePatient(data);
          // Only add welcoming message if chat was empty
          if (messages.length === 0) {
            setMessages([
              {
                id: "welcome-ctx",
                sender: "bot",
                text: `Olá Dra. Camila! Vejo que você está atendendo **${data.name}**. Como posso lhe auxiliar com o raciocínio diagnóstico de MTC para este caso hoje?`,
                timestamp: new Date()
              }
            ]);
          }
        })
        .catch(err => {
          console.error("Erro ao carregar contexto de paciente para assistente:", err);
          setActivePatient(null);
        });
    } else {
      setActivePatient(null);
      if (messages.length === 0) {
        setMessages([
          {
            id: "welcome-general",
            sender: "bot",
            text: "Olá Dra. Camila! Sou seu Assistente Inteligente MTC oficial. Como posso lhe ajudar a selecionar pontos, fórmulas ou estruturar prontuários hoje?",
            timestamp: new Date()
          }
        ]);
      }
    }
  }, [activePatientId]);

  // Scroll to bottom on updates
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, isOpen, loading]);

  const handleSendMessage = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!inputText.trim() || loading) return;

    const userMsg: Message = {
      id: `msg_${Math.random().toString(36).substring(2, 9)}`,
      sender: "user",
      text: inputText,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMsg]);
    setInputText("");
    setLoading(true);

    try {
      // Map history correctly for the chat API
      const chatHistory = messages
        .filter(m => m.id !== "welcome-ctx" && m.id !== "welcome-general")
        .map(m => ({
          role: m.sender === "user" ? "user" : "model",
          parts: [{ text: m.text }]
        }));

      // Extra context helper
      const pageCtx = `Página de exibição atual: ${location.pathname}`;

      const res = await api.chat(userMsg.text, activePatientId || undefined, chatHistory, pageCtx);
      
      const botMsg: Message = {
        id: `msg_${Math.random().toString(36).substring(2, 9)}`,
        sender: "bot",
        text: res.response || "Compreendo, por favor dê mais detalhes sobre a queixa.",
        timestamp: new Date()
      };
      
      setMessages(prev => [...prev, botMsg]);
    } catch (err: any) {
      console.error("Erro no chat do assistente:", err);
      setMessages(prev => [
        ...prev,
        {
          id: `msg_${Math.random().toString(36).substring(2, 9)}`,
          sender: "bot",
          text: "Desculpe, encontrei uma interrupção na conexão do canal inteligente. Gostaria de tentar novamente?",
          timestamp: new Date()
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  // Close and Clear the session to save system tokens instantly
  const handleCloseAndClear = () => {
    setMessages([]);
    setInputText("");
    setIsOpen(false);
    setLoading(false);
  };

  const handleClearHistory = () => {
    setMessages([
      {
        id: "clear-refresh",
        sender: "bot",
        text: activePatient 
          ? `Sessão restaurada para **${activePatient.name}**. Como posso ajudar?` 
          : "Sessão reiniciada. Qual dúvida clínica deseja esclarecer?",
        timestamp: new Date()
      }
    ]);
  };

  // Format local timestamps nicely
  const formatTime = (date: Date) => {
    return date.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  };

  return (
    <div className="fixed bottom-6 right-6 z-[60] font-sans">
      {/* Floating pulsing round ball icon */}
      <motion.button
        id="floating-gemini-ball"
        onClick={() => setIsOpen(!isOpen)}
        whileHover={{ scale: 1.08 }}
        whileTap={{ scale: 0.95 }}
        className={`w-14 h-14 rounded-full flex items-center justify-center cursor-pointer shadow-lg hover:shadow-xl transition-all ${isOpen ? 'bg-rose-500 text-white animate-none' : 'bg-gradient-to-r from-emerald-500 to-emerald-700 text-white'}`}
        title="Assistente BioAcupunt Inteligente"
      >
        <AnimatePresence mode="wait">
          {isOpen ? (
            <motion.div initial={{ rotate: -45, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: 45, opacity: 0 }} key="close-icon">
              <X size={20} strokeWidth={2.5} />
            </motion.div>
          ) : (
            <motion.div initial={{ scale: 0.7, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.7, opacity: 0 }} key="spark-icon" className="relative">
              <span className="absolute -top-1 -right-1 flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-amber-500"></span>
              </span>
              <Sparkles size={22} className="fill-emerald-100/10 text-[#FFEAAA] animate-pulse" />
            </motion.div>
          )}
        </AnimatePresence>
      </motion.button>

      {/* Slide-Up Chat Container Card */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 30, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 30, scale: 0.95 }}
            className="absolute bottom-16 right-0 w-[400px] max-w-[calc(100vw-32px)] h-[540px] bg-white rounded-3xl border border-neutral-200/85 flex flex-col overflow-hidden shadow-2xl"
          >
            {/* Header Block with gold touch */}
            <div className="bg-gradient-to-r from-neutral-900 to-emerald-950 text-white p-4.5 flex items-center justify-between border-b border-emerald-950 shadow-sm">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center">
                  <Sparkles size={16} className="text-amber-300 fill-amber-300/10" />
                </div>
                <div>
                  <h3 className="text-sm font-bold tracking-tight text-white flex items-center gap-1.5 leading-none">
                    Canal Inteligente
                  </h3>
                  <span className="text-[10px] text-neutral-450 tracking-wider font-semibold block mt-1 uppercase flex items-center gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-450 inline-block animate-pulse" /> Gemini AI • Online
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-1">
                <button 
                  onClick={handleClearHistory}
                  className="p-1 px-1.5 hover:bg-white/10 rounded-lg text-neutral-400 hover:text-white transition-colors cursor-pointer text-[10px] font-bold flex items-center gap-1"
                  title="Reiniciar conversa"
                >
                  <RefreshCw size={11} /> Reiniciar
                </button>
                <button 
                  onClick={handleCloseAndClear}
                  className="p-1.5 hover:bg-rose-500/10 rounded-lg text-neutral-400 hover:text-rose-400 transition-colors cursor-pointer"
                  title="Fechar e Liberar Tokens"
                >
                  <Minimize2 size={13} />
                </button>
              </div>
            </div>

            {/* Active Clinical Patient Context Banner */}
            {activePatient && (
              <div className="bg-emerald-50/70 border-b border-emerald-100/50 p-2.5 px-4.5 flex items-center justify-between gap-2.5 text-xs">
                <p className="text-emerald-900 leading-normal font-semibold truncate flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-500" />
                  Contexto: <strong className="text-emerald-950">{activePatient.name}</strong>
                </p>
                <span className="text-[9px] bg-emerald-100 text-emerald-800 px-1.5 py-0.5 rounded-md font-extrabold uppercase">ATIVO</span>
              </div>
            )}

            {/* Scrollable messages container */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-slate-50/50">
              {messages.map((m) => {
                const isBot = m.sender === "bot";
                return (
                  <div key={m.id} className={`flex gap-2.5 max-w-[85%] ${isBot ? 'mr-auto' : 'ml-auto flex-row-reverse'}`}>
                    <div className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 border select-none ${isBot ? 'bg-emerald-50 border-emerald-100 text-emerald-700' : 'bg-neutral-900 border-neutral-900 text-white'}`}>
                      {isBot ? <Bot size={13} /> : <User size={13} />}
                    </div>
                    <div className="space-y-1">
                      <div className={`p-3.5 rounded-2xl text-[12.5px] leading-relaxed select-text ${isBot ? 'bg-white border border-neutral-100 text-neutral-800 shadow-2xs' : 'bg-emerald-600 text-white font-medium shadow-sm'}`}>
                        <div className="whitespace-pre-line break-words">
                          <Markdown
                            components={{
                              strong: ({node, ...props}) => (
                                <strong 
                                  className={`font-bold ${isBot ? 'text-emerald-primary' : 'text-amber-200'}`} 
                                  {...props} 
                                />
                              ),
                              p: ({node, ...props}) => <p className="last:mb-0 mb-1.5" {...props} />,
                              ul: ({node, ...props}) => <ul className="list-disc pl-4 space-y-1 mb-1.5" {...props} />,
                              ol: ({node, ...props}) => <ol className="list-decimal pl-4 space-y-1 mb-1.5" {...props} />,
                              li: ({node, ...props}) => <li className="text-[12.5px]" {...props} />,
                            }}
                          >
                            {m.text}
                          </Markdown>
                        </div>
                      </div>
                      <span className="text-[9px] text-neutral-400 block px-1 text-right">{formatTime(m.timestamp)}</span>
                    </div>
                  </div>
                );
              })}
              
              {loading && (
                <div className="flex gap-2.5 mr-auto max-w-[85%]">
                  <div className="w-7 h-7 rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-700 flex items-center justify-center shrink-0 animate-spin">
                    <Bot size={13} />
                  </div>
                  <div className="p-3 bg-white border border-neutral-100 text-neutral-500 text-xs italic rounded-2xl flex items-center gap-2 shadow-2xs">
                    <span className="flex gap-1">
                      <span className="w-1.5 h-1.5 bg-neutral-400 rounded-full animate-bounce" style={{ animationDelay: "0ms" }} />
                      <span className="w-1.5 h-1.5 bg-neutral-400 rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                      <span className="w-1.5 h-1.5 bg-neutral-400 rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                    </span>
                    Analisando meridianos...
                  </div>
                </div>
              )}
              
              <div ref={messagesEndRef} />
            </div>

            {/* Input Submission Footer Form */}
            <form onSubmit={handleSendMessage} className="p-4 bg-white border-t border-neutral-150 flex items-center gap-3">
              <input
                type="text"
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                placeholder={activePatient ? `Perguntar sobre ${activePatient.name.split(' ')[0]}...` : "Perguntar sobre canais, pontos..."}
                disabled={loading}
                className="flex-1 bg-neutral-50 hover:bg-neutral-100/60 focus:bg-white text-sm px-4 py-3 rounded-xl border border-neutral-200 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all text-neutral-850"
              />
              <button
                type="submit"
                disabled={!inputText.trim() || loading}
                className="p-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl transition-colors disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center shrink-0 shadow-md hover:shadow-lg disabled:shadow-none"
              >
                <Send size={15} />
              </button>
            </form>

            {/* Regulatory secure warning */}
            <div className="p-1 px-4.5 bg-neutral-50 border-t border-neutral-150 text-[9.5px] text-neutral-400 font-mono text-center flex items-center justify-center gap-1">
              <Lock size={8} /> Canal encriptado • Dra. Camila Silva CRM/CRBM
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
