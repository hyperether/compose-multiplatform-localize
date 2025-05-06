# How to Implement the Localization Plugin in Your App

Here's a step-by-step guide on how to implement the localization plugin in your Compose Multiplatform application:

---

## Step 1: Add the Plugin Dependency

In your app's `settings.gradle.kts`, make sure you have access to the plugin:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

---

## Step 2: Apply the Plugin in Your Build Script

In your app's `build.gradle.kts`:

```kotlin
plugins {
    // Your existing plugins
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")

    // Add the localization plugin
    id("com.hyperether.localization") version "1.0.0"
}


kotlin {
    sourceSets {
        sourceSets["commonMain"].kotlin.srcDirs(
            File(
                layout.buildDirectory.get().asFile.path,
                "generated/compose/resourceGenerator/kotlin/commonCustomResClass"
            )
        )
    }
}
```

---

## Step 3: Create Your Resource Files

Structure your localized strings in XML files under:

```
src/commonMain/composeResources/
├── values/              # Default locale (usually English)
│   └── strings.xml
├── values-de/           # German locale
│   └── strings.xml
└── values-fr/           # French locale (and so on)
    └── strings.xml
```

**Example `values/strings.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">My App</string>
    <string name="welcome_message">Welcome to my app!</string>
</resources>
```

**Example `values-de/strings.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Meine App</string>
    <string name="welcome_message">Willkommen bei meiner App!</string>
</resources>
```

---

## Step 4: Using the Generated Classes in Your UI

The plugin will generate:

- `StringsDefault.kt`, `StringsDe.kt`, etc.
- `AppLocale.kt`
- `LocalizedStrings.kt` with utility functions

Use these in your UI:

```kotlin
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.Res
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import com.hyperether.resources.stringResource

@Composable
fun WelcomeScreen() {
    Text(stringResource(Res.string.welcome_message))
}

// To change the language:
fun changeLanguage() {
    currentLanguage.value = AppLocale.DE
    // Or back to default
    // currentLanguage.value = AppLocale.DEFAULT
}
```

---

## Step 5: Accessing Strings Outside Compose

For non-composable contexts:

```kotlin
import org.jetbrains.compose.resources.Res
import com.hyperether.resources.stringResourcePlain

fun getTitle(): String {
    return stringResourcePlain(Res.string.app_name)
}
```

---

## Step 6: (Optional) Direct Access to String Maps

```kotlin
import com.yourapp.resources.StringsDefault
import com.yourapp.resources.StringsDe

val englishWelcome = StringsDefault.strings["welcome_message"]
val germanWelcome = StringsDe.strings["welcome_message"]
```

---

## Step 7: Build Your App

Run your build and the plugin will:

- Scan your resource directories
- Generate Kotlin classes for each locale
- Create the necessary utility functions
- Add these to your source set
- Make them available for use in your app

---

🎉 Your app now has a complete localization solution that works with Compose Multiplatform string resources!