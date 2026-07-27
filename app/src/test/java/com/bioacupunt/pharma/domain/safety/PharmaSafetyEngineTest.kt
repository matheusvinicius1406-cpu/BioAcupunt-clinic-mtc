package com.bioacupunt.pharma.domain.safety

import com.bioacupunt.pharma.domain.model.CategoriaRegulatoria
import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import com.bioacupunt.pharma.domain.model.FormularioStatus
import com.bioacupunt.pharma.domain.model.InteracaoMedicamentosa
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.domain.model.SeveridadeInteracao
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O motor de segurança farmacológica é o segundo lugar no app (depois do
 * ClinicalSafetyEngine) onde uma resposta errada machuca uma paciente de verdade — dose
 * ou interação real, não um artigo de MTC. Testado como especificação: cada regra
 * FORBIDDEN tem um teste que prova que dispara, e o estado "não curado" nunca pode virar
 * "seguro" por omissão.
 */
class PharmaSafetyEngineTest {

    private val engine = PharmaSafetyEngine()

    private fun medicamento(
        id: String = "REG-1",
        principiosAtivos: List<String> = listOf("dipirona sódica"),
    ) = Medicamento(
        id = id,
        nomeComercial = "Novalgina",
        principiosAtivos = principiosAtivos,
        classeTerapeutica = "Analgésico",
        categoriaRegulatoria = CategoriaRegulatoria.REFERENCIA,
        empresaDetentora = "Fabricante Teste",
        situacaoAtiva = true,
    )

    private fun aprovado(
        medicamentoId: String = "REG-1",
        contraindicacoesAbsolutas: List<ClinicalFlag> = emptyList(),
        contraindicacoesRelativas: List<ClinicalFlag> = emptyList(),
        alergenos: List<String> = emptyList(),
        interacoes: List<InteracaoMedicamentosa> = emptyList(),
    ) = FormularioMedicamento(
        medicamentoId = medicamentoId,
        tenantId = 1L,
        posologiaAdulto = "500mg VO 6/6h",
        viaAdministracao = "Oral",
        contraindicacoesAbsolutas = contraindicacoesAbsolutas,
        contraindicacoesRelativas = contraindicacoesRelativas,
        alergenos = alergenos,
        interacoes = interacoes,
        status = FormularioStatus.APROVADO,
    )

    // -- "Não verificado" nunca vira "seguro" --------------------------------

    @Test
    fun unverifiedFormularioNeverBecomesSafe() {
        val verdict = engine.evaluate(
            medicamento = medicamento(),
            formulario = null,
            patientFlags = emptySet(),
            patientAllergies = emptyList(),
            activeFormularios = emptyList(),
        )
        assertFalse("Sem curadoria aprovada, o veredito nunca pode ser 'verified'", verdict.verified)
        assertFalse("Sem curadoria aprovada, o veredito nunca pode ser 'isClear'", verdict.isClear)
        assertTrue(
            "Deve haver um aviso explícito de 'não verificado'",
            verdict.findings.any { it.title.contains("Não verificado", ignoreCase = true) },
        )
    }

    @Test
    fun rascunhoAlsoCountsAsUnverified() {
        val rascunho = aprovado().copy(status = FormularioStatus.RASCUNHO)
        val verdict = engine.evaluate(medicamento(), rascunho, emptySet(), emptyList(), emptyList())
        assertFalse("Rascunho não aprovado não pode ser tratado como verificado", verdict.verified)
    }

    // -- Regra não pode derrubar as outras ------------------------------------

    @Test
    fun aFailingRuleCannotCrashThePharmaEngine() {
        val exploding = PharmaSafetyRule { error("boom") }
        val resilient = PharmaSafetyEngine(rules = listOf(exploding) + PharmaSafetyEngine.DEFAULT_RULES)

        val verdict = resilient.evaluate(
            medicamento = medicamento(),
            formulario = aprovado(contraindicacoesAbsolutas = listOf(ClinicalFlag.PREGNANCY)),
            patientFlags = setOf(ClinicalFlag.PREGNANCY),
            patientAllergies = emptyList(),
            activeFormularios = emptyList(),
        )
        assertTrue("Uma regra quebrada não pode derrubar as demais", verdict.isBlocked)
    }

    // -- Alergia ---------------------------------------------------------------

