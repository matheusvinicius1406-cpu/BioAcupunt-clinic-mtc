package com.bioacupunt.core.util

import kotlinx.serialization.json.Json

/**
 * Instância única de [Json] para (de)serialização de propósito geral no app —
 * fila de sync, payloads de repositório, etc.
 *
 * Antes cada chamador criava seu próprio `Json { ... }` com configs ligeiramente
 * diferentes; um lado codificando com `encodeDefaults` e outro decodificando com o
 * default silenciosamente divergia. Uma instância compartilhada garante que o que é
 * escrito é lido de volta com as mesmas regras.
 *
 * NOTA: o `MtcAssessmentMapper` mantém o seu próprio Json de propósito — a política de
 * falha dele (degradar campo corrompido para vazio, nunca crashar o prontuário) é
 * específica do domínio clínico e tem teste protegido. Não unificar com este.
 */
val AppJson: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
