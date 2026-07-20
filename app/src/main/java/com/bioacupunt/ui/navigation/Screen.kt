package com.bioacupunt.ui.navigation

sealed class Screen(val route: String, val label: String, val emoji: String = "") {
    // Auth
    data object Login       : Screen("login",       "Login",       "🔐")
    data object PinLock     : Screen("pin_lock",      "Bloqueio",    "🔒")

    // Main navigation (bottom bar)
    data object Dashboard   : Screen("dashboard",   "Início",      "🏠")
    data object Agenda      : Screen("agenda",      "Agenda",      "📅")
    data object CRM         : Screen("crm",         "Pacientes",   "👥")
    data object Biblioteca  : Screen("biblioteca",  "Biblioteca",  "📚")
    data object Ajustes     : Screen("ajustes",     "Ajustes",     "⚙️")

    // Secondary screens (não aparecem na bottom nav)
    data object Prontuario  : Screen("prontuario/{patientId}", "Prontuário",  "📋") {
        // `route` above is the *pattern* NavHost registers a destination for.
        // Navigating there needs a concrete path with the placeholder filled
        // in — string-concatenating "$route/$id" instead (as this call site
        // used to) produces "prontuario/{patientId}/123", which matches no
        // registered destination and throws at navigate() time.
        fun routeFor(patientId: Long) = "prontuario/$patientId"
    }
    data object Atendimento : Screen("atendimento/{appointmentId}", "Atendimento", "🩹") {
        fun routeFor(appointmentId: Long) = "atendimento/$appointmentId"
    }
    data object Evolucao : Screen("evolucao/{patientId}", "Evolução Clínica", "📈") {
        fun routeFor(patientId: Long) = "evolucao/$patientId"
    }
    data object Flashcards  : Screen("flashcards",  "Flashcards",  "🃏")
    data object Analytics   : Screen("analytics",   "Analytics",   "📊")
    data object Simulador   : Screen("simulador",   "Simulador",   "🧪")
    data object AiAssistant : Screen("ai_assistant","Assistente IA","🤖")
    data object Relatorios  : Screen("relatorios",  "Relatórios",  "📄")
    data object Curadoria   : Screen("curadoria",   "Curadoria",   "🧾")

    companion object {
        // Bottom navigation items
        val bottomNavItems: List<Screen> = listOf(
            Dashboard, Agenda, CRM, Biblioteca, Ajustes
        )
    }
}
