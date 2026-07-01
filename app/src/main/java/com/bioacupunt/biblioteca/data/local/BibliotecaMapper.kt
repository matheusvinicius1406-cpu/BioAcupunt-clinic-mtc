package com.bioacupunt.biblioteca.data.local

import com.bioacupunt.biblioteca.domain.model.BibliotecaNode

fun BibliotecaNodeEntity.toDomain() = BibliotecaNode(
    id = id,
    type = type,
    title = title,
    content = content,
    summary = summary,
    tags = tags.split(","),
    version = version,
    metadata = metadata
)
