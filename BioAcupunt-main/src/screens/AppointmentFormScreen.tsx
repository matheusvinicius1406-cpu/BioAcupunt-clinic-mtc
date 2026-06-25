import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../services/api";
import { ArrowLeft, Save, User, Calendar, Clock, FileText, CheckCircle2 } from "lucide-react";
import { motion } from "motion/react";
import { format } from "date-fns";

export default function AppointmentFormScreen() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const patientIdFromQuery = searchParams.get('patientId');

  const [patients, setPatients] = useState<any[]>([]);
  const [loadingPatients, setLoadingPatients] = useState(true);
  const [saving, setSaving] = useState(false);

  const [formData, setFormData] = useState({
    patientId: patientIdFromQuery || "",
    date: format(new Date(), 'yyyy-MM-dd'),
    time: "09:00",
    duration: 60,
    type: "ACUPUNTURA",
    notes: "",
    status: "SCHEDULED"
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    api.getPatients().then(data => {
      setPatients(data);
      setLoadingPatients(false);
    });
  }, []);

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.patientId) newErrors.patientId = "Paciente é obrigatório";
    if (!formData.date) newErrors.date = "Data é obrigatória";
    if (!formData.time) newErrors.time = "Hora é obrigatória";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setSaving(true);
    try {
      await api.createAppointment({
        ...formData,
        duration: Number(formData.duration)
      });
      navigate("/agenda");
    } catch (error) {
      console.error("Save appointment error:", error);
      alert("Erro ao agendar consulta.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto pb-12">
      <header className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700 transition-colors">
          <ArrowLeft size={24} />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Agendar Consulta</h1>
          <p className="text-gray-500">Defina o paciente, data e horário do atendimento</p>
        </div>
      </header>

      <motion.form 
        initial={{ opacity: 0, scale: 0.98 }}
        animate={{ opacity: 1, scale: 1 }}
        onSubmit={handleSubmit} 
        className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100"
      >
        <div className="space-y-6">
          {/* Paciente */}
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <User size={16} className="text-emerald-500" /> Selecionar Paciente *
            </label>
            <select
              disabled={!!patientIdFromQuery}
              className={`w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 ${errors.patientId ? 'ring-2 ring-rose-500' : ''}`}
              value={formData.patientId}
              onChange={(e) => setFormData({ ...formData, patientId: e.target.value })}
            >
              <option value="">Escolha um paciente...</option>
              {patients.map(p => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
            {errors.patientId && <p className="text-rose-500 text-xs font-medium mt-1 ml-1">{errors.patientId}</p>}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Data */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
                <Calendar size={16} className="text-emerald-500" /> Data *
              </label>
              <input
                type="date"
                className={`w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 ${errors.date ? 'ring-2 ring-rose-500' : ''}`}
                value={formData.date}
                onChange={(e) => setFormData({ ...formData, date: e.target.value })}
              />
            </div>

            {/* Hora */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
                <Clock size={16} className="text-emerald-500" /> Horário *
              </label>
              <input
                type="time"
                className={`w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 ${errors.time ? 'ring-2 ring-rose-500' : ''}`}
                value={formData.time}
                onChange={(e) => setFormData({ ...formData, time: e.target.value })}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Duração */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-2">Duração (minutos)</label>
              <div className="flex gap-2">
                {[30, 45, 60, 90].map(d => (
                  <button
                    key={d}
                    type="button"
                    onClick={() => setFormData({...formData, duration: d})}
                    className={`flex-1 py-3 rounded-xl font-bold transition-all ${
                      formData.duration === d 
                        ? 'bg-emerald-600 text-white shadow-md' 
                        : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                    }`}
                  >
                    {d}'
                  </button>
                ))}
              </div>
            </div>

            {/* Tipo */}
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-2">Tipo de Atendimento</label>
              <select
                className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
                value={formData.type}
                onChange={(e) => setFormData({ ...formData, type: e.target.value })}
              >
                <option value="ACUPUNTURA">Acupuntura Tradicional</option>
                <option value="FACIAL">Estética Facial</option>
                <option value="AURICULO">Auriculoterapia</option>
                <option value="OUTRO">Outro</option>
              </select>
            </div>
          </div>

          {/* Observações */}
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <FileText size={16} className="text-emerald-500" /> Observações (opcional)
            </label>
            <textarea
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 h-24"
              placeholder="Ex: Primeira consulta do paciente..."
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
            />
          </div>
        </div>

        <div className="mt-10 flex gap-4">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="flex-1 py-4 bg-gray-100 text-gray-600 rounded-2xl font-bold hover:bg-gray-200 transition-all"
          >
            Voltar
          </button>
          <button
            type="submit"
            disabled={saving}
            className="flex-[2] py-4 bg-emerald-600 text-white rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-100 flex items-center justify-center gap-2"
          >
            {saving ? "Agendando..." : <><CheckCircle2 size={20} /> Confirmar Agendamento</>}
          </button>
        </div>
      </motion.form>
    </div>
  );
}
