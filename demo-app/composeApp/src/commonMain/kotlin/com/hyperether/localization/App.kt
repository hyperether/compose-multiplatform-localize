package com.hyperether.localization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import com.hyperether.resources.stringResource
import com.hyperether.resources.stringResourcePlain
import composemultiplatformlocalize.composeapp.generated.resources.Res
import composemultiplatformlocalize.composeapp.generated.resources.app_name
import composemultiplatformlocalize.composeapp.generated.resources.welcome_message
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            var appName by remember { mutableStateOf("") }
            LaunchedEffect(currentLanguage.value) {
                appName = stringResourcePlain(Res.string.app_name)
            }
            Text(appName)
            Text(stringResource(Res.string.welcome_message))
            Button(onClick = {
                currentLanguage.value =
                    if (currentLanguage.value == AppLocale.DEFAULT) AppLocale.DE else AppLocale.DEFAULT
            }) {
                Text("Click me!")
            }

        }
    }
}