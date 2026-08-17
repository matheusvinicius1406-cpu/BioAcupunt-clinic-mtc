package com.bioacupunt.relatorios.domain.usecase

import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.repository.AppointmentRepository
import com.bioacupunt.core.util.Result
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.repository.CrmPatientRepository
import com.bioacupunt.prontuario.data.local.MtcAssessmentDao
import com.bioacupunt.prontuario.data.local.MtcAssessmentEntity
import com.bioacupunt.prontuario.data.local.toDomain
import com.bioacupunt.prontuario.data.local.toEntity
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine
import com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateReportUseCaseTest {

    // ── Fakes ──────────────────────────────────────────────

    private class FakeCrmPatientRepository(
        initial: List<CrmPatient>,
    ) : CrmPatientRepository {
        private val patients = MutableStateFlow(initial)

        override fun observeAll(): Flow<List<CrmPatient>> = patients
        override fun observeByStage(stage: String): Flow<List<CrmPatient>> =
            patients.map { list -> list.filter { it.stage == stage } }
        override fun search(query: String): Flow<List<CrmPatient>> =
            patients.map { list -> list.filter { it.name.contains(query, ignoreCase = true) } }
        override suspend fun getById(id: Long): Result<CrmPatient> =
            Result.Success(patients.value.first { it.id == id })
        override suspend fun save(entity: CrmPatient): Result<CrmPatient> = Result.Success(entity)
        override suspend fun saveAll(entities: List<CrmPatient>): Result<Int> = Result.Success(entities.size)
        override suspend fun stageCount(stage: String): Result<Int> = Result.Success(0)
        override suspend fun getPendingSync(since: String): Result<List<CrmPatient>> = Result.Success(emptyList())
        override suspend fun deleteById(id: Long): Result<Unit> = Result.Success(Unit)
    }

    private class FakeMtcAssessmentDao : MtcAssessmentDao {
        private val rows = MutableStateFlow<List<MtcAssessmentEntity>>(emptyList())

        fun seed(vararg assessments: MtcAssessment) {
            rows.value = assessments.map { it.toEntity() }
        }

        override fun observeForPatient(pid: Long): Flow<List<MtcAssessmentEntity>> =
            rows.map { list -> list.filter { it.patientId == pid } }

        override fun observeLatest(pid: Long): Flow<MtcAssessmentEntity?> =
            rows.map { list -> list.filter { it.patientId == pid }.maxByOrNull { it.date } }

        override suspend fun getById(id: Long): MtcAssessmentEntity? =
            rows.value.firstOrNull { it.id == id }

        override suspend fun save(entity: MtcAssessmentEntity): Long {
            rows.update { list -> list.filterNot { it.id == entity.id } + entity }
            return entity.id
        }

        override suspend fun delete(id: Long) {
            rows.update { list -> list.filterNot { it.id == id } }
        }

        override suspend fun flagsHistory(pid: Long): List<String> =
            rows.value.filter { it.patientId == pid && it.flagsCsv.isNotBlank() }.map { it.flagsCsv }

        override suspend fun latestGestationalWeeks(pid: Long): Int? =
            rows.value.filter { it.patientId == pid && it.gestationalWeeks != null }
                .maxByOrNull { it.date }?.gestationalWeeks

        override suspend fun count(pid: Long): Int =
            rows.value.count { it.patientId == pid }

        override suspend fun temperatureDistribution(): List<com.bioacupunt.prontuario.data.local.BucketCount> = emptyList()

        override suspend fun pendingSync(): List<MtcAssessmentEntity> = emptyList()
    }

    private class FakeAppointmentRepository(
        private val appointments: List<Appointment> = emptyList(),
    ) : AppointmentRepository {
        override fun observeByDate(date: String): Flow<List<Appointment>> = kotlinx.coroutines.flow.flowOf(appointments)
        override fun observeByPatient(patientId: Long): Flow<List<Appointment>> = kotlinx.coroutines.flow.flowOf(appointments)
        override fun observeByStatus(status: String): Flow<List<Appointment>> = kotlinx.coroutines.flow.flowOf(appointments)
        override fun observeBetween(start: String, end: String): Flow<List<Appointment>> = kotlinx.coroutines.flow.flowOf(appointments)
        override fun observeNextUpcoming(fromDate: String, fromTime: String): Flow<Appointment?> = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun getById(id: Long): Result<Appointment> = Result.Error(com.bioacupunt.core.util.AppError.DatabaseError())
        override suspend fun getByDateSync(date: String): List<Appointment> = appointments
        override suspend fun save(appointment: Appointment): Result<Appointment> = Result.Success(appointment)
        override suspend fun countByDate(date: String): Result<Int> = Result.Success(appointments.size)
        override suspend fun countByStatus(status: String): Result<Int> = Result.Success(appointments.size)
    }

    // ── Fixture ────────────────────────────────────────────

    private val ana = CrmPatient(
        id = 1L,
        name = "Ana Lima",
        phone = "11999990000",
        birthDate = "1985-03-10",
        mainComplaint = "Dor lombar crônica",
    )

    private fun useCase(
        patients: List<CrmPatient> = listOf(ana),
        dao: FakeMtcAssessmentDao = FakeMtcAssessmentDao(),
        appointments: List<Appointment> = emptyList(),
    ) = GenerateReportUseCase(
        crmPatientRepository = FakeCrmPatientRepository(patients),
        mtcAssessmentRepository = MtcAssessmentRepository(dao, ClinicalSafetyEngine()),
        appointmentRepository = FakeAppointmentRepository(appointments),
        clinicName = { "Clínica Teste" },
        professionalName = { "Dra. Camila" },
        tcleText = { "TCLE de teste." },
    )

    private fun assessment(patientId: Long = 1L, flags: Set<ClinicalFlag> = emptySet()): MtcAssessment =
        MtcAssessment(
            patientId = patientId,
            date = "2026-07-01",
            chiefComplaint = "Dor lombar há 3 meses",
            clinicalImpression = "Estagnação de Qi do Fígado atingindo a região lombar.",
            orientations = "Evitar esforço, aplicar calor local.",
            flags = flags,
        )

    // ── Tests ──────────────────────────────────────────────

    @Test
    fun `evolucao_resolves patient by name and fills body from chart`() = runTest {
        val dao = FakeMtcAssessmentDao()
        dao.seed(assessment())

        val result = useCase(dao = dao).invoke("evo", "Nota de Evolução", "Ana Lima")

        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data
        assertEquals(1L, report.patientId)
        assertEquals("Ana Lima", report.patientName)
        assertEquals("READY", report.status.name)
        assertTrue(report.body.contains("NOTA DE EVOLUÇÃO"))
        assertTrue(report.body.contains("Dor lombar há 3 meses"))
        assertTrue(report.body.contains("Estagnação de Qi do Fígado"))
        assertTrue(report.body.contains("Evitar esforço"))
    }

    @Test
    fun `first_assessment_includes anamnesis sections`() = runTest {
        val dao = FakeMtcAssessmentDao()
        dao.seed(assessment())

        val result = useCase(dao = dao).invoke("first", "Avaliação Inicial MTC", "Ana Lima")

        val body = (result as Result.Success).data.body
        assertTrue(body.contains("AVALIAÇÃO INICIAL MTC"))
        assertTrue(body.contains("Queixa principal: Dor lombar há 3 meses"))
        assertTrue(body.contains("Impressão clínica"))
    }

    @Test
    fun `unknown patient returns honest error and never a report`() = runTest {
        val result = useCase().invoke("evo", "Nota de Evolução", "Paciente Fantasma")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).kind.userMessage.contains("Nenhum paciente"))
    }

    @Test
    fun `ambiguous name returns honest error asking for full name`() = runTest {
        val use = useCase(
            patients = listOf(
                ana,
                ana.copy(id = 2L, name = "Ana Souza"),
            )
        )

        val result = use.invoke("evo", "Nota de Evolução", "Ana")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).kind.userMessage.contains("Mais de um paciente"))
    }

    @Test
    fun `consent_uses clinic TCLE and keeps typed name`() = runTest {
        val result = useCase().invoke("consent", "Termo de Consentimento", "Ana Lima")

        val report = (result as Result.Success).data
        assertEquals("Ana Lima", report.patientName)
        assertTrue(report.body.contains("TCLE de teste."))
        assertTrue(report.body.contains("Clínica Teste"))
    }

    @Test
    fun `referral_does not need chart and includes reason fields`() = runTest {
        val result = useCase().invoke("referral", "Encaminhamento Médico", "Ana Lima")

        val report = (result as Result.Success).data
        assertEquals(1L, report.patientId)
        assertTrue(report.body.contains("ENCAMINHAMENTO MÉDICO"))
        assertTrue(report.body.contains("Especialidade solicitada"))
    }

    @Test
    fun `flags aparecem no relatorio clinico quando registradas`() = runTest {
        val dao = FakeMtcAssessmentDao()
        dao.seed(assessment(flags = setOf(ClinicalFlag.PACEMAKER)))

        val result = useCase(dao = dao).invoke("evo", "Nota de Evolução", "Ana Lima")

        val body = (result as Result.Success).data.body
        assertTrue(body.contains("Marca-passo / DCI"))
    }

    @Test
    fun `financial report is generated without patient`() = runTest {
        val result = useCase().invoke("financial", "Relatório Financeiro", "")

        val report = (result as Result.Success).data
        assertTrue(report.body.contains("RELATÓRIO FINANCEIRO"))
        assertTrue(report.patientName.isBlank())
    }
}
