package com.run.peakflow.domain.validation

/**
 * Validation utilities for authentication
 */
object AuthValidation {

    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    private val PHONE_REGEX = "^[+]?[0-9]{10,15}$".toRegex()

    /**
     * Validates email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_REGEX.matches(email.trim())
    }

    /**
     * Validates phone number format
     */
    fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.trim().replace("-", "").replace(" ", "")
        return cleanPhone.isNotBlank() && PHONE_REGEX.matches(cleanPhone)
    }

    /**
     * Validates password strength
     * Requirements:
     * - At least 8 characters
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     * - Contains at least one digit
     */
    fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false

        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        return hasUpperCase && hasLowerCase && hasDigit
    }

    /**
     * Gets password strength feedback
     */
    fun getPasswordStrengthMessage(password: String): String? {
        return when {
            password.isEmpty() -> null
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isUpperCase() } -> "Password must contain an uppercase letter"
            !password.any { it.isLowerCase() } -> "Password must contain a lowercase letter"
            !password.any { it.isDigit() } -> "Password must contain a number"
            else -> null // Strong password
        }
    }

    /**
     * Validates if input is email or phone
     */
    fun isEmailOrPhone(input: String): Pair<Boolean, String?> {
        val trimmed = input.trim()
        return when {
            trimmed.isEmpty() -> false to "Email or phone is required"
            trimmed.contains("@") -> {
                if (isValidEmail(trimmed)) true to null
                else false to "Invalid email format"
            }

            else -> {
                if (isValidPhone(trimmed)) true to null
                else false to "Invalid phone format"
            }
        }
    }
}
