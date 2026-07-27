package com.bioacupunt.pharma.domain.ingestion

import com.bioacupunt.pharma.domain.model.CategoriaRegulatoria
import com.bioacupunt.pharma.domain.model.Medicamento
import kotlinx.serialization.Serializable

/**
 * Schema dos packs gerados por `scripts/pharma/build_anvisa_packs.py` a partir do CSV
 * aberto da ANVISA — mesmo espírito de `LibraryContentPack`
 * (`biblioteca/domain/ingestion/IngestionModels.kt`): um arquivo = um pack, `source`
 * documenta a proveniência. Formato de item diferente (dado de registro, não artigo).
 */
@Serializable
data class AnvisaMedicamentoPack(
    val source: String,
    val items: List<AnvisaMedicamentoItem>,
)

@Serializable
data class AnvisaMedicamentoItem(
    val id: String,
    val nomeComercial: String,
    val principiosAtivos: List<String> = emptyList(),
    val classeTerapeutica: String = "",
    val categoriaRegulatoria: String = "OUTRO",
    val empresaDetentora: String = "",
    val situacaoAtiva: Boolean = true,
)

/** Categoria desconhecida cai em [CategoriaRegulatoria.OUTRO] — nunca derruba o item. */
fun AnvisaMedicamentoItem.toDomain(): Medicamento = Medicamento(
    id = id,
    nomeComercial = nomeComercial,
    principiosAtivos = principiosAtivos,
    classeTerapeutica = classeTerapeutica,
    categoriaRegulatoria = runCatching { CategoriaRegulatoria.valueOf(categoriaRegulatoria) }
        .getOrDefault(CategoriaRegulatoria.OUTRO),
    empresaDetentora = empresaDetentora,
    situacaoAtiva = situacaoAtiva,
)
