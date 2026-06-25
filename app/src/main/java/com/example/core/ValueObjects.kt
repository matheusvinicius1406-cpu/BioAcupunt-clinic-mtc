package com.example.core

data class PatientId(val value: String) {
    init {
        require(value.isNotBlank()) { "PatientId cannot be blank" }
    }
}

data class AppointmentId(val value: String) {
    init {
        require(value.isNotBlank()) { "AppointmentId cannot be blank" }
    }
}

data class ProtocolId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProtocolId cannot be blank" }
    }
}

data class DiagnosisId(val value: String) {
    init {
        require(value.isNotBlank()) { "DiagnosisId cannot be blank" }
    }
}

data class DocumentId(val value: String) {
    init {
        require(value.isNotBlank()) { "DocumentId cannot be blank" }
    }
}

data class AttachmentId(val value: String) {
    init {
        require(value.isNotBlank()) { "AttachmentId cannot be blank" }
    }
}

data class Money(val amount: Double, val currency: String = "BRL") {
    init {
        require(amount >= 0.0) { "Amount cannot be negative" }
    }
    
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currencies must match: $currency vs ${other.currency}" }
        return Money(amount + other.amount, currency)
    }
    
    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currencies must match: $currency vs ${other.currency}" }
        val diff = amount - other.amount
        require(diff >= 0.0) { "Money cannot represent negative balance in this context" }
        return Money(diff, currency)
    }
}

data class PhoneNumber(val value: String) {
    init {
        require(value.matches(Regex("^\\+?[1-9]\\d{1,14}\$|^\\d{10,11}\$"))) { "Invalid phone number format: $value" }
    }
}

data class Email(val value: String) {
    init {
        require(value.contains("@")) { "Invalid email address: $value" }
    }
}

data class CPF(val value: String) {
    init {
        val cleanValue = value.replace(Regex("[^0-9]"), "")
        require(cleanValue.length == 11) { "CPF must contain exactly 11 digits" }
    }
}

data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String
)

data class PainScale(val value: Int) {
    init {
        require(value in 0..10) { "Pain scale must be between 0 and 10" }
    }
}

enum class PulseType {
    SUPERFICIAL, DEEP, SLOW, RAPID, SLIPPERY, WIRY, WEAK, THREADY, STRONG
}

data class PulseQuality(val type: PulseType, val description: String)

data class TongueAssessment(
    val color: String,
    val coating: String,
    val shape: String,
    val dentalImpressions: Boolean = false
)

data class SessionDuration(val minutes: Int) {
    init {
        require(minutes > 0) { "Session duration must be greater than 0 minutes" }
    }
}
