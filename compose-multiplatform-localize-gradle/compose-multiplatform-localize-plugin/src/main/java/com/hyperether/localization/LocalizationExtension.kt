package com.hyperether.localization

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring the localization plugin
 */
abstract class LocalizationExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * The package name where the generated classes will be placed
     */
    val outputPackage: Property<String> = objects.property(String::class.java)
        .convention("com.hyperether.resources")

    /**
     * Directory for localized resources relative to the project directory
     */
    val resourcesDir: Property<String> = objects.property(String::class.java)
        .convention("src/commonMain/composeResources")

    /**
     * The name suffix for the default locale (without the language code)
     */
    val defaultLocaleName: Property<String> = objects.property(String::class.java)
        .convention("Default")
}