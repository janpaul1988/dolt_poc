package org.example.db

import org.example.config.ModelConfig

/**
 * A "deadly simple" console CRUD frontend for [ModelConfigEntity]. No web framework, no HTML -
 * just a text menu loop over the repository. This is meant to be contrasted with editing the
 * YAML files by hand and re-running the app: here, changes are immediate and persisted per action.
 */
class ModelConfigCli(private val repo: ModelConfigRepository) {

    fun run() {
        loop@ while (true) {
            printMenu()
            when (readLine()?.trim()) {
                "1" -> list()
                "2" -> create()
                "3" -> update()
                "4" -> delete()
                "5", null -> break@loop
                else -> println("Unknown option.\n")
            }
        }
        println("Bye.")
    }

    private fun printMenu() {
        println(
            """
            |
            |=== Model configs (table: model_configs) ===
            |1) List
            |2) Create / upsert
            |3) Update
            |4) Delete
            |5) Exit
            |Choose:
            """.trimMargin()
        )
    }

    private fun list() {
        val all = repo.findAll()
        if (all.isEmpty()) println("(empty)") else all.forEach { println(it) }
    }

    private fun create() {
        print("id: "); val id = readLine().orEmpty().trim()
        if (id.isBlank()) { println("id required"); return }
        print("provider: "); val provider = readLine().orEmpty().trim().ifBlank { "openai" }
        print("temperature [0.7]: "); val temperature = readLine().orEmpty().trim().toDoubleOrNull() ?: 0.7
        print("maxTokens [1024]: "); val maxTokens = readLine().orEmpty().trim().toIntOrNull() ?: 1024
        repo.save(ModelConfigEntity(id, provider, temperature, maxTokens))
        println("Saved.")
    }

    private fun update() {
        print("id to update: "); val id = readLine().orEmpty().trim()
        val existing = repo.findById(id)
        if (existing == null) { println("Not found."); return }
        print("provider [${existing.provider}]: ")
        readLine()?.trim()?.takeIf { it.isNotBlank() }?.let { existing.provider = it }
        print("temperature [${existing.temperature}]: ")
        readLine()?.trim()?.toDoubleOrNull()?.let { existing.temperature = it }
        print("maxTokens [${existing.maxTokens}]: ")
        readLine()?.trim()?.toIntOrNull()?.let { existing.maxTokens = it }
        repo.save(existing)
        println("Updated.")
    }

    private fun delete() {
        print("id to delete: "); val id = readLine().orEmpty().trim()
        println(if (repo.delete(id)) "Deleted." else "Not found.")
    }
}

/** Seeds the DB from the already-merged YAML [models] list (upsert by id). Run once at startup. */
fun ModelConfigRepository.seedFrom(models: List<ModelConfig>) {
    models.forEach { m -> save(ModelConfigEntity(m.id, m.provider, m.temperature, m.maxTokens)) }
}
