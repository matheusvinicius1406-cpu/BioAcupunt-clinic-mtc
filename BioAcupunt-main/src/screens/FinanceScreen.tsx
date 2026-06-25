import { useState, useEffect } from "react";
import { 
  Plus, 
  ShoppingBag, 
  Trash2, 
  Coins, 
  ArrowUpRight, 
  ArrowDownRight
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

const initialTransactions = [
  { id: "t1", description: "Sessão de Acupuntura Sistêmica - Maria de Souza", amount: 120, type: "income", date: "2026-06-21", paymentMethod: "Pix", category: "Sessão Individual" },
  { id: "t2", description: "Venda de Pacote de 10 Sessões - João Santos", amount: 900, type: "income", date: "2026-06-20", paymentMethod: "Cartão", category: "Pacotes" },
  { id: "t3", description: "Sessão de Ventosaterapia Clín. - Ana Carvalho", amount: 80, type: "income", date: "2026-06-19", paymentMethod: "Dinheiro", category: "Sessão Individual" },
  { id: "t4", description: "Compra de Agulhas Estéreis de Acupuntura", amount: 150, type: "expense", date: "2026-06-15", paymentMethod: "Pix", category: "Insumos" },
  { id: "t5", description: "Aluguel da Sala Comercial - Junho", amount: 1200, type: "expense", date: "2026-06-05", paymentMethod: "Pix", category: "Infraestrutura" }
];

const initialPackages = [
  { id: "p_1", patientName: "Maria de Souza", packageName: "Pacote Acupuntura Ouro", totalSessions: 10, usedSessions: 4, amount: 1000, date: "2026-06-10" },
  { id: "p_2", patientName: "João Santos", packageName: "Plano Relaxamento Integrado", totalSessions: 6, usedSessions: 2, amount: 540, date: "2026-06-15" }
];

export default function FinanceScreen() {
  const [transactions, setTransactions] = useState<any[]>(() => {
    const saved = localStorage.getItem("bio_transactions");
    return saved ? JSON.parse(saved) : initialTransactions;
  });

  const [packages, setPackages] = useState<any[]>(() => {
    const saved = localStorage.getItem("bio_packages");
    return saved ? JSON.parse(saved) : initialPackages;
  });

  const [showTransactionModal, setShowTransactionModal] = useState(false);
  const [showPackageModal, setShowPackageModal] = useState(false);

  const [newTransaction, setNewTransaction] = useState({
    description: "",
    amount: "",
    type: "income",
    date: new Date().toISOString().split("T")[0],
    paymentMethod: "Pix",
    category: "Sessão Individual"
  });

  const [newPackage, setNewPackage] = useState({
    patientName: "",
    packageName: "Pacote Promocional MTC",
    totalSessions: "10",
    amount: "900",
    date: new Date().toISOString().split("T")[0]
  });

  useEffect(() => {
    localStorage.setItem("bio_transactions", JSON.stringify(transactions));
  }, [transactions]);

  useEffect(() => {
    localStorage.setItem("bio_packages", JSON.stringify(packages));
  }, [packages]);

  const totalIncome = transactions
    .filter(t => t.type === "income")
    .reduce((sum, t) => sum + parseFloat(t.amount), 0);

  const totalExpense = transactions
    .filter(t => t.type === "expense")
    .reduce((sum, t) => sum + parseFloat(t.amount), 0);

  const netBalance = totalIncome - totalExpense;

  const totalPix = transactions
    .filter(t => t.type === "income" && t.paymentMethod === "Pix")
    .reduce((sum, t) => sum + parseFloat(t.amount), 0);

  const totalCard = transactions
    .filter(t => t.type === "income" && t.paymentMethod === "Cartão")
    .reduce((sum, t) => sum + parseFloat(t.amount), 0);

  const totalCash = transactions
    .filter(t => t.type === "income" && (t.paymentMethod === "Dinheiro" || t.paymentMethod === "Dinheiro / Espécie"))
    .reduce((sum, t) => sum + parseFloat(t.amount), 0);

  const totalPending = 450.00;

  const formatBRL = (val: number) => {
    return val.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
  };

  const handleAddTransaction = (e: React.FormEvent) => {
    e.preventDefault();
    const item = {
      id: `t_new_${Math.random().toString(36).substring(2, 9)}`,
      description: newTransaction.description,
      amount: parseFloat(newTransaction.amount || "0"),
      type: newTransaction.type,
      date: newTransaction.date,
      paymentMethod: newTransaction.paymentMethod,
      category: newTransaction.category
    };

    setTransactions(prev => [item, ...prev]);
    setShowTransactionModal(false);
    setNewTransaction({
      description: "",
      amount: "",
      type: "income",
      date: new Date().toISOString().split("T")[0],
      paymentMethod: "Pix",
      category: "Sessão Individual"
    });
  };

  const handleAddPackage = (e: React.FormEvent) => {
    e.preventDefault();
    const item = {
      id: `p_new_${Math.random().toString(36).substring(2, 9)}`,
      patientName: newPackage.patientName,
      packageName: newPackage.packageName,
      totalSessions: parseInt(newPackage.totalSessions, 10) || 10,
      usedSessions: 0,
      amount: parseFloat(newPackage.amount || "0"),
      date: newPackage.date
    };

    setPackages(prev => [item, ...prev]);
    setShowPackageModal(false);
    setNewPackage({
      patientName: "",
      packageName: "Pacote Promocional MTC",
      totalSessions: "10",
      amount: "900",
      date: new Date().toISOString().split("T")[0]
    });

    const relatedTx = {
      id: `t_new_pkg_${Math.random().toString(36).substring(2, 9)}`,
      description: `Venda de Pacote [${item.packageName}] - ${item.patientName}`,
      amount: item.amount,
      type: "income",
      date: item.date,
      paymentMethod: "Pix",
      category: "Pacotes"
    };
    setTransactions(prev => [relatedTx, ...prev]);
  };

  const handleUseSession = (id: string) => {
    setPackages(prev => prev.map(item => {
      if (item.id === id && item.usedSessions < item.totalSessions) {
        return { ...item, usedSessions: item.usedSessions + 1 };
      }
      return item;
    }));
  };

  const handleDeleteTransaction = (id: string) => {
    if (window.confirm("Deseja realmente remover este lançamento financeiro?")) {
      setTransactions(prev => prev.filter(t => t.id !== id));
    }
  };

  const handleDeletePackage = (id: string) => {
    if (window.confirm("Deseja realmente remover este controle de pacote?")) {
      setPackages(prev => prev.filter(p => p.id !== id));
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8">
      
      {/* 4 Custom Financial Indicator Cards with premium layout */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        {/* Metric Card 1 */}
        <div className="bg-white p-5 rounded-2xl border border-emerald-light/60 shadow-sm flex items-center gap-4 hover-lift">
          <div className="w-12 h-12 bg-[#F4F7F5] border border-emerald-light text-emerald-primary rounded-xl flex items-center justify-center font-display font-black text-lg">
            R$
          </div>
          <div>
            <p className="text-[10px] font-bold text-neutral-400 uppercase tracking-widest leading-none">Receitas do Mês</p>
            <p className="text-2xl font-display font-semibold text-emerald-dark mt-1.5">{formatBRL(totalIncome)}</p>
          </div>
        </div>

        {/* Metric Card 2 */}
        <div className="bg-white p-5 rounded-2xl border border-emerald-light/60 shadow-sm flex items-center gap-4 hover-lift">
          <div className="w-12 h-12 bg-[#FAF7F2] border border-gold-lux/25 text-[#A6884E] rounded-xl flex items-center justify-center font-display font-black text-lg">
            R$
          </div>
          <div>
            <p className="text-[10px] font-bold text-[#A6884E] uppercase tracking-widest leading-none">Contas a Receber</p>
            <p className="text-2xl font-display font-semibold text-[#A6884E] mt-1.5">{formatBRL(totalPending)}</p>
          </div>
        </div>

        {/* Metric Card 3 */}
        <div className="bg-white p-5 rounded-2xl border border-emerald-light/60 shadow-sm flex items-center gap-4 hover-lift">
          <div className="w-12 h-12 bg-emerald-light/30 border border-emerald-medium/10 text-emerald-primary rounded-xl flex items-center justify-center font-mono font-bold text-base">
            Pix
          </div>
          <div>
            <p className="text-[10px] font-bold text-emerald-primary uppercase tracking-widest leading-none">Consolidado Pix</p>
            <p className="text-2xl font-display font-semibold text-emerald-primary mt-1.5">{formatBRL(totalPix)}</p>
          </div>
        </div>

        {/* Metric Card 4 */}
        <div className="bg-white p-5 rounded-2xl border border-emerald-light/60 shadow-sm flex items-center gap-4 hover-lift">
          <div className="w-12 h-12 bg-neutral-50 border border-neutral-200/50 text-neutral-500 rounded-xl flex items-center justify-center font-mono font-bold text-sm">
            POS
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-[10.5px] font-bold text-neutral-400 uppercase tracking-widest">Outros Meios</p>
            <div className="flex justify-between text-[11px] font-mono mt-1 border-b border-neutral-105/10 pb-1">
              <span className="text-neutral-500 font-sans">Card:</span>
              <span className="font-bold text-neural-800">{formatBRL(totalCard)}</span>
            </div>
            <div className="flex justify-between text-[11px] font-mono mt-1">
              <span className="text-neutral-500 font-sans">Dinheiro:</span>
              <span className="font-bold text-neutral-800">{formatBRL(totalCash)}</span>
            </div>
          </div>
        </div>

      </div>

      {/* Main double block section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        
        {/* Left Column: Controle de Pacotes Multissessões */}
        <div className="bg-white p-6 md:p-8 rounded-2xl border border-emerald-light/60 shadow-sm flex flex-col justify-between min-h-[500px]">
          <div>
            <div className="flex justify-between items-start gap-4 mb-6 pb-4 border-b border-neutral-100">
              <div>
                <h3 className="text-lg font-display font-semibold text-emerald-dark flex items-center gap-1.5">
                  <ShoppingBag size={18} className="text-emerald-primary" />
                  Sessões de Pacotes Adquiridos
                </h3>
                <p className="text-xs text-neutral-400 mt-0.5">
                  Controle as sessões de acupuntura ou protocolos contratados parceladamente.
                </p>
              </div>
              
              <button 
                onClick={() => setShowPackageModal(true)}
                className="flex items-center gap-1.5 px-4.5 py-2.5 bg-gradient-to-r from-emerald-primary to-emerald-dark hover:opacity-95 text-white rounded-xl text-xs font-bold shadow-md shadow-emerald-primary/10 cursor-pointer hover-lift shrink-0"
              >
                <Plus size={14} />
                Vender Pacote
              </button>
            </div>

            <div className="space-y-4 overflow-y-auto max-h-[380px] pr-1">
              {packages.length === 0 ? (
                <div className="text-center py-20 text-neutral-450 text-xs">
                  Nenhum pacote registrado.
                </div>
              ) : (
                packages.map((pkg) => (
                  <div key={pkg.id} className="p-4 bg-[#FBFDFB] border border-emerald-light/40 rounded-xl space-y-3 relative group hover:border-emerald-primary/30 transition-all shadow-3xs">
                    <button 
                      onClick={() => handleDeletePackage(pkg.id)}
                      className="absolute top-4 right-4 text-neutral-300 hover:text-rose-500 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                      title="Deletar pacote"
                    >
                      <Trash2 size={13} />
                    </button>

                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="text-xs font-bold text-neural-800 uppercase tracking-wider">{pkg.packageName}</h4>
                        <p className="text-[11px] text-[#A6884E] font-bold mt-1">Paciente: {pkg.patientName}</p>
                      </div>
                      <span className="text-xs font-bold text-neutral-800">{formatBRL(pkg.amount)}</span>
                    </div>

                    <div className="py-1">
                      <div className="flex justify-between items-center text-[10px] text-neutral-400 font-bold uppercase mb-1">
                        <span>Consumo de Sessões</span>
                        <span>{pkg.usedSessions} / {pkg.totalSessions}</span>
                      </div>
                      <div className="h-2 w-full bg-neutral-100 rounded-full overflow-hidden">
                        <div 
                          className="h-full bg-emerald-primary transition-all duration-300"
                          style={{ width: `${(pkg.usedSessions / pkg.totalSessions) * 100}%` }}
                        />
                      </div>
                    </div>

                    <div className="flex justify-between items-center pt-1 flex-wrap gap-2">
                      <span className="text-[10px] text-neutral-400 font-mono">Início: {new Date(pkg.date).toLocaleDateString("pt-BR")}</span>
                      <button 
                        onClick={() => handleUseSession(pkg.id)}
                        disabled={pkg.usedSessions >= pkg.totalSessions}
                        className="px-3 py-1.5 bg-emerald-light/50 hover:bg-emerald-light disabled:bg-neutral-50 border border-emerald-medium/15 disabled:border-neutral-200 text-emerald-dark disabled:text-neutral-400 rounded-lg text-[10px] font-bold cursor-pointer disabled:pointer-events-none hover-lift"
                      >
                        {pkg.usedSessions >= pkg.totalSessions ? "Concluído" : "✓ Registrar Presença"}
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
          
          <div className="border-t border-neutral-100 pt-4 mt-6 text-center">
            <span className="text-[11px] text-[#C9A96E] font-medium">✨ Valores parcelados de planos mostrados em tempo real</span>
          </div>
        </div>
        
        {/* Right Column: Livro de Caixa / lançamentos Financeiros */}
        <div className="bg-white p-6 md:p-8 rounded-2xl border border-emerald-light/60 shadow-sm flex flex-col justify-between min-h-[500px]">
          <div>
            <div className="flex justify-between items-start gap-4 mb-6 pb-4 border-b border-neutral-100">
              <div>
                <h3 className="text-lg font-display font-semibold text-emerald-dark flex items-center gap-1.5">
                  <Coins size={18} className="text-emerald-primary" />
                  Fluxo de Caixa / Livro-Caixa Clínico
                </h3>
                <p className="text-xs text-neutral-400 mt-0.5">
                  Tabela completa de receitas, despesas e faturamento manual.
                </p>
              </div>
              
              <button 
                onClick={() => setShowTransactionModal(true)}
                className="flex items-center gap-1.5 px-4 py-2.5 bg-white border border-emerald-light text-emerald-primary rounded-xl text-xs font-bold shadow-2xs hover:bg-emerald-light/40 transition-colors cursor-pointer hover-lift shrink-0"
              >
                <Plus size={14} />
                Lançar Registro
              </button>
            </div>

            <div className="space-y-3 overflow-y-auto max-h-[380px] pr-1">
              {transactions.length === 0 ? (
                <div className="text-center py-20 text-neutral-450 text-xs">
                  Nenhum faturamento registrado.
                </div>
              ) : (
                transactions.map((tx) => {
                  const isIncome = tx.type === "income";
                  return (
                    <div key={tx.id} className="p-3.5 bg-white border border-neutral-100 hover:border-emerald-light/40 rounded-xl flex items-center justify-between gap-4 relative group transition-colors shadow-3xs">
                      <button 
                        onClick={() => handleDeleteTransaction(tx.id)}
                        className="absolute top-2 right-2 text-neutral-300 hover:text-rose-500 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer p-0.5"
                        title="Deletar lançamento"
                      >
                        <Trash2 size={12} />
                      </button>

                      <div className="flex items-center gap-3 min-w-0 flex-1">
                        <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${isIncome ? 'bg-emerald-light/40 text-emerald-primary' : 'bg-rose-50 text-rose-500'}`}>
                          {isIncome ? <ArrowUpRight size={16} /> : <ArrowDownRight size={16} />}
                        </div>
                        <div className="min-w-0 flex-1">
                          <h4 className="text-xs font-bold text-neutral-850 truncate leading-tight">{tx.description}</h4>
                          <div className="flex items-center gap-2 mt-1 flex-wrap">
                            <span className="text-[9px] text-neutral-500 font-bold bg-[#F4F7F5] p-0.5 px-1.5 rounded">{tx.paymentMethod}</span>
                            <span className="h-1 w-1 bg-neutral-200 rounded-full" />
                            <span className="text-[9.5px] text-neutral-400 font-mono">{new Date(tx.date).toLocaleDateString("pt-BR")}</span>
                          </div>
                        </div>
                      </div>

                      <div className="text-right shrink-0">
                        <p className={`text-xs font-bold ${isIncome ? 'text-emerald-primary' : 'text-neutral-500'}`}>
                          {isIncome ? '+' : '-'} {formatBRL(tx.amount)}
                        </p>
                        <span className="text-[9px] text-neutral-450 font-bold block mt-0.5">{tx.category}</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          <div className="border-t border-neutral-100 pt-4 mt-6 text-center">
            <span className="text-xs text-neutral-400 font-mono">Consolidado Líquido: <span className="font-bold text-emerald-primary">{formatBRL(netBalance)}</span></span>
          </div>
        </div>

      </div>

      {/* Modal - Nova Transação Manual */}
      <AnimatePresence>
        {showTransactionModal && (
          <div className="fixed inset-0 bg-neutral-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-2xl max-w-lg w-full p-6 md:p-8 border border-emerald-light shadow-xl"
            >
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-lg font-display font-semibold text-emerald-dark">Novo Lançamento Caixa</h3>
                <button onClick={() => setShowTransactionModal(false)} className="w-8 h-8 rounded-full bg-neutral-100 flex items-center justify-center text-neutral-400 hover:text-neutral-600 font-bold cursor-pointer">
                  ✕
                </button>
              </div>

              <form onSubmit={handleAddTransaction} className="space-y-4">
                <div>
                  <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Fluxo de Caixa</label>
                  <div className="grid grid-cols-2 gap-2">
                    <button 
                      type="button"
                      onClick={() => setNewTransaction({...newTransaction, type: "income"})}
                      className={`py-3 text-xs font-bold rounded-xl transition-all cursor-pointer ${newTransaction.type === "income" ? 'bg-emerald-primary text-white shadow-md' : 'bg-[#F4F7F5]/50 border border-neutral-200 text-neutral-500'}`}
                    >
                      Receita (+)
                    </button>
                    <button 
                      type="button"
                      onClick={() => setNewTransaction({...newTransaction, type: "expense"})}
                      className={`py-3 text-xs font-bold rounded-xl transition-all cursor-pointer ${newTransaction.type === "expense" ? 'bg-rose-600 text-white shadow-md' : 'bg-[#F4F7F5]/50 border border-neutral-200 text-neutral-500'}`}
                    >
                      Despesa (-)
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Descrição do Item</label>
                  <input 
                    type="text" 
                    required
                    placeholder="Ex: Sessão avulsa, compra de insumos..."
                    value={newTransaction.description}
                    onChange={(e) => setNewTransaction({...newTransaction, description: e.target.value})}
                    className="w-full p-3 bg-[#F4F7F5]/40 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white transition-all text-neutral-700"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Valor (R$)</label>
                    <input 
                      type="number" 
                      required
                      placeholder="0.00"
                      value={newTransaction.amount}
                      onChange={(e) => setNewTransaction({...newTransaction, amount: e.target.value})}
                      className="w-full p-3 bg-[#F4F7F5]/40 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white text-neutral-700"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Data</label>
                    <input 
                      type="date" 
                      required
                      value={newTransaction.date}
                      onChange={(e) => setNewTransaction({...newTransaction, date: e.target.value})}
                      className="w-full p-3 bg-[#F4F7F5]/40 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white text-neutral-600"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Meio de Transação</label>
                    <select 
                      value={newTransaction.paymentMethod}
                      onChange={(e) => setNewTransaction({...newTransaction, paymentMethod: e.target.value})}
                      className="w-full p-3 bg-[#F4F7F5]/50 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary text-neutral-600"
                    >
                      <option value="Pix">Pix</option>
                      <option value="Cartão">Cartão de Crédito</option>
                      <option value="Dinheiro">Dinheiro / Espécie</option>
                      <option value="Transferência">TED / DOC</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Categoria</label>
                    <select 
                      value={newTransaction.category}
                      onChange={(e) => setNewTransaction({...newTransaction, category: e.target.value})}
                      className="w-full p-3 bg-[#F4F7F5]/50 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary text-neutral-600"
                    >
                      <option value="Sessão Individual">Sessão Individual</option>
                      <option value="Pacotes">Venda de Pacotes</option>
                      <option value="Insumos">Insumos de Acupuntura</option>
                      <option value="Infraestrutura">Custos de Sala / Outros</option>
                    </select>
                  </div>
                </div>

                <div className="pt-3 flex justify-end gap-3 border-t border-neutral-100">
                  <button 
                    type="button" 
                    onClick={() => setShowTransactionModal(false)}
                    className="px-5 py-2.5 bg-neutral-50 hover:bg-neutral-100 text-neutral-500 rounded-xl text-xs font-bold border border-neutral-200 cursor-pointer"
                  >
                    Cancelar
                  </button>
                  <button 
                    type="submit"
                    className="px-6 py-2.5 bg-gradient-to-r from-emerald-primary to-emerald-dark text-white rounded-xl text-xs font-bold shadow-md shadow-emerald-primary/10 cursor-pointer hover-lift"
                  >
                    Lançar no Caixa
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Modal - Vender Pacote */}
      <AnimatePresence>
        {showPackageModal && (
          <div className="fixed inset-0 bg-neutral-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-2xl max-w-lg w-full p-6 md:p-8 border border-emerald-light shadow-xl"
            >
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-lg font-display font-semibold text-emerald-dark">Nova Sessão de Contrato (Venda de Pacote)</h3>
                <button onClick={() => setShowPackageModal(false)} className="w-8 h-8 rounded-full bg-neutral-100 flex items-center justify-center text-neutral-400 hover:text-neutral-600 font-bold cursor-pointer">
                  ✕
                </button>
              </div>

              <form onSubmit={handleAddPackage} className="space-y-4">
                <div>
                  <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Nome do Paciente</label>
                  <input 
                    type="text" 
                    required
                    placeholder="Ex: Maria de Souza..."
                    value={newPackage.patientName}
                    onChange={(e) => setNewPackage({...newPackage, patientName: e.target.value})}
                    className="w-full p-3 bg-[#F4F7F5]/40 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white text-neutral-750"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Título do Pacote</label>
                  <input 
                    type="text" 
                    required
                    value={newPackage.packageName}
                    onChange={(e) => setNewPackage({...newPackage, packageName: e.target.value})}
                    className="w-full p-3 bg-[#F4F7F5]/40 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white text-neutral-750"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Total de Consultas</label>
                    <input 
                      type="number" 
                      required
                      value={newPackage.totalSessions}
                      onChange={(e) => setNewPackage({...newPackage, totalSessions: e.target.value})}
                      className="w-full p-3 bg-[#F4F7F5]/40 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white text-neutral-700"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Preço Global Cobrado (R$)</label>
                    <input 
                      type="number" 
                      required
                      value={newPackage.amount}
                      onChange={(e) => setNewPackage({...newPackage, amount: e.target.value})}
                      className="w-full p-3 bg-[#F4F7F5]/40 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white text-neutral-700"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">Data de Registro</label>
                  <input 
                    type="date" 
                    required
                    value={newPackage.date}
                    onChange={(e) => setNewPackage({...newPackage, date: e.target.value})}
                    className="w-full p-3 bg-[#F4F7F5]/40 border border-emerald-light rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-primary focus:bg-white text-neutral-600"
                  />
                </div>

                <div className="pt-3 flex justify-end gap-3 border-t border-neutral-100">
                  <button 
                    type="button" 
                    onClick={() => setShowPackageModal(false)}
                    className="px-5 py-2.5 bg-neutral-50 hover:bg-neutral-100 text-neutral-500 rounded-xl text-xs font-bold border border-neutral-200 cursor-pointer"
                  >
                    Cancelar
                  </button>
                  <button 
                    type="submit"
                    className="px-6 py-2.5 bg-gradient-to-r from-emerald-primary to-emerald-dark text-white rounded-xl text-xs font-bold shadow-md shadow-emerald-primary/10 cursor-pointer hover-lift"
                  >
                    Confirmar Contrato
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
