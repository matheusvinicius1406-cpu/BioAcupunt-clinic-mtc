package com.example.audit

import java.util.UUID

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val stateBefore: String?, // JSON/Serialized state before change
    val stateAfter: String?,  // JSON/Serialized state after change
    val source: String,       // e.g., "MobileApp", "WebConsole"
    val device: String,       // e.g., "Samsung S23 - Android 14"
    val ipAddress: String     // e.g., "192.168.1.15"
)

interface AuditRepository {
    suspend fun save(log: AuditLog)
    suspend fun fetchAll(): List<AuditLog>
    suspend fun fetchByUserId(userId: String): List<AuditLog>
}

class AuditEngine(private val auditRepository: AuditRepository) {
    suspend fun logAction(
        userId: String,
        action: String,
        before: String? = null,
        after: String? = null,
        source: String = "Android-Core",
        device: String = android.os.Build.MODEL ?: "Unknown Device",
        ipAddress: String = "127.0.0.1"
    ) {
        val auditLog = AuditLog(
            userId = userId,
            action = action,
            stateBefore = before,
            stateAfter = after,
            source = source,
            device = device,
            ipAddress = ipAddress
        )
        auditRepository.save(auditLog)
    }
}

class InMemoryAuditRepository : AuditRepository {
    private val logs = mutableListOf<AuditLog>()

    override suspend fun save(log: AuditLog) {
        logs.add(log)
    }

    override suspend fun fetchAll(): List<AuditLog> {
        return logs.toList()
    }

    override suspend fun fetchByUserId(userId: String): List<AuditLog> {
        return logs.filter { it.userId == userId }
    }
}
