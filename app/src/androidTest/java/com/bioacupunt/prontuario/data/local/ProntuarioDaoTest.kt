package com.bioacupunt.prontuario.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bioacupunt.data.local.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ProntuarioDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProntuarioDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            org.mockito.Mockito.mock(android.content.Context::class.java),
            AppDatabase::class.java
        ).build()
        dao = db.prontuarioDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertEntry_thenObserve() = runBlocking {
        val entry = ProntuarioEntryEntity(patientId = 1, body = "Evolução 1", date = "2026-07-01")
        dao.saveEntry(entry)
        val list = dao.observeEntries(1)
        assertEquals(1, list?.collect?.size ?: 0)
    }
}
