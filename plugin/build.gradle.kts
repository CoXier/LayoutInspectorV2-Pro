plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.10.1"
    kotlin("jvm") version "1.9.0"
}

group = "com.eric-li"
version = "1.0.7"

intellij {
    version.set("242.23726.103.2422.12816248")
    type.set("AI") // AI means Android Studio
    plugins.set(listOf("android"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("242")
        untilBuild.set("")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}