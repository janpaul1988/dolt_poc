// :run - the single entry point that ties :backend and :frontend together: it prepares the
// Dolt branches the app expects, then starts the Ktor frontend (which talks to the backend's
// DoltModelConfigService) so the whole app can be launched with one command.

plugins {
    application
}

application {
    mainClass.set("org.example.run.RunAppKt")
}

dependencies {
    implementation(project(":backend"))
    implementation(project(":frontend"))

    implementation("io.ktor:ktor-server-netty:2.3.12")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

tasks.register<JavaExec>("runApp") {
    group = "application"
    description = "Runs the whole app: wires up the backend Dolt service and starts the frontend web app on http://localhost:8080"
    mainClass.set("org.example.run.RunAppKt")
    classpath = sourceSets["main"].runtimeClasspath
}
