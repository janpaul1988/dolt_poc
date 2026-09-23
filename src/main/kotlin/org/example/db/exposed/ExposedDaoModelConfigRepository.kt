package org.example.db.exposed

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Compare each method here to its DSL counterpart in [ExposedModelConfigRepository]: no
 * `toRow()` mapping function, no `it[column] = value` assignment blocks, no manual "update, and
 * if 0 rows were touched then insert" upsert logic - [org.jetbrains.exposed.dao.EntityClass]
 * gives you `findById`/`new`/property assignment directly, letting the entity read like a plain
 * mutable object.
 */
class ExposedDaoModelConfigRepository(jdbcUrl: String = "jdbc:h2:file:./data/config-exposed-dao;INIT=RUNSCRIPT FROM 'classpath:schema.sql'") {
    private val db = Database.connect(jdbcUrl, driver = "org.h2.Driver", user = "sa", password = "")

    fun findAll(): List<ModelConfigDao> = transaction(db) { ModelConfigDao.all().toList() }

    fun findById(id: String): ModelConfigDao? = transaction(db) { ModelConfigDao.findById(id) }

    /** Insert or update (upsert). */
    fun save(id: String, provider: String, temperature: Double, maxTokens: Int): ModelConfigDao = transaction(db) {
        val entity = ModelConfigDao.findById(id) ?: ModelConfigDao.new(id) {}
        entity.provider = provider
        entity.temperature = temperature
        entity.maxTokens = maxTokens
        entity
    }

    fun delete(id: String): Boolean = transaction(db) {
        val existing = ModelConfigDao.findById(id) ?: return@transaction false
        existing.delete()
        true
    }
}
