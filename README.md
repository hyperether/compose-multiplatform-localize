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
    id("com.hyperether.localization") version "1.1.1"
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

- Be aware to use `com.hyperether.resources.stringResource` function instead of `org.jetbrains.compose.resources.stringResource`  
- Same function is made for easier transition from regular compose localization to our plugin.
- If you add new files clean project so plugin generates new classes
- Use these in your UI:

```kotlin
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
            // Change language by setting current language
            Text(currentLanguage.value.displayName)
            Text(currentLanguage.value.nativeName)
            Button(onClick = {
                currentLanguage.value =
                    if (currentLanguage.value == AppLocale.DEFAULT) AppLocale.DE else AppLocale.DEFAULT
            }) {
                Text("Click me!")
            }

            // List all locales in app
            Text("All supported locales: ")
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

            Spacer(modifier  = Modifier.height(20.dp))

            // Find locale by code
            Text("Find by code: ")
            Text("${AppLocale.findByCode("de")}")
            Text("${AppLocale.findByCode("ll")}")

            Spacer(modifier  = Modifier.height(20.dp))

            // Set locale with code
            Text("Set locale with code: ")
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
import com.hyperether.resources.StringsDefault
import com.hyperether.resources.StringsDe

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

## Checkout our IDE plugin for [navigation to strings.xml](https://plugins.jetbrains.com/plugin/27348-resource-locator)

- Plugin that leads developer directly to strings.xml file instead to Res class on command(ctrl)+click.
- [Repository](https://github.com/hyperether/compose-multiplatform-res-locator)