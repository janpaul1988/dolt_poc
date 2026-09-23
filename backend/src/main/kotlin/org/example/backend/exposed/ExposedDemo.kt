package org.example.backend.exposed

import org.example.models.ModelConfigRow

/**
 * Non-interactive CRUD smoke test comparing Exposed's two APIs, run with
 * `./gradlew :backend:runExposedDemo`. Exercises create -> read -> update -> delete against the
 * same `model_configs` table/schema.sql, just through separate H2 files so the two demos can't
 * collide over the same file lock when run independently.
 */
fun main() {
    println("=== Exposed DSL demo (Table + manual row<->object mapping) ===")
    val dsl = ExposedModelConfigService()
    dsl.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))
    println("After create: ${dsl.findAll()}")
    dsl.save(ModelConfigRow("qa-model", "azure-updated", 0.4, 4096))
    println("After update: ${dsl.findById("qa-model")}")
    println("Deleted qa-model: ${dsl.delete("qa-model")}")
    println("After delete: ${dsl.findAll()}")

    println()
    println("=== Exposed DAO demo (IdTable + EntityClass, no manual row mapping) ===")
    val dao = ExposedDaoModelConfigService()
    dao.save("qa-model", "azure", 0.3, 2048)
    println("After create: ${dao.findAll()}")
    dao.save("qa-model", "azure-updated", 0.4, 4096)
    println("After update: ${dao.findById("qa-model")}")
    println("Deleted qa-model: ${dao.delete("qa-model")}")
    println("After delete: ${dao.findAll()}")
}
