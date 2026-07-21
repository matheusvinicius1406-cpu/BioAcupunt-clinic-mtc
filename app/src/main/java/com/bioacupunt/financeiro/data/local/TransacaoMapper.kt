package com.bioacupunt.financeiro.data.local

fun TransacaoEntity.toDomain() = com.bioacupunt.financeiro.domain.model.Transacao(
    id = id,
    patientId = patientId,
    appointmentId = appointmentId,
    amountBrl = amountBrl,
    date = date,
    type = type,
    method = method,
    category = category,
    status = status,
    notes = notes,
    createdAt = createdAt
)

/** See CrmPatientMapper.toEntity for what [identity] and [pendingSync] mean. */
fun com.bioacupunt.financeiro.domain.model.Transacao.toEntity(
    now: String = "",
    identity: com.bioacupunt.sync.SyncIdentity = com.bioacupunt.sync.SyncIdentity.new(),
    pendingSync: Boolean = true,
): TransacaoEntity {
    val ts = now.ifBlank { java.time.Instant.now().toString() }
    return TransacaoEntity(
        id = id,
        patientId = patientId,
        appointmentId = appointmentId,
        amountBrl = amountBrl,
        date = date,
        type = type,
        method = method,
        category = category,
        status = status,
        notes = notes,
        createdAt = createdAt,
        updatedAt = ts,
        lastModified = ts,
        pendingSync = pendingSync,
        clientId = identity.clientId,
        serverId = identity.serverId,
        baseRev = identity.baseRev
    )
}
