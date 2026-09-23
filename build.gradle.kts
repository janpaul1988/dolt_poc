plugins {
    kotlin("jvm") version "2.4.20"
    application
}

application {
    mainClass.set("org.example.dolt.DoltWebAppKt")
}

tasks.register<JavaExec>("runDoltWeb") {
    group = "application"
    description = "Runs the Dolt-backed HTML CRUD web app on http://localhost:8080"
    mainClass.set("org.example.dolt.DoltWebAppKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runExposedDemo") {
    group = "application"
    description = "Runs the Exposed (Kotlin-native SQL DSL + DAO) CRUD demo."
    mainClass.set("org.example.db.exposed.ExposedDemoKt")
    classpath = sourceSets["main"].runtimeClasspath
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.h2database:h2:2.2.224")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")

    // JetBrains Exposed - Kotlin-native SQL DSL + DAO, backed by H2.
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")

    // Dolt = MySQL wire-compatible, so a plain MySQL driver connects to it.
    implementation("com.mysql:mysql-connector-j:9.0.0")

    // Ktor: minimal embedded web server for the HTML CRUD frontend.
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-html-builder:2.3.12")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}