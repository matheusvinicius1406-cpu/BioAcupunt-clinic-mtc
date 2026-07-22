package com.bioacupunt.biblioteca.data.packs

import android.content.Context
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack
import com.bioacupunt.core.util.AppJson
import kotlinx.serialization.decodeFromString

/**
 * PACOTE PCDT — carrega os 1996 itens extraídos de 171 Protocolos Clínicos
 * e Diretrizes Terapêuticas do Ministério da Saúde.
 *
 * O JSON está em assets/ porque tem 6 MB (inviável como Kotlin inline).
 * Usa o serializador kotlinx do app ([AppJson]) — o mesmo que a curadoria
 * usa para importar pacotes manuais.
 *
 * Uso na CuradoriaScreen:
 *   val pcdtPacks = PcdtAssetPack.load(context)
 */
object PcdtAssetPack {

    private const val ASSET_PATH = "packs/pack_pcdt.json"

    /**
     * Carrega os pacotes PCDT do JSON em assets.
     * O JSON é um array com um único pacote contendo source + items.
     * Retorna lista vazia se o arquivo não existir ou falhar a leitura —
     * nunca quebra a inicialização da tela.
     */
    fun load(context: Context): List<LibraryContentPack> = runCatching {
        val json = context.assets.open(ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }
        AppJson.decodeFromString<List<LibraryContentPack>>(json)
    }.getOrDefault(emptyList())
}
