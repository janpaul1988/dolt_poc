package org.example.frontend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.example.backend.dolt.DoltModelConfigService
import org.example.models.ModelConfigRow

/**
 * Minimal HTML CRUD frontend on top of Dolt, showing the whole workflow: edit rows (ordinary SQL
 * against a MySQL-compatible server), *review* the uncommitted result as a row-level diff before
 * anything is final, then either commit (confirm) or discard (reset --hard).
 *
 * This module only talks to [DoltModelConfigService] in `:backend` - it never opens a JDBC
 * connection or touches `:models`' table definitions itself.
 *
 * Run with `./gradlew :frontend:runDoltWeb`, then open http://localhost:8080
 */
fun main() {
    val repo = DoltModelConfigService()
    repo.ensureBranch(repo.developmentBranch, from = repo.mainBranch)
    repo.checkoutBranch(repo.mainBranch)

    embeddedServer(Netty, port = 8080) {
        module(repo)
    }.start(wait = true)
}

/** Wires up all routes against the given backend service - kept separate from [main] so tests can inject a service. */
fun Application.module(repo: DoltModelConfigService) {
    routing {
        get("/") {
            val flash = call.request.queryParameters["flash"]
            call.respondHtml { page(repo, flash) }
        }

        post("/checkout") {
            runCatching { repo.checkoutBranch(call.receiveParameters()["branch"].orEmpty().trim()) }
            call.respondRedirect("/")
        }

        post("/checkout-ref") {
            runCatching { repo.checkoutRef(call.receiveParameters()["ref"].orEmpty().trim()) }
            call.respondRedirect("/")
        }

        post("/tag") {
            val p = call.receiveParameters()
            val name = p["name"].orEmpty().trim()
            val ref = p["ref"].orEmpty().trim().ifBlank { repo.mainBranch }
            val message = p["message"].orEmpty().trim().ifBlank { null }
            if (name.isNotBlank()) repo.tag(name, ref, message)
            call.respondRedirect("/")
        }

        post("/merge") {
            val p = call.receiveParameters()
            val from = p["from"].orEmpty().trim().ifBlank { repo.developmentBranch }
            val into = p["into"].orEmpty().trim().ifBlank { repo.mainBranch }
            val result = runCatching { repo.merge(from, into) }
                .getOrElse { "Merge failed: ${it.message}" }
            call.respondRedirect("/?flash=${result.encodeURLQueryComponent()}")
        }

        post("/save") {
            val p = call.receiveParameters()
            if (!repo.isReadOnly()) {
                repo.upsert(
                    ModelConfigRow(
                        id = p["id"].orEmpty().trim(),
                        provider = p["provider"].orEmpty().trim().ifBlank { "openai" },
                        temperature = p["temperature"]?.toDoubleOrNull() ?: 0.7,
                        maxTokens = p["maxTokens"]?.toIntOrNull() ?: 1024
                    )
                )
            }
            call.respondRedirect("/")
        }

        post("/delete") {
            if (!repo.isReadOnly()) repo.delete(call.receiveParameters()["id"].orEmpty())
            call.respondRedirect("/")
        }

        post("/commit") {
            if (!repo.isReadOnly()) {
                val msg = call.receiveParameters()["message"].orEmpty().ifBlank { "Update model configs" }
                repo.commit(msg)
            }
            call.respondRedirect("/")
        }

        post("/discard") {
            if (!repo.isReadOnly()) repo.discard()
            call.respondRedirect("/")
        }
    }
}

