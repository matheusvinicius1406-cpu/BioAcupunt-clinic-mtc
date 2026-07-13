package com.bioacupunt.agenda.domain.usecase

import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.model.AppointmentType
import com.bioacupunt.agenda.domain.repository.AppointmentRepository
import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow

class GetAppointmentsByDate(
    private val repository: AppointmentRepository
) {
    operator fun invoke(date: String): Flow<List<Appointment>> {
        return repository.observeByDate(date)
    }
}

/**
 * Saving is where a double-booking would be created, so this is the one place that
 * has to check for it — nothing upstream (UI, ViewModel) can be trusted to catch it,
 * and there's only one practitioner's calendar to protect.
 */
class SaveAppointment(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(appointment: Appointment): Result<Appointment> {
        val sameDay = repository.getByDateSync(appointment.date)
        val conflict = sameDay.any { other ->
            other.id != appointment.id &&
                other.status != AppointmentStatus.CANCELLED.name &&
                overlaps(other, appointment)
        }
        if (conflict) {
            return Result.Error(AppError.ValidationError("Já existe uma consulta nesse horário."))
        }
        return repository.save(appointment)
    }

    private fun overlaps(a: Appointment, b: Appointment): Boolean {
        val aStart = toMinutesOfDay(a.time)
        val aEnd = aStart + a.durationMin
        val bStart = toMinutesOfDay(b.time)
        val bEnd = bStart + b.durationMin
        return aStart < bEnd && bStart < aEnd
    }

    private fun toMinutesOfDay(hhmm: String): Int {
        val parts = hhmm.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
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
