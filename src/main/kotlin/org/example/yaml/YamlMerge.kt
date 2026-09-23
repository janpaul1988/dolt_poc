package org.example.yaml

/**
 * Deep-merges two raw YAML maps (as produced by SnakeYAML: `Map<String, Any?>`, `List<Any?>`
 * and scalar leaves).
 *
 * Merge rules (applied key by key, recursively):
 *  - Map + Map        -> recursively merged
 *  - List + List      -> concatenated (this is how "layered" files combine e.g. a `models:`
 *                        or `testSuites:` list defined in several files into one list)
 *  - anything else    -> the later value wins (simple override), which lets an override file
 *                        replace a scalar (e.g. `app.version`) set by an earlier/base file
 */
@Suppress("UNCHECKED_CAST")
fun deepMerge(base: Map<String, Any?>, override: Map<String, Any?>): Map<String, Any?> {
    val result = LinkedHashMap<String, Any?>(base)
    for ((key, overrideValue) in override) {
        val baseValue = result[key]
        result[key] = when {
            baseValue is Map<*, *> && overrideValue is Map<*, *> ->
                deepMerge(baseValue as Map<String, Any?>, overrideValue as Map<String, Any?>)

            baseValue is List<*> && overrideValue is List<*> ->
                baseValue + overrideValue

            else -> overrideValue
        }
    }
    return result
}

/** Merges any number of layered YAML maps, in order, using [deepMerge]. */
fun mergeAll(layers: List<Map<String, Any?>>): Map<String, Any?> =
    layers.fold(emptyMap()) { acc, layer -> deepMerge(acc, layer) }
