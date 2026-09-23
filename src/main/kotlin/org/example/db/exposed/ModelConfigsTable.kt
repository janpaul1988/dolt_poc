package org.example.db.exposed

import org.jetbrains.exposed.sql.Table

/**
 * Exposed's equivalent of a flat SQL table definition: one plain Kotlin object describing the
 * columns, no annotations, no XML persistence unit - just a typed table definition backed by
 * `schema.sql`.
 */
object ModelConfigsTable : Table("model_configs") {
    val id = varchar("id", 100)
    val provider = varchar("provider", 100)
    val temperature = double("temperature")
    val maxTokens = integer("max_tokens")

    override val primaryKey = PrimaryKey(id)
}

/** Plain data class - Exposed reads/writes rows into/out of this, no entity base class required. */
data class ModelConfigRow(
    val id: String,
    val provider: String,
    val temperature: Double,
    val maxTokens: Int
)
