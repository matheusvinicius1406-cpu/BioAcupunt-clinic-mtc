package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mtc_prontuaries")
data class MtcProntuaryEntity(
    @PrimaryKey val patientId: String,
    val queixaPrincipal: String = "",
    val historico: String = "",
    
    // Anamnese Clínica Completa
    val sintomasFisicos: String = "",
    val sintomasEmocionais: String = "",
    val sono: String = "Normal",
    val energiaVital: String = "Normal",
    val digestao: String = "Normal",
    val dorLocalizacao: String = "",
    val dorNatureza: String = "",
    val medicamentos: String = "",
    val estiloVida: String = "",
    val sintomaIntensidade: Int = 5,
    val sintomaFrequencia: String = "Esporádica",
    val sintomaDuracao: String = "Semanas",
    val sintomaGatilhos: String = "Nenhum",
    val sintomaPadraoTemporal: String = "Sem padrão",
    val sintomaImpactoFuncional: String = "Leve",
    
    // Semiologia da Língua Estruturada
    val linguaCorpo: String = "Normal",
    val linguaSaburra: String = "Fina Branca",
    val linguaFormato: String = "Normal",
    val linguaFissuras: String = "Nenhuma",
    val linguaMarcasDentarias: Boolean = false,
    val linguaUmidade: String = "Normal",
    val linguaRegioes: String = "Homogênea",
    val linguaSaburraDistribuicao: String = "Geral",
    val linguaBordas: String = "Normais",
    val linguaEvolucaoTempo: String = "Estável",
    
    // Semiologia do Pulso Estruturado
    val pulso: String = "Moderado",
    val pulsoProfundidade: String = "Normal", // Superficial, Profundo
    val pulsoForca: String = "Normal",       // Forte, Fraco
    val pulsoRitmo: String = "Regular",       // Regular, Irregular
    val pulsoLateralidade: String = "Simétrico",
    val pulsoVelocidade: String = "Normal",
    val pulsoQualidadeEnergetica: String = "Moderado",
    val pulsoRespostaPressao: String = "Normal",
    
    // Estado do Shen (Mente e Espírito)
    val shen: String = "Com Brilho / Alerta",
    val shenPresencaEspirito: String = "Normal",
    val shenAnsiedade: String = "Ausente",
    val shenAgitacao: String = "Ausente",
    val shenDepressao: String = "Ausente",
    val shenClarezaMental: String = "Normal",
    val shenEstabilidadeEmocional: String = "Estável",
    val shenVitalidadeEspiritual: String = "Normal",
    val shenIrritabilidade: String = "Ausente",
    
    // Avaliação de Zang Fu (Órgãos e Vísceras)
    val zangFu: String = "Equilibrado",
    val zangFuSpleen: String = "Equilibrado", // Baço
    val zangFuLiver: String = "Equilibrado",  // Fígado
    val zangFuKidney: String = "Equilibrado", // Rim
    val zangFuHeart: String = "Equilibrado",  // Coração
    val zangFuLung: String = "Equilibrado",   // Pulmão
    val zangFuSpleenEstado: String = "Normal",
    val zangFuLiverEstado: String = "Normal",
    val zangFuKidneyEstado: String = "Normal",
    val zangFuHeartEstado: String = "Normal",
    val zangFuLungEstado: String = "Normal",
    
    // Cinco Elementos e Meridianos
    val cincoElementos: String = "Terra",
    val cincoElementosBalance: String = "Equilibrado",
    val nivelMadeira: Int = 50,
    val nivelFogo: Int = 50,
    val nivelTerra: Int = 50,
    val nivelMetal: Int = 50,
    val nivelAgua: Int = 50,
    val meridianos: String = "P, IG, BP, E",
    val meridianosBloqueios: String = "",
    val meridianosExcesso: String = "",
    val meridianosDeficiencia: String = "",
    val meridianosDorTrajeto: String = "",
    
    // Diagnósticos e Plano Terapêutico
    val diagnosticoEnergetico: String = "",
    val sindromesMtc: String = "",
    val protocolos: String = "",
    val timelineNotes: String = "",
    val diagnosticoConfianca: String = "Média" // Baixa, Média, Alta
)
