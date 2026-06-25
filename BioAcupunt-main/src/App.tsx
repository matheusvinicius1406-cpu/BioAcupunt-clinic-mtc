/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';

// Screens
import DashboardScreen from './screens/DashboardScreen';
import PatientsScreen from './screens/PatientsScreen';
import PatientFormScreen from './screens/PatientFormScreen';
import AvaliacaoScreen from './screens/AvaliacaoScreen';
import AgendaScreen from './screens/AgendaScreen';
import AppointmentFormScreen from './screens/AppointmentFormScreen';
import AppointmentDetailScreen from './screens/AppointmentDetailScreen';
import AnamneseScreen from './screens/AnamneseScreen';
import ChatScreen from './screens/ChatScreen';
import SinergiaScreen from './screens/SinergiaScreen';
import SinergiaDetailScreen from './screens/SinergiaDetailScreen';
import KnowledgeScreen from './screens/KnowledgeScreen';
import KnowledgeDetailScreen from './screens/KnowledgeDetailScreen';
import KnowledgeCategoryScreen from './screens/KnowledgeCategoryScreen';
import KnowledgeSearchScreen from './screens/KnowledgeSearchScreen';
import FinanceScreen from './screens/FinanceScreen';
import FinanceEntryScreen from './screens/FinanceEntryScreen';
import FinanceReportScreen from './screens/FinanceReportScreen';
import PackageManagementScreen from './screens/PackageManagementScreen';
import PackageSellScreen from './screens/PackageSellScreen';
import TreatmentScreen from './screens/TreatmentScreen';
import EvolutionScreen from './screens/EvolutionScreen';
import AjustesScreen from './screens/AjustesScreen';

export default function App() {
  return (
    <Router>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<DashboardScreen />} />
          
          {/* Pacientes */}
          <Route path="/pacientes" element={<PatientsScreen />} />
          <Route path="/pacientes/novo" element={<PatientFormScreen />} />
          <Route path="/pacientes/editar/:id" element={<PatientFormScreen />} />
          <Route path="/pacientes/avaliacao/:id" element={<AvaliacaoScreen />} />
          
          {/* Agenda */}
          <Route path="/agenda" element={<AgendaScreen />} />
          <Route path="/agenda/novo" element={<AppointmentFormScreen />} />
          <Route path="/agenda/:id" element={<AppointmentDetailScreen />} />
          
          {/* Atendimento / Anamnese */}
          <Route path="/anamnese" element={<PatientsScreen />} /> {/* Selecionar paciente primeiro */}
          <Route path="/anamnese/:patientId" element={<AnamneseScreen />} />
          <Route path="/tratamento/:patientId" element={<TreatmentScreen />} />
          
          {/* Evolução Clín. */}
          <Route path="/evolucao" element={<EvolutionScreen />} />

          {/* Inteligência */}
          <Route path="/chat" element={<ChatScreen />} />
          
          {/* Sinergia */}
          <Route path="/sinergia" element={<SinergiaScreen />} />
          <Route path="/sinergia/:id" element={<SinergiaDetailScreen />} />
          
          {/* Conhecimento */}
          <Route path="/conhecimento" element={<KnowledgeScreen />} />
          <Route path="/conhecimento/:id" element={<KnowledgeDetailScreen />} />
          <Route path="/conhecimento/categoria/:category" element={<KnowledgeCategoryScreen />} />
          <Route path="/conhecimento/busca" element={<KnowledgeSearchScreen />} />
          
          {/* Financeiro */}
          <Route path="/financeiro" element={<FinanceScreen />} />
          <Route path="/financeiro/lancamento" element={<FinanceEntryScreen />} />
          <Route path="/financeiro/relatorios" element={<FinanceReportScreen />} />
          <Route path="/financeiro/pacotes" element={<PackageManagementScreen />} />
          <Route path="/financeiro/pacotes/vender" element={<PackageSellScreen />} />
          
          {/* Ajustes */}
          <Route path="/ajustes" element={<AjustesScreen />} />
        </Route>
      </Routes>
    </Router>
  );
}

