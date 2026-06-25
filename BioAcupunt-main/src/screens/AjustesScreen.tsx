import { useState, useEffect } from "react";
import { 
  Save, 
  ShieldCheck, 
  Upload, 
  FileText, 
  ToggleLeft, 
  ToggleRight, 
  Cloud, 
  Check, 
  Layers, 
  Instagram, 
  Phone, 
  MapPin, 
  CheckCircle2, 
  LogOut 
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

export default function AjustesScreen() {
  // Reactive form state powered by localStorage persistence
  const [profName, setProfName] = useState(() => localStorage.getItem("bio_profName") || "Dra. Camila Silva");
  const [crbmNum, setCrbmNum] = useState(() => localStorage.getItem("bio_crbmNum") || "SP-12345");
  const [specialties, setSpecialties] = useState(() => localStorage.getItem("bio_specialties") || "Terapias Integradas • CRM / CRF / CRBM Acupuntura");
  
  const [phone, setPhone] = useState(() => localStorage.getItem("bio_phone") || "(11) 98765-4321");
  const [instagram, setInstagram] = useState(() => localStorage.getItem("bio_instagram") || "@dracamilasilva.mtc");
  const [address, setAddress] = useState(() => localStorage.getItem("bio_address") || "Av. Paulista, 1000 - SP");

  // Toggles state
  const [auriculoterapiaEnabled, setAuriculoterapiaEnabled] = useState(() => localStorage.getItem("bio_toggle_auriculo") !== "false");
  const [ventosaterapiaEnabled, setVentosaterapiaEnabled] = useState(() => localStorage.getItem("bio_toggle_ventosa") !== "false");
  const [moxabustaoEnabled, setMoxabustaoEnabled] = useState(() => localStorage.getItem("bio_toggle_moxa") !== "false");

  // Prices state
  const [priceAcu, setPriceAcu] = useState(() => localStorage.getItem("bio_price_acu") || "120");
  const [priceVent, setPriceVent] = useState(() => localStorage.getItem("bio_price_vent") || "80");
  const [priceAuri, setPriceAuri] = useState(() => localStorage.getItem("bio_price_auri") || "70");
  const [priceTuina, setPriceTuina] = useState(() => localStorage.getItem("bio_price_tuina") || "90");

  // PDF Terms & footer
  const [terms, setTerms] = useState(() => localStorage.getItem("bio_terms") || "Pelo presente termo de consentimento, autorizo a aplicação das agulhas de acupuntura sistêmica e auriculoterapia. Fui informado que o procedimento é seguro e livre de colaterais nocivos.");
  const [reportFooter, setReportFooter] = useState(() => localStorage.getItem("bio_reportFooter") || "© 2026 BIOACUPUNT • TECNOLOGIA & INTEGRAÇÃO MTC S/A");

  // Logo upload simulation
  const [uploadedLogo, setUploadedLogo] = useState<string | null>(() => localStorage.getItem("bio_logo") || null);

  // Cloud Integration status
  const [googleConnected, setGoogleConnected] = useState(() => localStorage.getItem("bio_gdrive_connected") === "true");

  // Layout save feedback status
  const [isSaved, setIsSaved] = useState(false);

  // Auto save routine & explicit saver
  const handleSaveAll = () => {
    localStorage.setItem("bio_profName", profName);
    localStorage.setItem("bio_crbmNum", crbmNum);
    localStorage.setItem("bio_specialties", specialties);
    localStorage.setItem("bio_phone", phone);
    localStorage.setItem("bio_instagram", instagram);
    localStorage.setItem("bio_address", address);
    localStorage.setItem("bio_toggle_auriculo", String(auriculoterapiaEnabled));
    localStorage.setItem("bio_toggle_ventosa", String(ventosaterapiaEnabled));
    localStorage.setItem("bio_toggle_moxa", String(moxabustaoEnabled));
    localStorage.setItem("bio_price_acu", priceAcu);
    localStorage.setItem("bio_price_vent", priceVent);
    localStorage.setItem("bio_price_auri", priceAuri);
    localStorage.setItem("bio_price_tuina", priceTuina);
    localStorage.setItem("bio_terms", terms);
    localStorage.setItem("bio_reportFooter", reportFooter);
    if (uploadedLogo) localStorage.setItem("bio_logo", uploadedLogo);
    localStorage.setItem("bio_gdrive_connected", String(googleConnected));

    // Emit event for layout sync
    window.dispatchEvent(new Event("storage"));

    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 2000);
  };

  const setFooterPreset = (preset: "curto" | "clinica") => {
    if (preset === "curto") {
      setReportFooter("© 2026 BIOACUPUNT • TECNOLOGIA & INTEGRAÇÃO MTC S/A");
    } else {
      setReportFooter(`© 2026 BIOACUPUNT • ${profName} • MTC REG:${crbmNum} • Clínica de Terapias Integradas`);
    }
  };

  const handleLogoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        if (typeof reader.result === "string") {
          setUploadedLogo(reader.result);
          localStorage.setItem("bio_logo", reader.result);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start max-w-7xl mx-auto space-y-6 lg:space-y-0">
      
      {/* Left Column (Main settings) */}
      <div className="lg:col-span-8 space-y-6">
        
        {/* Upper Preferences Control panel block */}
        <div className="bg-white p-6 rounded-3xl border border-[#E3ECE0] shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-[#F4F9F2] rounded-full -mr-10 -mt-10 opacity-60 pointer-events-none" />
          <div className="space-y-1.5 flex-1 z-10">
            <h2 className="text-2xl font-bold text-gray-900">
              Preferências Clínicas & Identidade Profissional
            </h2>
            <p className="text-gray-500 text-sm leading-relaxed max-w-xl">
              Configure as informações do conselho regional CRBM, gerencie a tabela de preços dos 4 serviços, de sua assinatura ou preencha os canais de contato da clínica.
            </p>
          </div>
          
          <div className="flex flex-col sm:flex-row md:flex-col items-stretch md:items-end gap-3 z-10">
            <button 
              onClick={handleSaveAll}
              className="flex items-center justify-center gap-2 px-6 py-3 bg-[#557A46] hover:bg-[#436136] text-white rounded-xl font-bold shadow-lg shadow-[#557A46]/10 transition-colors"
            >
              {isSaved ? <Check size={18} /> : <Save size={18} />}
              <span>{isSaved ? "SALVO COM SUCESSO!" : "SALVAR TODAS AS ALTERAÇÕES"}</span>
            </button>

            <div className="bg-[#EEF5EC] border border-[#CBDCC7] rounded-xl p-2 px-3 flex items-center gap-2">
              <ShieldCheck className="text-[#557A46]" size={16} />
              <div>
                <p className="text-[10px] font-black text-[#557A46] uppercase leading-none">Segurança da Clínica</p>
                <p className="text-xs font-bold text-gray-700 leading-none mt-1">Painel Privado • Drª Camila</p>
              </div>
            </div>
          </div>
        </div>

        {/* Bloco I: Dados do Diploma e Conselho Regional (CRBM) */}
        <div className="bg-white p-6 md:p-8 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-4">
            <h3 className="text-lg font-bold text-gray-900">Dados do Diploma e Conselho Regional (CRBM)</h3>
            <p className="text-xs text-gray-400 mt-0.5">Essas informações jurídicas são impressas ao topo de todos os relatórios gerados.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-12 gap-5">
            <div className="md:col-span-8">
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Nome da Profissional</label>
              <input 
                type="text" 
                value={profName}
                onChange={(e) => setProfName(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                placeholder="Dra. Camila Silva"
              />
            </div>
            <div className="md:col-span-4">
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Número do CRBM</label>
              <input 
                type="text" 
                value={crbmNum}
                onChange={(e) => setCrbmNum(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                placeholder="Ex: SP-12345"
              />
            </div>
            <div className="md:col-span-12">
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Especialidades ou Subeixos</label>
              <input 
                type="text" 
                value={specialties}
                onChange={(e) => setSpecialties(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                placeholder="Ex: Terapias Integradas • CRM / CRF / CRBM Acupuntura"
              />
            </div>
          </div>
        </div>

        {/* Bloco II: Canais de Contato e Endereço */}
        <div className="bg-white p-6 md:p-8 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-4">
            <h3 className="text-lg font-bold text-gray-900">Canais de Contato e Endereço da Clínica</h3>
            <p className="text-xs text-gray-400 mt-0.5">Dados exibidos no cabeçalho e rodapé dos impressos para facilitação do paciente.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1 flex items-center gap-1.5">
                <Phone size={12} className="text-[#557A46]" /> Telefone / WhatsApp
              </label>
              <input 
                type="text" 
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                placeholder="Ex: (11) 98765-4321"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1 flex items-center gap-1.5">
                <Instagram size={12} className="text-[#557A46]" /> Instagram (@)
              </label>
              <input 
                type="text" 
                value={instagram}
                onChange={(e) => setInstagram(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:focus:ring-2 focus:ring-[#557A46]"
                placeholder="Ex: @dracamilasilva.mtc"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1 flex items-center gap-1.5">
                <MapPin size={12} className="text-[#557A46]" /> Endereço de Atendimento
              </label>
              <input 
                type="text" 
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#557A46]"
                placeholder="Ex: Av. Paulista, 1000 - SP"
              />
            </div>
          </div>
        </div>

        {/* Bloco III: Habilitar/Desabilitar Técnicas Complementares */}
        <div className="bg-white p-6 md:p-8 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-4">
            <h3 className="text-lg font-bold text-gray-900">Habilitar/ Desabilitar Técnicas Complementares</h3>
            <p className="text-xs text-gray-400 mt-0.5">Se desativadas, o laudo diagnóstico do motor de IA mitigarão as metodologias do painel clínico.</p>
          </div>

          <div className="space-y-4">
            {/* Technique 1 */}
            <div className="flex items-center justify-between gap-6 p-4 bg-[#F9FBFA] border border-gray-100 rounded-2xl">
              <div>
                <h4 className="text-sm font-bold text-gray-800 uppercase tracking-tight">Auriculoterapia de Suporte</h4>
                <p className="text-xs text-gray-400 mt-1 max-w-xl">
                  Estimulação do microssistema do pavilhão auricular por sementes de vacária ou cristais de quartzo para equilibrar o Shen Mente.
                </p>
              </div>
              <button 
                onClick={() => setAuriculoterapiaEnabled(!auriculoterapiaEnabled)}
                className="text-[#557A46] focus:outline-none transition-colors"
              >
                {auriculoterapiaEnabled ? (
                  <ToggleRight size={44} className="text-[#557A46]" strokeWidth={1} />
                ) : (
                  <ToggleLeft size={44} className="text-gray-300" strokeWidth={1} />
                )}
              </button>
            </div>

            {/* Technique 2 */}
            <div className="flex items-center justify-between gap-6 p-4 bg-[#F9FBFA] border border-gray-100 rounded-2xl">
              <div>
                <h4 className="text-sm font-bold text-gray-800 uppercase tracking-tight">Ventosaterapia Clínica</h4>
                <p className="text-xs text-gray-400 mt-1 max-w-xl">
                  Aplicação de copos de sucção a vácuo para alívio de obstruções Bi de dor e estagnação muscular lombar/cervical.
                </p>
              </div>
              <button 
                onClick={() => setVentosaterapiaEnabled(!ventosaterapiaEnabled)}
                className="text-[#557A46] focus:outline-none transition-colors"
              >
                {ventosaterapiaEnabled ? (
                  <ToggleRight size={44} className="text-[#557A46]" strokeWidth={1} />
                ) : (
                  <ToggleLeft size={44} className="text-gray-300" strokeWidth={1} />
                )}
              </button>
            </div>

            {/* Technique 3 */}
            <div className="flex items-center justify-between gap-6 p-4 bg-[#F9FBFA] border border-gray-100 rounded-2xl">
              <div>
                <h4 className="text-sm font-bold text-gray-800 uppercase tracking-tight">Moxabustão Térmica</h4>
                <p className="text-xs text-gray-400 mt-1 max-w-xl">
                  Uso térmico do bastão de Artemísia nos pontos acupunturais para repor o fogo e dinamizar o calor Yang de pacientes frios ou debilitados.
                </p>
              </div>
              <button 
                onClick={() => setMoxabustaoEnabled(!moxabustaoEnabled)}
                className="text-[#557A46] focus:outline-none transition-colors"
              >
                {moxabustaoEnabled ? (
                  <ToggleRight size={44} className="text-[#557A46]" strokeWidth={1} />
                ) : (
                  <ToggleLeft size={44} className="text-gray-300" strokeWidth={1} />
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Bloco IV: Tabela de Serviços & Preço Base (MTC) */}
        <div className="bg-white p-6 md:p-8 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-4">
            <h3 className="text-lg font-bold text-gray-900">Tabela de Serviços & Preço Base (MTC)</h3>
            <p className="text-xs text-gray-400 mt-0.5">Defina os valores cobrados por sessão individual para cada um dos 4 procedimentos oficiais.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Sessão de ACUPUNTURA (R$)</label>
              <input 
                type="number" 
                value={priceAcu}
                onChange={(e) => setPriceAcu(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:focus:ring-2 focus:ring-[#557A46]"
                placeholder="R$ 120"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Sessão de VENTOSATERAPIA (R$)</label>
              <input 
                type="number" 
                value={priceVent}
                onChange={(e) => setPriceVent(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:focus:ring-2 focus:ring-[#557A46]"
                placeholder="R$ 80"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Sessão de AURICULOTERAPIA (R$)</label>
              <input 
                type="number" 
                value={priceAuri}
                onChange={(e) => setPriceAuri(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:focus:ring-2 focus:ring-[#557A46]"
                placeholder="R$ 70"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Sessão de TUI NA (R$)</label>
              <input 
                type="number" 
                value={priceTuina}
                onChange={(e) => setPriceTuina(e.target.value)}
                className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-xl text-sm font-bold text-gray-700 focus:focus:ring-2 focus:ring-[#557A46]"
                placeholder="R$ 90"
              />
            </div>
          </div>
        </div>

        {/* Bloco V: Integração com Nuvem e Google Drive */}
        <div className="bg-white p-6 md:p-8 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-4">
            <h3 className="text-lg font-bold text-gray-900">☁ 📁 Integração com a Nuvem e Backup</h3>
            <p className="text-xs text-gray-400 mt-0.5">Ative para salvar e consultar prontuários e exames dos pacientes na nuvem.</p>
          </div>

          <div className="bg-[#F9FBFA] p-8 rounded-3xl border border-[#E3ECE0] text-center max-w-xl mx-auto space-y-4">
            <div className="w-12 h-12 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mx-auto shadow-sm">
              <Cloud size={24} />
            </div>
            
            {googleConnected ? (
              <div className="space-y-3">
                <h4 className="text-base font-bold text-emerald-800 flex items-center justify-center gap-1.5">
                  <CheckCircle2 size={18} className="text-emerald-600 animate-bounce" /> Armazenamento Conectado
                </h4>
                <p className="text-xs text-gray-500 max-w-md mx-auto leading-relaxed">
                  Sua conta do Google Drive foi autenticada com sucesso! Sincronização em tempo real e uploads de exames médicos estão ativos.
                </p>
                <div className="pt-2">
                  <button 
                    onClick={() => setGoogleConnected(false)}
                    className="flex items-center gap-1.5 px-4 py-2 bg-rose-50 text-rose-600 hover:bg-rose-100 rounded-xl text-xs font-bold mx-auto border border-rose-200 transition-colors"
                  >
                    <LogOut size={12} /> Desconectar Google Drive
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <h4 className="text-base font-bold text-gray-800">Armazenamento Desconectado</h4>
                <p className="text-xs text-gray-500 max-w-md mx-auto leading-relaxed">
                  Você precisa autenticar o Google Drive para habilitar o upload de exames e a sincronização em tempo real dos relatórios de MTC.
                </p>
                <div className="pt-2">
                  <button 
                    onClick={() => setGoogleConnected(true)}
                    type="button"
                    className="flex items-center justify-center gap-2 px-6 py-3 bg-white border border-gray-200 text-gray-700 rounded-xl text-xs font-bold mx-auto hover:bg-gray-50 hover:border-gray-300 transition-all shadow-md"
                  >
                    {/* Google color design icon mockup */}
                    <div className="flex gap-0.5 items-center mr-1">
                      <span className="w-2.5 h-2.5 bg-red-500 rounded-full" />
                      <span className="w-2.5 h-2.5 bg-blue-500 rounded-full" />
                      <span className="w-2.5 h-2.5 bg-yellow-500 rounded-full" />
                      <span className="w-2.5 h-2.5 bg-green-500 rounded-full" />
                    </div>
                    CONECTAR GOOGLE DRIVE
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

      </div>

      {/* Right Column (Branding & Live PDF Signature Mock) */}
      <div className="lg:col-span-4 space-y-6">
        
        {/* Identidade Visual */}
        <div className="bg-white p-6 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-gray-900 flex items-center gap-1.5">
            <Layers size={18} className="text-[#557A46]" /> Identidade Visual e Credenciais
          </h3>
          <p className="text-xs text-gray-400 leading-relaxed">
            Selecione o arquivo de sua logo ou assinatura profissional (Png, Jpeg, Canva) para espelhamento nos cabeçalhos e relatórios clínico-digitais.
          </p>

          <label className="border-2 border-dashed border-[#CBD5E1] bg-[#F9FBFA] hover:bg-gray-50 rounded-2xl p-6 block text-center cursor-pointer transition-colors relative group">
            <input 
              type="file" 
              accept="image/*"
              className="hidden" 
              onChange={handleLogoUpload}
            />
            {uploadedLogo ? (
              <div className="space-y-2">
                <img src={uploadedLogo} alt="Logo" className="max-h-24 mx-auto object-contain rounded-lg" />
                <p className="text-[10px] font-bold text-emerald-600">Logo carregada com sucesso!</p>
              </div>
            ) : (
              <div className="space-y-2.5">
                <div className="w-10 h-10 bg-gray-100 text-gray-400 group-hover:text-emerald-600 group-hover:bg-emerald-50 rounded-full flex items-center justify-center mx-auto transition-colors">
                  <Upload size={18} />
                </div>
                <div>
                  <p className="text-xs font-bold text-gray-700">Carregar Logotipo ou Assinatura</p>
                  <p className="text-[10px] text-gray-400 mt-1">PNG transparente ou JPEG (Tamanho ideal: 300x120px)</p>
                </div>
              </div>
            )}
          </label>
        </div>

        {/* Termo de Consentimento */}
        <div className="bg-white p-6 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-gray-900 flex items-center gap-1.5">
            <FileText size={18} className="text-[#557A46]" /> Termo de Consentimento Livre e Esclarecido
          </h3>
          <p className="text-xs text-gray-400 leading-relaxed">
            Adicione cláusulas jurídicas personalizadas, avisos de contraindicações individuais ou orientações físicas que serão anexadas ao final da folha.
          </p>
          <textarea 
            value={terms}
            onChange={(e) => setTerms(e.target.value)}
            rows={4}
            className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-2xl text-xs text-gray-600 font-medium focus:outline-none focus:ring-2 focus:ring-[#557A46]"
            placeholder="Ex: Pelo presente termo de consentimento..."
          />
        </div>

        {/* Rodapé dos Relatórios */}
        <div className="bg-white p-6 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-gray-900 flex items-center gap-1.5">
            <FileText size={18} className="text-[#557A46]" /> Rodapé dos Relatórios
          </h3>
          <p className="text-xs text-gray-400 leading-relaxed">
            Texto oficial de rodapé impresso na base de todos os laudos clínicos e prescrições emitidas.
          </p>
          <textarea 
            value={reportFooter}
            onChange={(e) => setReportFooter(e.target.value)}
            rows={2}
            className="w-full p-3.5 bg-[#F9FBFA] border border-[#CBD5E1] rounded-2xl text-xs text-gray-600 font-medium focus:outline-none focus:ring-2 focus:ring-[#557A46]"
            placeholder="© 2026 BIOACUPUNT • TECNOLOGIA & INTEGRAÇÃO MTC S/A"
          />

          <div className="space-y-1.5">
            <span className="text-[9px] font-black tracking-wider text-gray-400 uppercase">Modelos Rápidos:</span>
            <div className="flex gap-2">
              <button 
                onClick={() => setFooterPreset("curto")}
                className="p-1.5 px-3 bg-[#EEF5EC] border border-[#CBDCC7] text-[#4E7A40] text-[10px] font-black rounded-lg transition-all hover:bg-[#e1eedc]"
              >
                Padrão Curto
              </button>
              <button 
                onClick={() => setFooterPreset("clinica")}
                className="p-1.5 px-3 bg-[#EEF5EC] border border-[#CBDCC7] text-[#4E7A40] text-[10px] font-black rounded-lg transition-all hover:bg-[#e1eedc]"
              >
                Completo Clínica
              </button>
            </div>
          </div>
        </div>

        {/* Visualização da Assinatura no PDF */}
        <div className="bg-white p-6 rounded-3xl border border-[#E3ECE0] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-gray-900 flex items-center gap-1.5">
            <CheckCircle2 size={18} className="text-[#557A46]" /> Visualização da Assinatura no PDF
          </h3>
          <p className="text-xs text-gray-400">Preview do selo profissional integrado na emissão dos laudos.</p>

          <div className="bg-[#FAFDF9] border border-[#CBDCC7] rounded-3xl p-6 text-center space-y-4 relative overflow-hidden shadow-inner">
            <div className="absolute top-0 right-0 w-20 h-20 bg-[#EFF7ED] rounded-full pointer-events-none -mr-6 -mt-6" />
            <div className="w-14 h-14 bg-[#EEF5EC] border border-[#D5E6CF] text-[#4E7A40] text-sm font-extrabold rounded-full flex items-center justify-center mx-auto shadow-sm">
              {profName ? `${profName.split(' ')[0][0] || 'C'}${profName.split(' ').slice(-1)[0][0] || 'S'}` : "CS"} • Silva
            </div>
            <div>
              <p className="text-base font-bold text-[#3F5F2F] font-display leading-tight">{profName || "Dra. Camila Silva"}</p>
              <p className="text-[10px] text-gray-500 font-bold mt-1 leading-tight tracking-tight uppercase">{specialties}</p>
              {crbmNum ? (
                <p className="text-[10px] font-mono font-bold text-[#E28A2B] bg-[#FFFBEB] p-1 px-3 border border-[#FEF3C7] rounded-full inline-block mt-3 uppercase tracking-wider">
                  REG: {crbmNum}
                </p>
              ) : (
                <p className="text-[10px] font-bold text-rose-500 bg-rose-50 p-1 px-3 border border-rose-100 rounded-full inline-block mt-3 uppercase">
                  [PENDENTE NA ABA AJUSTES]
                </p>
              )}
            </div>
          </div>
        </div>

      </div>

    </div>
  );
}
