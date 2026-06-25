package com.bioacupunt.di

import com.bioacupunt.data.repository.KnowledgeRepository
import com.bioacupunt.data.local.database.KnowledgeNodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideKnowledgeRepository(dao: KnowledgeNodeDao): KnowledgeRepository {
        return KnowledgeRepository(dao)
    }
}
