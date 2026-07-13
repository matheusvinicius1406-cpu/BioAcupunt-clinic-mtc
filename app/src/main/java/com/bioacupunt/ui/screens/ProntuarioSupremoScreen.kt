package com.bioacupunt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bioacupunt.prontuario.domain.model.BaGangDepth
import com.bioacupunt.prontuario.domain.model.BaGangPolarity
import com.bioacupunt.prontuario.domain.model.BaGangStrength
import com.bioacupunt.prontuario.domain.model.BaGangTemperature
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.Organ
import com.bioacupunt.prontuario.domain.model.PulseDepth
import com.bioacupunt.prontuario.domain.model.PulsePosition
import com.bioacupunt.prontuario.domain.model.PulseQuality
import com.bioacupunt.prontuario.domain.model.PulseReading
import com.bioacupunt.prontuario.domain.model.TongueBodyColor
import com.bioacupunt.prontuario.domain.model.TongueCoatingColor
import com.bioacupunt.prontuario.domain.model.Wrist
import com.bioacupunt.prontuario.domain.model.ZangFuPattern
import com.bioacupunt.prontuario.presentation.SupremoViewModel
import com.bioacupunt.ui.components.ClinicalSafetyPanel
import com.bioacupunt.ui.design.AxisSelector
import com.bioacupunt.ui.design.CompletenessBar
import com.bioacupunt.ui.design.SectionHeader
import com.bioacupunt.ui.design.SelectableChip
import com.bioacupunt.ui.design.SupremoCard
import com.bioacupunt.ui.theme.SemanticError

private enum class SupremoTab(val label: String) {
    SAFETY("Segurança"),
    BAGANG("Ba Gang"),
    ZANGFU("Zang Fu"),
    TONGUE("Língua"),
    PULSE("Pulso"),
}

