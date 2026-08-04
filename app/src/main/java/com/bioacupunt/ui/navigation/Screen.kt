package com.bioacupunt.ui.navigation

sealed class Screen(val route: String, val label: String) {
    // Auth
    data object Login       : Screen("login",       "Login")
    data object BiometricLock: Screen("biometric_lock","Biometria")

    // Main navigation (bottom bar)
    data object Dashboard   : Screen("dashboard",   "Início")
    data object Agenda      : Screen("agenda",      "Agenda")
    data object CRM         : Screen("crm",         "Pacientes")
    data object Biblioteca  : Screen("biblioteca",  "Biblioteca")
    data object Ajustes     : Screen("ajustes",     "Ajustes")

    // Secondary screens (não aparecem na bottom nav)
    //
    // `appointmentId` é query param OPCIONAL (default 0L = "fora de um atendimento").
    // Quando > 0, o Prontuário entra em "modo atendimento" (timer de sessão + botão
    // "Finalizar atendimento") — ver ProntuarioScreen. Isto substitui o que antes era
    // uma tela separada (`Atendimento`, removida 2026-08-04): o wizard de 5 passos
    // editava o MESMO MtcAssessment que as abas Anamnese/Plano já editam — duas
    // superfícies para o mesmo dado. Unificado numa tela só, por pedido explícito.
    data object Prontuario  : Screen("prontuario/{patientId}?appointmentId={appointmentId}", "Prontuário") {
        // `route` above is the *pattern* NavHost registers a destination for.
        // Navigating there needs a concrete path with the placeholder filled
        // in — string-concatenating "$route/$id" instead (as this call site
        // used to) produces "prontuario/{patientId}/123", which matches no
        // registered destination and throws at navigate() time.
        fun routeFor(patientId: Long, appointmentId: Long = 0L): String {
            val base = "prontuario/$patientId"
            return if (appointmentId > 0L) "$base?appointmentId=$appointmentId" else base
        }
    }
    data object Flashcards  : Screen("flashcards",  "Flashcards")
    data object Simulador   : Screen("simulador",   "Simulador")
    data object AiAssistant : Screen("ai_assistant","Inteligência")
    // Entrada patient-aware: alcançada a partir do Prontuário ("Perguntar à IA
    // sobre este caso"), não da bottom nav — AiAssistant acima continua sendo
    // a rota global/sem paciente, intocada.
    data object AiAssistantPatient : Screen("ai_assistant/{patientId}", "Inteligência") {
        fun routeFor(patientId: Long) = "ai_assistant/$patientId"
    }
    data object Relatorios  : Screen("relatorios",  "Relatórios")
    data object Financeiro  : Screen("financeiro",  "Financeiro")
    data object Conflitos   : Screen("conflitos",   "Conflitos")
    data object Curadoria   : Screen("curadoria",   "Curadoria")
    data object PipelineMonitor: Screen("pipeline",    "Pipeline")
    data object Farmacologia: Screen("farmacologia", "Farmacologia")
    data object FarmacologiaCuradoria: Screen("farmacologia_curadoria", "Curadoria Farmacológica")

    companion object {
        // Bottom navigation items — mockup order: Início, Pacientes, Prontuário,
        // Biblioteca, Mais (the "Mais" entry is synthesized by the nav shell).
        val bottomNavItems: List<Screen> = listOf(
            Dashboard, CRM, Prontuario, Biblioteca
        )
    }
}
