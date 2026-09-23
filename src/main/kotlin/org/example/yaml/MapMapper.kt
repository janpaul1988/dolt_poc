package org.example.yaml

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/**
 * Implement this on a data class's **companion object** whenever that class needs custom
 * construction logic that plain reflection cannot express - e.g. parsing "30s" into a
 * [java.time.Duration], validating/deriving fields, applying cross-field defaults, etc.
 *
 * Example:
 * ```
 * data class RetryPolicy(val maxAttempts: Int, val initialDelay: Duration, ...) {
 *     init { require(maxAttempts > 0) }
 *
 *     companion object : FromMap<RetryPolicy> {
 *         override fun fromMap(map: Map<String, Any?>): RetryPolicy = RetryPolicy(
 *             maxAttempts = (map["maxAttempts"] as? Number)?.toInt() ?: 3,
 *             initialDelay = Durations.parse(map["initialDelay"] as? String ?: "500ms"),
 *             ...
 *         )
 *     }
 * }
 * ```
 * [MapMapper] checks for this interface *before* falling back to generic reflection, so classes
 * that need it "opt in" simply by declaring the companion object - no changes to the mapper
 * itself are required.
 */
interface FromMap<T : Any> {
    fun fromMap(map: Map<String, Any?>): T
}

/**
 * Generic reflection-based mapper from a merged raw YAML `Map<String, Any?>` tree onto Kotlin
 * data classes.
 *
 * Supported constructor parameter types: String, Int, Long, Double, Float, Boolean, enums,
 * nested data classes, `List<T>` of any of the above, and plain `Map<String, Any?>` passthrough.
 * Missing keys fall back to the parameter's default value (if any) or `null` (if nullable);
 * otherwise mapping fails fast with a descriptive error.
 */
object MapMapper {

    fun <T : Any> mapTo(map: Map<String, Any?>, klass: KClass<T>): T {
        val companion = klass.companionObjectInstance
        if (companion is FromMap<*>) {
            @Suppress("UNCHECKED_CAST")
            return (companion as FromMap<T>).fromMap(map)
        }
        return mapReflectively(map, klass)
    }

    private fun <T : Any> mapReflectively(map: Map<String, Any?>, klass: KClass<T>): T {
        val ctor = klass.primaryConstructor
            ?: error("Cannot map onto ${klass.simpleName}: no primary constructor")

        val args = mutableMapOf<KParameter, Any?>()
        for (param in ctor.parameters) {
            val name = param.name ?: continue
            when {
                map.containsKey(name) -> args[param] = convert(map[name], param.type, "${klass.simpleName}.$name")
                param.isOptional -> Unit // let callBy fill in the declared default
                param.type.isMarkedNullable -> args[param] = null
                else -> error("Missing required key '$name' for ${klass.simpleName}")
            }
        }
        return ctor.callBy(args)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convert(value: Any?, type: KType, path: String): Any? {
        if (value == null) return null
        val erasure = type.jvmErasure
        return when {
            erasure == String::class -> value.toString()
            erasure == Int::class -> (value as Number).toInt()
            erasure == Long::class -> (value as Number).toLong()
            erasure == Double::class -> (value as Number).toDouble()
            erasure == Float::class -> (value as Number).toFloat()
            erasure == Boolean::class -> value as Boolean
            erasure == java.time.Duration::class -> Durations.parse(value.toString())
            erasure.java.isEnum ->
                java.lang.Enum.valueOf(erasure.java as Class<out Enum<*>>, value.toString().uppercase())

            erasure == List::class -> {
                val elementType = type.arguments.first().type
                    ?: error("Cannot resolve element type for $path")
                (value as List<*>).map { convert(it, elementType, "$path[]") }
            }

            erasure == Map::class -> value as Map<String, Any?>

            erasure.isData -> mapTo(value as Map<String, Any?>, erasure as KClass<Any>)

            else -> error("Unsupported type '${erasure.simpleName}' at $path")
        }
    }
}
