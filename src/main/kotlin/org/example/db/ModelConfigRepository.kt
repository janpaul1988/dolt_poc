package org.example.db

import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence

/**
 * Thin CRUD wrapper around a JPA [EntityManagerFactory]. Every method is a few lines because
 * Hibernate does the map<->row conversion, SQL generation and transaction/dirty-checking work
 * that [org.example.yaml.MapMapper] had to do by hand for the YAML case.
 */
class ModelConfigRepository(
    private val emf: EntityManagerFactory = Persistence.createEntityManagerFactory("configPU")
) : AutoCloseable {

    fun findAll(): List<ModelConfigEntity> =
        emf.createEntityManager().use { em ->
            em.createQuery("select m from ModelConfigEntity m order by m.id", ModelConfigEntity::class.java)
                .resultList
        }

    fun findById(id: String): ModelConfigEntity? =
        emf.createEntityManager().use { em -> em.find(ModelConfigEntity::class.java, id) }

    /** Insert or update (upsert) - used both by CRUD "create/update" and by YAML-seeding. */
    fun save(entity: ModelConfigEntity): ModelConfigEntity =
        transaction { em -> em.merge(entity) }

    fun delete(id: String): Boolean = transaction { em ->
        val existing = em.find(ModelConfigEntity::class.java, id)
        if (existing == null) {
            false
        } else {
            em.remove(existing)
            true
        }
    }

    private fun <T> transaction(block: (jakarta.persistence.EntityManager) -> T): T {
        val em = emf.createEntityManager()
        return try {
            em.transaction.begin()
            try {
                val result = block(em)
                em.transaction.commit()
                result
            } catch (e: Exception) {
                if (em.transaction.isActive) em.transaction.rollback()
                throw e
            }
        } finally {
            em.close()
        }
    }

    override fun close() = emf.close()
}
