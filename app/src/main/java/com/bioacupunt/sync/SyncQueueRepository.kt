package com.bioacupunt.sync

import com.bioacupunt.sync.data.local.SyncQueueEntity

class SyncQueueRepository(private val dao: com.bioacupunt.sync.data.local.SyncQueueDao) {
    suspend fun enqueue(entity: SyncQueueEntity) = dao.enqueue(entity)
}
