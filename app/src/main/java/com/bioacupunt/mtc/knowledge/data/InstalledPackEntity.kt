package com.bioacupunt.mtc.knowledge.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for installed knowledge packs.
 *
 * Tracks the installation lifecycle of each pack:
 * DOWNLOADED → VALIDATING → VALID → STAGED → ACTIVE
 *
 * Soft delete via [deleted].
 */
@Entity(
    tableName = "installed_packs",
    indices = [
        Index("tenantId"),
        Index("packId"),
        Index("status"),
        Index("deleted"),
    ],
)
data class InstalledPackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tenantId: Long,
    val packId: String,
    val version: String,
    val status: String,
    @ColumnInfo(defaultValue = "")
    val manifestJson: String = "",
    @ColumnInfo(defaultValue = "")
    val checksum: String = "",
    val installedAt: String = "",
    val activatedAt: String? = null,
    val deactivatedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deleted: Boolean = false,
    val deletedAt: String? = null,
)
