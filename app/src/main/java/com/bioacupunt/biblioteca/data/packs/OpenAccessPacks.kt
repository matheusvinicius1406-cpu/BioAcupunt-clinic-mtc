package com.bioacupunt.biblioteca.data.packs

import android.content.Context
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack
import com.bioacupunt.core.util.AppJson
import kotlinx.serialization.decodeFromString

/**
 * PACOTES DE ACESSO ABERTO — carrega os pacotes gerados a partir de fontes públicas
 * (ex.: PubMed, diretrizes da OMS) em `assets/packs/open_access/` (arquivos .json).
 *
 * Mesmo padrão de [PcdtAssetPack]: um JSON por arquivo, mesmo schema de
 * [LibraryContentPack], mesmo serializador ([AppJson]) que a curadoria já usa para
 * importar pacotes manuais. A diferença é que aqui cada arquivo é UM pacote (não uma
 * lista de pacotes) e o diretório é listado dinamicamente — o conteúdo é gerado por um
 * pipeline separado, então o app não pode depender de nomes de arquivo fixos.
 *
 * Degrada com segurança: um arquivo individual malformado é pulado (não derruba os
 * outros nem a tela — mesmo espírito de "codec degrada, não quebra"). Se o diretório
 * ainda não existir (pipeline não rodou), retorna lista vazia sem erro.
 *
 * Uso na CuradoriaScreen:
 *   val openAccessPacks = OpenAccessPacks.load(context)
 */
object OpenAccessPacks {

    private const val ASSET_DIR = "packs/open_access"

    /**
     * Carrega todos os pacotes `.json` de [ASSET_DIR]. Cada arquivo é decodificado
     * isoladamente — um pacote ruim é ignorado, os demais seguem carregando.
     */
    fun load(context: Context): List<LibraryContentPack> {
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
                    AppJson.decodeFromString<LibraryContentPack>(json)
                }.getOrNull()
            }
    }
}
