package com.bioacupunt.engines

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidationLayer @Inject constructor() {
    fun validateContent(content: String): Boolean {
        return true
    }
}
