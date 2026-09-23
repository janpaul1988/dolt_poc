// :models - Exposed table/DAO definitions and shared domain types for the `model_configs`
// data, plus the Dolt version-control domain model (branches, tags, commits, checkout state).
// No database driver here and no I/O - just schema + plain data classes, usable by any module
// (H2-backed demos and the Dolt-backed service alike) without pulling in JDBC drivers.

dependencies {
    api("org.jetbrains.exposed:exposed-core:0.55.0")
    api("org.jetbrains.exposed:exposed-dao:0.55.0")
}
