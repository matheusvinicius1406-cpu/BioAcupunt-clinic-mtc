package com.bioacupunt.agenda

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bioacupunt.agenda.data.local.AppointmentDao
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.security.SecurePreferences
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Lembretes de consulta REAIS via AlarmManager + notificação local.
 *
 * Antes isto era um toggle fantasma em Ajustes: `remember { mutableStateOf(true) }`
 * sem persistência, sem nenhum `AlarmManager`/`NotificationManager` no app inteiro.
 * Agora:
 *  - [schedule] agenda um alarme exato para cada consulta futura, `reminderMin`
 *    minutos antes (default 30), e o mantém se o app for morto.
 *  - [cancel] desliga o lembrete de uma consulta (cancelada/no-show).
 *  - [rescheduleAll] roda no boot e no start do app: reconstrói todos os alarmes,
 *    porque AlarmManager perde os pendentes num reboot.
 *  - [rescheduleFromPrefs] aplica mudança de preferência (ligar/desligar, minutos).
 *
 * O canal de notificação "Consultas" é criado aqui mesmo, então não existe notificação
 * sem canal (crash na API 26+).
 */
class AppointmentReminderScheduler(
    private val context: Context,
    private val dao: AppointmentDao,
    private val prefs: SecurePreferences,
) {
    companion object {
        const val CHANNEL_ID = "appointments"
        const val ACTION_SHOW = "com.bioacupunt.agenda.SHOW_APPOINTMENT_REMINDER"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PATIENT = "patient"
        const val EXTRA_TIME = "time"
        const val EXTRA_TYPE = "type"
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lembretes de consulta",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Avisa antes de cada consulta agendada" }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private val alarmManager: AlarmManager get() =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun notificationManager(): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    /** Agenda o lembrete de UMA consulta (se futura e não cancelada). */
    suspend fun schedule(
        appointmentId: Long,
        patientName: String,
        dateIso: String,
        time: String,
        type: String,
    ) {
        if (!prefs.notificationsEnabled) return
        val reminderMin = prefs.reminderMinutesBefore
        val trigger = runCatching {
            LocalDateTime.of(LocalDate.parse(dateIso), LocalTime.parse(time))
                .minusMinutes(reminderMin.toLong())
        }.getOrNull() ?: return
        // Só agenda o que ainda está no futuro; lembrete atrasado não dispara.
        if (!trigger.isAfter(LocalDateTime.now())) return

        val requestCode = appointmentId.toInt()
        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            action = ACTION_SHOW
            putExtra(EXTRA_TITLE, "Consulta em $reminderMin min")
            putExtra(EXTRA_PATIENT, patientName)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_TYPE, type)
        }
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = trigger.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAt, pending,
        )
    }

    /** Cancela o lembrete de uma consulta (cancelada, no-show, finalizada). */
    suspend fun cancel(appointmentId: Long) {
        val pending = PendingIntent.getBroadcast(
            context, appointmentId.toInt(),
            Intent(context, AppointmentReminderReceiver::class.java).setAction(ACTION_SHOW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
        pending.cancel()
    }

    /**
     * Reconstrói todos os alarmes a partir das consultas ativas no banco.
     * Chamado no boot (o sistema apaga alarmes em reboot) e quando o app volta
     * ao primeiro plano — manter o lembrete é a função, não o privilégio.
     */
    suspend fun rescheduleAll(tenantId: Long) {
        if (!prefs.notificationsEnabled) return
        val today = LocalDate.now().toString()
        dao.observeBetween(today, "9999-12-31", tenantId).first()
            .filter { !it.deleted }
            .filter { it.status != AppointmentStatus.CANCELLED.name && it.status != AppointmentStatus.NO_SHOW.name && it.status != AppointmentStatus.COMPLETED.name }
            .forEach { a ->
                schedule(a.id, a.patientName, a.date, a.time, a.type)
            }
    }

    /** Aplica mudança de preferência: desliga todos ou reagenda com o novo intervalo. */
    suspend fun rescheduleFromPrefs(tenantId: Long) {
        if (!prefs.notificationsEnabled) {
            val today = LocalDate.now().toString()
            dao.observeBetween(today, "9999-12-31", tenantId).first()
                .forEach { cancel(it.id) }
        } else {
            rescheduleAll(tenantId)
        }
    }

    /** Mostra a notificação disparada pelo receiver. Chamado fora de coroutine. */
    fun showNotification(title: String, patient: String, time: String, type: String) {
        ensureChannel(context)
        val label = runCatching {
            com.bioacupunt.agenda.domain.model.AppointmentType.valueOf(type).label
        }.getOrDefault(type)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$patient · $time — $label")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$patient · $time — $label"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager().notify(patient.hashCode() and 0x7fffffff, notification)
    }
}

/** Receiver que acorda quando o alarme dispara e mostra a notificação. */
class AppointmentReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AppointmentReminderScheduler.EXTRA_TITLE) ?: "Consulta"
        val patient = intent.getStringExtra(AppointmentReminderScheduler.EXTRA_PATIENT).orEmpty()
        val time = intent.getStringExtra(AppointmentReminderScheduler.EXTRA_TIME).orEmpty()
        val type = intent.getStringExtra(AppointmentReminderScheduler.EXTRA_TYPE).orEmpty()
        val scheduler = runCatching {
            AppointmentReminderScheduler(
                context,
                com.bioacupunt.di.AppContainer.appointmentDao,
                com.bioacupunt.di.AppContainer.securePreferences,
            )
        }.getOrNull() ?: return
        scheduler.showNotification(title, patient, time, type)
    }
}

/**
 * Reagenda todos os lembretes após reboot — AlarmManager não sobrevive a reboot,
 * e lembrete de consulta perdido por reboot é exatamente o tipo de falha que a
 * médica descobre em cima da hora. O schedule é idempotente (FLAG_UPDATE_CURRENT).
 */
class AppointmentBootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        runCatching { com.bioacupunt.di.AppContainer.rescheduleAppointmentReminders() }
    }
}
