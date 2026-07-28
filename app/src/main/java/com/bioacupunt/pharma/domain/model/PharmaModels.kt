package com.bioacupunt.pharma.domain.model

import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import kotlinx.serialization.Serializable

/**
 * Categoria regulatória ANVISA do produto (campo `CATEGORIA_REGULATORIA` do dataset
 * aberto). [OUTRO] é o fallback pra qualquer valor não mapeado — a ingestão nunca
 * descarta uma linha só porque a categoria não bate com um dos valores conhecidos.
 */
enum class CategoriaRegulatoria {
    GENERICO, SIMILAR, REFERENCIA, FITOTERAPICO, BIOLOGICO, NOVO, ESPECIFICO, DINAMIZADO, OUTRO
}

/**
 * Entrada do catálogo — camada de IDENTIFICAÇÃO, bulk-importada do dataset aberto da
 * ANVISA (`dados.anvisa.gov.br/dados/DADOS_ABERTOS_MEDICAMENTOS.csv`). Somente leitura,
 * global (não é dado de clínica — como [com.bioacupunt.biblioteca.data.MtcKnowledgeBase]).
 *
 * Isto NÃO contém posologia, interação, contraindicação ou composição de excipientes —
 * esses campos não existem em nenhuma fonte aberta em bulk (a bula em si só está
 * disponível no Bulário Eletrônico da ANVISA, um PDF por item, sem API oficial). Ver
 * [FormularioMedicamento] para a camada clínica curada pela médica.
 */
data class Medicamento(
    val id: String, // NUMERO_REGISTRO_PRODUTO — chave natural do registro ANVISA
    val nomeComercial: String,
    val principiosAtivos: List<String> = emptyList(),
    val classeTerapeutica: String = "",
    val categoriaRegulatoria: CategoriaRegulatoria = CategoriaRegulatoria.OUTRO,
    val empresaDetentora: String = "",
    val situacaoAtiva: Boolean = true,
)

/** Uma classe terapêutica do catálogo + quantos medicamentos ela tem — pra navegação sem busca. */
data class ClasseTerapeuticaSummary(
    val classeTerapeutica: String,
    val count: Int,
)

@Serializable
enum class SeveridadeInteracao { LEVE, MODERADA, GRAVE }

@Serializable
data class InteracaoMedicamentosa(
    val outroMedicamentoId: String,
    val outroNome: String,
    val severidade: SeveridadeInteracao,
    val descricao: String,
)

@Serializable
data class EfeitoAdverso(
    val descricao: String,
    val frequencia: String = "",
)

enum class FormularioStatus { RASCUNHO, APROVADO }

/**
 * Camada CLÍNICA curada pela médica, uma linha por [Medicamento.id] — nunca gerada, nunca
 * inferida. Ela preenche isto com a bula em mãos (mesmo padrão de curadoria humana de
 * `AskLibraryUseCase`/Curadoria da Biblioteca, R4). Só quando [status] é [FormularioStatus.APROVADO]
 * o [com.bioacupunt.pharma.domain.safety.PharmaSafetyEngine] considera este item "verificado" —
 * um rascunho nunca autoriza uma prescrição a passar por segura.
 */
data class FormularioMedicamento(
    val medicamentoId: String,
    val tenantId: Long,
    val posologiaAdulto: String = "",
    val posologiaPediatrica: String = "",
    val posologiaIdoso: String = "",
    val posologiaRenal: String = "",
    val posologiaHepatica: String = "",
    val viaAdministracao: String = "",
    val contraindicacoesAbsolutas: List<ClinicalFlag> = emptyList(),
    val contraindicacoesRelativas: List<ClinicalFlag> = emptyList(),
    /** Tags livres pesquisáveis: "glúten", "lactose", "soja", "gelatina", "corante", "álcool", etc. */
    val alergenos: List<String> = emptyList(),
    val interacoes: List<InteracaoMedicamentosa> = emptyList(),
    val efeitosAdversos: List<EfeitoAdverso> = emptyList(),
    /** Opcional, sempre rotulado como MTC na UI — nunca misturado às seções biomédicas. */
    val visaoIntegrativaMtc: String = "",
    val status: FormularioStatus = FormularioStatus.RASCUNHO,
    val autor: String = "",
    val atualizadoEm: String = "",
) {
    /**
     * Mínimo pra sair de RASCUNHO e virar APROVADO — mesmo espírito do gate de `citation`
     * vazia em `LibraryIngestion.stage` (R4): sem posologia e via, não há o que aprovar.
     */
    val meetsApprovalMinimum: Boolean
        get() = posologiaAdulto.isNotBlank() && viaAdministracao.isNotBlank()
}

/**
 * Prescrição ligada a um paciente — complementa (não substitui)
 * [com.bioacupunt.prontuario.domain.model.Medication], o campo livre já existente no
 * prontuário. Usada pelo fluxo "Smart Prescription", que sempre passa pelo
 * [com.bioacupunt.pharma.domain.safety.PharmaSafetyEngine] antes de salvar.
 */
data class Prescricao(
    val id: Long = 0,
    val patientId: Long,
    val tenantId: Long,
    /** Null quando o medicamento não está no catálogo (nome livre). */
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
    /** Preenchido apenas quando a médica passou por cima de um veto FORBIDDEN. */
    val overrideReason: String = "",
    val overrideBy: String = "",
    val overrideAt: String = "",
)
