import java.util.Properties

plugins {
    id("java-library")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.0"
    `kotlin-dsl`
}

// This is critical to fix the language version error
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        languageVersion = "1.6" // At least 1.6 as required by the error
        apiVersion = "1.6"
    }
}

// Set Java compatibility to match Kotlin
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Note: We're using kotlin-dsl plugin which handles Kotlin configuration

group = "com.hyperether"
version = "1.0.2"


gradlePlugin {
    website.set("https://www.hyperether.com/")
    vcsUrl.set("https://github.com/hyperether/compose-multiplatform-localize")

    plugins {
        create("compose-multiplatform-localize") {
            id = "com.hyperether.localization"
            implementationClass = "com.hyperether.localization.LocalizationPlugin"
            displayName = "compose-multiplatform-localize"
            description = "A plugin that generates Kotlin classes from XML localization files for Compose Multiplatform projects"
            tags.set(listOf("compose", "localization", "compose multiplatform", "kotlin", "kmp", "desktop", "android", "web", "iOS"))
        }
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
}