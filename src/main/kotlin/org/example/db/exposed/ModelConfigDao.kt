package org.example.db.exposed

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

/**
 * DAO-API version of [ModelConfigsTable]/[ModelConfigRow]. This removes the manual row<->object
 * mapping that the plain DSL version needed (no `toRow()`, no per-field `it[column] = value`
 * assignment blocks): properties on [ModelConfigDao] read/write the underlying row directly via
 * delegation, and [ModelConfigDao.Companion] (an [EntityClass]) gives you `all()`, `findById()`,
 * `new { }` for free - the closest Exposed gets to "JPA-style" ergonomics while staying a plain
 * Kotlin object with no annotations/proxies/bytecode weaving.
 */
object ModelConfigsIdTable : IdTable<String>("model_configs") {
    override val id = varchar("id", 100).entityId()
    val provider = varchar("provider", 100)
    val temperature = double("temperature")
    val maxTokens = integer("max_tokens")

    override val primaryKey = PrimaryKey(id)
}

class ModelConfigDao(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ModelConfigDao>(ModelConfigsIdTable)

    var provider by ModelConfigsIdTable.provider
    var temperature by ModelConfigsIdTable.temperature
    var maxTokens by ModelConfigsIdTable.maxTokens

    override fun toString() =
        "%-12s provider=%-8s temperature=%.2f maxTokens=%d".format(id.value, provider, temperature, maxTokens)
}
