package com.bioacupunt.prontuario.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "prontuario_documents",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId"), Index("addedAt")]
)
data class ProntuarioDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val uri: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val addedAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Dao
interface ProntuarioDocumentDao {
    @Query("SELECT * FROM prontuario_documents WHERE patientId = :pid ORDER BY addedAt DESC, id DESC")
    fun observe(pid: Long): Flow<List<ProntuarioDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: ProntuarioDocumentEntity): Long

    @Query("DELETE FROM prontuario_documents WHERE id = :id")
    suspend fun delete(id: Long)
}
