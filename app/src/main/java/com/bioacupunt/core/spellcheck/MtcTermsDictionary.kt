package com.bioacupunt.core.spellcheck

/**
 * Termos que o corretor NUNCA deve marcar como erro — jargão de MTC, pinyin, siglas
 * clínicas comuns no app e códigos de ponto de acupuntura. Sem isto, o corretor ortográfico
 * padrão (afinado para português comum) sublinharia "Zang Fu", "moxa" e "LI4" como erro em
 * praticamente toda nota clínica — ruído que ensinaria a médica a ignorar o corretor
 * inteiro, não só os falsos positivos.
 *
 * Lista fechada e pequena de propósito — cresce conforme aparecer falso-positivo real, não
 * tentando prever o dicionário inteiro de MTC de uma vez (mesmo YAGNI do resto do projeto).
 */
object MtcTermsDictionary {
    private val terms: Set<String> = setOf(
        // Jargão/técnicas de MTC
        "zang", "fu", "qi", "xue", "jing", "shen", "yin", "yang", "wei",
        "moxa", "moxabustão", "moxibustão", "auriculoterapia", "eletroacupuntura",
        "pinyin", "meridiano", "meridianos", "cun", "bagang", "guasha", "tuina", "shiatsu",
        "qigong", "dantian", "sanjiao",
        // Órgãos Zang Fu em pinyin comuns além dos já listados acima
        "pi", "gan", "dan", "xin", "fei", "pang", "guang",
        // Siglas clínicas/administrativas comuns no app
        "mtc", "cid", "anvisa", "lgpd", "tcle", "crm",
    )

    fun isKnownTerm(word: String): Boolean {
        val normalized = word.lowercase().trim('.', ',', ';', ':', '!', '?', '(', ')')
        if (normalized.isEmpty()) return true
        if (normalized in terms) return true
        if (isAcupuncturePointCode(normalized)) return true
        return false
    }

    /** Código de ponto de acupuntura: 1-3 letras (meridiano) + número (ex.: li4, st36, bl60, gv20). */
    private fun isAcupuncturePointCode(word: String): Boolean =
        POINT_CODE.matches(word)

    private val POINT_CODE = Regex("^[a-z]{1,3}[0-9]{1,3}$")
}
