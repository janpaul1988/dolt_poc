// :frontend - Ktor HTML CRUD webapp. Talks only to :backend's DoltModelConfigService; it never
// opens a JDBC connection or touches the Exposed table definitions directly.

plugins {
    application
}

application {
    mainClass.set("org.example.frontend.DoltWebAppKt")
}

dependencies {
    implementation(project(":backend"))

    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-html-builder:2.3.12")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

tasks.register<JavaExec>("runDoltWeb") {
    group = "application"
    description = "Runs the Dolt-backed HTML CRUD web app on http://localhost:8080"
    mainClass.set("org.example.frontend.DoltWebAppKt")
    classpath = sourceSets["main"].runtimeClasspath
}
