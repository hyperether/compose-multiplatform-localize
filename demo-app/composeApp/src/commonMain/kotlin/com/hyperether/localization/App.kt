package com.hyperether.localization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.AppLocale
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
import com.hyperether.resources.pluralStringResource
import com.hyperether.resources.stringArrayResource
import com.hyperether.resources.stringResource
import composemultiplatformlocalize.composeapp.generated.resources.Res
import composemultiplatformlocalize.composeapp.generated.resources.app_name
import composemultiplatformlocalize.composeapp.generated.resources.colors
import composemultiplatformlocalize.composeapp.generated.resources.formatted_message
import composemultiplatformlocalize.composeapp.generated.resources.greeting
import composemultiplatformlocalize.composeapp.generated.resources.items
import composemultiplatformlocalize.composeapp.generated.resources.notifications
import composemultiplatformlocalize.composeapp.generated.resources.price_tag
import composemultiplatformlocalize.composeapp.generated.resources.user_profile
import composemultiplatformlocalize.composeapp.generated.resources.welcome_message
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var appName by remember { mutableStateOf("") }
            var templateString by remember { mutableStateOf("") }
            var pluralString by remember { mutableStateOf("") }
            val arrayString = remember { mutableStateListOf<String>() }

            // Outside of compose example
            LaunchedEffect(currentLanguage.value) {
                appName = LocalizedStrings.get(Res.string.app_name)
                templateString = LocalizedStrings.getFormatted(Res.string.greeting, "John")
                pluralString = LocalizedStrings.getPlural(Res.plurals.items, 5, 5)
                arrayString.clear()
                arrayString.addAll(LocalizedStrings.getStringArray(Res.array.colors))
            }
            Text("Outside of compose examples:", color = Color.Red)
            Text("App name: $appName")
            Text("Greeting: $templateString")
            Text("Plural: $pluralString")
            Row {
                Text("Colors: ")
                arrayString.forEach {
                    Text(it, modifier = Modifier.padding(end = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Text("Inside compose examples:", color = Color.Red)

            // Simple string example
            Text(stringResource(Res.string.welcome_message))

            // String template example - single argument
            Text(stringResource(Res.string.greeting, "John"))

            // String template example - multiple arguments
            Text(stringResource(Res.string.user_profile, "Alice", 25))

            // String template example - float formatting
            Text(stringResource(Res.string.price_tag, 19.99))

            // String template example - positional arguments
            Text(stringResource(Res.string.formatted_message, "Bob", 3))

            // Plurals example - simple
            Text(pluralStringResource(Res.plurals.items, 1, 1))
            Text(pluralStringResource(Res.plurals.items, 5, 5))

            // Plurals example - with name and count
            Text(pluralStringResource(Res.plurals.notifications, 0, "Emma", 0))
            Text(pluralStringResource(Res.plurals.notifications, 1, "Emma", 1))
            Text(pluralStringResource(Res.plurals.notifications, 7, "Emma", 7))

            // Array example - all items
            Text("All colors: ${stringArrayResource(Res.array.colors).joinToString(", ")}")

            Button(onClick = {
                currentLanguage.value =
                    if (currentLanguage.value == AppLocale.DEFAULT) AppLocale.DE else AppLocale.DEFAULT
            }) {
                Text("Click me!")
            }

            // List all locales in app
            Text("All supported locales: ", color = Color.Red)
            Row {
                Column {
                    AppLocale.supportedLocales.forEach {
                        Text("${it.key}, ${it.value}")
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    AppLocale.supportedNativeLocales.forEach {
                        Text("${it.key}, ${it.value}")
                    }
                }

            }


            Spacer(modifier = Modifier.height(20.dp))

            // Set locale with code
            Text("Set locale with code: ", color = Color.Red)
            var code by remember { mutableStateOf("") }
            Row {
                TextField(code, { code = it })
                Button(onClick = {
                    currentLanguage.value = AppLocale.findByCode(code)
                }) {
                    Text("Change")
                }
            }

        }
    }
}