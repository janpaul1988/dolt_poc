package org.example.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.time.Duration

/**
 * Jackson-based equivalent of [Yaml]. Compare this whole file to [MapMapper] (~100 lines) plus
 * the `FromMap` interface: parsing, required/optional handling, nested objects, lists and enums
 * are all handled by jackson-module-kotlin reflecting over each data class's primary
 * constructor - the only code we still write ourselves is the merge step (unchanged, and
 * independent of the mapping technology) and the one custom [Duration] deserializer.
 */
class JacksonYaml {
    val mapper: ObjectMapper = ObjectMapper(YAMLFactory())
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .registerModule(SimpleModule().addDeserializer(Duration::class.java, ShorthandDurationDeserializer()))

    /** Parses, deep-merges (list-concatenating, same as [Yaml.parseAndMerge]) and maps onto [T]. */
    inline fun <reified T : Any> load(files: List<File>): T {
        val layers = files.map { file ->
            @Suppress("UNCHECKED_CAST")
            (mapper.readValue(file, Map::class.java) as Map<String, Any?>)
        }
        val merged = mergeAll(layers)
        return mapper.convertValue(merged, T::class.java)
    }
}
