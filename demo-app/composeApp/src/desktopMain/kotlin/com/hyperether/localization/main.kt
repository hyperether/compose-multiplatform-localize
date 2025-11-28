package com.hyperether.localization

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hyperether.resources.AppLocale
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
import composemultiplatformlocalize.composeapp.generated.resources.Res
import composemultiplatformlocalize.composeapp.generated.resources.app_name
import composemultiplatformlocalize.composeapp.generated.resources.welcome_message

fun main(args: Array<String>) {
    val appName = LocalizedStrings.get(Res.string.app_name)
    val isHeadless = args.contains("--headless")

    if (isHeadless) {
        runHeadless(args, appName)
    } else {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "ComposeMultiplatformLocalize",
            ) {
                App()
            }
        }
    }
}

fun runHeadless(args: Array<String>, appName: String) {
    println("Running in headless mode with app name: $appName")
    println("Arguments received: ${args.joinToString(", ")}")

    val argsMap = parseArguments(args)

    when (argsMap["command"]) {
        "test" -> {
            println("Running test command")
            println("Test parameter: ${argsMap["param"]}")
        }

        "locale" -> {
            val langCode = argsMap["lang"]
            if (langCode != null) {
                val newLocale = AppLocale.findByCode(langCode)
                currentLanguage.value = newLocale

                val localizedAppName = LocalizedStrings.get(Res.string.app_name)
                val localizedWelcome = LocalizedStrings.get(Res.string.welcome_message)

                println("Language changed to: ${newLocale.code}")
                println("Language name (English): ${newLocale.displayName}")
                println("Language name (Native): ${newLocale.nativeName}")
                println("App name in ${newLocale.code}: $localizedAppName")
                println("Welcome message in ${newLocale.code}: $localizedWelcome")
            } else {
                println("Error: --lang parameter is required")
                println("Available locales:")
                AppLocale.entries.forEach { locale ->
                    println("  ${locale.code} - ${locale.displayName} (${locale.nativeName})")
                }
            }
        }

        else -> {
            println("Available commands:")
            println("  --headless --command=test --param=value")
            println("  --headless --command=locale --lang=<code>")
            println("\nAvailable language codes:")
            AppLocale.entries.forEach { locale ->
                println("  ${locale.code} - ${locale.displayName} (${locale.nativeName})")
            }
        }
    }
}

private fun parseArguments(args: Array<String>): Map<String, String> {
    return args
        .filter { it.startsWith("--") && it.contains("=") }
        .associate {
            val (key, value) = it.removePrefix("--").split("=", limit = 2)
            key to value
        }
}