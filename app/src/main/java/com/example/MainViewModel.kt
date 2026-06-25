package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.navigation.Screen
import com.example.services.GeminiChatService
import com.example.data.MockData
import com.example.data.local.AppDatabase
import com.example.data.local.PatientEntity
import com.example.data.local.AppointmentEntity
import com.example.data.local.FinanceEntity
import com.example.data.local.MtcProntuaryEntity
import com.example.data.local.ClinicaConfigEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val text: String)

class MainViewModel : ViewModel() {

    private var database: AppDatabase? = null
    private var intelligenceDatabase: com.example.data.intelligence.IntelligenceDatabase? = null

    private val _patients = MutableStateFlow<List<PatientEntity>>(emptyList())
    val patients: StateFlow<List<PatientEntity>> = _patients

    private val _appointments = MutableStateFlow<List<AppointmentEntity>>(emptyList())
    val appointments: StateFlow<List<AppointmentEntity>> = _appointments

    private val _finances = MutableStateFlow<List<FinanceEntity>>(emptyList())
    val finances: StateFlow<List<FinanceEntity>> = _finances

    private val _selectedPatientId = MutableStateFlow<String>("")
    val selectedPatientId: StateFlow<String> = _selectedPatientId

    private val _activeProntuary = MutableStateFlow<MtcProntuaryEntity?>(null)
    val activeProntuary: StateFlow<MtcProntuaryEntity?> = _activeProntuary

    private val _clinicaConfig = MutableStateFlow<ClinicaConfigEntity>(ClinicaConfigEntity())
    val clinicaConfig: StateFlow<ClinicaConfigEntity> = _clinicaConfig

    fun initDatabase(context: Context) {
        if (database != null) return
        val db = AppDatabase.getDatabase(context)
        database = db
        
        intelligenceDatabase = androidx.room.Room.databaseBuilder(
            context,
            com.example.data.intelligence.IntelligenceDatabase::class.java,
            "intelligence_database"
        ).build()

        viewModelScope.launch {
            db.patientDao().getAllPatients().collect { list ->
                if (list.isEmpty()) {
                    seedPatients(db)
                } else {
                    _patients.value = list
                }
            }
        }

        viewModelScope.launch {
            db.appointmentDao().getAllAppointments().collect { list ->
                if (list.isEmpty()) {
                    seedAppointments(db)
                } else {
                    _appointments.value = list
                }
            }
        }

        viewModelScope.launch {
            db.financeDao().getAllFinances().collect { list ->
                if (list.isEmpty()) {
                    seedFinances(db)
                } else {
                    _finances.value = list
                }
            }
        }

        viewModelScope.launch {
            db.clinicaConfigDao().getConfig().collect { config ->
                if (config != null) {
                    _clinicaConfig.value = config
                } else {
                    val defaultConfig = ClinicaConfigEntity()
                    db.clinicaConfigDao().insertConfig(defaultConfig)
                    _clinicaConfig.value = defaultConfig
                }
            }
        }
    }

    fun getIntelligenceDao() = intelligenceDatabase?.intelligenceDao()

    private suspend fun seedPatients(db: AppDatabase) {
        val initial = listOf(
            PatientEntity("p1", "Maria Souza Silva", "Feminino", "Arquiteta", "(11) 98765-4321", "maria.souza@email.com", "UNDER_TREATMENT", 150.0, System.currentTimeMillis()),
            PatientEntity("p2", "João Alencar Ribeiro", "Masculino", "Engenheiro Civil", "(21) 99123-4567", "joao.alencar@email.com", "ACTIVE_EVALUATION", 0.0, System.currentTimeMillis()),
            PatientEntity("p3", "Ana Beatriz Ramos", "Feminino", "Designer", "(11) 97765-1122", "ana.beatriz@email.com", "STABLE", 300.0, System.currentTimeMillis())
        )
        initial.forEach { db.patientDao().insertPatient(it) }
    }

    private suspend fun seedAppointments(db: AppDatabase) {
        val initial = listOf(
            AppointmentEntity("a1", "p1", System.currentTimeMillis() + 1000 * 60 * 60 * 2, 50, "scheduled", "Acupuntura Sistêmica", "Foco nos pontos IG4, F3, VB20 e Yintang para controle de cefaleia."),
            AppointmentEntity("a2", "p2", System.currentTimeMillis() + 1000 * 60 * 60 * 4, 50, "scheduled", "Acupuntura + Moxabustão", "Aplicação de moxa no canal de bexiga (B23, B40) e R3."),
            AppointmentEntity("a3", "p3", System.currentTimeMillis() - 1000 * 60 * 60 * 24, 50, "completed", "Acupuntura Sistêmica", "Sessão tranquila. Paciente refere melhora na qualidade de sono.")
        )
        initial.forEach { db.appointmentDao().insertAppointment(it) }
    }