    @Test
    fun allergyToActiveIngredientIsForbidden_evenWithoutCuration() {
        // Roda mesmo com formulario == null: princípio ativo é dado ANVISA (bulk, real).
        val verdict = engine.evaluate(
            medicamento = medicamento(principiosAtivos = listOf("dipirona sódica")),
            formulario = null,
            patientFlags = emptySet(),
            patientAllergies = listOf("Dipirona"),
            activeFormularios = emptyList(),
        )
        assertTrue("Alergia ao princípio ativo deve bloquear", verdict.isBlocked)
    }

    @Test
    fun allergyMatchIsForbidden_curatedExcipient() {
        val verdict = engine.evaluate(
            medicamento = medicamento(principiosAtivos = listOf("amoxicilina")),
            formulario = aprovado(alergenos = listOf("lactose", "glúten")),
            patientFlags = emptySet(),
            patientAllergies = listOf("Lactose"),
            activeFormularios = emptyList(),
        )
        assertTrue("Alergia a excipiente curado deve bloquear", verdict.isBlocked)
    }

    @Test
    fun noMatchingAllergyDoesNotBlock() {
        val verdict = engine.evaluate(
            medicamento = medicamento(principiosAtivos = listOf("amoxicilina")),
            formulario = aprovado(alergenos = listOf("lactose")),
            patientFlags = emptySet(),
            patientAllergies = listOf("Frutos do mar"),
            activeFormularios = emptyList(),
        )
        assertFalse("Alergia não relacionada não deve bloquear", verdict.isBlocked)
    }

    // -- Contraindicação ---------------------------------------------------------

    @Test
    fun absoluteContraindicationIsForbidden() {
        val verdict = engine.evaluate(
            medicamento = medicamento(),
            formulario = aprovado(contraindicacoesAbsolutas = listOf(ClinicalFlag.PREGNANCY)),
            patientFlags = setOf(ClinicalFlag.PREGNANCY),
            patientAllergies = emptyList(),
            activeFormularios = emptyList(),
        )
        assertTrue(verdict.isBlocked)
    }

    @Test
    fun relativeContraindicationIsCautionNotForbidden() {
        val verdict = engine.evaluate(
            medicamento = medicamento(),
            formulario = aprovado(contraindicacoesRelativas = listOf(ClinicalFlag.ELDERLY_FRAIL)),
            patientFlags = setOf(ClinicalFlag.ELDERLY_FRAIL),
            patientAllergies = emptyList(),
            activeFormularios = emptyList(),
        )
        assertFalse("Contraindicação relativa não deve bloquear sozinha", verdict.isBlocked)
        assertTrue(verdict.findings.any { it.severity == PharmaSeverity.CAUTION })
    }

    // -- Interação, nos dois sentidos ---------------------------------------------

    @Test
    fun interactionDetectedBothDirections() {
        val outro = aprovado(
            medicamentoId = "REG-2",
            interacoes = listOf(
                InteracaoMedicamentosa("REG-1", "Novalgina", SeveridadeInteracao.GRAVE, "Risco hematológico"),
            ),
        )
        // "REG-1" (o medicamento sendo avaliado) não cadastrou a interação do seu lado —
        // só REG-2 cadastrou apontando pra REG-1. O motor tem que pegar nos dois sentidos.
        val esteFormulario = aprovado(medicamentoId = "REG-1")

        val verdict = engine.evaluate(
            medicamento = medicamento(id = "REG-1"),
            formulario = esteFormulario,
            patientFlags = emptySet(),
            patientAllergies = emptyList(),
            activeFormularios = listOf(outro),
        )
        assertTrue("Interação cadastrada só do lado do outro medicamento ainda deve disparar", verdict.isBlocked)
    }

    // -- Motor não bloqueia tudo por padrão -----------------------------------------

    @Test
    fun noFlagsNoAllergiesYieldsClearVerdictWhenApproved() {
        val verdict = engine.evaluate(
            medicamento = medicamento(),
            formulario = aprovado(),
            patientFlags = emptySet(),
            patientAllergies = emptyList(),
            activeFormularios = emptyList(),
        )
        assertTrue("Motor que bloqueia tudo por padrão é inútil — sem risco, veredito deve ser claro", verdict.isClear)
        assertFalse(verdict.isBlocked)
    }
}
