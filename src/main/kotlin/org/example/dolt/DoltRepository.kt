package org.example.dolt

import java.sql.Connection
import java.sql.DriverManager

data class ModelConfigRow(
    val id: String,
    val provider: String,
    val temperature: Double,
    val maxTokens: Int
)

/** One row of `dolt_status`: an uncommitted change in the working set. */
data class StatusRow(val tableName: String, val staged: Boolean, val status: String)

/** One row of `dolt_log`: a commit in the history. */
data class CommitRow(val hash: String, val message: String, val committer: String, val date: String)

/** One row of `dolt_branches`: a branch and the commit it currently points at. */
data class BranchRow(val name: String, val hash: String, val latestCommitter: String?, val latestMessage: String?)

/** One row of `dolt_tags`: a named pointer at a commit. */
data class TagRow(val name: String, val hash: String, val tagger: String?, val message: String?, val date: String?)

/**
 * One row of `dolt_diff_<table>`: shows old (`from_`) and new (`to_`) values side by side,
 * which is exactly what we render in the "pending changes" review screen.
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
    /** True once the user checked out a tag or a raw commit hash rather than a branch. */
    val detached: Boolean,
    /** Resolved commit hash of [ref], used to label a detached HEAD. */
    val resolvedHash: String
) {
    val label: String get() = if (detached) "detached HEAD (${resolvedHash.take(8)})" else ref
}

/**
 * Talks to a running `dolt sql-server` over the plain MySQL wire protocol.
 *
 * The CRUD half is ordinary SQL - Dolt is MySQL-compatible, so nothing special is needed.
 * The interesting half is [status]/[diff]/[log]/[commit]: Dolt exposes its version control
 * as system tables (`dolt_status`, `dolt_diff_*`, `dolt_log`) and stored procedures
 * (`DOLT_ADD`, `DOLT_COMMIT`, `DOLT_RESET`), so "show the user their pending changes before
 * they commit" is just a query - no audit tables or triggers to build yourself.
 *
 * Every read/write below runs against whatever is currently "checked out" in [state]. Dolt lets
 * you address a branch, tag, or commit directly as part of the database name
 * (`doltdb/<ref>`), so instead of relying on a stateful `DOLT_CHECKOUT` (which only lives for the
 * one JDBC connection that ran it), we just re-issue `USE `doltdb/<ref>`` on every fresh
 * connection. Branch-qualified databases are read-write; tag- or commit-qualified ones are
 * inherently read-only in Dolt, which is exactly the "detached HEAD" behaviour we want.
 */
