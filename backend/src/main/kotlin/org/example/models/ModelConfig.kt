package org.example.models

import org.jetbrains.exposed.sql.Table

/**
 * Exposed DSL table definition for the single `model_configs` table used by the Dolt-backed
 * CRUD service - one plain Kotlin object describing the columns, no annotations, no XML
 * persistence unit.
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
