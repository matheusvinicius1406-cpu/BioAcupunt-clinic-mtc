package com.bioacupunt.crm.domain.model

enum class PatientStage(val name: String, val label: String, val emoji: String) {
    FIRST_CONTACT("FIRST_CONTACT", "Primeiro contato", "\uD83D\uDCAC"),
    LEAD("LEAD", "Lead", "\uD83D\uDD0D"),
    ACTIVE("ACTIVE", "Ativo", "\uD83D\uDCAA"),
    TREATMENT("TREATMENT", "Em tratamento", "\uD83C\uDFE5"),
    MAINTENANCE("MAINTENANCE", "Manutenção", "\uD83D\uDE80"),
    INACTIVE("INACTIVE", "Inativo", "\uD83D\uDD06"),
    CHURNED("CHURNED", "Churn", "\uD83D\uDCA5")
}
