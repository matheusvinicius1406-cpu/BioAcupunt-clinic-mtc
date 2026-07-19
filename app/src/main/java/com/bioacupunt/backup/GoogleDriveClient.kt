package com.bioacupunt.backup

import android.content.Context
import android.content.Intent
import com.bioacupunt.observability.AppLogger
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * LOGIN GOOGLE + BACKUP NO DRIVE DA CONTA.
 *
 * Diferente do backup por seletor (que só escolhe uma pasta), aqui a médica **loga na
 * conta Google** e o backup vai para o Drive dela via API. Escopo mínimo `drive.file`:
 * o app só enxerga os arquivos que ele mesmo criou — nunca o resto do Drive dela.
 *
 * ## PRÉ-REQUISITO QUE SÓ VOCÊ PODE FAZER
 * Isto só autentica depois que existir um **cliente OAuth do tipo Android** no Google
 * Cloud Console, no seu projeto, com:
 *   - o nome do pacote do app (applicationId), e
 *   - a impressão **SHA-1** da chave que assina o APK (debug e/ou release),
 * com a **API do Google Drive** ativada e a tela de consentimento configurada.
 * Sem isso, [signInIntent] abre mas o login falha com DEVELOPER_ERROR (código 10).
 * Nenhum código aqui substitui esse passo — é configuração da sua conta Google.
 */
class GoogleDriveClient(
    private val context: Context,
    private val http: OkHttpClient = OkHttpClient(),
) {
    private val driveScope = Scope(DRIVE_FILE_SCOPE)

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
        GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(): Intent = signInClient.signInIntent

    fun lastAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)?.takeIf { GoogleSignIn.hasPermissions(it, driveScope) }

    fun accountFromResult(data: Intent?): Result<GoogleSignInAccount> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
        check(GoogleSignIn.hasPermissions(account, driveScope)) {
            "Permissão do Drive não concedida."
        }
        account
    }

    suspend fun signOut() = withContext(Dispatchers.IO) { runCatching { signInClient.signOut() } }

    /** Token OAuth com escopo Drive para a conta logada. Bloqueante — sempre em IO. */
    private fun freshToken(account: GoogleSignInAccount): String {
        val androidAccount = account.account ?: error("Conta Google sem Account nativo.")
        return GoogleAuthUtil.getToken(context, androidAccount, "oauth2:$DRIVE_FILE_SCOPE")
    }

    data class DriveFile(val id: String, val name: String, val modifiedTime: String)

    suspend fun uploadBackup(account: GoogleSignInAccount, fileName: String, bytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = freshToken(account)
                val metadata = JSONObject().put("name", fileName).toString()
                val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
                    .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .addPart(bytes.toRequestBody("application/zip".toMediaType()))
                    .build()
                val request = Request.Builder()
                    .url("$UPLOAD_URL?uploadType=multipart&fields=id")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                http.newCall(request).execute().use { res ->
                    val payload = res.body?.string().orEmpty()
                    check(res.isSuccessful) { "Drive recusou o upload (HTTP ${res.code}): $payload" }
                    JSONObject(payload).getString("id")
                }
            }.onFailure { AppLogger.e("GoogleDriveClient", "uploadBackup falhou", it) }
        }

    suspend fun listBackups(account: GoogleSignInAccount): Result<List<DriveFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = freshToken(account)
                val q = "name contains 'bioacupunt-backup' and trashed = false"
                val url = "$FILES_URL?q=${java.net.URLEncoder.encode(q, "UTF-8")}" +
                    "&fields=files(id,name,modifiedTime)&orderBy=modifiedTime desc"
                val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").get().build()
                http.newCall(request).execute().use { res ->
                    val payload = res.body?.string().orEmpty()
                    check(res.isSuccessful) { "Drive recusou a listagem (HTTP ${res.code}): $payload" }
                    val files = JSONObject(payload).optJSONArray("files") ?: return@use emptyList()
                    (0 until files.length()).map { i ->
                        val o = files.getJSONObject(i)
                        DriveFile(o.getString("id"), o.getString("name"), o.optString("modifiedTime"))
                    }
                }
            }.onFailure { AppLogger.e("GoogleDriveClient", "listBackups falhou", it) }
        }

    suspend fun downloadFile(account: GoogleSignInAccount, fileId: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = freshToken(account)
                val request = Request.Builder()
                    .url("$FILES_URL/$fileId?alt=media")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()
                http.newCall(request).execute().use { res ->
                    check(res.isSuccessful) { "Drive recusou o download (HTTP ${res.code})." }
                    res.body?.bytes() ?: error("Resposta vazia do Drive.")
                }
            }.onFailure { AppLogger.e("GoogleDriveClient", "downloadFile falhou", it) }
        }

    companion object {
        // Escopo mínimo: só arquivos criados pelo app. Nunca o Drive inteiro da médica.
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
    }
}
