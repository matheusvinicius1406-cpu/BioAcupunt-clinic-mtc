package com.bioacupunt.agenda.domain.model

enum class AppointmentType(val label: String) {
    ACUPUNCTURE("Acupuntura"),
    FIRST("Primeira consulta"),
    FOLLOW_UP("Retorno"),
    MOXIBUSTION("Moxabustão"),
    ELECTRO("Eletroacupuntura"),
    CUPPING("Ventosa"),
    GUASHA("Gua Sha"),
    OTHER("Outro")
}
