package com.bioacupunt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.SemanticSuccess

/**
 * Renderiza os artigos da biblioteca, que são escritos em markdown completo (headers,
 * negrito, tabelas — ver `MtcKnowledgeBase.kt`) como texto de verdade em vez de despejar
 * os caracteres crus (`#`, `##`, `**`, `| a | b |`) na tela. Sem dependência nova: um
 * parser linha-a-linha propositalmente simples, só para os padrões que o conteúdo do
 * app de fato usa — não é um motor de markdown genérico.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.split("\n")
    Column(modifier = modifier) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.isBlank() -> Spacer(Modifier.height(8.dp))

                line.startsWith("### ") -> Text(
                    line.removePrefix("### ").trim(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )

                line.startsWith("## ") -> Text(
                    line.removePrefix("## ").trim(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )

                line.startsWith("# ") -> Text(
                    line.removePrefix("# ").trim(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                )

                line.trimStart().startsWith("✅ ") -> {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = SemanticSuccess, modifier = Modifier.padding(top = 2.dp))
                        Text(inlineMarkdown(line.trimStart().removePrefix("✅ ")), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                line.trimStart().startsWith("❌ ") -> {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Default.Cancel, null, tint = SemanticError, modifier = Modifier.padding(top = 2.dp))
                        Text(inlineMarkdown(line.trimStart().removePrefix("❌ ")), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                isTableRow(line) -> {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && isTableRow(lines[i])) {
                        tableLines.add(lines[i])
                        i++
                    }
                    i-- // the outer loop's i++ accounts for the last consumed line
                    MarkdownTable(tableLines)
                }

                else -> Text(inlineMarkdown(line), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 1.dp))
            }
            i++
        }
    }
}

private fun isTableRow(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("|") && trimmed.endsWith("|")
}

/** A separator row like `|---|:---:|---|` — never real cell content. */
private fun isTableSeparator(line: String): Boolean =
    line.trim().trim('|').split("|").all { it.trim().all { c -> c == '-' || c == ':' } }

private fun tableCells(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

@Composable
private fun MarkdownTable(rawLines: List<String>) {
    val dataLines = rawLines.filterNot { isTableSeparator(it) }
    if (dataLines.isEmpty()) return
    val header = tableCells(dataLines.first())
    val body = dataLines.drop(1).map { tableCells(it) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            header.forEach { cell ->
                Text(
                    cell,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp, horizontal = 4.dp),
                )
            }
        }
        HorizontalDivider()
        body.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Text(
                        cell,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp, horizontal = 4.dp),
                    )
                }
                // A short row (fewer cells than the header) still fills the remaining columns.
                repeat((header.size - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}

/** `**bold**` → real bold spans. Everything else passes through untouched. */
private fun inlineMarkdown(text: String) = buildAnnotatedString {
    var rest = text
    while (true) {
        val start = rest.indexOf("**")
        if (start == -1) { append(rest); break }
        val end = rest.indexOf("**", start + 2)
        if (end == -1) { append(rest); break }
        append(rest.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(rest.substring(start + 2, end))
        }
        rest = rest.substring(end + 2)
    }
}
