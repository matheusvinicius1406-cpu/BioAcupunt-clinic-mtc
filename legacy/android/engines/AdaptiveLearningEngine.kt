package com.bioacupunt.engines

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveLearningEngine @Inject constructor() {
    fun getRecommendations(): List<String> {
        return emptyList()
    }
}
