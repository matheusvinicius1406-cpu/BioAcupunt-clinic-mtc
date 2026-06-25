package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinica_configs")
data class ClinicaConfigEntity(
    @PrimaryKey val id: String = "main_config",
    val profissionalNome: String = "Dra. Camila Silva",
    val crbm: String = "SP-12345",
    val telefone: String = "(11) 98765-4321",
    val instagram: String = "@dracamilasilva.mtc",
    val endereco: String = "Av. Paulista, 1000 - São Paulo, SP",
    val honorarios: String = "Sessão Acupuntura Sistêmica — R$120\nSessão Ventosaterapia Clínica — R$80\nSessão Auriculoterapia Francesa — R$70\nSessão Massagem Tui Na — R$90",
    val auriculoterapia: Boolean = true,
    val ventosaterapia: Boolean = true,
    val moxabustao: Boolean = true
)
