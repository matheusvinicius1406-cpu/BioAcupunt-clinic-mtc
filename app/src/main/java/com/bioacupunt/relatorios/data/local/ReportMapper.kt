package com.bioacupunt.relatorios.data.local

import com.bioacupunt.relatorios.domain.model.Report
import com.bioacupunt.relatorios.domain.model.ReportStatus

fun ReportEntity.toDomain() = Report(
    id = id,
    type = type,
    title = title,
    body = body,
    filtersJson = filtersJson,
    generatedAt = generatedAt,
    patientId = patientId,
    status = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.DRAFT)
)
