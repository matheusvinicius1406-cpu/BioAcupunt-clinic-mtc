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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.bioacupunt.pharma.domain.safety.PharmaFinding
import com.bioacupunt.pharma.domain.safety.PharmaSeverity
import com.bioacupunt.pharma.domain.safety.PharmaVerdict
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.SemanticInfo
import com.bioacupunt.ui.theme.SemanticSuccess
import com.bioacupunt.ui.theme.SemanticWarning
import com.bioacupunt.ui.theme.TextMuted

/**
 * Renders a [PharmaVerdict] above a proposed prescription — mesmo contrato de UX do
 * [ClinicalSafetyPanel], clonado (não generalizado por herança) porque os dois motores
 * são propositalmente independentes.
 *
 * Três estados são visualmente distintos, nunca ambíguos entre si:
 *  - **Não verificado** ([PharmaVerdict.verified] falso): nem o motor bloqueou, nem
 *    liberou — não há curadoria clínica pra este item ainda. Cor neutra/informativa,
 *    nunca a mesma cor de "sem contraindicações".
 *  - **Bloqueado**: FORBIDDEN, alto e não dispensável, override exige justificativa
 *    ≥10 caracteres.
 *  - **Claro**: verificado E sem findings além de informativos.
 */
@Composable
fun PharmaSafetyPanel(
    verdict: PharmaVerdict,
    modifier: Modifier = Modifier,
    onOverride: ((reason: String) -> Unit)? = null,
) {
    if (!verdict.verified) {
        SafetyBanner(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            tint = TextMuted,
            title = "Não verificado clinicamente",
            subtitle = "Apenas dado de registro ANVISA — sem posologia/interação/contraindicação curada.",
            modifier = modifier,
        )
        return
    }

    if (verdict.isClear) {
        SafetyBanner(
            icon = Icons.Default.CheckCircle,
            tint = SemanticSuccess,
            title = "Sem contraindicações detectadas",
            subtitle = "Triagem automática executada contra o prontuário desta paciente.",
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
                title = "Prescrição bloqueada — ${verdict.blocking.size} contraindicação(ões)",
                subtitle = "Esta prescrição não será confirmada sem justificativa.",
            )
        }

        verdict.findings.forEach { PharmaFindingCard(it) }

        if (verdict.isBlocked && onOverride != null) {
            PharmaOverrideAction(onOverride = onOverride)
        }
    }
}

@Composable
private fun PharmaFindingCard(finding: PharmaFinding) {
    val tint = when (finding.severity) {
        PharmaSeverity.FORBIDDEN -> SemanticError
        PharmaSeverity.CAUTION -> SemanticWarning
        PharmaSeverity.INFO -> SemanticInfo
    }
    val icon = when (finding.severity) {
        PharmaSeverity.FORBIDDEN -> Icons.Default.Block
        PharmaSeverity.CAUTION -> Icons.Default.Warning
        PharmaSeverity.INFO -> Icons.Default.Info
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

/** Mesmo padrão de fricção deliberada de [ClinicalSafetyPanel] — override exige justificativa ≥10 chars. */
@Composable
private fun PharmaOverrideAction(onOverride: (String) -> Unit) {
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
                    text = "Registre a justificativa clínica. Ela ficará vinculada à prescrição, " +
                        "ao seu usuário e ao horário.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
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
