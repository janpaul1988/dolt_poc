package org.example.models

/** One row of `dolt_status`: an uncommitted change in the working set. */
data class StatusRow(val tableName: String, val staged: Boolean, val status: String)

/** One row of `dolt_log`: a commit in the history. */
data class CommitRow(val hash: String, val message: String, val committer: String, val date: String)

/** One row of `dolt_branches`: a branch and the commit it currently points at. */
data class BranchRow(val name: String, val hash: String, val latestCommitter: String?, val latestMessage: String?)

/** One row of `dolt_tags`: a named pointer at a commit. */
data class TagRow(val name: String, val hash: String, val tagger: String?, val message: String?, val date: String?)

/**
 * One row of `dolt_diff_model_configs`: shows old (`from_`) and new (`to_`) values side by side,
 * which is exactly what's rendered in the "pending changes" review screen.
 */
data class DiffRow(
    val diffType: String,
    val fromId: String?,
    val toId: String?,
    val fromProvider: String?,
    val toProvider: String?,
    val fromTemperature: Double?,
    val toTemperature: Double?,
    val fromMaxTokens: Int?,
    val toMaxTokens: Int?
)

/** The single checkout that the whole app is currently "looking at". */
data class CheckoutState(
    /** Branch name, tag name, or commit hash - whatever was last checked out. */
    val ref: String,
    /** True once a tag or a raw commit hash was checked out rather than a branch. */
    val detached: Boolean,
    /** Resolved commit hash of [ref], used to label a detached HEAD. */
    val resolvedHash: String
) {
    val label: String get() = if (detached) "detached HEAD (${resolvedHash.take(8)})" else ref
}
