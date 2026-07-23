package com.bioacupunt.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecurePreferences(context: Context) {
    companion object {
        private const val FILE = "bio_secure_prefs"
    }

    private val prefs: SharedPreferences = run {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            FILE,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun edit(scope: (SharedPreferences.Editor) -> Unit) {
        prefs.edit().also(scope).apply()
    }

    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)

    fun putString(key: String, value: String) = edit { it.putString(key, value) }

    var authToken: String
        get() = prefs.getString("auth_token", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("auth_token") } else edit { it.putString("auth_token", value) }

    var refreshToken: String
        get() = prefs.getString("refresh_token", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("refresh_token") } else edit { it.putString("refresh_token", value) }

    var userId: Long
        get() = prefs.getLong("user_id", 0L)
        set(value) = edit { it.putLong("user_id", value) }

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("user_email") } else edit { it.putString("user_email", value) }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = edit { it.putBoolean("is_logged_in", value) }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", false)
        set(value) = edit { it.putBoolean("biometric_enabled", value) }

    var hasOnboarded: Boolean
        get() = prefs.getBoolean("has_onboarded", false)
        set(value) = edit { it.putBoolean("has_onboarded", value) }

    var biometricPassword: String
        get() = prefs.getString("biometric_password", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("biometric_password") } else edit { it.putString("biometric_password", value) }

    // ── Login offline (PIN local) ────────────────────────────────────────
    // Só o sal e o hash PBKDF2 são guardados; o PIN em si nunca toca o disco.
    var pinSalt: String
        get() = prefs.getString("pin_salt", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("pin_salt") } else edit { it.putString("pin_salt", value) }

    var pinHash: String
        get() = prefs.getString("pin_hash", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("pin_hash") } else edit { it.putString("pin_hash", value) }

    val hasLocalPin: Boolean get() = pinSalt.isNotBlank() && pinHash.isNotBlank()

    var googleDriveLinked: Boolean
        get() = prefs.getBoolean("google_drive_linked", false)
        set(value) = edit { it.putBoolean("google_drive_linked", value) }

    var geminiApiKey: String?
        get() = prefs.getString("gemini_api_key", null)
        set(value) = if (value.isNullOrBlank()) edit { it.remove("gemini_api_key") } else edit { it.putString("gemini_api_key", value) }

    /** URL de onde baixar o modelo local (.task do Gemma). Vazio = usa o padrão do código. */
    var localModelUrl: String
        get() = prefs.getString("local_model_url", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("local_model_url") } else edit { it.putString("local_model_url", value) }

    var currentTenantId: Long?
        get() = prefs.getLong("current_tenant_id", -1L).takeIf { it != -1L }
        set(value) = if (value == null || value <= 0) edit { it.remove("current_tenant_id") } else edit { it.putLong("current_tenant_id", value) }

    /** Base URL of the backend the app talks to (e.g. the deployed HTTPS URL). */
    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("server_url") } else edit { it.putString("server_url", value) }

    // ── Ajustes: identidade profissional, TCLE, preços, técnicas ──────────
    var professionalName: String
        get() = prefs.getString("professional_name", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("professional_name") } else edit { it.putString("professional_name", value) }

    var professionalSpecialty: String
        get() = prefs.getString("professional_specialty", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("professional_specialty") } else edit { it.putString("professional_specialty", value) }

    var professionalRegistration: String
        get() = prefs.getString("professional_registration", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("professional_registration") } else edit { it.putString("professional_registration", value) }

    var tcleText: String
        get() = prefs.getString("tcle_text", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("tcle_text") } else edit { it.putString("tcle_text", value) }

    var sessionPriceBrl: String
        get() = prefs.getString("session_price_brl", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("session_price_brl") } else edit { it.putString("session_price_brl", value) }

    var firstConsultPriceBrl: String
        get() = prefs.getString("first_consult_price_brl", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("first_consult_price_brl") } else edit { it.putString("first_consult_price_brl", value) }

    var darkModeEnabled: Boolean
        get() = prefs.getBoolean("dark_mode_enabled", false)
        set(value) = edit { it.putBoolean("dark_mode_enabled", value) }

    /** CSV of [com.bioacupunt.prontuario.domain.safety.Technique] names the practitioner
     * has enabled. Blank means "all enabled" (the default, before she's touched this). */
    var enabledTechniquesCsv: String
        get() = prefs.getString("enabled_techniques", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("enabled_techniques") } else edit { it.putString("enabled_techniques", value) }

    // ── Perfil profissional (contato + apresentação) ─────────────────────────
    // Antes estes campos viviam só como estado de Compose na tela de Ajustes e
    // sumiam ao trocar de aba. Agora persistem como o resto do perfil.
    var professionalPhone: String
        get() = prefs.getString("professional_phone", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("professional_phone") } else edit { it.putString("professional_phone", value) }

    var professionalEmail: String
        get() = prefs.getString("professional_email", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("professional_email") } else edit { it.putString("professional_email", value) }

    var professionalBio: String
        get() = prefs.getString("professional_bio", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("professional_bio") } else edit { it.putString("professional_bio", value) }

    /** URI (content://) da logo/foto escolhida. Requer persistable permission ao gravar. */
    var professionalLogoUri: String
        get() = prefs.getString("professional_logo_uri", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("professional_logo_uri") } else edit { it.putString("professional_logo_uri", value) }

    // ── Clínica (identidade + horário de funcionamento) ──────────────────────
    var clinicName: String
        get() = prefs.getString("clinic_name", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("clinic_name") } else edit { it.putString("clinic_name", value) }

    var clinicWorkStart: String
        get() = prefs.getString("clinic_work_start", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("clinic_work_start") } else edit { it.putString("clinic_work_start", value) }

    var clinicWorkEnd: String
        get() = prefs.getString("clinic_work_end", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("clinic_work_end") } else edit { it.putString("clinic_work_end", value) }

    /** CSV dos dias de atendimento (ex.: "SEG,TER,QUA,QUI,SEX"). */
    var clinicWorkDaysCsv: String
        get() = prefs.getString("clinic_work_days", "") ?: ""
        set(value) = if (value.isBlank()) edit { it.remove("clinic_work_days") } else edit { it.putString("clinic_work_days", value) }

    fun clearAll() {
        edit { it.clear() }
    }
}
