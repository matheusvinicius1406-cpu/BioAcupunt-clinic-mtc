package com.bioacupunt.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_nodes")
data class KnowledgeNode(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val content: String,
    val summary: String,
    val tags: String, // Comma separated
    val version: Int,
    val metadata: String // JSON string
)
