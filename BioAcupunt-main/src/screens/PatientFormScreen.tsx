import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../services/api";
import { ArrowLeft, Save, User, Phone, Mail, Calendar, Hash, MapPin, UserCheck } from "lucide-react";
import { motion } from "motion/react";

export default function PatientFormScreen() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    phone: "",
    cpf: "",
    birthDate: "",
    gender: "",
    occupation: "",
    address: "",
    status: "ACTIVE"
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (id) {
      setLoading(true);
      api.getPatient(id).then(data => {
        setFormData({
          name: data.name || "",
          email: data.email || "",
          phone: data.phone || "",
          cpf: data.cpf || "",
          birthDate: data.birthDate ? data.birthDate.split('T')[0] : "",
          gender: data.gender || "",
          occupation: data.occupation || "",
          address: data.address || "",
          status: data.status || "ACTIVE"
        });
        setLoading(false);
      }).catch(() => {
        setLoading(false);
      });
    }
  }, [id]);

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.name.trim()) newErrors.name = "Nome é obrigatório";
    if (formData.cpf && !/^\d{11}$|^\d{3}\.\d{3}\.\d{3}-\d{2}$/.test(formData.cpf)) {
      newErrors.cpf = "CPF inválido";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setSaving(true);
    try {
      if (id) {
        await api.updatePatient(id, formData);
      } else {
        const newPatient = await api.createPatient(formData);
        navigate(`/pacientes/avaliacao/${newPatient.id}`);
        return;
      }
      navigate("/pacientes");
    } catch (error) {
      console.error("Save patient error:", error);
      alert("Erro ao salvar paciente.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="p-10 text-center animate-pulse">Carregando dados...</div>;

  return (
    <div className="max-w-4xl mx-auto pb-12">
      <header className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate(-1)} className="p-2 hover:bg-emerald-50 rounded-full text-emerald-700 transition-colors">
          <ArrowLeft size={24} />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{id ? "Editar Paciente" : "Novo Paciente"}</h1>
          <p className="text-gray-500">Preencha as informações básicas do prontuário</p>
        </div>
      </header>

      <motion.form 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        onSubmit={handleSubmit} 
        className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100"
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Nome */}
          <div className="md:col-span-2">
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <User size={16} className="text-emerald-500" /> Nome Completo *
            </label>
            <input
              type="text"
              className={`w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 transition-all ${errors.name ? 'ring-2 ring-rose-500' : ''}`}
              placeholder="Ex: João da Silva"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
            {errors.name && <p className="text-rose-500 text-xs font-medium mt-1 ml-1">{errors.name}</p>}
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <Mail size={16} className="text-emerald-500" /> Email
            </label>
            <input
              type="email"
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              placeholder="email@exemplo.com"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            />
          </div>

          {/* Telefone */}
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <Phone size={16} className="text-emerald-500" /> Telefone / WhatsApp
            </label>
            <input
              type="tel"
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              placeholder="(00) 00000-0000"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
            />
          </div>

          {/* CPF */}
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <Hash size={16} className="text-emerald-500" /> CPF
            </label>
            <input
              type="text"
              className={`w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 ${errors.cpf ? 'ring-2 ring-rose-500' : ''}`}
              placeholder="000.000.000-00"
              value={formData.cpf}
              onChange={(e) => setFormData({ ...formData, cpf: e.target.value })}
            />
            {errors.cpf && <p className="text-rose-500 text-xs font-medium mt-1 ml-1">{errors.cpf}</p>}
          </div>

          {/* Data Nascimento */}
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <Calendar size={16} className="text-emerald-500" /> Data de Nascimento
            </label>
            <input
              type="date"
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              value={formData.birthDate}
              onChange={(e) => setFormData({ ...formData, birthDate: e.target.value })}
            />
          </div>

          {/* Gênero */}
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2">Gênero</label>
            <select
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              value={formData.gender}
              onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
            >
              <option value="">Selecione...</option>
              <option value="MASCULINO">Masculino</option>
              <option value="FEMININO">Feminino</option>
              <option value="OUTRO">Outro</option>
            </select>
          </div>

          {/* Profissão */}
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2">Profissão</label>
            <input
              type="text"
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              placeholder="Ex: Engenheiro"
              value={formData.occupation}
              onChange={(e) => setFormData({ ...formData, occupation: e.target.value })}
            />
          </div>

          {/* Endereço */}
          <div className="md:col-span-2">
            <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
              <MapPin size={16} className="text-emerald-500" /> Endereço
            </label>
            <input
              type="text"
              className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
              placeholder="Rua, número, bairro..."
              value={formData.address}
              onChange={(e) => setFormData({ ...formData, address: e.target.value })}
            />
          </div>

          {/* Status */}
          {id && (
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center gap-2">
                <UserCheck size={16} className="text-emerald-500" /> Status
              </label>
              <select
                className="w-full p-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500"
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
              >
                <option value="ACTIVE">Ativo</option>
                <option value="INACTIVE">Inativo</option>
              </select>
            </div>
          )}
        </div>

        <div className="mt-10 flex gap-4">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="flex-1 py-4 bg-gray-100 text-gray-600 rounded-2xl font-bold hover:bg-gray-200 transition-all"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={saving}
            className="flex-[2] py-4 bg-emerald-600 text-white rounded-2xl font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-200 flex items-center justify-center gap-2"
          >
            {saving ? "Salvando..." : <><Save size={20} /> {id ? "Salvar Alterações" : "Salvar e Continuar"}</>}
          </button>
        </div>
      </motion.form>
    </div>
  );
}
