package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finances")
data class FinanceEntity(
    @PrimaryKey val id: String,
    val type: String, // receita, despesa
    val description: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val paymentMethod: String
)
