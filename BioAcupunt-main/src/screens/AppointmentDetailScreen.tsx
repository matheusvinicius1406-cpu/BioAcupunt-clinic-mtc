import { useState, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { api } from "../services/api";
import { ArrowLeft, Calendar, Clock, User, ClipboardList, CheckCircle, XCircle, Trash2, Edit } from "lucide-react";
import { motion } from "motion/react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";

export default function AppointmentDetailScreen() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [appointment, setAppointment] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    if (id) {
      loadAppointment();
    }
  }, [id]);

  const loadAppointment = async () => {
    try {
      const data = await api.getAppointments();
      const app = data.find((a: any) => a.id === id);
      setAppointment(app);
    } catch (error) {
      console.error("Error loading appointment:", error);
    } finally {
      setLoading(false);
    }
  };

  const updateStatus = async (status: string) => {
    if (!id || updating) return;
    setUpdating(true);
    try {
      await api.updateAppointment(id, { status });
      await loadAppointment();
    } catch (error) {
      alert("Erro ao atualizar status.");
    } finally {
      setUpdating(false);
    }
  };

  const deleteAppointment = async () => {
    if (!id || !window.confirm("Deseja realmente excluir este agendamento?")) return;
    try {
      await api.deleteAppointment(id);
      navigate("/agenda");
    } catch (error) {
      alert("Erro ao excluir agendamento.");
    }
  };

  if (loading) return <div className="p-10 text-center animate-pulse">Carregando detalhes...</div>;
  if (!appointment) return <div className="p-10 text-center">Consulta não encontrada.</div>;

  const statusColors: any = {
    'SCHEDULED': 'bg-blue-100 text-blue-700',
    'CONFIRMED': 'bg-emerald-100 text-emerald-700',
    'DONE': 'bg-gray-100 text-gray-700',
    'CANCELLED': 'bg-rose-100 text-rose-700'
  };

  return (
    <div className="max-w-4xl mx-auto pb-12">
      <header className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700">
            <ArrowLeft size={24} />
          </button>
          <h1 className="text-3xl font-bold text-gray-900">Detalhes da Consulta</h1>
        </div>
        <div className="flex gap-2">
           <button 
            onClick={deleteAppointment}
            className="p-3 text-rose-600 hover:bg-rose-50 rounded-xl transition-colors border border-rose-100"
            title="Excluir"
          >
            <Trash2 size={20} />
          </button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100"
          >
            <div className="flex justify-between items-start mb-8">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 bg-emerald-100 rounded-2xl flex items-center justify-center text-emerald-700 font-bold text-2xl uppercase">
                  {appointment.patient?.name.charAt(0)}
                </div>
                <div>
                  <h2 className="text-2xl font-bold text-gray-900">{appointment.patient?.name}</h2>
                  <span className={`inline-block mt-1 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${statusColors[appointment.status]}`}>
                    {appointment.status}
                  </span>
                </div>
              </div>
              <Link 
                to={`/pacientes/editar/${appointment.patientId}`}
                className="p-2 text-gray-400 hover:text-emerald-600 rounded-lg transition-colors"
                title="Editar Paciente"
              >
                <Edit size={20} />
              </Link>
            </div>

            <div className="grid grid-cols-2 gap-8 py-8 border-y border-gray-50">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
                  <Calendar size={24} />
                </div>
                <div>
                  <p className="text-xs font-bold text-gray-400 uppercase">Data</p>
                  <p className="text-lg font-bold text-gray-900">
                    {format(new Date(appointment.date), "dd 'de' MMMM", { locale: ptBR })}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
                  <Clock size={24} />
                </div>
                <div>
                  <p className="text-xs font-bold text-gray-400 uppercase">Horário</p>
                  <p className="text-lg font-bold text-gray-900">{appointment.time}</p>
                </div>
              </div>
            </div>

            <div className="mt-8">
              <h3 className="text-sm font-bold text-gray-400 uppercase mb-3 px-1">Observações</h3>
              <p className="bg-gray-50 p-4 rounded-2xl text-gray-700 leading-relaxed min-h-[100px]">
                {appointment.notes || "Nenhuma observação registrada para este agendamento."}
              </p>
            </div>
          </motion.div>

          <div className="flex gap-4">
            <button 
              onClick={() => updateStatus('CONFIRMED')}
              disabled={appointment.status === 'CONFIRMED' || updating}
              className="flex-1 py-4 bg-emerald-50 text-emerald-700 rounded-2xl font-bold hover:bg-emerald-100 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
            >
              <CheckCircle size={20} /> Confirmar
            </button>
            <button 
              onClick={() => updateStatus('CANCELLED')}
              disabled={appointment.status === 'CANCELLED' || updating}
              className="flex-1 py-4 bg-rose-50 text-rose-700 rounded-2xl font-bold hover:bg-rose-100 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
            >
              <XCircle size={20} /> Cancelar
            </button>
          </div>
        </div>

        <div className="lg:col-span-1 space-y-6">
          <div className="bg-emerald-900 p-8 rounded-3xl shadow-xl text-white">
            <h3 className="text-lg font-bold mb-6 flex items-center gap-2">
              <ClipboardList size={20} /> Prontuário
            </h3>
            <p className="text-emerald-100 text-sm mb-8 leading-relaxed">
              Acesse o histórico completo do paciente para realizar o atendimento e registrar a evolução.
            </p>
            <Link 
              to={`/anamnese/${appointment.patientId}`}
              className="block w-full py-4 bg-white text-emerald-900 rounded-2xl font-bold text-center hover:bg-emerald-50 transition-all shadow-lg"
            >
              Abrir Atendimento
            </Link>
          </div>

          <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
            <h3 className="font-bold text-gray-900 mb-4">Ação Rápida</h3>
            <button 
              onClick={() => updateStatus('DONE')}
              disabled={appointment.status === 'DONE'}
              className="w-full py-4 border-2 border-emerald-600 text-emerald-600 rounded-2xl font-bold hover:bg-emerald-600 hover:text-white transition-all flex items-center justify-center gap-2"
            >
              Finalizar Atendimento
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
