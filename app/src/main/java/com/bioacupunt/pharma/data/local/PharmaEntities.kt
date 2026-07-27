package com.bioacupunt.pharma.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Catálogo ANVISA — camada de IDENTIFICAÇÃO, bulk-importada (somente leitura em runtime,
 * repovoada a partir de `assets/packs/pharma_anvisa/` uma única vez no boot se vazia; ver
 * `AppContainer.seedPharmaCatalogIfNeeded`). Global, sem tenantId — é referência pública,
 * igual `MtcKnowledgeBase`, não dado de clínica.
 */
@Entity(tableName = "medicamentos")
data class MedicamentoEntity(
    @PrimaryKey val id: String, // NUMERO_REGISTRO_PRODUTO da ANVISA
    val nomeComercial: String,
    val principiosAtivosCsv: String,
    val classeTerapeutica: String,
    val categoriaRegulatoria: String,
    val empresaDetentora: String,
    val situacaoAtiva: Boolean,
)

/**
 * Índice de busca textual do catálogo — mesmo padrão de
 * [com.bioacupunt.biblioteca.data.local.fts.ArticleFtsEntity]: tabela virtual FTS4
 * independente, carregando uma cópia enxuta dos campos pesquisáveis (não faz JOIN de
 * volta pra [MedicamentoEntity] — a tela busca aqui e resolve o item completo por
 * [MedicamentoEntity.id] só quando abre o detalhe).
 */
@Fts4
@Entity(tableName = "medicamentos_fts")
data class MedicamentoFtsEntity(
    val medicamentoId: String,
    val nomeComercial: String,
    val principiosAtivosCsv: String,
    val classeTerapeutica: String,
)

/**
 * Camada CLÍNICA curada pela médica — uma linha por (medicamentoId, tenantId). Chave
 * natural composta, sem id substituto: não existe "formulário sem medicamento".
 *
 * `interacoesJson`/`efeitosAdversosJson` são JSON porque são lidos e escritos juntos,
 * nunca consultados campo a campo — mesmo raciocínio de `patternsJson`/`tongueJson` em
 * `MtcAssessmentEntity`. `contraindicacoes*Csv`/`alergenosCsv` são CSV, pesquisáveis via
 * SQL, mesmo raciocínio de `flagsCsv`.
 */
@Entity(
    tableName = "formulario_medicamento",
    primaryKeys = ["medicamentoId", "tenantId"],
    indices = [Index("tenantId")],
)
data class FormularioMedicamentoEntity(
    val medicamentoId: String,
    val tenantId: Long,
    val posologiaAdulto: String = "",
    val posologiaPediatrica: String = "",
    val posologiaIdoso: String = "",
    val posologiaRenal: String = "",
    val posologiaHepatica: String = "",
    val viaAdministracao: String = "",
    val contraindicacoesAbsolutasCsv: String = "",
    val contraindicacoesRelativasCsv: String = "",
    val alergenosCsv: String = "",
    val interacoesJson: String = "[]",
    val efeitosAdversosJson: String = "[]",
    val visaoIntegrativaMtc: String = "",
    val status: String = "RASCUNHO",
    val autor: String = "",
    val atualizadoEm: String = "",
)

/**
 * Prescrição ligada a paciente — complementa (não substitui)
 * [com.bioacupunt.prontuario.data.local.MedicationEntity], o campo livre já existente.
 * Soft delete via `active` (mesmo padrão de `AppointmentRepositoryImpl`/`CrmPatientRepositoryImpl`).
 */
@Entity(
    tableName = "prescricoes",
    foreignKeys = [
        ForeignKey(
            entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class,
            parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("patientId"), Index("tenantId")],
)
data class PrescricaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val tenantId: Long,
    val medicamentoId: String? = null,
    val medicamentoNomeLivre: String = "",
    val dose: String,
    val frequencia: String,
    val duracao: String = "",
    val viaAdministracao: String = "",
    val observacoes: String = "",
    val prescritoPor: String = "",
    val prescritoEm: String = "",
    val active: Boolean = true,
    val overrideReason: String = "",
    val overrideBy: String = "",
    val overrideAt: String = "",
)
