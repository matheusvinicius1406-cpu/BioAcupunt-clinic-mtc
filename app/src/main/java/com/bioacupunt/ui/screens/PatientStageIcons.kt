package com.bioacupunt.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import com.bioacupunt.crm.domain.model.PatientStage

/** One real icon per CRM stage — replaces the emoji field the domain model used to carry. */
fun PatientStage.icon(): ImageVector = when (this) {
    PatientStage.FIRST_CONTACT -> Icons.AutoMirrored.Filled.Chat
    PatientStage.LEAD -> Icons.Default.PersonSearch
    PatientStage.ACTIVE -> Icons.Default.CheckCircle
    PatientStage.TREATMENT -> Icons.Default.LocalHospital
    PatientStage.MAINTENANCE -> Icons.Default.Verified
    PatientStage.INACTIVE -> Icons.Default.PauseCircle
    PatientStage.CHURNED -> Icons.Default.PersonOff
}
