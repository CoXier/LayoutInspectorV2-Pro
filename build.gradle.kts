buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.10.1"
    kotlin("jvm") version "1.7.21"
}

group = "com.jianxinli"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
}

intellij {
    version.set("213.7172.25.2113.9014738")
    type.set("AI") // AI means Android Studio
    plugins.set(listOf("android"))
}