/**
 * PRONTUÁRIO SUPREMO
 *
 * The Segurança tab is **first**, not last. Ordering is a clinical decision, not a
 * layout one: contraindications must be visible before the practitioner has invested
 * effort in a plan, because a warning shown after the plan is written is a warning
 * that gets argued with instead of obeyed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProntuarioSupremoScreen(
    viewModel: SupremoViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember { SupremoTab.entries }

    Column(modifier.fillMaxSize()) {

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            CompletenessBar(progress = state.completeness)
            state.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, color = SemanticError, style = MaterialTheme.typography.bodySmall)
            }
        }

        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
            tabs.forEachIndexed { index, item ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(item.label) },
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (tabs[tab]) {
                SupremoTab.SAFETY -> {
                    item {
                        SupremoCard {
                            SectionHeader(
                                title = "Triagem de segurança",
                                subtitle = "Verificação determinística. Executada a cada " +
                                    "alteração do prontuário, antes de qualquer sugestão.",
                            )
                            Spacer(Modifier.height(14.dp))
                            ClinicalSafetyPanel(
                                verdict = state.verdict,
                                onOverride = { /* auditado no ViewModel: próxima iteração */ },
                            )
                        }
                    }
                    item {
                        SupremoCard {
                            SectionHeader(
                                title = "Condições clínicas",
                                subtitle = "Marcadas aqui ou herdadas de sessões anteriores.",
                            )
                            Spacer(Modifier.height(14.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ClinicalFlag.entries.forEach { flag ->
                                    val fromHistory = flag in state.standingFlags
                                    SelectableChip(
                                        // A flag inherited from a past session is shown as
                                        // selected and labelled — it is in force whether or
                                        // not it was re-ticked today.
                                        label = if (fromHistory) "${flag.label} ·" else flag.label,
                                        selected = flag in state.effectiveFlags,
                                        onClick = { viewModel.toggleFlag(flag) },
                                        accent = SemanticError,
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "· = registrada em sessão anterior e ainda vigente.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (ClinicalFlag.PREGNANCY in state.effectiveFlags) {
                        item {
                            SupremoCard {
                                SectionHeader(title = "Idade gestacional")
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = state.draft.gestationalWeeks?.toString().orEmpty(),
                                    onValueChange = {
                                        viewModel.updateGestationalWeeks(it.toIntOrNull())
                                    },
                                    label = { Text("Semanas") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                SupremoTab.BAGANG -> item {
                    SupremoCard {
                        SectionHeader(
                            title = "Ba Gang — Oito Princípios",
                            subtitle = "Quatro eixos. Toque de novo para desmarcar.",
                        )
                        Spacer(Modifier.height(16.dp))
                        val bg = state.draft.baGang

                        AxisSelector(
                            label = "Yin / Yang",
                            options = listOf(
                                BaGangPolarity.YIN to "Yin",
                                BaGangPolarity.YANG to "Yang",
                            ),
                            selected = bg.polarity,
                            unsetValue = BaGangPolarity.UNSET,
                            onSelect = { viewModel.updateBaGang(bg.copy(polarity = it)) },
                        )
                        Spacer(Modifier.height(16.dp))
                        AxisSelector(
                            label = "Exterior / Interior",
                            options = listOf(
                                BaGangDepth.EXTERIOR to "Exterior",
                                BaGangDepth.INTERIOR to "Interior",
                            ),
                            selected = bg.depth,
                            unsetValue = BaGangDepth.UNSET,
                            onSelect = { viewModel.updateBaGang(bg.copy(depth = it)) },
                        )
                        Spacer(Modifier.height(16.dp))
                        AxisSelector(
                            label = "Frio / Calor",
                            options = listOf(
                                BaGangTemperature.COLD to "Frio",
                                BaGangTemperature.HEAT to "Calor",
                            ),
                            selected = bg.temperature,
                            unsetValue = BaGangTemperature.UNSET,
                            onSelect = { viewModel.updateBaGang(bg.copy(temperature = it)) },
                        )
                        Spacer(Modifier.height(16.dp))
                        AxisSelector(
                            label = "Deficiência / Excesso",
                            options = listOf(
                                BaGangStrength.DEFICIENCY to "Xu",
                                BaGangStrength.EXCESS to "Shi",
                            ),
                            selected = bg.strength,
                            unsetValue = BaGangStrength.UNSET,
                            onSelect = { viewModel.updateBaGang(bg.copy(strength = it)) },
                        )
                    }
                }

                SupremoTab.ZANGFU -> item {
                    SupremoCard {
                        SectionHeader(
                            title = "Zang Fu",
                            subtitle = "Órgãos implicados no padrão.",
                        )
                        Spacer(Modifier.height(14.dp))
                        val selected = state.draft.patterns.map { it.organ }.toSet()
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Organ.entries.forEach { organ ->
                                SelectableChip(
                                    label = organ.label,
                                    selected = organ in selected,
                                    onClick = {
                                        viewModel.togglePattern(ZangFuPattern(organ = organ))
                                    },
                                )
                            }
                        }
                    }
                }

                SupremoTab.TONGUE -> item {
                    SupremoCard {
                        SectionHeader(title = "Língua")
                        Spacer(Modifier.height(14.dp))
                        val tongue = state.draft.tongue

                        Text(
                            "COR DO CORPO",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TongueBodyColor.entries
                                .filter { it != TongueBodyColor.UNSET }
                                .forEach { color ->
                                    SelectableChip(
                                        label = color.label,
                                        selected = tongue.bodyColor == color,
                                        onClick = {
                                            viewModel.updateTongue(
                                                tongue.copy(
                                                    bodyColor = if (tongue.bodyColor == color) {
                                                        TongueBodyColor.UNSET
                                                    } else {
                                                        color
                                                    },
                                                ),
                                            )
                                        },
                                    )
                                }
                        }

                        Spacer(Modifier.height(18.dp))
                        Text(
                            "SABURRA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TongueCoatingColor.entries
                                .filter { it != TongueCoatingColor.UNSET }
                                .forEach { color ->
                                    SelectableChip(
                                        label = color.label,
                                        selected = tongue.coatingColor == color,
                                        onClick = {
                                            viewModel.updateTongue(
                                                tongue.copy(
                                                    coatingColor = if (tongue.coatingColor == color) {
                                                        TongueCoatingColor.UNSET
                                                    } else {
                                                        color
                                                    },
                                                ),
                                            )
                                        },
                                    )
                                }
                        }
                    }
                }

                SupremoTab.PULSE -> {
                    item {
                        SupremoCard {
                            SectionHeader(
                                title = "Pulso",
                                subtitle = "Cun / Guan / Chi, em três profundidades, " +
                                    "nos dois punhos.",
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.draft.pulse.rateBpm?.toString().orEmpty(),
                                onValueChange = { viewModel.updatePulseRate(it.toIntOrNull()) },
                                label = { Text("Frequência (bpm)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    // 2 wrists x 3 positions = 6 cards. Depth and quality inside each.
                    Wrist.entries.forEach { wrist ->
                        PulsePosition.entries.forEach { position ->
                            item {
                                PulseCard(
                                    wrist = wrist,
                                    position = position,
                                    readings = state.draft.pulse.readings,
                                    onToggle = { depth, quality ->
                                        val current = state.draft.pulse
                                            .at(wrist, position, depth)
                                        val qualities = current?.qualities.orEmpty()
                                        val next = if (quality in qualities) {
                                            qualities - quality
                                        } else {
                                            qualities + quality
                                        }
                                        viewModel.setPulseReading(
                                            PulseReading(wrist, position, depth, next),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PulseCard(
    wrist: Wrist,
    position: PulsePosition,
    readings: List<PulseReading>,
    onToggle: (PulseDepth, PulseQuality) -> Unit,
) {
    SupremoCard {
        SectionHeader(title = "${position.label} · punho ${wrist.label.lowercase()}")
        PulseDepth.entries.forEach { depth ->
            Spacer(Modifier.height(14.dp))
            Text(
                depth.label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val selected = readings
                .firstOrNull {
                    it.wrist == wrist && it.position == position && it.depth == depth
                }
                ?.qualities
                .orEmpty()

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PulseQuality.entries.forEach { quality ->
                    SelectableChip(
                        label = quality.label,
                        selected = quality in selected,
                        onClick = { onToggle(depth, quality) },
                    )
                }
            }
        }
    }
}
