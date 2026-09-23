package org.example.yaml

import java.io.File
import org.yaml.snakeyaml.Yaml as SnakeYaml

/**
 * Facade that ties together:
 *  1. parsing each YAML file into a raw `Map<String, Any?>` (via SnakeYAML),
 *  2. deep-merging all of them into a single layered map ([deepMerge] / [mergeAll]) -
 *     lists that appear under the same key in multiple files are concatenated,
 *  3. mapping the merged map onto a typed Kotlin data class ([MapMapper]).
 */
class Yaml {
    private val snakeYaml = SnakeYaml()

    /** Parses and deep-merges [files] (in order) into a single raw map, without typed mapping. */
    fun parseAndMerge(files: List<File>): Map<String, Any?> {
        val layers = files.map { file ->
            @Suppress("UNCHECKED_CAST")
            (snakeYaml.load(file.readText()) as? Map<String, Any?>) ?: emptyMap()
        }
        return mergeAll(layers)
    }

    /** Parses, merges and maps [files] directly onto [T]. */
    inline fun <reified T : Any> load(files: List<File>): T =
        MapMapper.mapTo(parseAndMerge(files), T::class)
}
