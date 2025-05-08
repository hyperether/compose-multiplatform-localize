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
            val capitalized = localeName.capitalize()
            locales[capitalized] = File(valueDir, "strings.xml")
        }

        project.logger.lifecycle("Found locales: ${locales.keys}")

        val resourceDir = outputDir.get().asFile
        resourceDir.mkdirs()

        locales.forEach { (localeName, file) ->
            if (file.exists()) {
                val translations = parseXml(file)
                val outputFile = File(resourceDir, "Strings$localeName.kt")
                outputFile.writeText(buildKotlinCode(localeName, translations, outputPackage.get()))
            } else {
                project.logger.warn("Locale file does not exist: $file")
            }
        }

        generateAppLocaleEnum(resourceDir, locales.keys.toList(), outputPackage.get())

        generateLocalizedStringsClass(resourceDir, locales.keys.toList(), outputPackage.get())
    }

    private fun parseXml(file: File): Map<String, String> {
        logger.warn("Parse xml for $file")
        val translations = mutableMapOf<String, String>()
        if (!file.exists()) {
            logger.warn("File does not exist: $file")
            return translations
        }

        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")

            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as Element
                val key = node.getAttribute("name")
                val value = node.textContent
                translations[key] = value
            }
        } catch (e: Exception) {
            logger.error("Error parsing XML file: ${file.absolutePath}", e)
            throw GradleException("Failed to parse XML file: ${file.absolutePath}", e)
        }
        return translations
    }

    private fun buildKotlinCode(localeName: String, translations: Map<String, String>, packageName: String): String {
        val mapEntries = translations.entries.joinToString(",\n") { (key, value) ->
            val escapedValue = value.replace("\"", "\\\"")
            """    "$key" to "$escapedValue""""
        }

        return """
            @file:Suppress("unused")
            package $packageName
            
            object Strings$localeName {
                val strings: Map<String, String> = mapOf(
$mapEntries
                )
            }
        """.trimIndent()
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
            val enumName = locale.toUpperCase()
            val langCode = locale.toLowerCase()
            val englishName = languageDisplayNames[langCode]?.first ?: locale.capitalize()
            val nativeName = languageDisplayNames[langCode]?.second ?: locale.capitalize()

            enumEntries.add(enumName)
            langCodeMap.add("\"$langCode\"")
            displayNames.add("\"$englishName\"")
            nativeNames.add("\"$nativeName\"")
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
        }

        val mappingEntries = locales.joinToString(",\n        ") { locale ->
            val enumName = if (locale == "Default") "DEFAULT" else locale.toUpperCase()
            "AppLocale.$enumName to Strings$locale.strings"
        }

        val localizedStringsContent = """
            package $packageName
            
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import org.jetbrains.compose.resources.StringResource
            
            $importStatements
            
            /**
             * Current language state for the application
             */
            val currentLanguage = mutableStateOf(AppLocale.DEFAULT)
            
            /**
             * Generated LocalizedStrings utility class
             */
            object LocalizedStrings {
                private val strings = mapOf(
                        $mappingEntries
                )
                
                /**
                 * Get localized string by key and locale
                 * 
                 * @param key The string resource key
                 * @param locale The locale to use
                 * @return The localized string or fallback to default locale if missing
                 */
                fun get(key: String, locale: AppLocale): String {
                    return strings[locale]?.get(key) ?: strings[AppLocale.DEFAULT]?.get(key)
                        ?: "???"
                }
            }
            
            /**
             * Get localized string resource in composable context
             * 
             * @param key The string resource generated by Compose Multiplatform
             * @return The localized string for the current language
             */
            @Composable
            fun stringResource(key: StringResource): String {
                val locale = currentLanguage.value
                return LocalizedStrings.get(key.key, locale)
            }
            
            /**
             * Get localized string resource in non-composable context
             * 
             * @param key The string resource generated by Compose Multiplatform
             * @return The localized string for the current language
             */
            fun stringResourcePlain(key: StringResource): String {
                val locale = currentLanguage.value
                return LocalizedStrings.get(key.key, locale)
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