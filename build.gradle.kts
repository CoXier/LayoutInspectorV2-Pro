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
    version.set("202.7660.26.42.7486908")
    type.set("AI") // AI means Android Studio
    plugins.set(listOf("android"))
}