package com.hyperether.localization

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.GradleException
import org.gradle.api.Action
import org.gradle.api.Task
import java.util.regex.Pattern

sealed class StringResource {
    data class SimpleString(val key: String, val value: String) : StringResource()
    data class FormattedString(val key: String, val value: String, val formatArgs: List<String>) : StringResource()
    data class PluralString(val key: String, val items: Map<String, String>) : StringResource()
    data class StringArray(val key: String, val items: List<String>) : StringResource()
}

abstract class GenerateTranslationsTask : DefaultTask() {
    @get:Input
    abstract val outputPackage: Property<String>

    @get:Input
    abstract val defaultLocaleName: Property<String>

    @get:Input
    abstract val resourcesDir: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generateTranslations() {
        val resDir = File(project.projectDir, resourcesDir.get())

        val locales = mutableMapOf<String, File>()

        val defaultLocaleName = defaultLocaleName.get()
        locales[defaultLocaleName] = File(resDir, "values/strings.xml")

        val valuesDirs = resDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("values-")
        }

        valuesDirs?.forEach { valueDir ->
            val localeName = valueDir.name.substring("values-".length)
            val capitalized = localeName.capitalize().replace("-", "_")
            locales[capitalized] = File(valueDir, "strings.xml")
        }

        project.logger.lifecycle("Found locales: ${locales.keys}")

        val resourceDir = outputDir.get().asFile
        resourceDir.mkdirs()

        locales.forEach { (localeName, file) ->
            if (file.exists()) {
                val resources = parseXmlResources(file)
                val outputFile = File(resourceDir, "Strings$localeName.kt")
                outputFile.writeText(buildKotlinCode(localeName, resources, outputPackage.get()))
            } else {
                project.logger.warn("Locale file does not exist: $file")
            }
        }

