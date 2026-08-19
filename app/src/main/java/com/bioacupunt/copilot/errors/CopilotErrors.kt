package com.bioacupunt.copilot.errors

/**
 * §48 ERROR STATES
 *
 * Structured errors for the copilot pipeline.
 * Never hides errors with generic responses.
 */
sealed class CopilotError(
    val message: String,
    val recoverable: Boolean = true,
) {
    data object NoEvidence : CopilotError(
        message = "Nenhuma evidência encontrada na biblioteca para esta pergunta.",
        recoverable = true,
    )

    data object ModelUnavailable : CopilotError(
        message = "Modelo de IA indisponível. Verifique se o modelo foi baixado em Ajustes > IA.",
        recoverable = true,
    )

    data object RetrievalFailed : CopilotError(
        message = "Falha na recuperação de conhecimento.",
        recoverable = true,
    )

    data object ValidationFailed : CopilotError(
        message = "Resposta do modelo não passou na validação de evidência.",
        recoverable = true,
    )

    data object PatientContextUnavailable : CopilotError(
        message = "Contexto do paciente não disponível.",
        recoverable = true,
    )

    data object UnauthorizedContext : CopilotError(
        message = "Acesso não autorizado ao contexto de outro paciente.",
        recoverable = false,
    )

    data object UnsupportedRequest : CopilotError(
        message = "Tipo de requisição não suportado pelo copiloto.",
        recoverable = false,
    )

    data object CloudUnavailable : CopilotError(
        message = "Serviço em nuvem indisponível. Modo offline ativo.",
        recoverable = true,
    )

    data class Unknown(val errorMsg: String) : CopilotError(
        message = errorMsg,
        recoverable = true,
    )
}
