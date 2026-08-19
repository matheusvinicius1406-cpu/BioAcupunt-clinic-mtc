package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.*
import com.bioacupunt.clinic.domain.nlp.QuestionnaireToObservationMapper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class QuestionnaireToObservationMapperTest {

    private lateinit var mapper: QuestionnaireToObservationMapper

    @Before
    fun setup() {
        mapper = QuestionnaireToObservationMapper()
    }

    @Test
    fun map_itemWithObservationMapping_createsObservation() {
        val questionnaire = makeQuestionnaire(
            items = listOf(
                QuestionnaireItem(
                    id = "sleep_quality",
                    type = QuestionnaireItemType.SINGLE_CHOICE,
                    label = "Qualidade do sono",
                    observationMapping = ObservationType.SLEEP,
                    options = listOf(
                        QuestionnaireOption(id = "good", label = "Boa", value = "good"),
                        QuestionnaireOption(id = "bad", label = "Ruim", value = "bad"),
                    ),
                ),
            ),
        )
        val response = makeResponse(answers = mapOf("sleep_quality" to "bad"))

        val observations = mapper.map(questionnaire, response, tenantId = 1, patientId = 1, encounterId = 1)

        assertEquals(1, observations.size)
        assertEquals(ObservationType.SLEEP, observations[0].type)
        assertTrue(observations[0].content.contains("Ruim"))
        assertEquals(ObservationStatus.DRAFT, observations[0].status)
    }

    @Test
    fun map_itemWithoutMapping_noObservation() {
        val questionnaire = makeQuestionnaire(
            items = listOf(
                QuestionnaireItem(
                    id = "name",
                    type = QuestionnaireItemType.TEXT,
                    label = "Nome completo",
                    observationMapping = null, // no mapping
                ),
            ),
        )
        val response = makeResponse(answers = mapOf("name" to "Maria"))

        val observations = mapper.map(questionnaire, response, tenantId = 1, patientId = 1, encounterId = 1)

        assertEquals(0, observations.size)
    }

    @Test
    fun map_emptyAnswer_requiredItem_skipped() {
        val questionnaire = makeQuestionnaire(
            items = listOf(
                QuestionnaireItem(
                    id = "pain_location",
                    type = QuestionnaireItemType.TEXT,
                    label = "Local da dor",
                    required = true,
                    observationMapping = ObservationType.PAIN,
                ),
            ),
        )
        val response = makeResponse(answers = mapOf("pain_location" to ""))

        val observations = mapper.map(questionnaire, response, tenantId = 1, patientId = 1, encounterId = 1)

        assertEquals(0, observations.size)
    }

    @Test
    fun map_statusIsAlwaysDraft() {
        val questionnaire = makeQuestionnaire(
            items = listOf(
                QuestionnaireItem(
                    id = "tongue_color",
                    type = QuestionnaireItemType.SINGLE_CHOICE,
                    label = "Cor da língua",
                    observationMapping = ObservationType.TONGUE,
                    options = listOf(QuestionnaireOption(id = "red", label = "Vermelha", value = "red")),
                ),
            ),
        )
        val response = makeResponse(answers = mapOf("tongue_color" to "red"))

        val observations = mapper.map(questionnaire, response, tenantId = 1, patientId = 1, encounterId = 1)

        assertEquals(1, observations.size)
        assertEquals(ObservationStatus.DRAFT, observations[0].status)
        assertEquals(ObservationSource.AI_EXTRACTED_DRAFT, observations[0].source)
    }

    @Test
    fun map_optionLabelUsedInsteadOfId() {
        val questionnaire = makeQuestionnaire(
            items = listOf(
                QuestionnaireItem(
                    id = "pain_level",
                    type = QuestionnaireItemType.SINGLE_CHOICE,
                    label = "Intensidade da dor",
                    observationMapping = ObservationType.PAIN,
                    options = listOf(
                        QuestionnaireOption(id = "severe", label = "Intensa", value = "severe"),
                    ),
                ),
            ),
        )
        val response = makeResponse(answers = mapOf("pain_level" to "severe"))

        val observations = mapper.map(questionnaire, response, tenantId = 1, patientId = 1, encounterId = 1)

        assertEquals(1, observations.size)
        assertTrue("Should use option label, not ID", observations[0].content.contains("Intensa"))
    }

    private fun makeQuestionnaire(
        items: List<QuestionnaireItem> = emptyList(),
    ) = Questionnaire(
        id = "test_questionnaire",
        version = 1,
        title = "Teste",
        sections = listOf(
            QuestionnaireSection(id = "section_1", title = "Seção 1", items = items),
        ),
    )

    private fun makeResponse(
        answers: Map<String, String> = emptyMap(),
    ) = QuestionnaireResponse(
        id = 0,
        tenantId = 1,
        questionnaireId = "test_questionnaire",
        questionnaireVersion = 1,
        patientId = 1,
        encounterId = 1,
        answers = answers,
        status = ResponseStatus.COMPLETED,
        createdAt = "2026-01-01",
    )
}
