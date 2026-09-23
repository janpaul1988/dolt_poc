package org.example.backend.dolt

/** Validates and passes through a branch/tag/commit-hash ref before it's used to build a JDBC URL. */
internal object DoltRefs {
    fun sanitize(ref: String): String {
        require(ref.isNotBlank() && ref.none { it == '`' || it == ';' || it.isWhitespace() }) { "Invalid ref: $ref" }
        return ref
    }
}

/**
 * Pure formatting of a `DOLT_MERGE` outcome into a human-readable summary, kept separate from
 * [DoltModelConfigService] so it's unit-testable without a live Dolt server.
 */
internal fun describeMergeResult(
    from: String,
    into: String,
    fastForward: Boolean,
    conflictingTables: List<String>
): String = when {
    conflictingTables.isNotEmpty() ->
        "Merge aborted: conflicts in ${conflictingTables.joinToString()}. " +
            "Resolve manually with the Dolt CLI, then retry."
    fastForward -> "Fast-forwarded '$into' to '$from'."
    else -> "Merged '$from' into '$into'."
}
