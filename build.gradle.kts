plugins {
    kotlin("jvm") version "2.4.20"
    application
    kotlin("plugin.jpa") version "2.4.20"
}

application {
    mainClass.set("org.example.MainKt")
}

tasks.register<JavaExec>("runJacksonDemo") {
    group = "application"
    description = "Runs the Jackson-based YAML loading demo, side by side with the reflection MapMapper."
    mainClass.set("org.example.yaml.JacksonDemoKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runDoltWeb") {
    group = "application"
    description = "Runs the Dolt-backed HTML CRUD web app on http://localhost:8080"
    mainClass.set("org.example.dolt.DoltWebAppKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runExposedDemo") {
    group = "application"
    description = "Runs the Exposed (Kotlin-native SQL DSL) CRUD demo, side by side with the JPA/Hibernate one."
    mainClass.set("org.example.db.exposed.ExposedDemoKt")
    classpath = sourceSets["main"].runtimeClasspath
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.yaml:snakeyaml:2.2")

    // Jackson-based YAML mapping, for comparison against the hand-rolled MapMapper.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    implementation("org.hibernate.orm:hibernate-core:6.6.2.Final")
    implementation("com.h2database:h2:2.2.224")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")

    // JetBrains Exposed - Kotlin-native SQL DSL, for comparison against JPA/Hibernate.
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