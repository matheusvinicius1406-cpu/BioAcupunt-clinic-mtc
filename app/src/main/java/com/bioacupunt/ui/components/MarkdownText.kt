package com.bioacupunt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RENDERIZADOR DE MARKDOWN LEVE, 100% offline
 *
 * O conteúdo da biblioteca é escrito em Markdown (`#`, `**`, listas, tabelas, blocos
 * de código). Antes ele era jogado num `Text` cru — então `**negrito**`, `# título` e
 * `| tabelas |` apareciam literais na tela da médica. Este renderizador interpreta o
 * subconjunto de Markdown que os artigos usam, sem nenhuma dependência de rede/CDN.
 *
 * Filosofia de falha, igual ao resto do app clínico: **degrada, não quebra**. Qualquer
 * linha que não casar com um padrão conhecido é mostrada como parágrafo de texto puro —
 * nunca some, nunca crasha. Pior mostrar um `*` solto do que esconder uma contraindicação.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block -> MarkdownBlock(block) }
    }
}

// ── Modelo de blocos ────────────────────────────────────────────────────────

internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val items: List<String>) : MdBlock
    data class Numbered(val items: List<String>) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Table(val rows: List<List<String>>) : MdBlock
}

// ── Parsing (linha a linha, agrupando) ──────────────────────────────────────

internal fun parseMarkdown(md: String): List<MdBlock> {
    val lines = md.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MdBlock>()
    var i = 0

    val paragraph = StringBuilder()
    fun flushParagraph() {
        val text = paragraph.toString().trim()
        if (text.isNotEmpty()) blocks.add(MdBlock.Paragraph(text))
        paragraph.setLength(0)
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        when {
            // Bloco de código cercado ```
            trimmed.startsWith("```") -> {
                flushParagraph()
                val sb = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    sb.appendLine(lines[i]); i++
                }
                blocks.add(MdBlock.Code(sb.toString().trimEnd('\n')))
            }

            // Título # ## ###
            Regex("^#{1,6}\\s+").containsMatchIn(trimmed) -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length
                blocks.add(MdBlock.Heading(level, trimmed.dropWhile { it == '#' }.trim()))
            }

            // Tabela: linha começando com | e a próxima sendo o separador |---|
            trimmed.startsWith("|") && i + 1 < lines.size &&
                lines[i + 1].trim().matches(Regex("^\\|?[\\s:|-]+\\|?$")) &&
                lines[i + 1].contains("-") -> {
                flushParagraph()
                val rows = mutableListOf<List<String>>()
                rows.add(splitTableRow(trimmed))
                i++ // pula a linha de dados de cabeçalho, agora consumir separador
                i++ // pula o separador
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    rows.add(splitTableRow(lines[i].trim())); i++
                }
                i-- // compensa o i++ do fim do while externo
                blocks.add(MdBlock.Table(rows))
            }

            // Lista com marcador - ou *
            Regex("^[-*]\\s+").containsMatchIn(trimmed) -> {
                flushParagraph()
                val items = mutableListOf<String>()
                while (i < lines.size && Regex("^[-*]\\s+").containsMatchIn(lines[i].trim())) {
                    items.add(lines[i].trim().replaceFirst(Regex("^[-*]\\s+"), "")); i++
                }
                i--
                blocks.add(MdBlock.Bullet(items))
            }

            // Lista numerada 1. 2.
            Regex("^\\d+\\.\\s+").containsMatchIn(trimmed) -> {
                flushParagraph()
                val items = mutableListOf<String>()
                while (i < lines.size && Regex("^\\d+\\.\\s+").containsMatchIn(lines[i].trim())) {
                    items.add(lines[i].trim().replaceFirst(Regex("^\\d+\\.\\s+"), "")); i++
                }
                i--
                blocks.add(MdBlock.Numbered(items))
            }

            // Linha em branco separa parágrafos
            trimmed.isEmpty() -> flushParagraph()

            // Texto comum: acumula no parágrafo corrente
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed)
            }
        }
        i++
    }
    flushParagraph()
    return blocks
}

private fun splitTableRow(row: String): List<String> =
    row.trim().trim('|').split("|").map { it.trim() }

// ── Inline: **negrito**, *itálico*, `código` ────────────────────────────────

internal fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val rest = text.substring(i)
        when {
            rest.startsWith("**") -> {
                val end = rest.indexOf("**", startIndex = 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(rest.substring(2, end)) }
                    i += end + 2
                } else { append("**"); i += 2 }
            }
            rest.startsWith("`") -> {
                val end = rest.indexOf("`", startIndex = 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)) { append(rest.substring(1, end)) }
                    i += end + 1
                } else { append("`"); i += 1 }
            }
            rest.startsWith("*") -> {
                val end = rest.indexOf("*", startIndex = 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(rest.substring(1, end)) }
                    i += end + 1
                } else { append("*"); i += 1 }
            }
            else -> { append(text[i]); i += 1 }
        }
    }
}

// ── Render ──────────────────────────────────────────────────────────────────

@Composable
private fun MarkdownBlock(block: MdBlock) {
    when (block) {
        is MdBlock.Heading -> Text(
            parseInline(block.text),
            style = when (block.level) {
                1 -> MaterialTheme.typography.titleLarge
                2 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        is MdBlock.Paragraph -> Text(
            parseInline(block.text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        is MdBlock.Bullet -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            block.items.forEach { item ->
                Row {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text(parseInline(item), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        is MdBlock.Numbered -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            block.items.forEachIndexed { idx, item ->
                Row {
                    Text("${idx + 1}.  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text(parseInline(item), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        is MdBlock.Code -> Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(
                block.text,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is MdBlock.Table -> Row(
            Modifier.horizontalScroll(rememberScrollState())
        ) {
            Column {
                block.rows.forEachIndexed { rIdx, row ->
                    val header = rIdx == 0 // a 1ª linha da tabela é sempre o cabeçalho
                    Row {
                        row.forEach { cell ->
                            Text(
                                parseInline(cell),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
