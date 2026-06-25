package com.example.data.intelligence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val front: String,
    val back: String,
    val category: String,
    val difficulty: Int // 1: Fácil, 2: Médio, 3: Difícil
)
