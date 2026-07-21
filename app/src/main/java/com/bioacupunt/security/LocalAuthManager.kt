package com.bioacupunt.security

/**
 * Gate de login **offline** do app. Não fala com backend nenhum: a médica entra com o
 * PIN local (ou biometria) mesmo sem internet. O PIN vive só como hash em
 * [SecurePreferences]; aqui ficam as transições (definir, verificar, trocar, limpar).
 */
class LocalAuthManager(private val prefs: SecurePreferences) {

    fun hasPin(): Boolean = prefs.hasLocalPin

    /** Define/redefine o PIN. Gera sal novo — trocar de PIN invalida o hash antigo. */
    fun setPin(pin: String): Boolean {
        if (!LocalPinAuth.isValidPin(pin)) return false
        val salt = LocalPinAuth.newSaltHex()
        prefs.pinSalt = salt
        prefs.pinHash = LocalPinAuth.hash(pin, salt)
        return true
    }

    fun verifyPin(pin: String): Boolean = LocalPinAuth.verify(pin, prefs.pinSalt, prefs.pinHash)

    fun clearPin() {
        prefs.pinSalt = ""
        prefs.pinHash = ""
    }

    var biometricEnabled: Boolean
        get() = prefs.biometricEnabled
        set(value) { prefs.biometricEnabled = value }
}
