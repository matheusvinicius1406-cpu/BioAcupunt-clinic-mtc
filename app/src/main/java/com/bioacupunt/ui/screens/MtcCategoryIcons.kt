package com.bioacupunt.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector
import com.bioacupunt.biblioteca.domain.model.MtcCategory

/** One real icon per category — replaces the emoji field the domain model used to carry. */
fun MtcCategory.icon(): ImageVector = when (this) {
    MtcCategory.MERIDIANOS -> Icons.Default.Route
    MtcCategory.PONTOS -> Icons.Default.GpsFixed
    MtcCategory.CINCO_ELEMENTOS -> Icons.Default.LocalFlorist
    MtcCategory.BA_GANG -> Icons.Default.Balance
    MtcCategory.SINDROME_ORGAOS -> Icons.Default.MonitorHeart
    MtcCategory.LINGUA -> Icons.Default.RecordVoiceOver
    MtcCategory.PULSO -> Icons.Default.Favorite
    MtcCategory.TECNICAS -> Icons.Default.Construction
    MtcCategory.FITOTERAPIA -> Icons.Default.LocalFlorist
    MtcCategory.MOXIBUSTAO -> Icons.Default.LocalFireDepartment
    MtcCategory.DIETOTERAPIA -> Icons.Default.Restaurant
    MtcCategory.QIGONG -> Icons.Default.SelfImprovement
    MtcCategory.CLINICA_MEDICA -> Icons.Default.LocalHospital
}
