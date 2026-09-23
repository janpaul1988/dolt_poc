package org.example.db.exposed

/**
 * Non-interactive CRUD smoke test for the Exposed repository, run with `./gradlew runExposedDemo`.
 * Exercises create -> read -> update -> delete against the same `model_configs` table/schema.sql
 * used by the JPA version, just through a separate H2 file (data/config-exposed) so the two
 * demos can't collide over the same file lock when run independently.
 */
/**
 * Non-interactive CRUD smoke test comparing Exposed's two APIs, run with `./gradlew runExposedDemo`.
 * Exercises create -> read -> update -> delete against the same `model_configs` table/schema.sql
 * used by the JPA version, just through separate H2 files so the demos can't collide over the
 * same file lock when run independently.
 */
fun main() {
    println("=== Exposed DSL demo (Table + manual row<->object mapping) ===")
    val dsl = ExposedModelConfigRepository()
    dsl.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))
    println("After create: ${dsl.findAll()}")
    dsl.save(ModelConfigRow("qa-model", "azure-updated", 0.4, 4096))
    println("After update: ${dsl.findById("qa-model")}")
    println("Deleted qa-model: ${dsl.delete("qa-model")}")
    println("After delete: ${dsl.findAll()}")

    println()
    println("=== Exposed DAO demo (IdTable + EntityClass, no manual row mapping) ===")
    val dao = ExposedDaoModelConfigRepository()
    dao.save("qa-model", "azure", 0.3, 2048)
    println("After create: ${dao.findAll()}")
    dao.save("qa-model", "azure-updated", 0.4, 4096)
    println("After update: ${dao.findById("qa-model")}")
    println("Deleted qa-model: ${dao.delete("qa-model")}")
    println("After delete: ${dao.findAll()}")
}
