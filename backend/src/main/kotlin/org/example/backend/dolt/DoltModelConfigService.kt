package org.example.backend.dolt

import org.example.models.BranchRow
import org.example.models.CheckoutState
import org.example.models.CommitRow
import org.example.models.DiffRow
import org.example.models.ModelConfigRow
import org.example.models.ModelConfigsTable
import org.example.models.StatusRow
import org.example.models.TagRow
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Talks to a running `dolt sql-server` over the plain MySQL wire protocol, through Exposed.
 *
 * The CRUD half uses Exposed's typed DSL against [ModelConfigsTable] (shared with the H2 demos
 * in [org.example.backend.exposed]) - Dolt is MySQL-compatible, so the same table definition
 * works unchanged. The version-control half (`dolt_status`, `dolt_diff_*`, `dolt_log`,
 * `dolt_branches`, `dolt_tags`, and stored procedures like `DOLT_COMMIT`/`DOLT_MERGE`) has no
 * typed Exposed API, so those go through Exposed's `Transaction.exec()` raw-SQL escape hatch -
 * still routed through Exposed's connection/transaction management, just without the DSL layer.
 *
 * Every read/write below runs against whatever is currently "checked out" in [state]. Dolt lets
 * you address a branch, tag, or commit directly as part of the database name
 * (`doltdb/<ref>`), so rather than relying on a stateful `DOLT_CHECKOUT` (which only lives for
 * the one JDBC connection that ran it), each transaction connects to a fresh `Database` handle
 * whose JDBC URL already embeds the current ref. Branch-qualified databases are read-write;
 * tag- or commit-qualified ones are inherently read-only in Dolt, which is exactly the
 * "detached HEAD" behaviour this app wants.
 */
