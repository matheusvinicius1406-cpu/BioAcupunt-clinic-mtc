package com.bioacupunt.ui.navigation

sealed class Screen(val route: String, val label: String, val emoji: String = "") {
    // Auth
    data object Login       : Screen("login",       "Login",       "🔐")

    // Main navigation (bottom bar)
    data object Dashboard   : Screen("dashboard",   "Início",      "🏠")
    data object Agenda      : Screen("agenda",      "Agenda",      "📅")
    data object CRM         : Screen("crm",         "Pacientes",   "👥")
    data object Biblioteca  : Screen("biblioteca",  "Biblioteca",  "📚")
    data object Ajustes     : Screen("ajustes",     "Ajustes",     "⚙️")

    // Secondary screens (não aparecem na bottom nav)
    data object Prontuario  : Screen("prontuario/{patientId}", "Prontuário",  "📋")
    data object Flashcards  : Screen("flashcards",  "Flashcards",  "🃏")
    data object Analytics   : Screen("analytics",   "Analytics",   "📊")
    data object Simulador   : Screen("simulador",   "Simulador",   "🧪")
    data object AiAssistant : Screen("ai_assistant","Assistente IA","🤖")
    data object Relatorios  : Screen("relatorios",  "Relatórios",  "📄")

    companion object {
        // Bottom navigation items
        val bottomNavItems: List<Screen> = listOf(
            Dashboard, Agenda, CRM, Biblioteca, Ajustes
        )
    }
}
