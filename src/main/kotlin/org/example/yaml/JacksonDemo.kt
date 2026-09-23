package org.example.yaml

import org.example.config.AppConfig
import java.io.File

/**
 * Standalone comparison: same two layered YAML files, loaded once via the hand-rolled
 * reflection [MapMapper]/[Yaml], once via [JacksonYaml]. Run with `./gradlew runJacksonDemo`.
 */
fun main() {
    val files = listOf(
        File("src/main/resources/config-base.yaml"),
        File("src/main/resources/config-override.yaml")
    )

    val viaReflection: AppConfig = Yaml().load(files)
    val viaJackson: AppConfig = JacksonYaml().load(files)

    println("=== Yaml() / MapMapper (hand-rolled reflection, ~100 lines + FromMap interface) ===")
    println(viaReflection)

    println()
    println("=== JacksonYaml (jackson-module-kotlin, ~25 lines + 1 custom Duration deserializer) ===")
    println(viaJackson)

    println()
    println("Equal result: ${viaReflection == viaJackson}")
}
