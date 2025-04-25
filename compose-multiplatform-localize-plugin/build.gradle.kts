import java.util.Properties

plugins {
    id("java-library")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.0"
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// Note: We're using kotlin-dsl plugin which handles Kotlin configuration

group = "com.hyperether"
version = "1.0.0"


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