package com.bioacupunt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bioacupunt.prontuario.domain.safety.SafetyFinding
import com.bioacupunt.prontuario.domain.safety.SafetyVerdict
import com.bioacupunt.prontuario.domain.safety.Severity
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.SemanticInfo
import com.bioacupunt.ui.theme.SemanticSuccess
import com.bioacupunt.ui.theme.SemanticWarning

/**
 * Renders a [SafetyVerdict] above any proposed protocol.
 *
 * UX contract, deliberately chosen:
 *
 *  - A FORBIDDEN finding is **loud and not dismissible**. It cannot be collapsed
 *    away, because a contraindication the practitioner scrolled past is worse than
 *    no software at all.
 *  - Override is possible but **deliberately effortful and audited** — a licensed
 *    professional must be able to act on their own judgement (the software is
 *    decision *support*), but never by accident. The reason is required.
 *  - A clear verdict is shown too. Silence is ambiguous: "no alert" and "not
 *    checked" must never look the same to the doctor.
 */
@Composable
fun ClinicalSafetyPanel(
    verdict: SafetyVerdict,
    modifier: Modifier = Modifier,
    onOverride: ((reason: String) -> Unit)? = null,
) {
    if (verdict.isClear) {
        SafetyBanner(
            icon = Icons.Default.CheckCircle,
            tint = SemanticSuccess,
            title = "Sem contraindicações detectadas",
            subtitle = "Triagem automática executada para este protocolo.",
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (verdict.isBlocked) {
            SafetyBanner(
                icon = Icons.Default.Block,
                tint = SemanticError,
                title = "Protocolo bloqueado — ${verdict.blocking.size} contraindicação(ões)",
                subtitle = "Este protocolo não será sugerido automaticamente.",
            )
        }

        verdict.findings.forEach { FindingCard(it) }

        if (verdict.isBlocked && onOverride != null) {
            OverrideAction(onOverride = onOverride)
        }
    }
}

@Composable
private fun FindingCard(finding: SafetyFinding) {
    val tint = when (finding.severity) {
        Severity.FORBIDDEN -> SemanticError
        Severity.CAUTION -> SemanticWarning
        Severity.INFO -> SemanticInfo
    }
    val icon = when (finding.severity) {
        Severity.FORBIDDEN -> Icons.Default.Block
        Severity.CAUTION -> Icons.Default.Warning
        Severity.INFO -> Icons.Default.Info
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = finding.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = finding.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = finding.flag.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun SafetyBanner(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Override is intentionally a two-step action with a mandatory justification.
 * The friction is the point: it keeps clinical authority with the professional
 * while making the decision explicit and auditable (LGPD/CFM-friendly).
 */
@Composable
private fun OverrideAction(onOverride: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = if (expanded) "Cancelar" else "Assumir responsabilidade e prosseguir",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Text(
                    text = "Registre a justificativa clínica. Ela ficará no prontuário, " +
                        "vinculada ao seu usuário e ao horário.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Justificativa clínica (obrigatória)") },
                    minLines = 2,
                )
                TextButton(
                    onClick = { onOverride(reason.trim()) },
                    enabled = reason.trim().length >= 10,
                ) {
                    Text("Confirmar e registrar", color = SemanticError)
                }
            }
        }
    }
}
