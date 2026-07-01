package com.bioacupunt.biblioteca.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bioacupunt.biblioteca.domain.model.BibliotecaNode

@Entity(tableName = "biblioteca_nodes")
data class BibliotecaNodeEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val content: String,
    val summary: String,
    val tags: String,
    val version: Int,
    val metadata: String
)
