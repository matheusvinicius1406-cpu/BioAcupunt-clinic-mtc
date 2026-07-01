package com.bioacupunt.agenda.domain.usecase

import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.model.AppointmentType
import com.bioacupunt.agenda.domain.repository.AppointmentRepository
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow

class GetAppointmentsByDate(
    private val repository: AppointmentRepository
) {
    operator fun invoke(date: String): Flow<List<Appointment>> {
        return repository.observeByDate(date)
    }
}

class SaveAppointment(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(appointment: Appointment): Result<Appointment> {
        return repository.save(appointment)
    }
}

class UpdateAppointmentStatus(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentId: Long, status: AppointmentStatus): Result<Appointment> {
        val current = repository.getById(appointmentId)
        if (current is Result.Error) return current
        val appointment = (current as Result.Success).data.copy(status = status.name)
        return repository.save(appointment)
    }
}

class CalculateDayStats(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(date: String): Map<String, Any> {
        val list = appointmentRepository.getByDateSync(date)
        val total = list.size
        val revenue = list.filter { it.paid }.sumOf { it.valueBrl }
        return mapOf(
            "appointments" to total,
            "revenue" to revenue,
            "paid" to list.count { it.paid },
            "pending" to list.count { !it.paid }
        )
    }
}
