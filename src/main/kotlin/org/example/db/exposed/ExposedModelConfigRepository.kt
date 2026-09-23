package org.example.db.exposed

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Repository built on Exposed's SQL DSL: a typed query builder that reads almost like the SQL
 * itself, with no entity manager, annotations, or separate mapping layer. Each CRUD method is a
 * one-line `transaction { ... }` block. Compare to [ExposedDaoModelConfigRepository], which uses
 * Exposed's higher-level DAO API for the same table.
 */
class ExposedModelConfigRepository(jdbcUrl: String = "jdbc:h2:file:./data/config-exposed;INIT=RUNSCRIPT FROM 'classpath:schema.sql'") {
    private val db = Database.connect(jdbcUrl, driver = "org.h2.Driver", user = "sa", password = "")

    fun findAll(): List<ModelConfigRow> = transaction(db) {
        ModelConfigsTable.selectAll().orderBy(ModelConfigsTable.id).map { it.toRow() }
    }

    fun findById(id: String): ModelConfigRow? = transaction(db) {
        ModelConfigsTable.selectAll().where { ModelConfigsTable.id eq id }.singleOrNull()?.toRow()
    }

    /** Insert or update (upsert). */
    fun save(row: ModelConfigRow): ModelConfigRow = transaction(db) {
        val updated = ModelConfigsTable.update({ ModelConfigsTable.id eq row.id }) {
            it[provider] = row.provider
            it[temperature] = row.temperature
            it[maxTokens] = row.maxTokens
        }
        if (updated == 0) {
            ModelConfigsTable.insert {
                it[id] = row.id
                it[provider] = row.provider
                it[temperature] = row.temperature
                it[maxTokens] = row.maxTokens
            }
        }
        row
    }

    fun delete(id: String): Boolean = transaction(db) {
        ModelConfigsTable.deleteWhere { ModelConfigsTable.id eq id } > 0
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toRow() = ModelConfigRow(
        id = this[ModelConfigsTable.id],
        provider = this[ModelConfigsTable.provider],
        temperature = this[ModelConfigsTable.temperature],
        maxTokens = this[ModelConfigsTable.maxTokens]
    )
}
