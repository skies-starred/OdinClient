pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8"
}

stonecutter.create(rootProject) {
    versions("1.21.8", "1.21.10")
    vcsVersion = "1.21.10"
}