class DoltModelConfigService(
    private val host: String = "127.0.0.1",
    private val port: Int = 3307,
    private val user: String = "root",
    private val password: String = ""
) {
    /** Branches this app manages; `main` is the protected/production branch. */
    val mainBranch = "main"
    val developmentBranch = "development"

    @Volatile
    private var state = CheckoutState(ref = mainBranch, detached = false, resolvedHash = "")

    /** Database handle "as of" the currently checked out ref - for all CRUD and history reads/writes. */
    private fun currentDatabase(): Database = databaseFor(state.ref)

    private fun databaseFor(ref: String): Database = Database.connect(
        url = "jdbc:mysql://$host:$port/doltdb/${DoltRefs.sanitize(ref)}",
        driver = "com.mysql.cj.jdbc.Driver",
        user = user,
        password = password
    )

    /** Database handle for the base `doltdb` catalog, unqualified - for branch/tag/merge admin actions. */
    private fun baseDatabase(): Database = Database.connect(
        url = "jdbc:mysql://$host:$port/doltdb",
        driver = "com.mysql.cj.jdbc.Driver",
        user = user,
        password = password
    )

    fun checkoutState(): CheckoutState = state

    /** True while the current checkout cannot (or should not) be written to. */
    fun isReadOnly(): Boolean = state.detached || state.ref == mainBranch

    // ---------- plain CRUD (Exposed DSL against the shared ModelConfigsTable) ----------

    fun findAll(): List<ModelConfigRow> = transaction(currentDatabase()) {
        ModelConfigsTable.selectAll().orderBy(ModelConfigsTable.id).map {
            ModelConfigRow(
                id = it[ModelConfigsTable.id],
                provider = it[ModelConfigsTable.provider],
                temperature = it[ModelConfigsTable.temperature],
                maxTokens = it[ModelConfigsTable.maxTokens]
            )
        }
    }

    fun upsert(row: ModelConfigRow) {
        check(!isReadOnly()) { "Cannot write to '${state.label}'" }
        transaction(currentDatabase()) {
            val updated = ModelConfigsTable.update({ ModelConfigsTable.id eq row.id }) {
                it[provider] = row.provider
                it[temperature] = row.temperature
                it[maxTokens] = row.maxTokens
            }
            if (updated == 0) {
                ModelConfigsTable.insert {
                    it[id] = row.id
                    it[provider] = row.provider
                    it[temperature] = row.temperature
                    it[maxTokens] = row.maxTokens
                }
            }
        }
    }

    fun delete(id: String) {
        check(!isReadOnly()) { "Cannot write to '${state.label}'" }
        transaction(currentDatabase()) {
            ModelConfigsTable.deleteWhere { ModelConfigsTable.id eq id }
        }
    }

    // ---------- Dolt version control (raw SQL via Exposed's exec(), same connection semantics) ----------

    /** Uncommitted changes in the working set, i.e. `dolt status`. */
    fun status(): List<StatusRow> = transaction(currentDatabase()) {
        exec("SELECT table_name, staged, status FROM dolt_status") { rs ->
            buildList { while (rs.next()) add(StatusRow(rs.getString(1), rs.getBoolean(2), rs.getString(3))) }
        }.orEmpty()
    }

    /**
     * Row-level diff of the working set against the last commit (HEAD): each row carries the
     * old and new values plus a diff_type of added/modified/removed. This is the "let the user
     * review the result of their changes before committing" query.
     */
    fun pendingDiff(): List<DiffRow> {
        if (isReadOnly()) return emptyList()
        return transaction(currentDatabase()) {
            exec(
                """
                SELECT diff_type,
                       from_id, to_id,
                       from_provider, to_provider,
                       from_temperature, to_temperature,
                       from_max_tokens, to_max_tokens
                FROM dolt_diff_model_configs
                WHERE to_commit = 'WORKING' OR from_commit = 'WORKING'
                """.trimIndent()
            ) { rs ->
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
            }.orEmpty()
        }
    }

    /** History of whatever is currently checked out - changes as you switch branch/tag/commit. */
    fun log(limit: Int = 15): List<CommitRow> = transaction(currentDatabase()) {
        exec("SELECT commit_hash, message, committer, date FROM dolt_log ORDER BY date DESC LIMIT $limit") { rs ->
            buildList {
                while (rs.next()) add(
                    CommitRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4).toString())
                )
            }
        }.orEmpty()
    }

    /** Stage everything and commit - the "confirm my reviewed changes" action. */
    fun commit(message: String): String {
        check(!isReadOnly()) { "Cannot commit to '${state.label}'" }
        return transaction(currentDatabase()) {
            exec("CALL DOLT_ADD('-A')")
            exec("CALL DOLT_COMMIT('-m', ?)", listOf(VarCharColumnType() to message)) { rs ->
                if (rs.next()) rs.getString(1) else ""
            }.orEmpty()
        }
    }

    /** Throw away all uncommitted changes - the "discard my draft" action. */
    fun discard() {
        if (isReadOnly()) return
        transaction(currentDatabase()) { exec("CALL DOLT_RESET('--hard')") }
    }

    // ---------- branches, tags, checkout & merges ----------

    /** All branches and the tip commit they point at, i.e. `dolt_branches`. */
    fun branches(): List<BranchRow> = transaction(baseDatabase()) {
        exec("SELECT name, hash, latest_committer, latest_commit_message FROM dolt_branches ORDER BY name") { rs ->
            buildList {
                while (rs.next()) add(
                    BranchRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4))
                )
            }
        }.orEmpty()
    }

    /** Create `development` off of `main` the first time the app runs, so both always exist. */
    fun ensureBranch(name: String, from: String = mainBranch) {
        transaction(baseDatabase()) {
            val exists = exec(
                "SELECT COUNT(*) FROM dolt_branches WHERE name = ?",
                listOf(VarCharColumnType() to name)
            ) { rs -> rs.next() && rs.getInt(1) > 0 } ?: false
            if (!exists) {
                exec("CALL DOLT_BRANCH(?, ?)", listOf(VarCharColumnType() to name, VarCharColumnType() to from))
            }
        }
    }

    /** Resolves the HEAD commit hash of whatever `state.ref` currently points at. */
    private fun resolveHead(): String = transaction(currentDatabase()) {
        exec("SELECT commit_hash FROM dolt_log ORDER BY date DESC LIMIT 1") { rs ->
            if (rs.next()) rs.getString(1) else ""
        }.orEmpty()
    }

    /** Check out a real branch - always leaves the app in "attached", writable mode. */
    fun checkoutBranch(name: String) {
        val target = DoltRefs.sanitize(name)
        require(branches().any { it.name == target }) { "No such branch: $target" }
        setStateOrRevert(CheckoutState(ref = target, detached = false, resolvedHash = ""))
    }

    /** Check out a tag or a raw commit hash - always leaves the app in "detached HEAD" mode. */
    fun checkoutRef(ref: String) {
        val target = DoltRefs.sanitize(ref)
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
    fun tags(): List<TagRow> = transaction(baseDatabase()) {
        exec("SELECT tag_name, tag_hash, tagger, message, date FROM dolt_tags ORDER BY date DESC") { rs ->
            buildList {
                while (rs.next()) add(
                    TagRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)?.toString())
                )
            }
        }.orEmpty()
    }

    /** Tag whatever `ref` currently points at (defaults to `main`) with a custom label. */
    fun tag(name: String, ref: String = mainBranch, message: String? = null) {
        transaction(baseDatabase()) {
            if (message.isNullOrBlank()) {
                exec("CALL DOLT_TAG(?, ?)", listOf(VarCharColumnType() to name, VarCharColumnType() to ref))
            } else {
                exec(
                    "CALL DOLT_TAG(?, ?, '-m', ?)",
                    listOf(VarCharColumnType() to name, VarCharColumnType() to ref, VarCharColumnType() to message)
                )
            }
        }
    }

    /**
     * Merge `from` into `into`: checks out the target branch first, then runs `DOLT_MERGE`.
     * Returns a human-readable summary (fast-forward, merged, or the conflict count) built by
     * the pure, independently-unit-tested [describeMergeResult].
     *
     * `@@dolt_allow_commit_conflicts = 1` is required so a conflicting merge returns a normal
     * result row instead of throwing and auto-rolling-back the transaction. This app has no UI
     * to resolve conflicts row-by-row (`dolt_conflicts_*` tables), so on any conflict it
     * immediately runs `DOLT_MERGE('--abort')` to leave the target branch exactly as it was, and
     * reports which table(s) conflicted so the user can resolve them with the Dolt CLI.
     */
    fun merge(from: String, into: String): String = transaction(baseDatabase()) {
        exec("CALL DOLT_CHECKOUT(?)", listOf(VarCharColumnType() to into))
        exec("SET @@dolt_allow_commit_conflicts = 1")
        exec("CALL DOLT_MERGE(?)", listOf(VarCharColumnType() to from)) { rs ->
            if (rs.next()) {
                val fastForward = rs.getInt("fast_forward")
                val conflicts = rs.getInt("conflicts")
                val conflictingTables = if (conflicts > 0) {
                    val tables = exec("SELECT `table` FROM dolt_conflicts") { crs ->
                        buildList { while (crs.next()) add(crs.getString(1)) }
                    }.orEmpty()
                    exec("CALL DOLT_MERGE('--abort')")
                    tables.ifEmpty { listOf("model_configs") }
                } else {
                    emptyList()
                }
                describeMergeResult(from, into, fastForward == 1, conflictingTables)
            } else {
                "Merge completed."
            }
        }.orEmpty()
    }
}
