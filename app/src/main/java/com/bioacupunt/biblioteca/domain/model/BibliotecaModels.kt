package com.bioacupunt.biblioteca.domain.model

data class BibliotecaNode(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val summary: String,
    val tags: List<String>,
    val version: Int,
    val metadata: String
)