private fun HTML.page(repo: DoltModelConfigService, flash: String? = null) {
    val checkout = repo.checkoutState()
    val readOnly = repo.isReadOnly()
    val rows = repo.findAll()
    val pending = repo.pendingDiff()
    val commits = repo.log()
    val branches = repo.branches()
    val tags = repo.tags()

    head {
        title("dolt_poc - model configs (Dolt)")
        style { unsafe { raw(CSS) } }
    }
    body {
        div("wrap") {
            header {
                h1 { +"Model configs" }
                p("sub") {
                    +"Checked out: "
                    span(if (checkout.detached) "branch detached" else "branch") { +checkout.label }
                    if (readOnly) span("chip removed") { +"read-only" }
                    +" - every change is reviewable before it is committed."
                }
            }

            if (!flash.isNullOrBlank()) {
                div("flash") { +flash }
            }

            section {
                h2 { +"Branches" }
                p("muted") { +"Switch the active branch, tag main with a custom label, or merge development into main." }
                table {
                    thead { tr { th { +"branch" }; th { +"head commit" }; th { +"last message" }; th { +"" } } }
                    tbody {
                        branches.forEach { b ->
                            val isCurrent = !checkout.detached && b.name == checkout.ref
                            tr {
                                td { if (isCurrent) span("branch") { +b.name } else +b.name }
                                td { code { +b.hash.take(8) } }
                                td("muted") { +(b.latestMessage ?: "") }
                                td("actions") {
                                    if (!isCurrent) {
                                        form(action = "/checkout", method = FormMethod.post) {
                                            hiddenInput(name = "branch") { value = b.name }
                                            submitInput(classes = "btn small") { value = "Checkout" }
                                        }
                                    } else {
                                        span("muted") { +"current" }
                                    }
                                }
                            }
                        }
                    }
                }
                div("commitbar") {
                    form(action = "/tag", method = FormMethod.post, classes = "row-form") {
                        hiddenInput(name = "ref") { value = repo.mainBranch }
                        textInput(name = "name") { placeholder = "tag label (e.g. 1.0)"; required = true }
                        textInput(name = "message") { placeholder = "optional message" }
                        submitInput(classes = "btn") { value = "Tag main" }
                    }
                    form(action = "/merge", method = FormMethod.post, classes = "row-form") {
                        hiddenInput(name = "from") { value = repo.developmentBranch }
                        hiddenInput(name = "into") { value = repo.mainBranch }
                        submitInput(classes = "btn primary") { value = "Merge development -> main" }
                    }
                }
                if (tags.isNotEmpty()) {
                    table {
                        thead { tr { th { +"tag" }; th { +"commit" }; th { +"message" }; th { +"date" }; th { +"" } } }
                        tbody {
                            tags.forEach { t ->
                                val isCurrent = checkout.detached && checkout.ref == t.name
                                tr {
                                    td { span("chip added") { +t.name } }
                                    td { code { +t.hash.take(8) } }
                                    td("muted") { +(t.message ?: "") }
                                    td("muted") { +(t.date ?: "") }
                                    td("actions") {
                                        if (!isCurrent) {
                                            form(action = "/checkout-ref", method = FormMethod.post) {
                                                hiddenInput(name = "ref") { value = t.name }
                                                submitInput(classes = "btn small") { value = "Checkout" }
                                            }
                                        } else {
                                            span("muted") { +"current" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            section {
                h2 { +"Current data" }
                if (readOnly) {
                    p("muted") { +"Read-only checkout - switch to 'development' to edit." }
                }
                table {
                    thead {
                        tr {
                            th { +"id" }; th { +"provider" }; th { +"temperature" }; th { +"max tokens" }
                            if (!readOnly) th { +"" }
                        }
                    }
                    tbody {
                        if (rows.isEmpty()) {
                            tr { td { attributes["colspan"] = if (readOnly) "4" else "5"; classes = setOf("empty"); +"No rows yet" } }
                        }
                        rows.forEach { r ->
                            val formId = "f-${r.id}"
                            tr {
                                td { +r.id }
                                if (readOnly) {
                                    td { +r.provider }
                                    td { +r.temperature.toString() }
                                    td { +r.maxTokens.toString() }
                                } else {
                                    td {
                                        textInput(name = "provider") {
                                            attributes["form"] = formId; value = r.provider
                                        }
                                    }
                                    td {
                                        numberInput(name = "temperature") {
                                            attributes["form"] = formId; step = "0.1"; value = r.temperature.toString()
                                        }
                                    }
                                    td {
                                        numberInput(name = "maxTokens") {
                                            attributes["form"] = formId; value = r.maxTokens.toString()
                                        }
                                    }
                                    td("actions") {
                                        form(action = "/save", method = FormMethod.post) {
                                            id = formId
                                            hiddenInput(name = "id") { value = r.id }
                                            submitInput(classes = "btn small") { value = "Save" }
                                        }
                                        form(action = "/delete", method = FormMethod.post) {
                                            hiddenInput(name = "id") { value = r.id }
                                            submitInput(classes = "btn small danger") { value = "Delete" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!readOnly) {
                section {
                    h2 { +"Add / update a model" }
                    form(action = "/save", method = FormMethod.post, classes = "row-form") {
                        textInput(name = "id") { placeholder = "id"; required = true }
                        textInput(name = "provider") { placeholder = "provider" }
                        numberInput(name = "temperature") { placeholder = "temperature"; step = "0.1" }
                        numberInput(name = "maxTokens") { placeholder = "maxTokens" }
                        submitInput(classes = "btn") { value = "Save" }
                    }
                }

                section {
                    h2 {
                        +"Pending changes "
                        span("badge") { +"${pending.size}" }
                    }
                    if (pending.isEmpty()) {
                        p("muted") { +"Working set is clean - nothing to review." }
                    } else {
                        p("muted") { +"These are your uncommitted edits, diffed against the last commit. Adjust them above, or confirm below." }
                        table {
                            thead {
                                tr { th { +"change" }; th { +"id" }; th { +"provider" }; th { +"temperature" }; th { +"max tokens" } }
                            }
                            tbody {
                                pending.forEach { d ->
                                    tr {
                                        td { span("chip ${d.diffType}") { +d.diffType } }
                                        td { +(d.toId ?: d.fromId ?: "") }
                                        td { diffCell(d.fromProvider, d.toProvider, d.diffType) }
                                        td { diffCell(d.fromTemperature?.toString(), d.toTemperature?.toString(), d.diffType) }
                                        td { diffCell(d.fromMaxTokens?.toString(), d.toMaxTokens?.toString(), d.diffType) }
                                    }
                                }
                            }
                        }
                        div("commitbar") {
                            form(action = "/commit", method = FormMethod.post, classes = "row-form") {
                                textInput(name = "message") { placeholder = "Commit message"; required = true }
                                submitInput(classes = "btn primary") { value = "Commit changes" }
                            }
                            form(action = "/discard", method = FormMethod.post) {
                                submitInput(classes = "btn danger") { value = "Discard all" }
                            }
                        }
                    }
                }
            }

            section {
                h2 { +"History" }
                p("muted") { +"Commits on '${checkout.label}'. Check out a commit to view/detach at that point." }
                table {
                    thead { tr { th { +"commit" }; th { +"message" }; th { +"committer" }; th { +"date" }; th { +"" } } }
                    tbody {
                        commits.forEach { c ->
                            val isCurrent = checkout.detached && checkout.ref == c.hash
                            tr {
                                td { code { +c.hash.take(8) } }
                                td { +c.message }
                                td("muted") { +c.committer }
                                td("muted") { +c.date }
                                td("actions") {
                                    if (!isCurrent) {
                                        form(action = "/checkout-ref", method = FormMethod.post) {
                                            hiddenInput(name = "ref") { value = c.hash }
                                            submitInput(classes = "btn small") { value = "Checkout" }
                                        }
                                    } else {
                                        span("muted") { +"current" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Renders "old -> new" for modified cells, or just the value for added/removed rows. */
internal fun FlowContent.diffCell(from: String?, to: String?, diffType: String) {
    when {
        diffType == "added" -> span("new") { +(to ?: "") }
        diffType == "removed" -> span("old") { +(from ?: "") }
        from != to -> {
            span("old") { +(from ?: "") }
            span("arrow") { +" -> " }
            span("new") { +(to ?: "") }
        }
        else -> +(to ?: "")
    }
}

private val CSS = """
:root {
  --bg: #0f1115; --panel: #171a21; --line: #262b36; --text: #e6e9ef;
  --muted: #8b93a7; --accent: #6aa9ff; --green: #4ade80; --red: #f87171; --amber: #fbbf24;
}
* { box-sizing: border-box; }
body {
  margin: 0; background: var(--bg); color: var(--text);
  font: 14px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}
.wrap { max-width: 1040px; margin: 0 auto; padding: 32px 20px 64px; }
header h1 { margin: 0 0 4px; font-size: 26px; letter-spacing: -0.02em; }
.sub { color: var(--muted); margin: 0 0 28px; }
.branch {
  background: #1e2a3d; color: var(--accent); padding: 2px 8px;
  border-radius: 999px; font-family: ui-monospace, monospace; font-size: 12px;
}
.branch.detached { background: #33280c; color: var(--amber); }
.flash {
  background: #1e2a3d; border: 1px solid var(--accent); color: var(--text);
  padding: 10px 14px; border-radius: 8px; margin-bottom: 20px; font-size: 13px;
}
section {
  background: var(--panel); border: 1px solid var(--line); border-radius: 12px;
  padding: 18px 20px; margin-bottom: 20px;
}
h2 { font-size: 15px; text-transform: uppercase; letter-spacing: 0.08em;
     color: var(--muted); margin: 0 0 14px; font-weight: 600; }
table { width: 100%; border-collapse: collapse; }
th {
  text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: 0.06em;
  color: var(--muted); padding: 8px 10px; border-bottom: 1px solid var(--line);
}
td { padding: 8px 10px; border-bottom: 1px solid #1d222c; vertical-align: middle; }
tr:last-child td { border-bottom: none; }
tbody tr:hover { background: #1b1f28; }
td.empty, .muted { color: var(--muted); }
td.actions { text-align: right; white-space: nowrap; }
input[type=text], input[type=number] {
  background: #0d1016; border: 1px solid var(--line); color: var(--text);
  padding: 6px 9px; border-radius: 7px; font-size: 13px; width: 100%; max-width: 190px;
}
input:focus { outline: none; border-color: var(--accent); }
.btn {
  background: #222835; color: var(--text); border: 1px solid var(--line);
  padding: 7px 14px; border-radius: 7px; cursor: pointer; font-size: 13px;
}
.btn:hover { background: #2b3242; }
.btn.small { padding: 5px 11px; font-size: 12px; }
.btn.primary { background: var(--accent); border-color: var(--accent); color: #07101f; font-weight: 600; }
.btn.danger { border-color: #4a2630; color: var(--red); }
.btn.danger:hover { background: #351a22; }
.row-form { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin: 0; }
td.actions form { display: inline-block; margin: 0 0 0 6px; }
.badge {
  background: var(--amber); color: #21180a; border-radius: 999px;
  padding: 1px 9px; font-size: 12px; font-weight: 700; margin-left: 6px;
}
.chip { padding: 2px 9px; border-radius: 999px; font-size: 11px; font-weight: 600; }
.chip.added { background: #10301d; color: var(--green); }
.chip.modified { background: #33280c; color: var(--amber); }
.chip.removed { background: #331a1f; color: var(--red); }
.old { color: var(--red); text-decoration: line-through; opacity: 0.8; }
.new { color: var(--green); }
.arrow { color: var(--muted); }
.commitbar {
  display: flex; gap: 12px; align-items: center; justify-content: space-between;
  margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--line); flex-wrap: wrap;
}
code { font-family: ui-monospace, monospace; color: var(--accent); font-size: 12px; }
"""