class DoltRepository(
    private val url: String = "jdbc:mysql://127.0.0.1:3307/doltdb",
    private val user: String = "root",
    private val password: String = ""
) {
    /** Branches this app manages; `main` is the protected/production branch. */
    val mainBranch = "main"
    val developmentBranch = "development"

    @Volatile
    private var state = CheckoutState(ref = mainBranch, detached = false, resolvedHash = "")

    private fun sanitizeRef(ref: String): String {
        require(ref.isNotBlank() && ref.none { it == '`' || it == ';' || it.isWhitespace() }) { "Invalid ref: $ref" }
        return ref
    }

    /** Connects to the base `doltdb` catalog, unqualified - for branch/tag/merge admin actions. */
    private fun <T> connectBase(block: (Connection) -> T): T =
        DriverManager.getConnection(url, user, password).use(block)

    /** Connects "as of" the currently checked out ref - for all CRUD and history reads/writes. */
    private fun <T> connect(block: (Connection) -> T): T = connectBase { c ->
        c.createStatement().execute("USE `doltdb/${sanitizeRef(state.ref)}`")
        block(c)
    }

    fun checkoutState(): CheckoutState = state

    /** True while the current checkout cannot (or should not) be written to. */
    fun isReadOnly(): Boolean = state.detached || state.ref == mainBranch

    // ---------- plain CRUD (ordinary MySQL SQL) ----------

    fun findAll(): List<ModelConfigRow> = connect { c ->
        c.prepareStatement("SELECT id, provider, temperature, max_tokens FROM model_configs ORDER BY id")
            .executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(
                        ModelConfigRow(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getInt(4))
                    )
                }
            }
    }

    fun upsert(row: ModelConfigRow) {
        check(!isReadOnly()) { "Cannot write to '${state.label}'" }
        connect { c ->
            c.prepareStatement(
                """
                INSERT INTO model_configs (id, provider, temperature, max_tokens)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE provider = VALUES(provider),
                                        temperature = VALUES(temperature),
                                        max_tokens = VALUES(max_tokens)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, row.id)
                ps.setString(2, row.provider)
                ps.setDouble(3, row.temperature)
                ps.setInt(4, row.maxTokens)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: String) {
        check(!isReadOnly()) { "Cannot write to '${state.label}'" }
        connect { c ->
            c.prepareStatement("DELETE FROM model_configs WHERE id = ?").use { ps ->
                ps.setString(1, id)
                ps.executeUpdate()
            }
        }
    }

    // ---------- Dolt version control ----------

    /** Uncommitted changes in the working set, i.e. `dolt status`. */
    fun status(): List<StatusRow> = connect { c ->
        c.prepareStatement("SELECT table_name, staged, status FROM dolt_status").executeQuery().use { rs ->
            buildList {
                while (rs.next()) add(StatusRow(rs.getString(1), rs.getBoolean(2), rs.getString(3)))
            }
        }
    }

    /**
     * Row-level diff of the working set against the last commit (HEAD): each row carries the
     * old and new values plus a diff_type of added/modified/removed. This is the "let the user
     * review the result of their changes before committing" query.
     */
    fun pendingDiff(): List<DiffRow> {
        if (isReadOnly()) return emptyList()
        return connect { c ->
            c.prepareStatement(
                """
                SELECT diff_type,
                       from_id, to_id,
                       from_provider, to_provider,
                       from_temperature, to_temperature,
                       from_max_tokens, to_max_tokens
                FROM dolt_diff_model_configs
                WHERE to_commit = 'WORKING' OR from_commit = 'WORKING'
                """.trimIndent()
            ).executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(
                        DiffRow(
                            diffType = rs.getString("diff_type"),
                            fromId = rs.getString("from_id"),
                            toId = rs.getString("to_id"),
                            fromProvider = rs.getString("from_provider"),
                            toProvider = rs.getString("to_provider"),
                            fromTemperature = rs.getObject("from_temperature") as? Double,
                            toTemperature = rs.getObject("to_temperature") as? Double,
                            fromMaxTokens = (rs.getObject("from_max_tokens") as? Number)?.toInt(),
                            toMaxTokens = (rs.getObject("to_max_tokens") as? Number)?.toInt()
                        )
                    )
                }
            }
        }
    }

    /** History of whatever is currently checked out - changes as you switch branch/tag/commit. */
    fun log(limit: Int = 15): List<CommitRow> = connect { c ->
        c.prepareStatement("SELECT commit_hash, message, committer, date FROM dolt_log ORDER BY date DESC LIMIT $limit")
            .executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(
                        CommitRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4).toString())
                    )
                }
            }
    }

    /** Stage everything and commit - the "confirm my reviewed changes" action. */
    fun commit(message: String): String {
        check(!isReadOnly()) { "Cannot commit to '${state.label}'" }
        return connect { c ->
            c.createStatement().execute("CALL DOLT_ADD('-A')")
            c.prepareStatement("CALL DOLT_COMMIT('-m', ?)").use { ps ->
                ps.setString(1, message)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else "" }
            }
        }
    }

    /** Throw away all uncommitted changes - the "discard my draft" action. */
    fun discard() {
        if (isReadOnly()) return
        connect { c -> c.createStatement().execute("CALL DOLT_RESET('--hard')") }
    }

    // ---------- branches, tags, checkout & merges ----------

    /** All branches and the tip commit they point at, i.e. `dolt_branches`. */
    fun branches(): List<BranchRow> = connectBase { c ->
        c.createStatement().executeQuery(
            "SELECT name, hash, latest_committer, latest_commit_message FROM dolt_branches ORDER BY name"
        ).use { rs ->
            buildList {
                while (rs.next()) add(
                    BranchRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4))
                )
            }
        }
    }

    /** Create `development` off of `main` the first time the app runs, so both always exist. */
    fun ensureBranch(name: String, from: String = mainBranch) = connectBase { c ->
        val exists = c.createStatement().executeQuery(
            "SELECT COUNT(*) FROM dolt_branches WHERE name = '${name.replace("'", "")}'"
        ).use { rs -> rs.next() && rs.getInt(1) > 0 }
        if (!exists) {
            c.prepareStatement("CALL DOLT_BRANCH(?, ?)").use { ps ->
                ps.setString(1, name)
                ps.setString(2, from)
                ps.execute()
            }
        }
    }

    /** Resolves the HEAD commit hash of whatever `state.ref` currently points at. */
    private fun resolveHead(): String = connect { c ->
        c.createStatement().executeQuery("SELECT commit_hash FROM dolt_log ORDER BY date DESC LIMIT 1").use { rs ->
            if (rs.next()) rs.getString(1) else ""
        }
    }

    /** Check out a real branch - always leaves the app in "attached", writable mode. */
    fun checkoutBranch(name: String) {
        val target = sanitizeRef(name)
        require(branches().any { it.name == target }) { "No such branch: $target" }
        setStateOrRevert(CheckoutState(ref = target, detached = false, resolvedHash = ""))
    }

    /** Check out a tag or a raw commit hash - always leaves the app in "detached HEAD" mode. */
    fun checkoutRef(ref: String) {
        val target = sanitizeRef(ref)
        setStateOrRevert(CheckoutState(ref = target, detached = true, resolvedHash = ""))
    }

    /**
     * Applies [candidate] and resolves its HEAD hash; if the ref turns out to be invalid (bad
     * commit hash, unknown tag, ...) rolls back to whatever was checked out before, so a typo
     * never leaves the whole app stuck on a broken checkout.
     */
    private fun setStateOrRevert(candidate: CheckoutState) {
        val previous = state
        state = candidate
        val resolved = try {
            resolveHead()
        } catch (e: Exception) {
            state = previous
            throw IllegalArgumentException("No such ref: '${candidate.ref}'", e)
        }
        if (resolved.isBlank()) {
            state = previous
            throw IllegalArgumentException("No such ref: '${candidate.ref}'")
        }
        state = candidate.copy(resolvedHash = resolved)
    }

    /** All tags, i.e. `dolt_tags`. */
    fun tags(): List<TagRow> = connectBase { c ->
        c.createStatement().executeQuery(
            "SELECT tag_name, tag_hash, tagger, message, date FROM dolt_tags ORDER BY date DESC"
        ).use { rs ->
            buildList {
                while (rs.next()) add(
                    TagRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)?.toString())
                )
            }
        }
    }

    /** Tag whatever `ref` currently points at (defaults to `main`) with a custom label. */
    fun tag(name: String, ref: String = mainBranch, message: String? = null) = connectBase { c ->
        if (message.isNullOrBlank()) {
            c.prepareStatement("CALL DOLT_TAG(?, ?)").use { ps ->
                ps.setString(1, name)
                ps.setString(2, ref)
                ps.execute()
            }
        } else {
            c.prepareStatement("CALL DOLT_TAG(?, ?, '-m', ?)").use { ps ->
                ps.setString(1, name)
                ps.setString(2, ref)
                ps.setString(3, message)
                ps.execute()
            }
        }
    }

    /**
     * Merge `from` into `into`: checks out the target branch first, then runs `DOLT_MERGE`.
     * Returns a human-readable summary (fast-forward, merged, or the conflict count).
     *
     * `@@dolt_allow_commit_conflicts = 1` is required so a conflicting merge returns a normal
     * result row instead of throwing and auto-rolling-back the transaction. This app has no UI
     * to resolve conflicts row-by-row (`dolt_conflicts_*` tables), so on any conflict we
     * immediately `DOLT_MERGE('--abort')` to leave the target branch exactly as it was, and
     * report which table(s) conflicted so the user can resolve them with the Dolt CLI.
     */
    fun merge(from: String, into: String): String = connectBase { c ->
        c.prepareStatement("CALL DOLT_CHECKOUT(?)").use { ps -> ps.setString(1, into); ps.execute() }
        c.createStatement().execute("SET @@dolt_allow_commit_conflicts = 1")
        c.prepareStatement("CALL DOLT_MERGE(?)").use { ps ->
            ps.setString(1, from)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    val fastForward = rs.getInt("fast_forward")
                    val conflicts = rs.getInt("conflicts")
                    when {
                        conflicts > 0 -> {
                            val tables = c.createStatement()
                                .executeQuery("SELECT `table` FROM dolt_conflicts")
                                .use { crs -> buildList { while (crs.next()) add(crs.getString(1)) } }
                            c.createStatement().execute("CALL DOLT_MERGE('--abort')")
                            "Merge aborted: conflicts in ${tables.ifEmpty { listOf("model_configs") }.joinToString()}. " +
                                "Resolve manually with the Dolt CLI, then retry."
                        }
                        fastForward == 1 -> "Fast-forwarded '$into' to '$from'."
                        else -> "Merged '$from' into '$into'."
                    }
                } else "Merge completed."
            }
        }
    }
}
