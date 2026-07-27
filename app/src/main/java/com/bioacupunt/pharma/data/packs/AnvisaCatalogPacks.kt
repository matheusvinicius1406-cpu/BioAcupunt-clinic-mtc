package com.bioacupunt.pharma.data.packs

import android.content.Context
import com.bioacupunt.core.util.AppJson
import com.bioacupunt.pharma.domain.ingestion.AnvisaMedicamentoPack
import kotlinx.serialization.decodeFromString

/**
 * Carrega os packs do catálogo ANVISA em `assets/packs/pharma_anvisa/` (arquivos .json) — clone
 * exato do padrão de [com.bioacupunt.biblioteca.data.packs.OpenAccessPacks]: um arquivo
 * por pack, decodificado isoladamente (arquivo malformado é pulado, não derruba os
 * outros nem o boot do app), diretório listado dinamicamente (nunca nomes fixos, o
 * pipeline pode gerar qualquer quantidade de batches).
 */
object AnvisaCatalogPacks {

    private const val ASSET_DIR = "packs/pharma_anvisa"

    fun load(context: Context): List<AnvisaMedicamentoPack> {
        val fileNames = runCatching {
            context.assets.list(ASSET_DIR)?.toList() ?: emptyList()
        }.getOrDefault(emptyList())

        return fileNames
            .filter { it.endsWith(".json", ignoreCase = true) }
            .mapNotNull { fileName ->
                runCatching {
                    val json = context.assets.open("$ASSET_DIR/$fileName")
                        .bufferedReader()
                        .use { it.readText() }
                    AppJson.decodeFromString<AnvisaMedicamentoPack>(json)
                }.getOrNull()
            }
    }
}
