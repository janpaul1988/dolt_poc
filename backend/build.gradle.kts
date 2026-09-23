// :backend - Exposed table/DAO definitions and domain types for `model_configs` plus the Dolt
// version-control model (formerly the separate :models module, merged in here), and the CRUD
// services built on top of them: two H2-backed Exposed demos (DSL vs DAO API) plus the
// Exposed-backed Dolt version-control service. No web framework here - :frontend depends on
// this module and calls into it.

dependencies {
    api("org.jetbrains.exposed:exposed-core:0.55.0")
    api("org.jetbrains.exposed:exposed-dao:0.55.0")

    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("com.mysql:mysql-connector-j:9.0.0")
    implementation("com.h2database:h2:2.2.224")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

tasks.register<JavaExec>("runExposedDemo") {
    group = "application"
    description = "Runs the Exposed (Kotlin-native SQL DSL + DAO) CRUD demo."
    mainClass.set("org.example.backend.exposed.ExposedDemoKt")
    classpath = sourceSets["main"].runtimeClasspath
}
