package org.example.yaml

import java.time.Duration

/** Parses short-hand duration strings such as "500ms", "30s", "2m", "1h" into [Duration]. */
object Durations {
    private val pattern = Regex("""^(\d+)(ms|s|m|h)$""")

    fun parse(raw: String): Duration {
        val trimmed = raw.trim()
        val match = pattern.matchEntire(trimmed)
            ?: error("Invalid duration '$raw', expected e.g. '500ms', '30s', '2m', '1h'")
        val (amount, unit) = match.destructured
        val value = amount.toLong()
        return when (unit) {
            "ms" -> Duration.ofMillis(value)
            "s" -> Duration.ofSeconds(value)
            "m" -> Duration.ofMinutes(value)
            "h" -> Duration.ofHours(value)
            else -> error("Unreachable")
        }
    }
}
