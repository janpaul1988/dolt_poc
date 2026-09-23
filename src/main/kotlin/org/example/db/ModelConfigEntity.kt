package org.example.db

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * JPA entity mapped directly onto the flat `model_configs` table (see schema.sql).
 * Compare this to [org.example.config.ModelConfig] + [org.example.yaml.MapMapper]: no reflective
 * map-walking, no manual `req`/`opt` calls - JPA/Hibernate handles reading, writing, dirty
 * checking and SQL generation purely from these annotations.
 */
@Entity
@Table(name = "model_configs")
class ModelConfigEntity(
    @Id
    @Column(name = "id")
    var id: String = "",

    @Column(name = "provider", nullable = false)
    var provider: String = "",

    @Column(name = "temperature", nullable = false)
    var temperature: Double = 0.7,

    @Column(name = "max_tokens", nullable = false)
    var maxTokens: Int = 1024
) {
    // JPA requires a no-arg constructor; the defaults above satisfy that.
    override fun toString(): String =
        "%-12s provider=%-8s temperature=%.2f maxTokens=%d".format(id, provider, temperature, maxTokens)
}
