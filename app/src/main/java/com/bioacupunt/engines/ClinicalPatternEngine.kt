package com.bioacupunt.engines

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClinicalPatternEngine @Inject constructor() {
    fun detectGaps(): List<String> {
        return emptyList()
    }
}
