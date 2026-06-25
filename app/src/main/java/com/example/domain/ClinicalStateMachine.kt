package com.example.domain

class ClinicalStateMachine {
    fun evaluateStateTransition(current: ClinicalState, riskFlag: Boolean): ClinicalState {
        // Risk Engine: Any high risk flag blocks transition past ACTIVE_EVALUATION
        if (riskFlag && current == ClinicalState.ACTIVE_EVALUATION) {
            return ClinicalState.ACTIVE_EVALUATION
        }
        
        return when (current) {
            ClinicalState.NEW -> ClinicalState.ACTIVE_EVALUATION
            ClinicalState.ACTIVE_EVALUATION -> ClinicalState.DIAGNOSED
            ClinicalState.DIAGNOSED -> ClinicalState.UNDER_TREATMENT
            ClinicalState.UNDER_TREATMENT -> ClinicalState.STABLE
            ClinicalState.STABLE -> ClinicalState.STABLE
        }
    }
}
