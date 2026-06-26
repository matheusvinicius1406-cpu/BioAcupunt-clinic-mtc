import { useState, useEffect } from 'react';
import { Outlet, Link, useLocation } from 'react-router-dom';
import { GeminiAssistant } from './GeminiAssistant';
import { 
  LayoutDashboard, 
  Users, 
  Calendar, 
  ClipboardList, 
  TrendingUp, 
  Brain, 
  Sparkles, 
  Wallet, 
  Settings,
  Clock,
  Play,
  Pause,
  RotateCcw,
  Shield,
  Layers
} from 'lucide-react';

export function Layout() {
  const location = useLocation();
  const currentPath = location.pathname;

  // Real Timer Retenção state for the acupuncture needles (20 minutes default)
  const [timerLeft, setTimerLeft] = useState(1200); // 20 minutes in seconds
  const [timerActive, setTimerActive] = useState(false);
  const [showTimerControls, setShowTimerControls] = useState(false);

  useEffect(() => {
    let interval: any = null;
    if (timerActive && timerLeft > 0) {
      interval = setInterval(() => {
        setTimerLeft((prev) => prev - 1);
      }, 1000);
    } else if (timerLeft === 0) {
      setTimerActive(false);
    }
    return () => clearInterval(interval);
  }, [timerActive, timerLeft]);

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleResetTimer = () => {
    setTimerLeft(1200);
    setTimerActive(false);
  };

  const menuItems = [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/pacientes', label: 'Pacientes', icon: Users },
    { path: '/agenda', label: 'Agenda', icon: Calendar },
    { path: '/anamnese', label: 'Atendimento', icon: ClipboardList },
    { path: '/evolucao', label: 'Evolução Clín.', icon: TrendingUp },
    { path: '/conhecimento', label: 'Inteligência', icon: Brain },
    { path: '/sinergia', label: 'Sinergia', icon: Sparkles },
    { path: '/financeiro', label: 'Financeiro', icon: Wallet },
    { path: '/ajustes', label: 'Ajustes', icon: Settings },
  ];

  // Helper to determine active tab based on route
  const isTabActive = (itemPath: string) => {
    if (itemPath === '/') {
      return currentPath === '/' || currentPath === '';
    }
    if (itemPath === '/conhecimento') {
      return currentPath.startsWith('/conhecimento') || currentPath.startsWith('/chat');
    }
    return currentPath.startsWith(itemPath);
  };

  // Get professional settings to display in signature / header
  const [profName, setProfName] = useState(() => localStorage.getItem("bio_profName") || "Dra. Camila Silva");
  const [crbmNum, setCrbmNum] = useState(() => localStorage.getItem("bio_crbmNum") || "SP-12345");
  const [specialties, setSpecialties] = useState(() => localStorage.getItem("bio_specialties") || "Terapias Integradas • CRM / CRF / CRBM Acupuntura");

  // Keep professional details updated if changed in Ajustes
  useEffect(() => {
    const checkSettings = () => {
      setProfName(localStorage.getItem("bio_profName") || "Dra. Camila Silva");
      setCrbmNum(localStorage.getItem("bio_crbmNum") || "SP-12345");
      setSpecialties(localStorage.getItem("bio_specialties") || "Terapias Integradas • CRM / CRF / CRBM Acupuntura");
    };

    window.addEventListener("storage", checkSettings);
    const interval = setInterval(checkSettings, 1000); // fallback polling for fast sync

    return () => {
      window.removeEventListener("storage", checkSettings);
      clearInterval(interval);
    };
  }, []);

  return (
    <div className="min-h-screen bg-[#0D1117] flex flex-col font-sans selection:bg-[#00C896]/30 selection:text-[#00C896]">
      {/* Dynamic Top Navigation Header - Elegant Glassmorphism Effect */}
      <header className="bg-[#161B22]/95 backdrop-blur-md border-b border-[#30363D] px-4 md:px-12 py-3.5 sticky top-0 z-50 shadow-sm">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          
          {/* Logo / Left Part */}
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#00C896] flex items-center justify-center text-[#0D1117] font-bold text-sm tracking-widest shadow-sm border border-[#00C896]/30 transition-transform hover:rotate-12 duration-300">
              CS
            </div>
            <div>
              <div className="flex items-center gap-2">
                <Link to="/" className="text-2xl font-display font-semibold text-[#E6EDF3] tracking-normal flex items-center gap-1.5 hover:opacity-90">
                  BioAcupunt
                  <span className="text-xs font-serif font-semibold tracking-widest text-[#0D1117] uppercase px-1.5 py-0.5 bg-[#00C896] rounded-md scale-90">
                    EXCELLENCE
                  </span>
                </Link>
              </div>
              <span className="text-[9.5px] font-black tracking-widest text-[#00C896] uppercase block mt-0.5">
                {specialties}
              </span>
            </div>
          </div>

          {/* Right Header Status / Timer controls */}
          <div className="flex flex-wrap items-center gap-3">
            {/* Interactive needle timer */}
            <div className="relative">
              <button 
                onClick={() => setShowTimerControls(!showTimerControls)}
                className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-full border text-xs font-semibold shadow-xs transition-transform hover:scale-105 bg-[#161B22] border-[#30363D] text-[#8B949E] cursor-pointer"
              >
                <Clock size={13} className={timerActive ? "animate-spin text-[#00C896]" : "text-[#00C896]"} />
                <span>RETENÇÃO DE AGULHAS</span>
                <span className="bg-[#00C896] text-[#0D1117] font-mono px-2 py-0.5 rounded ml-1 text-xs">{formatTimer(timerLeft)}</span>
              </button>

              {showTimerControls && (
                <div className="absolute top-full right-0 mt-2 bg-[#161B22] rounded-2xl p-5 border border-[#30363D] shadow-xl z-50 w-64 text-center">
                  <p className="text-xs font-bold text-[#8B949E] uppercase tracking-wider mb-2">Timer de Retenção de Agulhas</p>
                  <p className="text-3xl font-mono font-black text-[#E6EDF3] mb-4">{formatTimer(timerLeft)}</p>
                  <div className="flex justify-center gap-2">
                    <button 
                      onClick={() => setTimerActive(!timerActive)}
                      className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold text-[#0D1117] shadow-md transition-colors cursor-pointer ${timerActive ? 'bg-amber-500 hover:bg-amber-600' : 'bg-[#00C896] hover:bg-[#00DDA6]'}`}
                    >
                      {timerActive ? <Pause size={12} /> : <Play size={12} />}
                      {timerActive ? 'Pausar' : 'Iniciar'}
                    </button>
                    <button 
                      onClick={handleResetTimer}
                      className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold bg-[#30363D] hover:bg-[#3E444D] text-[#E6EDF3] border border-[#30363D] transition-colors cursor-pointer"
                    >
                      <RotateCcw size={12} />
                      Zerar
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Active CRBM indicator badge */}
            <span className="flex items-center gap-1.5 px-3.5 py-1.5 bg-[#161B22] border border-[#30363D] text-[#8B949E] text-xs font-semibold rounded-full shadow-2xs">
              <span className="w-2 h-2 rounded-full bg-[#00C896] animate-pulse" />
              Selo BioAcupunt Supremo
            </span>
          </div>

        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-8 flex flex-col gap-6">
        
        {/* Secondary Title line */}
        <div className="flex flex-col gap-1">
          <p className="text-[10px] font-black tracking-widest text-[#00C896] uppercase">
            MTC ANALYSIS & BIOMEDICINE • SUPREMO SYSTEM
          </p>
          <div className="flex items-center justify-between flex-wrap gap-4">
            <h1 className="text-3.5xl font-display font-medium text-[#E6EDF3] tracking-tight">
              Olá, <span className="text-[#00C896] italic font-semibold">{profName}</span>
            </h1>
            <span className="text-[11px] font-mono font-bold text-[#8B949E] bg-[#161B22] p-1 px-3 border border-[#30363D] rounded-full">
              {new Date().toLocaleDateString("pt-BR", { weekday: "long", year: "numeric", month: "long", day: "numeric" })}
            </span>
          </div>
        </div>

        {/* Beautiful Horizontal Tabs navigation menu */}
        <nav className="bg-[#161B22] p-2 rounded-2xl border border-[#30363D] shadow-sm flex flex-wrap gap-1 z-10 w-full">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const active = isTabActive(item.path);
            
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`
                  flex items-center gap-2.5 px-4 py-2.5 rounded-xl transition-all duration-300 text-xs font-medium cursor-pointer
                  ${active 
                    ? 'bg-[#00C896] text-[#0D1117] shadow-lg shadow-[#00C896]/15 font-semibold' 
                    : 'text-[#8B949E] hover:bg-[#30363D] hover:text-[#E6EDF3]'}
                `}
              >
                <Icon size={14} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        {/* Content Outlet with smooth motion fade effects */}
        <div className="flex-1 flex flex-col min-h-0 bg-transparent">
          <Outlet />
        </div>

      </main>

      {/* Footer info line as displayed in bottom margins */}
      <footer className="bg-[#0D1117] border-t border-[#30363D] py-8 px-12 mt-16 text-center text-xs font-mono text-[#8B949E]">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex flex-col items-center md:items-start gap-1">
            <p className="flex items-center justify-center gap-1 font-bold text-[#00C896] tracking-wide">
              BIOACUPUNT CLINICAL PLATFORM <span className="text-[#00C896] font-serif">★ SUPREMO</span>
            </p>
            <p className="text-[10px] text-[#8B949E]">Tecnologia avançada para Medicina Tradicional Chinesa</p>
          </div>
          <div className="text-center md:text-right">
            <p className="text-[11px]">© 2026 BIOACUPUNT • SUPREMO EDITION</p>
            <p className="text-[10px] text-[#30363D] font-mono mt-1">Conformidade com Regulamentos Gerais de Saúde & Proteção de Dados</p>
          </div>
        </div>
      </footer>

      {/* Floating Gemini Assistant widget */}
      <GeminiAssistant />
    </div>
  );
}
