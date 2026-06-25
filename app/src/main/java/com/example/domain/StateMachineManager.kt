package com.example.domain

object StateMachineManager {
    fun transition(currentState: PatientState, action: String): PatientState {
        return when (currentState) {
            PatientState.NEW -> {
                if (action == "START_EVALUATION") PatientState.ACTIVE_EVALUATION else currentState
            }
            PatientState.ACTIVE_EVALUATION -> {
                if (action == "FINISH_DIAGNOSIS") PatientState.DIAGNOSED else currentState
            }
            PatientState.DIAGNOSED -> {
                if (action == "START_TREATMENT") PatientState.UNDER_TREATMENT else currentState
            }
            PatientState.UNDER_TREATMENT -> {
                if (action == "STABILIZE") PatientState.STABLE else currentState
            }
            PatientState.STABLE -> {
                if (action == "RELAPSE") PatientState.UNDER_TREATMENT else currentState
            }
        }
    }
}