        generateSharedTypes(resourceDir, outputPackage.get())
        generateAppLocaleEnum(resourceDir, locales.keys.toList(), outputPackage.get())
        generateLocalizedStringsClass(resourceDir, locales.keys.toList(), outputPackage.get())
    }

    private fun parseXmlResources(file: File): List<StringResource> {
        logger.warn("Parse xml for $file")
        val resources = mutableListOf<StringResource>()
        if (!file.exists()) {
            logger.warn("File does not exist: $file")
            return resources
        }

        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            
            // Parse simple strings
            val stringNodes = doc.getElementsByTagName("string")
            for (i in 0 until stringNodes.length) {
                val node = stringNodes.item(i) as Element
                val key = node.getAttribute("name")
                val value = processStringValue(node.textContent)
                
                // Check if it's a formatted string
                val formatArgs = extractFormatArgs(value)
                if (formatArgs.isNotEmpty()) {
                    resources.add(StringResource.FormattedString(key, value, formatArgs))
                } else {
                    resources.add(StringResource.SimpleString(key, value))
                }
            }
            
            // Parse plurals
            val pluralNodes = doc.getElementsByTagName("plurals")
            for (i in 0 until pluralNodes.length) {
                val pluralNode = pluralNodes.item(i) as Element
                val key = pluralNode.getAttribute("name")
                val items = mutableMapOf<String, String>()
                
                val itemNodes = pluralNode.getElementsByTagName("item")
                for (j in 0 until itemNodes.length) {
                    val itemNode = itemNodes.item(j) as Element
                    val quantity = itemNode.getAttribute("quantity")
                    val value = processStringValue(itemNode.textContent)
                    items[quantity] = value
                }
                resources.add(StringResource.PluralString(key, items))
            }
            
            // Parse string arrays
            val arrayNodes = doc.getElementsByTagName("string-array")
            for (i in 0 until arrayNodes.length) {
                val arrayNode = arrayNodes.item(i) as Element
                val key = arrayNode.getAttribute("name")
                val items = mutableListOf<String>()
                
                val itemNodes = arrayNode.getElementsByTagName("item")
                for (j in 0 until itemNodes.length) {
                    val itemNode = itemNodes.item(j) as Element
                    val value = processStringValue(itemNode.textContent)
                    items.add(value)
                }
                resources.add(StringResource.StringArray(key, items))
            }
            
        } catch (e: Exception) {
            logger.error("Error parsing XML file: ${file.absolutePath}", e)
            throw GradleException("Failed to parse XML file: ${file.absolutePath}", e)
        }
        return resources
    }
    
    private fun processStringValue(value: String): String {
        return value
            .replace("\\'", "'")  // Unescape apostrophes
            .replace("\\\"", "\"") // Unescape quotes
            .replace("\\n", "\n")  // Convert newlines
            .replace("\\t", "\t")  // Convert tabs
    }
    
    private fun extractFormatArgs(value: String): List<String> {
        val formatPattern = Pattern.compile("%(\\d+\\$)?[dioxXeEfFgGaAcspn%]")
        val matcher = formatPattern.matcher(value)
        val args = mutableListOf<String>()
        
        while (matcher.find()) {
            val arg = matcher.group()
            if (!args.contains(arg) && arg != "%%") {
                args.add(arg)
            }
        }
        return args
    }

    private fun buildKotlinCode(localeName: String, resources: List<StringResource>, packageName: String): String {
        val simpleStrings = mutableListOf<String>()
        val formattedStrings = mutableListOf<String>()
        val pluralStrings = mutableListOf<String>()
        val stringArrays = mutableListOf<String>()
        
        resources.forEach { resource ->
            when (resource) {
                is StringResource.SimpleString -> {
                    val escapedValue = resource.value.replace("\"", "\\\"")
                    // Check if it contains format specifiers and should be treated as formatted
                    val formatArgs = extractFormatArgs(resource.value)
                    if (formatArgs.isNotEmpty()) {
                        formattedStrings.add("""    "${resource.key}" to FormattedString("$escapedValue", listOf(${formatArgs.joinToString(", ") { "\"$it\"" }}))""")
                    } else {
                        simpleStrings.add("""    "${resource.key}" to "$escapedValue"""")
                    }
                }
                is StringResource.FormattedString -> {
                    val escapedValue = resource.value.replace("\"", "\\\"")
                    formattedStrings.add("""    "${resource.key}" to FormattedString("$escapedValue", listOf(${resource.formatArgs.joinToString(", ") { "\"$it\"" }}))""")
                }
                is StringResource.PluralString -> {
                    val items = resource.items.entries.joinToString(", ") { (quantity, value) ->
                        val escapedValue = value.replace("\"", "\\\"")
                        """"$quantity" to "$escapedValue""""
                    }
                    pluralStrings.add("""    "${resource.key}" to mapOf($items)""")
                }
                is StringResource.StringArray -> {
                    val items = resource.items.joinToString(", ") { value ->
                        val escapedValue = value.replace("\"", "\\\"")
                        """"$escapedValue""""
                    }
                    stringArrays.add("""    "${resource.key}" to listOf($items)""")
                }
            }
        }

        val stringMapEntries = if (simpleStrings.isNotEmpty()) simpleStrings.joinToString(",\n") else "    // No simple strings"
        val formattedMapEntries = if (formattedStrings.isNotEmpty()) formattedStrings.joinToString(",\n") else "    // No formatted strings"
        val pluralMapEntries = if (pluralStrings.isNotEmpty()) pluralStrings.joinToString(",\n") else "    // No plurals"
        val arrayMapEntries = if (stringArrays.isNotEmpty()) stringArrays.joinToString(",\n") else "    // No string arrays"

        return """
            @file:Suppress("unused")
            package $packageName
            
            object Strings$localeName {
                val strings: Map<String, String> = mapOf(
$stringMapEntries
                )
                
                val formattedStrings: Map<String, FormattedString> = mapOf(
$formattedMapEntries
                )
                
                val plurals: Map<String, Map<String, String>> = mapOf(
$pluralMapEntries
                )
                
                val stringArrays: Map<String, List<String>> = mapOf(
$arrayMapEntries
                )
            }
        """.trimIndent()
    }

    /**
     * Generates shared type definitions used across all locale files
     */
    private fun generateSharedTypes(resourceDir: File, packageName: String) {
        val sharedTypesContent = """
            @file:Suppress("unused")
            package $packageName
            
            /**
             * Represents a formatted string with its template and format arguments
             */
            data class FormattedString(val template: String, val formatArgs: List<String>)
        """.trimIndent()

        File(resourceDir, "SharedTypes.kt").writeText(sharedTypesContent)
    }

    /**
     * Generates an AppLocale enum based on the found locales with predefined display names
     */
    private fun generateAppLocaleEnum(resourceDir: File, locales: List<String>, packageName: String) {
        val enumEntries = mutableListOf<String>()
        val langCodeMap = mutableListOf<String>()
        val displayNames = mutableListOf<String>()
        val nativeNames = mutableListOf<String>()

        // Predefined map of language codes to display names (English and native)
        val languageDisplayNames = mapOf(
            "en" to Pair("English", "English"),
            "es" to Pair("Spanish", "Español"),
            "fr" to Pair("French", "Français"),
            "de" to Pair("German", "Deutsch"),
            "it" to Pair("Italian", "Italiano"),
            "pt" to Pair("Portuguese", "Português"),
            "ru" to Pair("Russian", "Русский"),
            "ja" to Pair("Japanese", "日本語"),
            "ko" to Pair("Korean", "한국어"),
            "zh" to Pair("Chinese", "中文"),
            "ar" to Pair("Arabic", "العربية"),
            "hi" to Pair("Hindi", "हिन्दी"),
            "tr" to Pair("Turkish", "Türkçe"),
            "pl" to Pair("Polish", "Polski"),
            "nl" to Pair("Dutch", "Nederlands"),
            "sv" to Pair("Swedish", "Svenska"),
            "da" to Pair("Danish", "Dansk"),
            "no" to Pair("Norwegian", "Norsk"),
            "fi" to Pair("Finnish", "Suomi"),
            "el" to Pair("Greek", "Ελληνικά"),
            "cs" to Pair("Czech", "Čeština"),
            "hu" to Pair("Hungarian", "Magyar"),
            "ro" to Pair("Romanian", "Română"),
            "uk" to Pair("Ukrainian", "Українська"),
            "he" to Pair("Hebrew", "עברית"),
            "th" to Pair("Thai", "ไทย"),
            "vi" to Pair("Vietnamese", "Tiếng Việt"),
            "id" to Pair("Indonesian", "Bahasa Indonesia"),
            "ms" to Pair("Malay", "Bahasa Melayu"),
            "bg" to Pair("Bulgarian", "Български"),
            "hr" to Pair("Croatian", "Hrvatski"),
            "sr" to Pair("Serbian", "Српски"),
            "sk" to Pair("Slovak", "Slovenčina"),
            "sl" to Pair("Slovenian", "Slovenščina")
        )

        enumEntries.add("DEFAULT")
        langCodeMap.add("\"en\"")  // Assuming default is English
        displayNames.add("\"${languageDisplayNames["en"]?.first}\"")
        nativeNames.add("\"${languageDisplayNames["en"]?.second}\"")

        locales.filter { it != "Default" }.forEach { locale ->
            val enumName = locale.toUpperCase().replace("-", "_")
            val langCode = locale.toLowerCase()
            val englishName = languageDisplayNames[langCode]?.first ?: locale.capitalize()
            val nativeName = languageDisplayNames[langCode]?.second ?: locale.capitalize()

            enumEntries.add(enumName)
            langCodeMap.add("\"$langCode\"")
            if (langCode.contains("_")) {
                val langCodeParts = langCode.split("_")
                val englishNameWithCode = languageDisplayNames[langCodeParts[0]]?.first ?: locale.capitalize()
                val nativeNameWithCode = languageDisplayNames[langCodeParts[0]]?.second ?: locale.capitalize()
                displayNames.add("\"$englishNameWithCode ($langCode)\"")
                nativeNames.add("\"$nativeNameWithCode ($langCode)\"")
            } else {
                displayNames.add("\"$englishName\"")
                nativeNames.add("\"$nativeName\"")
            }
        }

        val enumContent = """
        package $packageName
        
        /**
         * Generated AppLocale enum - represents available locales in the app
         */
        enum class AppLocale(
            val code: String, 
            val displayName: String,
            val nativeName: String
        ) {
            ${enumEntries.zip(langCodeMap.zip(displayNames.zip(nativeNames))).joinToString(",\n            ") { (name, details) ->
            val (code, names) = details
            val (display, native) = names
            "$name($code, $display, $native)"
        }};
            
            companion object {
                /**
                 * Get all supported locales with their English display names
                 * @return Map of language codes to English display names
                 */
                val supportedLocales: Map<String, String> by lazy {
                    values().associate { locale ->
                        locale.code to locale.displayName
                    }
                }
                
                /**
                 * Get all supported locales with their native display names
                 * @return Map of language codes to native display names
                 */
                val supportedNativeLocales: Map<String, String> by lazy {
                    values().associate { locale ->
                        locale.code to locale.nativeName
                    }
                }
                
                /**
                 * Find locale by language code
                 * @param code The language code to search for
                 * @return The matching AppLocale or DEFAULT if not found
                 */
                fun findByCode(code: String): AppLocale {
                    return values().find { it.code == code } ?: DEFAULT
                }
            }
        }
    """.trimIndent()

        File(resourceDir, "AppLocale.kt").writeText(enumContent)
    }

    /**
     * Generates a LocalizedStrings utility class to use with the generated locale classes
     */
    private fun generateLocalizedStringsClass(resourceDir: File, locales: List<String>, packageName: String) {
        val importStatements = locales.joinToString("\n") { locale ->
            "import $packageName.Strings$locale"
        } + "\nimport $packageName.FormattedString"

        val stringMappingEntries = locales.joinToString(",\n        ") { locale ->
            val enumName = if (locale == "Default") "DEFAULT" else locale.toUpperCase()
            "AppLocale.$enumName to Strings$locale.strings"
        }
        
        val formattedMappingEntries = locales.joinToString(",\n        ") { locale ->
            val enumName = if (locale == "Default") "DEFAULT" else locale.toUpperCase()
            "AppLocale.$enumName to Strings$locale.formattedStrings"
        }
        
        val pluralMappingEntries = locales.joinToString(",\n        ") { locale ->
            val enumName = if (locale == "Default") "DEFAULT" else locale.toUpperCase()
            "AppLocale.$enumName to Strings$locale.plurals"
        }
        
        val arrayMappingEntries = locales.joinToString(",\n        ") { locale ->
            val enumName = if (locale == "Default") "DEFAULT" else locale.toUpperCase()
            "AppLocale.$enumName to Strings$locale.stringArrays"
        }

        val localizedStringsContent = """
            package $packageName
            
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.getValue
            import org.jetbrains.compose.resources.StringResource
            import org.jetbrains.compose.resources.PluralStringResource  
            import org.jetbrains.compose.resources.StringArrayResource
            
            $importStatements
            
            /**
             * Current language state for the application
             */
            val currentLanguage = mutableStateOf(AppLocale.DEFAULT)
            
            /**
             * Generated LocalizedStrings utility class with Android string features support
             */
            object LocalizedStrings {
                private val strings = mapOf(
                        $stringMappingEntries
                )
                
                private val formattedStrings = mapOf(
                        $formattedMappingEntries
                )
                
                private val plurals = mapOf(
                        $pluralMappingEntries
                )
                
                private val stringArrays = mapOf(
                        $arrayMappingEntries
                )
                
                /**
                 * Get localized simple string by key and locale
                 */
                fun get(key: String, locale: AppLocale = currentLanguage.value): String {
                    return strings[locale]?.get(key) ?: strings[AppLocale.DEFAULT]?.get(key) ?: "???"
                }
                
                /**
                 * Get localized simple string by StringResource and locale
                 */
                fun get(key: StringResource, locale: AppLocale = currentLanguage.value): String {
                    return get(key.key, locale)
                }
                
                /**
                 * Get formatted string with parameters
                 */
                fun getFormatted(key: String, vararg args: Any, locale: AppLocale = currentLanguage.value): String {
                    val formattedString = formattedStrings[locale]?.get(key) ?: formattedStrings[AppLocale.DEFAULT]?.get(key)
                    return if (formattedString != null) {
                        try {
                            String.format(formattedString.template, *args)
                        } catch (e: Exception) {
                            formattedString.template
                        }
                    } else {
                        "???"
                    }
                }
                
                /**
                 * Get formatted string with parameters by StringResource
                 */
                fun getFormatted(key: StringResource, vararg args: Any, locale: AppLocale = currentLanguage.value): String {
                    return getFormatted(key.key, *args, locale = locale)
                }
                
                /**
                 * Get plural string based on quantity with format support
                 */
                fun getPlural(key: String, quantity: Int, vararg formatArgs: Any, locale: AppLocale = currentLanguage.value): String {
                    val pluralMap = plurals[locale]?.get(key) ?: plurals[AppLocale.DEFAULT]?.get(key)
                    return if (pluralMap != null) {
                        val quantityKey = when {
                            quantity == 0 && pluralMap.containsKey("zero") -> "zero"
                            quantity == 1 && pluralMap.containsKey("one") -> "one"
                            quantity == 2 && pluralMap.containsKey("two") -> "two"
                            quantity > 2 && pluralMap.containsKey("many") -> "many"
                            else -> "other"
                        }
                        val template = pluralMap[quantityKey] ?: pluralMap["other"] ?: "???"
                        if (formatArgs.isNotEmpty()) {
                            try {
                                String.format(template, *formatArgs)
                            } catch (e: Exception) {
                                template
                            }
                        } else {
                            template
                        }
                    } else {
                        "???"
                    }
                }
                
                /**
                 * Get plural string based on quantity by PluralStringResource
                 */  
                fun getPlural(key: PluralStringResource, quantity: Int, vararg formatArgs: Any, locale: AppLocale = currentLanguage.value): String {
                    return getPlural(key.key, quantity, *formatArgs, locale = locale)
                }
                
                /**
                 * Get string array by key
                 */
                fun getStringArray(key: String, locale: AppLocale = currentLanguage.value): List<String> {
                    return stringArrays[locale]?.get(key) ?: stringArrays[AppLocale.DEFAULT]?.get(key) ?: emptyList()
                }
                
                /**
                 * Get string array by StringArrayResource
                 */
                fun getStringArray(key: StringArrayResource, locale: AppLocale = currentLanguage.value): List<String> {
                    return getStringArray(key.key, locale)
                }
            }
            
            /**
             * Get localized string resource in composable context - reacts to language changes
             */
            @Composable
            fun stringResource(key: StringResource): String {
                val currentLang by currentLanguage
                return LocalizedStrings.get(key, currentLang)
            }
            
            /**
             * Get formatted string resource in composable context - for strings with format args
             */
            @Composable
            fun stringResource(key: StringResource, vararg formatArgs: Any): String {
                val currentLang by currentLanguage
                return LocalizedStrings.getFormatted(key, *formatArgs, locale = currentLang)
            }
            
            /**
             * Get plural string resource in composable context - reacts to language changes
             */
            @Composable
            fun pluralStringResource(key: PluralStringResource, quantity: Int): String {
                val currentLang by currentLanguage
                return LocalizedStrings.getPlural(key, quantity, locale = currentLang)
            }
            
            /**
             * Get plural string resource with format args in composable context
             */
            @Composable
            fun pluralStringResource(key: PluralStringResource, quantity: Int, vararg formatArgs: Any): String {
                val currentLang by currentLanguage
                return LocalizedStrings.getPlural(key, quantity, *formatArgs, locale = currentLang)
            }
            
            /**
             * Get string array resource in composable context - reacts to language changes
             */
            @Composable
            fun stringArrayResource(key: StringArrayResource): List<String> {
                val currentLang by currentLanguage
                return LocalizedStrings.getStringArray(key, currentLang)
            }
        """.trimIndent()

        File(resourceDir, "LocalizedStrings.kt").writeText(localizedStringsContent)
    }
}

class LocalizationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("localization", LocalizationExtension::class.java)

        val generateTranslateFile = project.tasks.register("generateTranslateFile", GenerateTranslationsTask::class.java)

        project.tasks.named("generateTranslateFile").configure(object : Action<Task> {
            override fun execute(task: Task) {
                // Cast to our specific task type
                if (task is GenerateTranslationsTask) {
                    task.group = "build"
                    task.description = "Generates Kotlin classes for localization from XML resources"
                    task.outputPackage.set(extension.outputPackage)
                    task.defaultLocaleName.set(extension.defaultLocaleName)
                    task.resourcesDir.set(extension.resourcesDir)

                    val packagePath = extension.outputPackage.get().replace('.', '/')
                    task.outputDir.set(project.layout.buildDirectory.dir("generated/compose/resourceGenerator/kotlin/commonCustomResClass/$packagePath"))
                }
            }
        })

        project.afterEvaluate {
            project.tasks.findByName("generateComposeResClass")?.dependsOn(generateTranslateFile)
        }
    }
}