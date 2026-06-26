import axios from 'axios';

const apiInstance = axios.create({
  baseURL: '/api',
});

export const api = {
  // Pacientes
  getPatients: (params?: any) => apiInstance.get('/patients', { params }).then(res => res.data),
  getPatient: (id: string) => apiInstance.get(`/patients/${id}`).then(res => res.data),
  createPatient: (data: any) => apiInstance.post('/patients', data).then(res => res.data),
  updatePatient: (id: string, data: any) => apiInstance.put(`/patients/${id}`, data).then(res => res.data),
  deletePatient: (id: string) => apiInstance.delete(`/patients/${id}`).then(res => res.data),
  getAnamnese: (patientId: string) => apiInstance.get(`/clinical/history/${patientId}`).then(res => res.data),
  updateAnamnese: (patientId: string, data: any) => apiInstance.post(`/clinical/save/${patientId}`, data).then(res => res.data),

  // Agenda
  getAppointments: (params?: any) => apiInstance.get('/appointments', { params }).then(res => res.data),
  createAppointment: (data: any) => apiInstance.post('/appointments', data).then(res => res.data),
  updateAppointment: (id: string, data: any) => apiInstance.put(`/appointments/${id}`, data).then(res => res.data),
  deleteAppointment: (id: string) => apiInstance.delete(`/appointments/${id}`).then(res => res.data),

  // Chat / IA
  chat: (prompt: string, patientId?: string, history?: any[], context?: string) => 
    apiInstance.post('/ai/chat', { message: prompt, patientId, history, context }).then(res => res.data),
  diagnose: (anamnesisId: string) => apiInstance.post(`/clinical/analyze`, { anamnesisId }).then(res => res.data),

  // Financeiro
  getFinanceSummary: (params?: any) => apiInstance.get('/finance/summary', { params }).then(res => res.data),
  getFinanceTransactions: (params?: any) => apiInstance.get('/finance', { params }).then(res => res.data),
  createTransaction: (data: any) => apiInstance.post('/finance', data).then(res => res.data),
  getPackages: () => apiInstance.get('/packages').then(res => res.data),
  createPackage: (data: any) => apiInstance.post('/packages', data).then(res => res.data),
  sellPackage: (packageId: string, patientId: string) => apiInstance.post(`/packages/sell`, { packageId, patientId }).then(res => res.data),
  useSession: (sessionId: string) => apiInstance.post(`/packages/session/${sessionId}/use`).then(res => res.data),

  // Conhecimento
  getKnowledge: (params?: any) => apiInstance.get('/knowledge', { params }).then(res => res.data),
  getKnowledgeById: (id: string) => apiInstance.get(`/knowledge/${id}`).then(res => res.data),
  searchKnowledge: (query: string) => apiInstance.get('/knowledge/search', { params: { q: query } }).then(res => res.data),
  getCategories: () => apiInstance.get('/knowledge/categories').then(res => res.data),
  getThemeDetails: (themeId: string) => apiInstance.get(`/knowledge/theme/${themeId}`).then(res => res.data),
  generateSimulationCase: (difficulty: string, theme?: string) => apiInstance.post('/knowledge/simulator/generate', { difficulty, theme }).then(res => res.data),
  evaluateSimulationCase: (caseData: any, userAnswers: any) => apiInstance.post('/knowledge/simulator/evaluate', { caseData, userAnswers }).then(res => res.data),
  iaEducatorConvert: (rawText: string, format: string) => apiInstance.post('/knowledge/ia-educator', { rawText, format }).then(res => res.data),
  getMenteClinicaInsights: () => apiInstance.get('/knowledge/mente-clinica/insights').then(res => res.data),
  getCdssSchema: () => apiInstance.get('/knowledge/cdss/schema').then(res => res.data),
  evaluateCdss: (selectedSymptoms: string[]) => apiInstance.post('/knowledge/cdss/evaluate', { selectedSymptoms }).then(res => res.data),
  
  // PDF Document Management
  getPdfList: () => apiInstance.get('/knowledge/pdf/list').then(res => res.data),
  uploadPdf: (data: { title: string; fileName: string; base64Data: string; category?: string }) => 
    apiInstance.post('/knowledge/pdf/upload', data).then(res => res.data),
  getPdfLogs: () => apiInstance.get('/knowledge/pdf/logs').then(res => res.data),

  // Sinergia
  getSynergy: (params?: any) => apiInstance.get('/protocols/synergies', { params }).then(res => res.data),
  getSynergyById: (id: string) => apiInstance.get(`/protocols/synergies/${id}`).then(res => res.data),
};
