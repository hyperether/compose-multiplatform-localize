package com.hyperether.localization

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppTest {

    @Test
    fun testAppDisplaysWelcomeMessage() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Click me!").assertIsDisplayed()
    }

    @Test
    fun testAppDisplaysLocalizationExamples() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Outside of compose examples:").assertIsDisplayed()
        onNodeWithText("Inside compose examples:").assertIsDisplayed()
    }

    @Test
    fun testAppDisplaysSupportedLocales() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("All supported locales: ").assertIsDisplayed()
    }

    @Test
    fun testAppDisplaysLocaleCodeInput() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Set locale with code: ").assertIsDisplayed()
        onNodeWithText("Change").assertIsDisplayed()
    }
}
