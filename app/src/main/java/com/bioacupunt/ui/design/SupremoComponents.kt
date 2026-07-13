package com.bioacupunt.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.OnPrimary
import com.bioacupunt.ui.theme.Outline
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.Surface as SupremoSurface

/**
 * BIOACUPUNT DESIGN SYSTEM — "Supremo"
 *
 * These are the primitives every clinical screen composes from. They exist so the
 * app stops being a pile of one-off `Box(Modifier.background(...))` calls that drift
 * apart screen by screen — which is what makes software look amateur to a
 * professional user even when the logic underneath is sound.
 *
 * Two rules the components enforce, both aimed at a doctor working fast between
 * patients:
 *
 *  1. **A choice is always visibly made or visibly not made.** [SelectableChip] has a
 *     distinct unselected state with a real border, never a ghost. On a tongue or
 *     pulse form, "I didn't record this" and "I recorded normal" must never look
 *     alike — that ambiguity is a charting error waiting to happen.
 *  2. **Touch targets stay ≥ 44dp.** Charting happens one-handed, standing, in a
 *     hurry. Small targets produce mis-taps, and a mis-tap on a clinical form is a
 *     wrong record.
 */

// ---------------------------------------------------------------------------
// Surfaces
// ---------------------------------------------------------------------------

/** The standard content container. One elevation, one radius, everywhere. */
@Composable
fun SupremoCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SupremoSurface)
            .border(1.dp, Outline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content,
    )
}

/**
 * Section heading. Serif, with a short gold rule under it — the one deliberate
 * flourish in the system, borrowed from clinical/editorial typesetting. It signals
 * "this is a document about a person", not "this is a CRUD form".
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(width = 36.dp, height = 2.dp)
                .background(
                    Brush.horizontalGradient(listOf(Accent, Accent.copy(alpha = 0f))),
                ),
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Selection
// ---------------------------------------------------------------------------

/**
 * The workhorse of the whole chart: Ba Gang, tongue, pulse and flags are all
 * chip grids. Unselected is a real outlined state, never a ghost — see rule 1.
 */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Primary,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent else Color.Transparent,
        label = "chipBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) accent else Outline,
        label = "chipBorder",
    )
    val fg = if (selected) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .heightIn44()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = fg,
            textAlign = TextAlign.Center,
        )
    }
}

/** Enforces rule 2 in one place, so no screen can quietly opt out of it. */
private fun Modifier.heightIn44(): Modifier = this.height(44.dp)

/**
 * A binary axis with an explicit "not recorded" middle — the shape Ba Gang actually
 * has. Modelling it as two independent toggles would let the practitioner select
 * both Cold *and* Heat, which is not a valid Ba Gang reading. The UI should make
 * invalid clinical states unrepresentable, not merely discouraged.
 */
@Composable
fun <T> AxisSelector(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    unsetValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selected == unsetValue) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "não registrado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                SelectableChip(
                    label = text,
                    selected = selected == value,
                    // Tapping the selected option clears it: recording is reversible
                    // without hunting for a "clear" button.
                    onClick = { onSelect(if (selected == value) unsetValue else value) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Progress
// ---------------------------------------------------------------------------

/**
 * Chart completeness. Deliberately *informational*, never blocking: a chart that
 * refuses to save until every field is filled teaches the practitioner to enter junk
 * to get past it, which is worse than an honestly incomplete record.
 */
@Composable
fun CompletenessBar(progress: Float, modifier: Modifier = Modifier) {
    val pct = (progress.coerceIn(0f, 1f) * 100).toInt()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "PRONTUÁRIO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$pct%",
                style = MaterialTheme.typography.labelSmall,
                color = if (pct >= 80) Primary else Accent,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Outline),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(Accent, Primary))),
            )
        }
    }
}
