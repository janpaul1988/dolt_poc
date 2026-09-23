// :backend - Exposed table/DAO definitions and domain types for `model_configs` plus the Dolt
// version-control model (formerly the separate :models module, merged in here), and the
// Exposed-backed Dolt version-control service (`DoltModelConfigService`) built on top of them.
// This is the only data-access layer in the app - everything goes through Exposed against the
// Dolt SQL server. No web framework here - :frontend depends on this module and calls into it.

dependencies {
    api("org.jetbrains.exposed:exposed-core:0.55.0")
    api("org.jetbrains.exposed:exposed-dao:0.55.0")

    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("com.mysql:mysql-connector-j:9.0.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
}
