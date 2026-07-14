package com.bioacupunt.agenda.domain.repository

import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow

interface AppointmentRepository {
    fun observeByDate(date: String): Flow<List<Appointment>>
    fun observeByPatient(patientId: Long): Flow<List<Appointment>>
    fun observeByStatus(status: String): Flow<List<Appointment>>
    fun observeBetween(start: String, end: String): Flow<List<Appointment>>
    fun observeNextUpcoming(fromDate: String, fromTime: String): Flow<Appointment?>
    suspend fun getById(id: Long): Result<Appointment>
    suspend fun getByDateSync(date: String): List<Appointment>
    suspend fun save(appointment: Appointment): Result<Appointment>
    suspend fun countByDate(date: String): Result<Int>
    suspend fun countByStatus(status: String): Result<Int>
}