    private suspend fun seedFinances(db: AppDatabase) {
        val initial = listOf(
            FinanceEntity("f1", "receita", "Consulta Maria Souza Silva", 150.0, System.currentTimeMillis(), "Sessão Individual", "pix"),
            FinanceEntity("f2", "receita", "Pacote 10 Sessões - Ana Ramos", 1200.0, System.currentTimeMillis() - 1000 * 60 * 60 * 12, "Pacote Tratamento", "cartao_credito"),
            FinanceEntity("f3", "despesa", "Agulhas DongBang 0.25x30mm", 85.0, System.currentTimeMillis() - 1000 * 60 * 60 * 24, "Materiais Clínicos", "pix")
        )
        initial.forEach { db.financeDao().insertFinance(it) }
    }

    fun addPatient(patient: PatientEntity) {
        viewModelScope.launch {
            database?.patientDao()?.insertPatient(patient)
        }
    }

    fun addAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch {
            database?.appointmentDao()?.insertAppointment(appointment)
        }
    }

    fun addFinance(finance: FinanceEntity) {
        viewModelScope.launch {
            database?.financeDao()?.insertFinance(finance)
        }
    }

    fun deletePatient(id: String) {
        viewModelScope.launch {
            database?.patientDao()?.deletePatient(id)
        }
    }

    fun deleteAppointment(id: String) {
        viewModelScope.launch {
            database?.appointmentDao()?.deleteAppointment(id)
        }
    }

    fun deleteFinance(id: String) {
        viewModelScope.launch {
            database?.financeDao()?.deleteFinance(id)
        }
    }

    fun selectPatient(patientId: String) {
        _selectedPatientId.value = patientId
        viewModelScope.launch {
            database?.mtcProntuaryDao()?.getProntuaryByPatient(patientId)?.collect { prontuary ->
                _activeProntuary.value = prontuary ?: MtcProntuaryEntity(patientId)
            }
        }
    }

    fun saveProntuary(prontuary: MtcProntuaryEntity) {
        viewModelScope.launch {
            database?.mtcProntuaryDao()?.insertProntuary(prontuary)
            _activeProntuary.value = prontuary
        }
    }

    fun saveClinicaConfig(config: ClinicaConfigEntity) {
        viewModelScope.launch {
            database?.clinicaConfigDao()?.insertConfig(config)
            _clinicaConfig.value = config
        }
    }

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    private val _geminiResponse = MutableStateFlow("")
    val geminiResponse: StateFlow<String> = _geminiResponse

    // Floating AI Chat State
    private val _isIaChatOpen = MutableStateFlow(false)
    val isIaChatOpen: StateFlow<Boolean> = _isIaChatOpen

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("model", "Olá! Sou o assistente de IA BioAcupunt treinado com protocolos suíços. Como posso ajudar com seus atendimentos e diagnósticos de MTC hoje?")
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _isIaLoading = MutableStateFlow(false)
    val isIaLoading: StateFlow<Boolean> = _isIaLoading

    fun toggleIaChat() {
        _isIaChatOpen.value = !_isIaChatOpen.value
    }

    fun setIaChatOpen(open: Boolean) {
        _isIaChatOpen.value = open
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("model", "Olá! Sou o assistente de IA BioAcupunt treinado com protocolos suíços. Como posso ajudar com seus atendimentos e diagnósticos de MTC hoje?")
        )
    }

    fun consultarGemini(prompt: String) {
        viewModelScope.launch {
            _geminiResponse.value = "Carregando..."
            try {
                // RAG context extraction
                val context = extractRagContext(prompt)
                val finalPrompt = if (context.isNotEmpty()) {
                    "Contexto clínico local:\n$context\n\nPergunta do usuário: $prompt"
                } else {
                    prompt
                }
                
                val response = GeminiChatService.chat(finalPrompt, emptyList())
                if (response.startsWith("Erro:")) {
                    // Fallback local response
                    _geminiResponse.value = getLocalFallbackResponse(prompt)
                } else {
                    _geminiResponse.value = response
                }
            } catch (e: Exception) {
                _geminiResponse.value = getLocalFallbackResponse(prompt)
            }
        }
    }

    fun sendIaChatMessage(text: String) {
        if (text.isBlank()) return
        
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(ChatMessage("user", text))
        _chatMessages.value = currentList
        
        _isIaLoading.value = true
        
        viewModelScope.launch {
            try {
                // Extract local context for RAG
                val context = extractRagContext(text)
                val finalPrompt = if (context.isNotEmpty()) {
                    "Use o seguinte contexto clínico clássico para embasar sua resposta:\n$context\n\nPergunta do usuário: $text"
                } else {
                    text
                }
                
                val history = currentList.map { Pair(it.role, it.text) }
                
                val response = GeminiChatService.chat(finalPrompt, history)
                
                val updatedList = _chatMessages.value.toMutableList()
                if (response.startsWith("Erro:")) {
                    // Fallback offline
                    updatedList.add(ChatMessage("model", getLocalFallbackResponse(text)))
                } else {
                    updatedList.add(ChatMessage("model", response))
                }
                _chatMessages.value = updatedList
            } catch (e: Exception) {
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(ChatMessage("model", getLocalFallbackResponse(text)))
                _chatMessages.value = updatedList
            } finally {
                _isIaLoading.value = false
            }
        }
    }

    private fun extractRagContext(text: String): String {
        val query = text.lowercase()
        val foundItems = mutableListOf<String>()
        
        // Search in Knowledge
        for (item in MockData.knowledge) {
            if (item.title.lowercase().contains(query) || 
                item.content.lowercase().contains(query) || 
                item.tags.any { it.lowercase().contains(query) }) {
                foundItems.add("[Conhecimento: ${item.title}] ${item.content}")
            }
        }
        
        // Search in Synergies
        for (syn in MockData.synergies) {
            if (syn.title.lowercase().contains(query) || 
                syn.description.lowercase().contains(query) || 
                syn.rationale.lowercase().contains(query)) {
                foundItems.add("[Sinergia: ${syn.title}] Descrição: ${syn.description}. Raciocínio MTC: ${syn.rationale}. Pontos: ${syn.mainPoints.joinToString()}")
            }
        }
        
        return foundItems.joinToString("\n\n")
    }

    private fun getLocalFallbackResponse(text: String): String {
        val query = text.lowercase()
        
        // Match specific high-yield clinical entities
        if (query.contains("e36") || query.contains("zusanli")) {
            return "📌 [Resposta Local BioAcupunt]\nO ponto **E36 (Zusanli)** é o ponto mais importante para tonificar o Qi e o Sangue, localizado 3 cun abaixo do bordo inferior da patela. Ele aumenta significativamente a imunidade celular, fortalece o Baço e o Estômago, e regula os canais digestivos clássicos. Altamente recomendado para fadiga crônica e imunodeficiência."
        }
        if (query.contains("fígado") || query.contains("estagnação") || query.contains("qi")) {
            return "📌 [Resposta Local BioAcupunt]\nA **Estagnação do Qi do Fígado** é a síndrome mais comum decorrente de estresse emocional. Clinicamente, manifesta-se por distensão abdominal, irritabilidade, pulso em corda e língua com bordos vermelhos. Tratamento clássico: Dispersar o Fígado e mover o Qi com a combinação clássica **F3 (Taichong)** + **CS6 (Neiguan)** + **VB34 (Yanglingquan)**."
        }
        if (query.contains("ansiedade") || query.contains("insônia") || query.contains("sono")) {
            return "📌 [Resposta Local BioAcupunt]\nPara **Ansiedade e Insônia**, os protocolos clássicos de acalmamento mental focam em sedar o espírito (Shen). Pontos recomendados:\n- **Yintang**: Entre as sobrancelhas (induz calma imediata).\n- **C7 (Shenmen)**: No punho, acalma o Coração.\n- **VG20 (Baihui)**: No topo da cabeça, pacifica a mente."
        }
        
        return "📌 [Resposta Local BioAcupunt - Modo Offline]\nDesculpe, a conexão com a nuvem do Gemini falhou ou a chave de API não está configurada no seu painel de Segredos. Com base na base clássica do BioAcupunt:\n\nSugerimos realizar anamnese integrativa focada no exame da Língua (cor do corpo, saburra) e Pulso (frequência, profundidade, tensão), associada aos pontos sistêmicos reguladores gerais de MTC como E36, IG4 e F3."
    }
}
