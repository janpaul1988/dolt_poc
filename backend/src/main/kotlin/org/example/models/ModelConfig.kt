package org.example.models

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

/**
 * Exposed DAO model for the single `model_configs` table used by the Dolt-backed CRUD service.
 * [ModelConfigsTable] is the one and only column definition (Dolt is MySQL-compatible, so this
 * plain Kotlin object works unchanged against it); [ModelConfigDao] is the object you actually
 * work with in the service layer - `all()`, `findById()`, `new { }` and delegated properties
 * mean no per-column `it[column] = ...` assignment blocks and no manual `ResultRow -> data class`
 * mapping anywhere. No annotations, no XML persistence unit, no bytecode weaving.
 */
object ModelConfigsTable : IdTable<String>("model_configs") {
    override val id = varchar("id", 100).entityId()
    val provider = varchar("provider", 100)
    val temperature = double("temperature")
    val maxTokens = integer("max_tokens")

    override val primaryKey = PrimaryKey(id)
}

class ModelConfigDao(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ModelConfigDao>(ModelConfigsTable)

    var provider by ModelConfigsTable.provider
    var temperature by ModelConfigsTable.temperature
    var maxTokens by ModelConfigsTable.maxTokens

    /** Plain DTO snapshot - what crosses the boundary into `:frontend`, which never sees Exposed. */
    fun toRow() = ModelConfigRow(id.value, provider, temperature, maxTokens)

    override fun toString() =
        "%-12s provider=%-8s temperature=%.2f maxTokens=%d".format(id.value, provider, temperature, maxTokens)
}

/** Plain data class used as the wire-format DTO for `:frontend` - it never touches Exposed. */
data class ModelConfigRow(
    val id: String,
    val provider: String,
    val temperature: Double,
    val maxTokens: Int
)
