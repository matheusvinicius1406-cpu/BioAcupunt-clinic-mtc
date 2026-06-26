import { useState, useEffect } from "react";
import { api } from "../services/api";
import { 
  Calendar, 
  Clock, 
  ChevronRight, 
  Plus
} from "lucide-react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";

const TIME_SLOTS = [
  "08:00",
  "09:00",
  "10:00",
  "11:00",
  "13:00",
  "14:00",
  "15:00",
  "16:00",
  "17:00"
];

const prefilledBookings = [
  { time: "09:00", patientName: "Maria de Souza", type: "Sessão", status: "RESERVADO", patientId: "p1" },
  { time: "11:00", patientName: "João Santos", type: "Avaliação", status: "RESERVADO", patientId: "p2" },
  { time: "15:00", patientName: "Ana Carvalho", type: "Retorno", status: "RESERVADO", patientId: "p3" }
];

export default function AgendaScreen() {
  const [patients, setPatients] = useState<any[]>([]);
  const [bookings, setBookings] = useState<any[]>(() => {
    const saved = localStorage.getItem("bio_schedule_bookings");
    return saved ? JSON.parse(saved) : prefilledBookings;
  });

  const [slotDrafts, setSlotDrafts] = useState<Record<string, { patientName: string; type: string; patientId: string }>>({});

  useEffect(() => {
    api.getPatients().then(data => {
      setPatients(data);
      if (data.length > 0) {
        setBookings(prev => prev.map(bk => {
          const match = data.find((p: any) => p.name.toLowerCase() === bk.patientName.toLowerCase());
          if (match) bk.patientId = match.id;
          return bk;
        }));
      }
    });
  }, []);

  useEffect(() => {
    localStorage.setItem("bio_schedule_bookings", JSON.stringify(bookings));
  }, [bookings]);

  const handleDraftChange = (time: string, field: string, value: string) => {
    setSlotDrafts(prev => ({
      ...prev,
      [time]: {
        ...(prev[time] || { patientName: "", type: "Sessão", patientId: "" }),
        [field]: value
      }
    }));
  };

  const handleConfirmBooking = (time: string) => {
    const draft = slotDrafts[time];
    if (!draft || !draft.patientName.trim()) return;

    const existingPatient = patients.find(p => p.name.toLowerCase() === draft.patientName.trim().toLowerCase());
    const matchedId = existingPatient ? existingPatient.id : `p_temp_${Math.random().toString(36).substring(2, 9)}`;

    const newBooking = {
      time,
      patientName: draft.patientName.trim(),
      type: draft.type || "Sessão",
      status: "RESERVADO",
      patientId: matchedId
    };

    setBookings(prev => [...prev.filter(b => b.time !== time), newBooking]);
    
    setSlotDrafts(prev => {
      const copy = { ...prev };
      delete copy[time];
      return copy;
    });
  };

  const handleCancelBooking = (time: string) => {
    if (window.confirm(`Deseja remover o agendamento de ${time}?`)) {
      setBookings(prev => prev.filter(b => b.time !== time));
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      
      {/* Upper header section layout with Swiss quality feeling */}
      <div className="bg-white p-8 rounded-2xl border border-emerald-light/60 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-[#F4F7F5] rounded-full -mr-10 -mt-10 opacity-60 pointer-events-none" />
        <div className="space-y-1.5 flex-1 z-10">
          <h2 className="text-2xl font-display font-semibold text-emerald-dark flex items-center gap-2">
            <Calendar size={22} className="text-emerald-primary" />
            Agenda de Atendimento Clínico
          </h2>
          <p className="text-[#6B7280] text-xs leading-relaxed max-w-2xl">
            Gerencie os horários para acupuntura, biomedicina integrativa e acompanhamento. Conecte de forma imediata as fichas clínicas.
          </p>
        </div>

        {/* Right Green Pill indicator of active clinical session */}
        <div className="bg-emerald-light/40 border border-emerald-medium/10 rounded-xl p-4 flex items-center gap-3 self-start md:self-auto z-10 shrink-0 select-none">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-primary animate-pulse" />
          <div>
            <p className="text-[9px] font-black text-emerald-primary uppercase leading-none tracking-widest">ECOSSISTEMA DE AGENDAS</p>
            <p className="text-xs font-bold text-neutral-800 mt-1.5 pb-0.5 leading-none">Dra. Camila Silva</p>
          </div>
        </div>
      </div>

      {/* Grid of Hourly slots */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {TIME_SLOTS.map((time) => {
          const booking = bookings.find(b => b.time === time);
          const draft = slotDrafts[time] || { patientName: "", type: "Sessão", patientId: "" };
          const isReserved = !!booking;

          return (
            <div 
              key={time}
              className={`rounded-2xl border p-5 space-y-4 transition-all relative ${
                isReserved 
                  ? 'bg-white border-emerald-light shadow-sm' 
                  : 'bg-white border-dashed border-neutral-200'
              }`}
            >
              
              {/* Card topbar showing state */}
              <div className="flex justify-between items-center pb-2 border-b border-neutral-50/50">
                <span className="text-xs font-bold text-emerald-dark bg-emerald-light/40 p-1 px-3 rounded-lg flex items-center gap-1">
                  <Clock size={11} className="text-emerald-primary" />
                  {time}
                </span>

                <span className={`text-[9.5px] font-black tracking-wider uppercase px-2 py-0.5 rounded border ${
                  isReserved ? 'bg-[#FAF7F2] text-[#A6884E] border-[#C9A96E]/20' : 'text-neutral-400 border-transparent'
                }`}>
                  {isReserved ? "RESERVADO" : "LIVRE"}
                </span>
              </div>

              {isReserved ? (
                // Reserved State View
                <div className="space-y-4">
                  <div className="flex items-start gap-3">
                    <div className="w-10 h-10 bg-emerald-light text-emerald-primary rounded-full flex items-center justify-center font-bold text-sm border border-emerald-medium/10 shrink-0 uppercase shadow-3xs">
                      {booking.patientName.charAt(0)}
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="text-xs font-bold text-neutral-800 tracking-tight leading-tight truncate">{booking.patientName}</h4>
                      <span className={`p-1 px-2.5 text-[9px] font-bold rounded-lg inline-block mt-2 ${
                        booking.type === 'Avaliação' ? 'bg-blue-50 text-blue-600 border border-blue-100' :
                        booking.type === 'Retorno' ? 'bg-amber-50 text-amber-600 border border-amber-100' :
                        'bg-emerald-light text-emerald-primary border border-emerald-medium/10'
                      }`}>
                        {booking.type}
                      </span>
                    </div>
                  </div>

                  {/* Actions for reserved slot */}
                  <div className="pt-3 border-t border-neutral-100 flex justify-between items-center">
                    <button 
                      onClick={() => handleCancelBooking(time)}
                      className="text-xs font-bold text-rose-500 hover:text-rose-700 hover:underline cursor-pointer"
                    >
                      Remover horário
                    </button>

                    <Link 
                      to={`/anamnese/${booking.patientId}`}
                      className="flex items-center gap-1 px-3.5 py-1.5 bg-gradient-to-r from-emerald-primary to-emerald-dark text-white rounded-xl text-[11px] font-bold shadow-md shadow-emerald-primary/10 transition-colors cursor-pointer hover:opacity-95"
                    >
                      Prontuário
                      <ChevronRight size={10} strokeWidth={2.5} />
                    </Link>
                  </div>
                </div>
              ) : (
                // Free Slot Booking Form Stage
                <div className="space-y-4">
                  <div>
                    <label className="block text-[9px] font-black text-neutral-400 uppercase mb-1.5 tracking-wider">Identificar Paciente</label>
                    <input 
                      type="text" 
                      placeholder="Nome completo..."
                      list="patients_select_datalist"
                      value={draft.patientName}
                      onChange={(e) => handleDraftChange(time, "patientName", e.target.value)}
                      className="w-full p-2.5 bg-[#F4F7F5]/50 border border-emerald-light rounded-xl text-xs font-medium text-neutral-700 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white"
                    />
                    <datalist id="patients_select_datalist">
                      {patients.map(p => <option key={p.id} value={p.name} />)}
                    </datalist>
                  </div>

                  {/* Type Selector Pills */}
                  <div>
                    <label className="block text-[9px] font-black text-neutral-400 uppercase mb-1.5 tracking-wider">Atendimento</label>
                    <div className="flex gap-1.5">
                      {["Avaliação", "Retorno", "Sessão"].map((tType) => {
                        const isSel = draft.type === tType;
                        return (
                          <button
                            key={tType}
                            type="button"
                            onClick={() => handleDraftChange(time, "type", tType)}
                            className={`flex-1 p-1.5 text-[9.5px] font-bold rounded-lg transition-all border cursor-pointer ${
                              isSel 
                                ? 'bg-emerald-primary text-white border-emerald-primary shadow-xs' 
                                : 'bg-[#F4F7F5]/45 border-neutral-200 text-neutral-500 hover:text-neutral-700'
                            }`}
                          >
                            {tType}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Action CTA Confirm */}
                  <div className="pt-2">
                    <button 
                      onClick={() => handleConfirmBooking(time)}
                      disabled={!draft.patientName.trim()}
                      className="w-full text-center p-2.5 bg-emerald-light/40 hover:bg-emerald-light disabled:bg-neutral-50 border border-emerald-medium/15 disabled:border-neutral-200 text-emerald-dark disabled:text-neutral-300 rounded-xl text-xs font-bold transition-all cursor-pointer"
                    >
                      ✓ Confirmar Reserva
                    </button>
                  </div>
                </div>
              )}

            </div>
          );
        })}
      </div>

    </div>
  );
}
