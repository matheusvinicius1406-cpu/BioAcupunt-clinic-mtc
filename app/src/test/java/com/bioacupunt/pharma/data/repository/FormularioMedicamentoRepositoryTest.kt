package com.bioacupunt.pharma.data.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.pharma.data.local.FormularioMedicamentoDao
import com.bioacupunt.pharma.data.local.FormularioMedicamentoEntity
import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import com.bioacupunt.pharma.domain.model.FormularioStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * `approve()` é o único caminho que liga [FormularioStatus.APROVADO] — o gate que faz o
 * PharmaSafetyEngine tratar um item como verificado. Se ele aprovasse sem o mínimo
 * (posologia adulto + via), um rascunho vazio passaria a autorizar prescrição.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioMedicamentoRepositoryTest {

    private class FakeFormularioDao : FormularioMedicamentoDao {
        private val rowsFlow = MutableStateFlow<List<FormularioMedicamentoEntity>>(emptyList())

        override suspend fun save(entity: FormularioMedicamentoEntity) {
            rowsFlow.value = rowsFlow.value
                .filterNot { it.medicamentoId == entity.medicamentoId && it.tenantId == entity.tenantId } + entity
        }

        override suspend fun getById(medicamentoId: String, tenantId: Long): FormularioMedicamentoEntity? =
            rowsFlow.value.firstOrNull { it.medicamentoId == medicamentoId && it.tenantId == tenantId }

        override fun observeApproved(tenantId: Long): Flow<List<FormularioMedicamentoEntity>> =
            rowsFlow.map { list -> list.filter { it.tenantId == tenantId && it.status == "APROVADO" } }

        override suspend fun getApprovedByIds(ids: List<String>, tenantId: Long): List<FormularioMedicamentoEntity> =
            rowsFlow.value.filter { it.tenantId == tenantId && it.status == "APROVADO" && it.medicamentoId in ids }
    }

    private lateinit var dao: FakeFormularioDao
    private lateinit var repository: FormularioMedicamentoRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeFormularioDao()
        repository = FormularioMedicamentoRepositoryImpl(dao)
    }

    @Test
    fun `approve refuses a draft missing posologia and via`() = runTest {
        repository.save(FormularioMedicamento(medicamentoId = "REG-1", tenantId = 1L))

        val result = repository.approve("REG-1", 1L, "dra")

        assertTrue(result is Result.Error)
        assertEquals(FormularioStatus.RASCUNHO, dao.getById("REG-1", 1L)?.status?.let { FormularioStatus.valueOf(it) })
    }

    @Test
    fun `approve succeeds once the minimum fields are filled`() = runTest {
        repository.save(
            FormularioMedicamento(
                medicamentoId = "REG-1", tenantId = 1L,
                posologiaAdulto = "500mg 6/6h", viaAdministracao = "Oral",
            )
        )

        val result = repository.approve("REG-1", 1L, "dra")

        assertTrue(result is Result.Success)
        assertEquals(FormularioStatus.APROVADO, (result as Result.Success).data.status)
    }

    @Test
    fun `approve on a nonexistent draft is an error, not a crash`() = runTest {
        val result = repository.approve("REG-NUNCA-SALVO", 1L, "dra")
        assertTrue(result is Result.Error)
    }

    @Test
    fun `observeApproved never includes drafts`() = runTest {
        repository.save(FormularioMedicamento(medicamentoId = "REG-1", tenantId = 1L, posologiaAdulto = "1", viaAdministracao = "Oral"))
        repository.save(FormularioMedicamento(medicamentoId = "REG-2", tenantId = 1L, posologiaAdulto = "1", viaAdministracao = "Oral"))
        repository.approve("REG-1", 1L, "dra")
        // REG-2 fica em rascunho, nunca aprovado.

        val ids = dao.observeApproved(1L).first().map { it.medicamentoId }

        assertEquals(listOf("REG-1"), ids)
    }

    @Test
    fun `formularios from a different tenant never leak into getApprovedByIds`() = runTest {
        repository.save(FormularioMedicamento(medicamentoId = "REG-1", tenantId = 1L, posologiaAdulto = "1", viaAdministracao = "Oral"))
        repository.approve("REG-1", 1L, "dra")

        val forOtherTenant = repository.getApprovedByIds(listOf("REG-1"), tenantId = 2L)

        assertTrue("formulário do tenant 1 não pode aparecer pro tenant 2", forOtherTenant.isEmpty())
    }
}
