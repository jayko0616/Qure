package com.qure.app.account

/** One requirement a password has to meet. The sign-up form shows each as a checklist row. */
enum class PasswordRule { minLength, special, lowercase, uppercase }

/**
 * The password rules, as a pure function so the form can show live feedback and the store can
 * refuse a weak password with the same logic. Applied at sign-up only: sign-in checks what was
 * registered, and the built-in development account deliberately does not meet these.
 */
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
