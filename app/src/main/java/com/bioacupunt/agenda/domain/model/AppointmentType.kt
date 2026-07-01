package com.bioacupunt.agenda.domain.model

enum class AppointmentType(val name: String, val label: String) {
    ACUPUNCTURE("ACUPUNCTURE", "Acupuntura"),
    FIRST("FIRST", "Primeira consulta"),
    FOLLOW_UP("FOLLOW_UP", "Retorno"),
    MOXIBUSTION("MOXIBUSTION", "Moxabustão"),
    ELECTRO("ELECTRO", "Eletroacupuntura"),
    CUPPING("CUPPING", "Ventosa"),
    GUASHA("GUASHA", "Gua Sha"),
    OTHER("OTHER", "Outro")
}
