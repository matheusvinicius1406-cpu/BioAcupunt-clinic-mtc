package com.bioacupunt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bioacupunt.core.spellcheck.MtcTermsDictionary
import com.bioacupunt.core.spellcheck.SpellCheckService
import com.bioacupunt.core.spellcheck.tokenizeForSpellCheck
import com.bioacupunt.ui.theme.statusColors
import kotlinx.coroutines.delay

/**
 * `OutlinedTextField` com corretor ortográfico nativo do Android (ver [SpellCheckService]) —
 * não IA, não motor próprio. Sublinha em vermelho as palavras que o serviço de correção do
 * aparelho marca como possível erro (menos as que estão em [MtcTermsDictionary], para não
 * sublinhar jargão de MTC como "Zang Fu"/"moxa"), e mostra um chip por palavra suspeita
 * abaixo do campo — tocar abre as sugestões + "Ignorar".
 *
 * "Ignorar" vale só para esta instância do campo (estado local, não persistido) — cobre a
 * sessão de digitação atual sem precisar de uma tabela nova só pra palavra ignorada; se a
 * mesma palavra continuar incomodando a médica em toda sessão, o lugar certo é crescer
 * [MtcTermsDictionary], não este estado.
 */
@Composable
fun SpellCheckedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    minLines: Int = 1,
) {
    val context = LocalContext.current
    val service = remember(context) { SpellCheckService(context) }
    var misspelled by remember { mutableStateOf<Map<String, SpellCheckService.WordResult>>(emptyMap()) }
    var ignored by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(value, ignored) {
        delay(SPELLCHECK_DEBOUNCE_MS)
        val candidates = tokenizeForSpellCheck(value)
            .map { it.lowercase() }
            .distinct()
            .filterNot { it.length <= 2 || MtcTermsDictionary.isKnownTerm(it) || it in ignored }
        misspelled = if (candidates.isEmpty()) {
            emptyMap()
        } else {
            runCatching { service.check(candidates) }.getOrDefault(emptyMap())
                .filterValues { it.misspelled && it.suggestions.isNotEmpty() }
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            minLines = minLines,
            visualTransformation = { text -> TransformedText(highlightMisspelled(text, misspelled.keys), OffsetMapping.Identity) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (misspelled.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            MisspelledWordChips(
                misspelled = misspelled,
                onAccept = { word, suggestion -> onValueChange(replaceWord(value, word, suggestion)) },
                onIgnore = { word -> ignored = ignored + word },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MisspelledWordChips(
    misspelled: Map<String, SpellCheckService.WordResult>,
    onAccept: (String, String) -> Unit,
    onIgnore: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        misspelled.forEach { (word, result) ->
            MisspelledWordChip(
                word = word,
                suggestions = result.suggestions,
                onAccept = { suggestion -> onAccept(word, suggestion) },
                onIgnore = { onIgnore(word) },
            )
        }
    }
}

@Composable
private fun MisspelledWordChip(
    word: String,
    suggestions: List<String>,
    onAccept: (String) -> Unit,
    onIgnore: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val warning = statusColors().warning
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(word) },
            leadingIcon = { Icon(Icons.Default.Warning, null, modifier = Modifier.size(14.dp), tint = warning) },
            colors = AssistChipDefaults.assistChipColors(labelColor = warning),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(text = { Text(suggestion) }, onClick = { onAccept(suggestion); expanded = false })
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Ignorar") }, onClick = { onIgnore(); expanded = false })
        }
    }
}

private fun highlightMisspelled(text: AnnotatedString, misspelledWords: Set<String>): AnnotatedString {
    if (misspelledWords.isEmpty() || text.text.isBlank()) return text
    val pattern = Regex("\\b(" + misspelledWords.joinToString("|") { Regex.escape(it) } + ")\\b", RegexOption.IGNORE_CASE)
    val builder = AnnotatedString.Builder(text)
    pattern.findAll(text.text).forEach { match ->
        builder.addStyle(
            SpanStyle(textDecoration = TextDecoration.Underline, color = Color(0xFFB3261E)),
            match.range.first,
            match.range.last + 1,
        )
    }
    return builder.toAnnotatedString()
}

private fun replaceWord(text: String, word: String, suggestion: String): String {
    val regex = Regex("\\b" + Regex.escape(word) + "\\b", RegexOption.IGNORE_CASE)
    return regex.replaceFirst(text, suggestion)
}

private const val SPELLCHECK_DEBOUNCE_MS = 600L
