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

fun com.bioacupunt.financeiro.domain.model.Transacao.toEntity(now: String = ""): TransacaoEntity {
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
        updatedAt = ts
    )
}
