plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.10.1"
    kotlin("jvm") version "1.7.21"
}

group = "com.eric-li"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
}

intellij {
    version.set("221.6008.13.2211.9514443")
    type.set("AI") // AI means Android Studio
    plugins.set(listOf("android"))
}