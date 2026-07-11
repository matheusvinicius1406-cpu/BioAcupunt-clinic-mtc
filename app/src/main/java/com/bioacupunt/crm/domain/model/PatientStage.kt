package com.bioacupunt.crm.domain.model

enum class PatientStage(val label: String, val emoji: String) {
    FIRST_CONTACT("Primeiro contato", "💬"),
    LEAD("Lead", "🔍"),
    ACTIVE("Ativo", "💪"),
    TREATMENT("Em tratamento", "🏥"),
    MAINTENANCE("Manutenção", "🚀"),
    INACTIVE("Inativo", "🔆"),
    CHURNED("Churn", "💥")
}
