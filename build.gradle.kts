// Root project: no code of its own - just shared configuration for the three real modules
// (backend, frontend, run). See README.md for what each module contains and how to run them.

plugins {
    kotlin("jvm") version "2.4.20" apply false
}

allprojects {
    group = "org.example"
    version = "1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(24)
    }

    dependencies {
        "testImplementation"(platform("io.kotest:kotest-bom:5.9.1"))
        "testImplementation"("io.kotest:kotest-runner-junit5")
        "testImplementation"("io.kotest:kotest-assertions-core")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
