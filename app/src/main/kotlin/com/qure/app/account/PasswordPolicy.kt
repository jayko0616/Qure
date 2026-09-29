package com.qure.app.account

enum class PasswordRule { minLength, special, lowercase, uppercase }

object PasswordPolicy {

    const val minLengthChars = 8

    fun satisfied(password: String): Set<PasswordRule> = buildSet {
        if (password.length >= minLengthChars) add(PasswordRule.minLength)
        if (password.any { !it.isLetterOrDigit() && !it.isWhitespace() }) add(PasswordRule.special)
        if (password.any { it.isLowerCase() }) add(PasswordRule.lowercase)
        if (password.any { it.isUpperCase() }) add(PasswordRule.uppercase)
    }

    fun violations(password: String): Set<PasswordRule> =
        PasswordRule.entries.toSet() - satisfied(password)

    fun isValid(password: String): Boolean = violations(password).isEmpty()
}
