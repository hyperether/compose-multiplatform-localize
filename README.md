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
    id("com.hyperether.localization") version "2.0.0"
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
├── values/              # Default locale
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
    <string name="greeting">Hello, %s!</string>
    <plurals name="items">
        <item quantity="one">One item</item>
        <item quantity="other">%d items</item>
    </plurals>
    <string-array name="colors">
        <item>Red</item>
        <item>Blue</item>
        <item>Green</item>
        <item>Yellow</item>
    </string-array>
</resources>
```

**Example `values-de/strings.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Meine App</string>
    <string name="welcome_message">Willkommen bei meiner App!</string>
    <string name="greeting">Hallo, %s!</string>
    <plurals name="items">
        <item quantity="one">Ein Element</item>
        <item quantity="other">%d Elemente</item>
    </plurals>
    <string-array name="colors">
        <item>Rot</item>
        <item>Blau</item>
        <item>Grün</item>
        <item>Gelb</item>
    </string-array>
</resources>
```

---

## Step 4: Using the Generated Classes in Your UI

- Be aware to use `com.hyperether.resources.stringResource` function instead of `org.jetbrains.compose.resources.stringResource`  
- Same function is made for easier transition from regular compose localization to our plugin.
- If you add new files clean project so plugin generates new classes
- Use these in your UI:

```kotlin
@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
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
```

---

## Step 5: Accessing Strings Outside Compose

For non-composable contexts:

```kotlin
import org.jetbrains.compose.resources.Res

LocalizedStrings.get(Res.string.app_name)
LocalizedStrings.getFormatted(Res.string.greeting, "John")
LocalizedStrings.getPlural(Res.plurals.items, 5, 5)
LocalizedStrings.getStringArray(Res.array.colors)
```

---

## Step 6: Build Your App

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