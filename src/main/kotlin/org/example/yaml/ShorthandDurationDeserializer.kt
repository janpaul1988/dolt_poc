package org.example.yaml

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.time.Duration

/**
 * The *only* piece of custom logic Jackson needs for this whole config model: how to turn a
 * shorthand string like "500ms"/"30s" into a [Duration]. Everything else - required/optional
 * fields, nested objects, lists, enums - falls out of jackson-module-kotlin reading the data
 * class's constructor directly, no `MapMapper`/`FromMap` equivalent required.
 *
 * Registered once on the [com.fasterxml.jackson.databind.ObjectMapper] (see [JacksonYaml]),
 * it then applies everywhere a `Duration` field occurs, not just on one class.
 */
class ShorthandDurationDeserializer : StdDeserializer<Duration>(Duration::class.java) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Duration =
        Durations.parse(parser.text)
}
