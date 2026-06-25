package com.bioacupunt.engines

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimulationEngineV2 @Inject constructor() {
    fun generateCase(): String {
        return "Caso Clínico"
    }
}
