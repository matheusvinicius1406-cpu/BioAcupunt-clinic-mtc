package com.bioacupunt.crm.presentation

import androidx.compose.ui.graphics.Color
import com.bioacupunt.crm.domain.model.PatientStage
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.PrimaryDark
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.SemanticInfo
import com.bioacupunt.ui.theme.SemanticSuccess
import com.bioacupunt.ui.theme.TextMuted

/** Single source of truth for "what colour represents this pipeline stage" — used by
 * the Dashboard Kanban, the Patients list avatars and any other stage badge. */
val PatientStage.uiColor: Color
    get() = when (this) {
        PatientStage.FIRST_CONTACT -> SemanticInfo
        PatientStage.LEAD -> Accent
        PatientStage.ACTIVE -> Primary
        PatientStage.TREATMENT -> PrimaryDark
        PatientStage.MAINTENANCE -> SemanticSuccess
        PatientStage.INACTIVE -> TextMuted
        PatientStage.CHURNED -> SemanticError
    }
