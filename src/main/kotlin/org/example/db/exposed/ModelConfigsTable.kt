package org.example.db.exposed

import org.jetbrains.exposed.sql.Table

/**
 * Exposed's equivalent of the flat `schema.sql` + [org.example.db.ModelConfigEntity] pair:
 * one plain Kotlin object describing the table, no annotations, no XML persistence unit.
 * Points at the *same* schema.sql, so this is genuinely the same table as the JPA version -
 * just accessed through a different, Kotlin-native API.
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
