// Root build file. Deliberately thin.
//
// NOTE: there is no org.jetbrains.kotlin.android plugin here, and that is not an oversight.
// AGP 9 ships Kotlin built in and registers its own `kotlin` extension; applying KGP on top fails
// with "Cannot add extension with name 'kotlin'". See gradle/VERSIONS.md.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
