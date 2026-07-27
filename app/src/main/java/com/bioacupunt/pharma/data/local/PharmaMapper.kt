package com.bioacupunt.pharma.data.local

import com.bioacupunt.core.util.AppJson
import com.bioacupunt.pharma.domain.model.CategoriaRegulatoria
import com.bioacupunt.pharma.domain.model.EfeitoAdverso
import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import com.bioacupunt.pharma.domain.model.FormularioStatus
import com.bioacupunt.pharma.domain.model.InteracaoMedicamentosa
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.domain.model.Prescricao
import com.bioacupunt.prontuario.data.local.decodeFlags
import com.bioacupunt.prontuario.data.local.encodeFlags
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

fun MedicamentoEntity.toDomain(): Medicamento = Medicamento(
    id = id,
    nomeComercial = nomeComercial,
    principiosAtivos = decodeCsv(principiosAtivosCsv),
    classeTerapeutica = classeTerapeutica,
    categoriaRegulatoria = runCatching { CategoriaRegulatoria.valueOf(categoriaRegulatoria) }
        .getOrDefault(CategoriaRegulatoria.OUTRO),
    empresaDetentora = empresaDetentora,
    situacaoAtiva = situacaoAtiva,
)

fun Medicamento.toEntity(): MedicamentoEntity = MedicamentoEntity(
    id = id,
    nomeComercial = nomeComercial,
    principiosAtivosCsv = encodeCsv(principiosAtivos),
    classeTerapeutica = classeTerapeutica,
    categoriaRegulatoria = categoriaRegulatoria.name,
    empresaDetentora = empresaDetentora,
    situacaoAtiva = situacaoAtiva,
)

fun Medicamento.toFtsEntity(): MedicamentoFtsEntity = MedicamentoFtsEntity(
    medicamentoId = id,
    nomeComercial = nomeComercial,
    principiosAtivosCsv = encodeCsv(principiosAtivos),
    classeTerapeutica = classeTerapeutica,
)

fun FormularioMedicamentoEntity.toDomain(): FormularioMedicamento = FormularioMedicamento(
    medicamentoId = medicamentoId,
    tenantId = tenantId,
    posologiaAdulto = posologiaAdulto,
    posologiaPediatrica = posologiaPediatrica,
    posologiaIdoso = posologiaIdoso,
    posologiaRenal = posologiaRenal,
    posologiaHepatica = posologiaHepatica,
    viaAdministracao = viaAdministracao,
    contraindicacoesAbsolutas = decodeFlags(contraindicacoesAbsolutasCsv).toList(),
    contraindicacoesRelativas = decodeFlags(contraindicacoesRelativasCsv).toList(),
    alergenos = decodeCsv(alergenosCsv),
    interacoes = decodeJsonOr(interacoesJson, emptyList()),
    efeitosAdversos = decodeJsonOr(efeitosAdversosJson, emptyList()),
    visaoIntegrativaMtc = visaoIntegrativaMtc,
    status = runCatching { FormularioStatus.valueOf(status) }.getOrDefault(FormularioStatus.RASCUNHO),
    autor = autor,
    atualizadoEm = atualizadoEm,
)

fun FormularioMedicamento.toEntity(): FormularioMedicamentoEntity = FormularioMedicamentoEntity(
    medicamentoId = medicamentoId,
    tenantId = tenantId,
    posologiaAdulto = posologiaAdulto,
    posologiaPediatrica = posologiaPediatrica,
    posologiaIdoso = posologiaIdoso,
    posologiaRenal = posologiaRenal,
    posologiaHepatica = posologiaHepatica,
    viaAdministracao = viaAdministracao,
    contraindicacoesAbsolutasCsv = encodeFlags(contraindicacoesAbsolutas.toSet()),
    contraindicacoesRelativasCsv = encodeFlags(contraindicacoesRelativas.toSet()),
    alergenosCsv = encodeCsv(alergenos),
    interacoesJson = encodeJson(interacoes),
    efeitosAdversosJson = encodeJson(efeitosAdversos),
    visaoIntegrativaMtc = visaoIntegrativaMtc,
    status = status.name,
    autor = autor,
    atualizadoEm = atualizadoEm,
)

fun PrescricaoEntity.toDomain(): Prescricao = Prescricao(
    id = id,
    patientId = patientId,
    tenantId = tenantId,
    medicamentoId = medicamentoId,
    medicamentoNomeLivre = medicamentoNomeLivre,
    dose = dose,
    frequencia = frequencia,
    duracao = duracao,
    viaAdministracao = viaAdministracao,
    observacoes = observacoes,
    prescritoPor = prescritoPor,
    prescritoEm = prescritoEm,
    active = active,
    overrideReason = overrideReason,
    overrideBy = overrideBy,
    overrideAt = overrideAt,
)

fun Prescricao.toEntity(): PrescricaoEntity = PrescricaoEntity(
    id = id,
    patientId = patientId,
    tenantId = tenantId,
    medicamentoId = medicamentoId,
    medicamentoNomeLivre = medicamentoNomeLivre,
    dose = dose,
    frequencia = frequencia,
    duracao = duracao,
    viaAdministracao = viaAdministracao,
    observacoes = observacoes,
    prescritoPor = prescritoPor,
    prescritoEm = prescritoEm,
    active = active,
    overrideReason = overrideReason,
    overrideBy = overrideBy,
    overrideAt = overrideAt,
)

private fun decodeCsv(csv: String): List<String> =
    csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }

private fun encodeCsv(values: List<String>): String = values.joinToString(",")

private inline fun <reified T> decodeJsonOr(raw: String, fallback: T): T =
    runCatching { AppJson.decodeFromString<T>(raw) }.getOrDefault(fallback)

private inline fun <reified T> encodeJson(value: T): String =
    runCatching { AppJson.encodeToString(value) }.getOrDefault("[]")
