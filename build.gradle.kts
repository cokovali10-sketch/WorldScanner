import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20" apply false
}

group = "org.worldscanner"
version = "3.0.0"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }
}
