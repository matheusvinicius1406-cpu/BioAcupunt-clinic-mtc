package com.example.data

import com.example.domain.PatientState
import java.util.Date

data class Patient(
    val id: String,
    val name: String,
    val sex: String,
    val profession: String,
    val phone: String,
    val email: String,
    val status: PatientState,
    val balance: Double
)

data class Appointment(
    val id: String,
    val patientId: String,
    val date: Date,
    val duration: Int,
    val status: String,
    val treatmentType: String,
    val notes: String
)

data class Finance(
    val id: String,
    val type: String,
    val description: String,
    val amount: Double,
    val date: Date,
    val category: String,
    val paymentMethod: String
)

data class Synergy(
    val id: String,
    val title: String,
    val description: String,
    val procedure: String,
    val category: String,
    val mainPoints: List<String>,
    val rationale: String,
    val precautions: String,
    val steps: List<String>
)

data class Knowledge(
    val id: String,
    val title: String,
    val category: String,
    val subcategory: String,
    val content: String,
    val summary: String,
    val tags: List<String>
)

object MockData {
    val patients = listOf(
        Patient("p1", "Maria Souza Silva", "Feminino", "Arquiteta", "(11) 98765-4321", "maria.souza@email.com", PatientState.UNDER_TREATMENT, 150.0),
        Patient("p2", "João Alencar Ribeiro", "Masculino", "Engenheiro Civil", "(21) 99123-4567", "joao.alencar@email.com", PatientState.ACTIVE_EVALUATION, 0.0),
        Patient("p3", "Ana Beatriz Ramos", "Feminino", "Designer", "(11) 97765-1122", "ana.beatriz@email.com", PatientState.STABLE, 300.0)
    )

    val appointments = listOf(
        Appointment("a1", "p1", Date(System.currentTimeMillis() + 1000 * 60 * 60 * 2), 50, "scheduled", "Acupuntura Sistêmica", "Foco nos pontos IG4, F3, VB20 e Yintang para controle de cefaleia."),
        Appointment("a2", "p2", Date(System.currentTimeMillis() + 1000 * 60 * 60 * 4), 50, "scheduled", "Acupuntura + Moxabustão", "Aplicação de moxa no canal de bexiga (B23, B40) e R3."),
        Appointment("a3", "p3", Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24), 50, "completed", "Acupuntura Sistêmica", "Sessão tranquila. Paciente refere melhora na qualidade de sono.")
    )

    val finances = listOf(
        Finance("f1", "receita", "Consulta Maria Souza Silva", 150.0, Date(), "Sessão Individual", "pix"),
        Finance("f2", "receita", "Pacote 10 Sessões - Ana Ramos", 1200.0, Date(), "Pacote Tratamento", "cartao_credito"),
        Finance("f3", "despesa", "Agulhas DongBang 0.25x30mm", 85.0, Date(), "Materiais Clínicos", "pix")
    )

    val synergies = listOf(
        Synergy("syn1", "Clínico: Harmonização de Fígado e Baço", "Protocolo terapêutico clássico na desarmonia gastrointestinal desencadeada por estresse.", "Tonificação e Dispersão", "Clínica", listOf("F3", "BP6", "E36", "VB34"), "F3 acalma e espalha o Qi do Fígado Estagnado...", "Evitar estímulos excessivos em gestantes.", listOf("1. Higienize", "2. Agulhe F3", "3. Agulhe E36")),
        Synergy("syn2", "Insônia e Ansiedade Severa", "Sedação do espírito (Shen) e pacificação mental de efeito imediato.", "Sedação", "Calmante", listOf("C7", "Yintang", "VG20", "CS6"), "Yintang e VG20 trazem serenidade...", "Não realizar sedação profunda em debilitados.", listOf("1. Yintang", "2. C7 e CS6", "3. Manter 30 min"))
    )

    val knowledge = listOf(
        Knowledge("k1", "O Ponto E36 (Zusanli) e Imunidade", "Pontos Principais", "Canal do Estômago", "O E36 (Zusanli) está localizado...", "Guia clínico e anatômico", listOf("Imunidade", "Fortalecimento", "Estômago")),
        Knowledge("k2", "Tratamento da Estagnação de Qi do Fígado", "Síndromes Clínicas", "Fígado", "O Fígado (Gans) é responsável por assegurar...", "Diagnóstico e combinação", listOf("Fígado", "Estresse", "Ansiedade"))
    )
}
