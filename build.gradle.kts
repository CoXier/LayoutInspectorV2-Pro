buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.10.0"
    kotlin("jvm") version "1.7.21"
}

group = "com.jianxinli"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
}

intellij {
    version.set("AI-202.7660.26.42.7486908")
}