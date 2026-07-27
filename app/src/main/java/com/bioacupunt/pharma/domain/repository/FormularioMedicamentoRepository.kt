package com.bioacupunt.pharma.domain.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import kotlinx.coroutines.flow.Flow

/** Camada clínica curada pela médica — a única fonte que o PharmaSafetyEngine considera "verificada". */
interface FormularioMedicamentoRepository {
    suspend fun getById(medicamentoId: String, tenantId: Long): FormularioMedicamento?
    suspend fun save(formulario: FormularioMedicamento): Result<FormularioMedicamento>

    /** Rejeita se [FormularioMedicamento.meetsApprovalMinimum] for falso (R4: sem posologia/via, não aprova). */
    suspend fun approve(medicamentoId: String, tenantId: Long, autor: String): Result<FormularioMedicamento>

    fun observeApproved(tenantId: Long): Flow<List<FormularioMedicamento>>
    suspend fun getApprovedByIds(ids: List<String>, tenantId: Long): List<FormularioMedicamento>
}
