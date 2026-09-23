package org.example.config

import org.example.yaml.Durations
import org.example.yaml.FromMap
import java.time.Duration

enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }

data class AppInfo(
    val name: String,
    val version: String = "0.0.1"
)

data class ModelConfig(
    val id: String,
    val provider: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024
)

data class TestSuite(
    val name: String,
    val tags: List<String> = emptyList(),
    val prompts: List<String> = emptyList()
)

data class ThresholdConfig(
    val minAccuracy: Double,
    val maxLatencyMs: Long,
    val severity: Severity = Severity.MEDIUM
)

/**
 * Needs custom construction logic (string -> [Duration] parsing plus cross-field validation),
 * so it opts into that by implementing [FromMap] on its companion object. [MapMapper] detects
 * this automatically and calls [Companion.fromMap] instead of generic reflection.
 */
data class RetryPolicy(
    // Defaults live directly on the constructor now: idiomatic Kotlin (no map-reading code
    // needed at all) covers what FromMap.fromMap used to do by hand for missing keys.
    val maxAttempts: Int = 3,
    val initialDelay: Duration = Durations.parse("500ms"),
    val maxDelay: Duration = Durations.parse("30s"),
    val backoffMultiplier: Double = 2.0
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be >= 1.0" }
        require(maxDelay >= initialDelay) { "maxDelay must be >= initialDelay" }
    }

    companion object : FromMap<RetryPolicy> {
        override fun fromMap(map: Map<String, Any?>): RetryPolicy = RetryPolicy(
            maxAttempts = (map["maxAttempts"] as? Number)?.toInt() ?: 3,
            initialDelay = Durations.parse(map["initialDelay"] as? String ?: "500ms"),
            maxDelay = Durations.parse(map["maxDelay"] as? String ?: "30s"),
            backoffMultiplier = (map["backoffMultiplier"] as? Number)?.toDouble() ?: 2.0
        )
    }
}

/**
 * Root config, assembled from several layered YAML files. `models` and `testSuites` are lists
 * that get concatenated across files (see [org.example.yaml.deepMerge]); `retry` and `thresholds`
 * are single objects whose scalar fields get overridden by later files.
 */
data class AppConfig(
    val app: AppInfo,
    val models: List<ModelConfig> = emptyList(),
    val testSuites: List<TestSuite> = emptyList(),
    val retry: RetryPolicy,
    val thresholds: ThresholdConfig
)
