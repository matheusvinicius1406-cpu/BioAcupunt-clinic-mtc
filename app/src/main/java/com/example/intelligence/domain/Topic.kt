package com.example.intelligence.domain

data class Topic(
    val id: String,
    val title: String,
    val category: String,
    val summary: String
)

interface IntelligenceRepository {
    suspend fun getTopics(): List<Topic>
    // Other methods...
}
