package org.example

import org.example.config.AppConfig
import org.example.db.ModelConfigCli
import org.example.db.ModelConfigRepository
import org.example.db.seedFrom
import org.example.yaml.Yaml
import java.io.File

fun main() {
    val files = listOf(
        File("src/main/resources/config-base.yaml"),
        File("src/main/resources/config-override.yaml")
    )

    val config: AppConfig = Yaml().load(files)
    println("Loaded ${config.models.size} model(s) from merged YAML: ${config.models.map { it.id }}")

    ModelConfigRepository().use { repo ->
        repo.seedFrom(config.models) // one-time import; DB is now the live source of truth
        ModelConfigCli(repo).run()
    }
}