import { useState, useEffect } from "react";
import { api } from "../services/api";
import { 
  Users, 
  Calendar, 
  TrendingUp, 
  Clock, 
  MessageSquare,
  ChevronRight,
  Plus,
  BookOpen,
  Sparkles,
  ShieldAlert,
  Sliders,
  BadgeAlert
} from "lucide-react";
import { motion } from "motion/react";
import { Link } from "react-router-dom";

export default function DashboardScreen() {
  const [stats, setStats] = useState({
    totalPatients: 0,
    appointmentsToday: 0,
    monthlyIncome: 0,
    nextAppointment: null as any
  });
  const [loading, setLoading] = useState(true);
  const [profName] = useState(() => localStorage.getItem("bio_profName") || "Dra. Camila Silva");

  useEffect(() => {
    async function loadDashboardData() {
      try {
        const patients = await api.getPatients();
        const appointments = await api.getAppointments({ date: new Date().toISOString().split('T')[0] });
        const finance = await api.getFinanceSummary({ period: 'month' });
        
        setStats({
          totalPatients: patients.length,
          appointmentsToday: appointments.length,
          monthlyIncome: finance?.totalIncome || 0,
          nextAppointment: appointments[1] || appointments[0] || null // Fetch next actual appointment
        });
      } catch (error) {
        console.error("Erro dashboard:", error);
      } finally {
        setTimeout(() => {
          setLoading(false);
        }, 300); // Small delay to appreciate the smooth shimmer transition
      }
    }
    loadDashboardData();
  }, []);

  // Structural entry animation presets
  const containerVariants = {
    hidden: { opacity: 0 },
    show: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1
      }
    }
  };

  const itemVariants: any = {
    hidden: { opacity: 0, y: 15 },
    show: { opacity: 1, y: 0, transition: { duration: 0.4 } }
  };

  if (loading) {
    return (
      <div className="space-y-8">
        {/* Shimmer skeleton screen as requested by user */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map((n) => (
            <div key={n} className="bg-white p-6 rounded-3xl border border-emerald-light/40 shadow-2xs space-y-4">
              <div className="w-12 h-12 rounded-2xl animate-shimmer" />
              <div className="h-4 w-28 rounded-md animate-shimmer" />
              <div className="h-8 w-20 rounded-md animate-shimmer" />
            </div>
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div className="bg-white p-8 rounded-3xl border border-emerald-light/40 shadow-2xs h-64 animate-shimmer" />
          <div className="bg-white p-8 rounded-3xl border border-emerald-light/40 shadow-2xs h-64 animate-shimmer" />
        </div>
      </div>
    );
  }

  const cards = [
    { 
      label: 'Pacientes Ativos', 
      value: stats.totalPatients, 
      description: 'Prontuários registrados na plataforma',
      icon: Users, 
      color: 'from-emerald-primary/10 to-emerald-medium/10', 
      iconColor: 'text-emerald-primary',
      link: '/pacientes' 
    },
    { 
      label: 'Consultas Hoje', 
      value: stats.appointmentsToday, 
      description: 'Sessões de acupuntura e estética integradas',
      icon: Calendar, 
      color: 'from-[#FAF7F2] to-[#EAD4A6]/10', 
      iconColor: 'text-[#C9A96E]',
      link: '/agenda' 
    },
    { 
      label: 'Faturamento Mensal', 
      value: `R$ ${stats.monthlyIncome.toLocaleString('pt-BR')}`, 
      description: 'Fluxo financeiro consolidado',
      icon: TrendingUp, 
      color: 'from-emerald-dark/10 to-emerald-primary/10', 
      iconColor: 'text-emerald-dark',
      link: '/financeiro' 
    },
  ];

  return (
    <motion.div 
      variants={containerVariants}
      initial="hidden"
      animate="show"
      className="space-y-8"
    >
      {/* Metrics Summary Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {cards.map((card, idx) => (
          <motion.div
            key={card.label}
            variants={itemVariants}
            className="bg-white p-6 rounded-3xl shadow-sm border border-emerald-light/60 hover-lift relative overflow-hidden group flex flex-col justify-between"
          >
            {/* Background design elements to elevate visual luxury */}
            <div className={`absolute top-0 right-0 w-32 h-32 bg-gradient-to-br ${card.color} opacity-40 -mr-10 -mt-10 rounded-full blur-xl group-hover:scale-110 transition-transform duration-500`} />
            
            <div>
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-white to-[#F4F7F5] border border-emerald-light shadow-2xs flex items-center justify-center mb-6">
                <card.icon size={22} className={card.iconColor} />
              </div>
              <p className="text-xs font-bold text-neutral-400 uppercase tracking-widest">{card.label}</p>
              <p className="text-3xl font-display font-semibold text-emerald-dark mt-1">{card.value}</p>
            </div>
            
            <p className="text-[11px] text-neutral-400 mt-4 font-sans line-clamp-1 border-t border-neutral-100 pt-3">
              {card.description}
            </p>
            <Link to={card.link} className="absolute inset-0 z-10" />
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        
        {/* Next Patients / Appointments list with refined styling */}
        <motion.section 
          variants={itemVariants}
          className="bg-white p-8 rounded-3xl shadow-sm border border-emerald-light/60 flex flex-col justify-between"
        >
          <div>
            <div className="flex justify-between items-center mb-6">
              <div>
                <h2 className="text-lg font-display font-semibold text-emerald-dark">Próximo Atendimento</h2>
                <p className="text-xs text-neutral-400 mt-0.5">Sessão imediata integrada na fila</p>
              </div>
              <Link to="/agenda" className="text-emerald-primary text-xs font-bold flex items-center gap-1 hover:text-emerald-dark transition-colors">
                Agenda Completa <ChevronRight size={14} />
              </Link>
            </div>

            {stats.nextAppointment ? (
              <div className="p-5 rounded-2xl bg-[#FBFDFB] border border-emerald-medium/15 hover:border-emerald-primary/30 transition-all shadow-3xs flex items-center gap-4">
                <div className="w-14 h-14 bg-gradient-to-tr from-emerald-primary to-emerald-dark text-white rounded-full flex items-center justify-center font-display font-medium text-xl shadow-md">
                  {stats.nextAppointment.patient?.name.charAt(0)}
                </div>
                <div className="flex-1 min-w-0">
                  <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-primary bg-emerald-light/40 px-2 py-0.5 rounded-full mb-1">
                    <Sparkles size={8} /> INTEGRATIVA
                  </span>
                  <p className="font-semibold text-neutral-800 text-sm truncate">{stats.nextAppointment.patient?.name}</p>
                  <div className="flex items-center gap-3 mt-1.5 text-xs text-neutral-400">
                    <span className="flex items-center gap-1 bg-neutral-100 px-2 py-0.5 rounded text-neutral-600 font-mono"><Clock size={13} /> {stats.nextAppointment.time}</span>
                    <span className="h-1 w-1 bg-neutral-300 rounded-full" />
                    <span className="truncate">Medicina Tradicional Chinesa</span>
                  </div>
                </div>
                <Link 
                  to={`/anamnese/${stats.nextAppointment.patientId}`}
                  className="px-4.5 py-2.5 bg-gradient-to-r from-emerald-primary to-emerald-dark text-white rounded-xl text-xs font-bold shadow-md shadow-emerald-primary/10 hover-lift active:scale-95 cursor-pointer"
                >
                  Atender
                </Link>
              </div>
            ) : (
              <div className="text-center py-12 bg-[#FCFDFD] rounded-2xl border border-dashed border-emerald-light/60 flex flex-col items-center justify-center">
                <Calendar size={36} className="text-emerald-primary/40 mb-3" />
                <p className="text-xs font-medium text-neutral-400">Sem consultas agendadas para hoje.</p>
                <Link
                  to="/agenda"
                  className="mt-3 inline-flex items-center gap-1 text-xs text-emerald-primary font-bold hover:underline"
                >
                  <Plus size={12} /> Agendar Nova Consulta
                </Link>
              </div>
            )}
          </div>

          <div className="border-t border-neutral-100 pt-6 mt-6 flex items-center justify-between text-xs text-neutral-400">
            <span className="flex items-center gap-1.5 font-medium text-emerald-primary bg-emerald-light/10 px-2 py-1 rounded">
              <span className="w-2 h-2 rounded-full bg-emerald-primary animate-pulse" />
              Sincronizado em tempo real com o Supabase
            </span>
            <span className="font-mono">v2.1.0</span>
          </div>
        </motion.section>

        {/* Access Hub Section with dark background, emerald gradients, and quality golden seals */}
        <motion.section 
          variants={itemVariants}
          className="bg-gradient-to-br from-emerald-dark to-[#041F17] p-8 rounded-3xl shadow-xl text-white relative overflow-hidden flex flex-col justify-between"
        >
          {/* Glowing biological ambient effect */}
          <div className="absolute top-0 right-0 w-80 h-80 bg-emerald-primary/10 -mr-16 -mt-16 rounded-full blur-3xl pointer-events-none" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-[#C9A96E]/5 -ml-12 -mb-12 rounded-full blur-2xl pointer-events-none" />
          
          <div className="relative z-10">
            <div className="flex justify-between items-start mb-6">
              <div>
                <h2 className="text-lg font-display font-medium tracking-wide">Acesso Rápido Integrado</h2>
                <p className="text-xs text-neutral-400 mt-0.5">Atalhos para processos clínicos rotineiros</p>
              </div>
              <span className="text-[9px] font-black tracking-widest text-[#C9A96E] border border-[#C9A96E]/30 px-2 py-0.5 rounded font-mono uppercase bg-black/20">
                SWISS BIO-TECH
              </span>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Link to="/pacientes" className="bg-white/[0.04] border border-white/[0.06] hover:bg-white/[0.08] hover:border-white/[0.12] p-4.5 rounded-2xl transition-all duration-300 flex flex-col gap-3 group">
                <div className="w-10 h-10 bg-emerald-primary rounded-xl flex items-center justify-center text-white shadow-md group-hover:scale-105 transition-transform">
                  <Plus size={18} />
                </div>
                <div>
                  <span className="font-bold text-sm block">Novo Paciente</span>
                  <span className="text-[10px] text-neutral-400 mt-0.5 block">Gerar prontuário biomédico</span>
                </div>
              </Link>
              
              <Link to="/conhecimento" className="bg-white/[0.04] border border-white/[0.06] hover:bg-white/[0.08] hover:border-white/[0.12] p-4.5 rounded-2xl transition-all duration-300 flex flex-col gap-3 group">
                <div className="w-10 h-10 bg-[#C9A96E] rounded-xl flex items-center justify-center text-white shadow-md group-hover:scale-105 transition-transform">
                  <MessageSquare size={18} />
                </div>
                <div>
                  <span className="font-bold text-sm block">Consultar IA</span>
                  <span className="text-[10px] text-neutral-400 mt-0.5 block">Perguntar ao modelo Gemini</span>
                </div>
              </Link>

              <Link to="/sinergia" className="bg-white/[0.04] border border-white/[0.06] hover:bg-white/[0.08] hover:border-white/[0.12] p-4.5 rounded-2xl transition-all duration-300 flex flex-col gap-3 group">
                <div className="w-10 h-10 bg-teal-600 rounded-xl flex items-center justify-center text-white shadow-md group-hover:scale-105 transition-transform">
                  <BookOpen size={18} />
                </div>
                <div>
                  <span className="font-bold text-sm block">Protocolos</span>
                  <span className="text-[10px] text-neutral-400 mt-0.5 block">Catálogo de sinergias & MTC</span>
                </div>
              </Link>

              <Link to="/financeiro" className="bg-white/[0.04] border border-white/[0.06] hover:bg-white/[0.08] hover:border-white/[0.12] p-4.5 rounded-2xl transition-all duration-300 flex flex-col gap-3 group">
                <div className="w-10 h-10 bg-amber-600 rounded-xl flex items-center justify-center text-white shadow-md group-hover:scale-105 transition-transform">
                  <TrendingUp size={18} />
                </div>
                <div>
                  <span className="font-bold text-sm block">Financeiro</span>
                  <span className="text-[10px] text-neutral-400 mt-0.5 block">Novo lançamento de caixa</span>
                </div>
              </Link>
            </div>
          </div>
          
          <div className="border-t border-white/[0.06] pt-6 mt-6 relative z-10 flex items-center justify-between">
            <span className="text-[11px] text-neutral-400 flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-[#C9A96E]" />
              Selo de Excelência Clínica Ativa
            </span>
            <div className="w-8 h-8 rounded-full border border-[#C9A96E]/20 bg-[#C9A96E]/10 flex items-center justify-center text-[#C9A96E] font-serif font-black text-xs">
              ★
            </div>
          </div>
        </motion.section>
      </div>
    </motion.div>
  );
}
