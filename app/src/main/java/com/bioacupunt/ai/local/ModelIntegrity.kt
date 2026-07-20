package com.bioacupunt.ai.local

import com.bioacupunt.core.util.toHex
import java.io.File
import java.security.MessageDigest

/**
 * Integrity verification for downloaded model files.
 *
 * ## Why this is not paranoia
 *
 * A model file is not data — it is *executed* by the inference runtime. A corrupted or
 * substituted `.litertlm` is, at best, a crash in the middle of a consultation and, at
 * worst, an attacker-controlled input to a native C++ runtime. The app downloads
 * multiple gigabytes over whatever network the clinic happens to have, from a URL that
 * could be MITM'd or simply misconfigured, and then hands the bytes straight to native
 * code.
 *
 * So the rule is: **a model file is not trusted until its SHA-256 matches a hash pinned
 * in the source code.** Size is checked first because it is nearly free and catches the
 * common case (truncated download, HTML error page saved as a `.task`), and hashing is
 * only done on files that pass.
 *
 * The verifier **fails closed**: an unpinned model, a size mismatch, or a hash mismatch
 * all mean "do not load". There is no "warn and continue" path, because a warning the
 * doctor taps past is not a security control.
 */
object ModelIntegrity {

    sealed interface Result {
        data object Valid : Result
        data object Missing : Result
        data class SizeMismatch(val expected: Long, val actual: Long) : Result
        data class HashMismatch(val expected: String, val actual: String) : Result
        data class NotPinned(val modelId: String) : Result
    }

    /** Streamed so a 4 GB model never lands in the heap. */
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    fun verify(file: File, model: LocalModel): Result {
        if (!model.isVerifiable) return Result.NotPinned(model.id)
        if (!file.exists() || file.length() == 0L) return Result.Missing

        val actualSize = file.length()
        if (model.sizeBytes > 0 && actualSize != model.sizeBytes) {
            return Result.SizeMismatch(expected = model.sizeBytes, actual = actualSize)
        }

        val actualHash = sha256(file)
        if (!actualHash.equals(model.sha256, ignoreCase = true)) {
            return Result.HashMismatch(expected = model.sha256, actual = actualHash)
        }
        return Result.Valid
    }

    /** The single question the rest of the app is allowed to ask. */
    fun isTrusted(file: File, model: LocalModel): Boolean = verify(file, model) is Result.Valid
}
