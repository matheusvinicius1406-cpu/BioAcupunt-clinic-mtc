package com.bioacupunt.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

/**
 * Reinício limpo do app após restaurar um backup.
 *
 * Depois de trocar os arquivos do banco por baixo de uma conexão Room aberta, a única
 * forma segura de abrir o banco restaurado é o processo recomeçar do zero. Relança a
 * activity de entrada e encerra o processo atual — na volta, o Room abre os bytes novos.
 */
object AppRestarter {
    fun restart(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        if (intent != null) context.startActivity(intent)
        if (context is Activity) context.finish()
        // Garante que nenhuma conexão/estado antigo sobreviva ao restore.
        exitProcess(0)
    }
}
