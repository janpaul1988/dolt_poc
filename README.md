# aitester

A small multi-module Kotlin project with two CRUD demos against the same flat `model_configs`
table, showing two different Kotlin data-access styles - and, for the more interesting one, a
full Dolt version-control workflow (branches, tags, merges, commit history) on top of Exposed.

## Modules

| Module | Contains | Depends on |
|---|---|---|
| `backend` | Exposed `Table`/`IdTable`/DAO definitions for `model_configs` and the shared Dolt domain types (`CommitRow`, `BranchRow`, `TagRow`, `DiffRow`, `CheckoutState`, ...), plus the CRUD services built on them: the H2-backed Exposed DSL/DAO demo (`org.example.backend.exposed`) and the Exposed-backed Dolt version-control service (`org.example.backend.dolt.DoltModelConfigService`). | - |
| `frontend` | The Ktor HTML CRUD web app (`org.example.frontend.DoltWebApp`). Talks only to `backend`'s `DoltModelConfigService` - it never opens a JDBC connection itself. | `backend` |
| `run` | The single entry point that ties `backend` and `frontend` together: runs the backend's Exposed DSL/DAO demo, then starts the frontend web app - one command boots the whole thing. | `backend`, `frontend` |

Every module has its own unit tests (`./gradlew test` runs all of them; see below for what each
suite covers).

The Dolt demo (`frontend` + `backend`) is the interesting one: it's a small Ktor web app that lets
you edit rows, review the change as a diff before committing, browse history scoped to whatever
is checked out, switch between `main`/`development` branches, tag `main`, merge `development` into
`main`, and check out any branch, tag, or raw commit hash (as a read-only "detached HEAD") - all
backed by Dolt's version-control system tables (`dolt_status`, `dolt_diff_*`, `dolt_log`,
`dolt_branches`, `dolt_tags`) and stored procedures (`DOLT_COMMIT`, `DOLT_MERGE`, `DOLT_TAG`, ...),
issued through Exposed rather than raw JDBC.

### How Dolt access uses Exposed

- Plain CRUD (`model_configs`) uses Exposed's typed DSL against `org.example.models.ModelConfigsTable`
  (part of `backend`) - identical table definition to the H2 demo, just pointed at a Dolt
  connection instead.
- Dolt's version-control system tables/procedures have no typed Exposed API, so those go through
  Exposed's `Transaction.exec()` raw-SQL escape hatch (with bound parameters, not string
  concatenation) - still routed through Exposed's connection/transaction management, just without
  the DSL layer.
- Dolt lets you address a branch, tag, or commit directly as part of the database name
  (`doltdb/<ref>`), so instead of relying on a stateful `DOLT_CHECKOUT` (which only lives for the
  one JDBC connection that ran it), each transaction connects to a fresh Exposed `Database` handle
  whose JDBC URL already embeds the currently checked-out ref.

## Prerequisites

- JDK 24 (the Gradle toolchain will provision this automatically if it's missing)
- [Dolt](https://docs.dolthub.com/introduction/installation) CLI, for the Dolt demo only
- Optionally, a MySQL client, to poke at the running Dolt server by hand

Nothing else needs installing - the Exposed/H2 demo is fully embedded (no server process), and
Dolt's SQL server speaks the plain MySQL wire protocol, so the app talks to it with an ordinary
MySQL JDBC driver (via Exposed).

### Installing Dolt

macOS (Homebrew):

```bash
brew install dolt
```

Linux / macOS (install script):

```bash
sudo bash -c 'curl -L https://github.com/dolthub/dolt/releases/latest/download/install.sh | bash'
```

Windows: download the installer from the [Dolt releases page](https://github.com/dolthub/dolt/releases).

Verify it's on your `PATH`:

```bash
dolt version
```

### Installing a MySQL client (optional)

You don't need a MySQL *server* - Dolt's own `dolt sql-server` is the server, and it's wire
compatible with MySQL. A MySQL *client* is only useful if you want to poke around by hand instead
of (or in addition to) using the webapp.

macOS (Homebrew):

```bash
brew install mysql-client
# then, if you want `mysql` on your PATH:
brew link --force mysql-client
```

Linux (Debian/Ubuntu):

```bash
sudo apt-get install mysql-client
```

Connect to a running Dolt server (see below) with:

```bash
mysql -h 127.0.0.1 -P 3307 -u root doltdb
```

## Setting up the Dolt database

`doltdb/` holds Dolt's own repository state under `doltdb/.dolt/` (plus `.doltcfg/`). Both are
generated, machine-local database storage - like a `.git` folder that also happens to contain your
data - so they're gitignored rather than committed. `doltdb/config.yaml` (server config) and
`doltdb/schema.sql` (table + seed data) *are* tracked, so anyone can reconstruct the same database
from scratch:

```bash
cd doltdb
dolt init                      # creates .dolt/ - only needed once
dolt sql < schema.sql          # creates model_configs and seeds a few rows
dolt add -A
dolt commit -m "Initial schema and seed data"
dolt branch development        # the webapp expects both 'main' and 'development' to exist
```

(If you already have a `doltdb/.dolt/` from before, none of this is necessary - just start the
server, below.)

## Starting / stopping the Dolt database

The app connects to a `dolt sql-server` over JDBC at `127.0.0.1:3307`, database `doltdb` - it
does **not** start the server itself, so start it manually first, in its own terminal:

```bash
cd doltdb
dolt sql-server --host 127.0.0.1 --port 3307
```

Leave that running. You'll see `Server ready. Accepting connections.` once it's up.

To **stop** it, press `Ctrl+C` in that terminal. If it's running in the background instead, find
and stop the process by port:

```bash
lsof -ti:3307        # prints the PID listening on 3307
kill <PID>            # stop it gracefully
```

Dolt keeps all committed history and branches on disk under `doltdb/.dolt/` regardless of whether
the server is running - stopping the server does not delete or reset any data.

## Running the app

With the Dolt server running (see above), start everything - the backend Exposed demo and the
frontend web app - with one command:

```bash
./gradlew :run:runApp
```

This prints the backend's Exposed DSL/DAO CRUD smoke test output, then starts the Ktor frontend on
http://localhost:8080. From there you can:
- edit rows and review the pending diff before committing,
- browse full commit history, scoped to whatever branch/tag/commit is currently checked out,
- switch between the `main` (protected, read-only) and `development` (writable) branches,
- check out a tag or a raw commit hash, entering a read-only "detached HEAD" state,
- tag `main` with a custom label (e.g. `1.0`),
- merge `development` into `main` (conflicts abort the merge and are reported, rather than
  crashing the app).

You can also run each piece independently if you only want one of them:

```bash
./gradlew :backend:runExposedDemo   # just the Exposed DSL + DAO CRUD smoke test (no Dolt server needed)
./gradlew :frontend:runDoltWeb      # just the Dolt web app (needs the Dolt server running)
```

## Running the tests

```bash
./gradlew test
```

This runs every module's suite:
- `backend`: table/column definitions and `CheckoutState` label logic; full CRUD round-trips for
  both the DSL and DAO H2 services (against in-memory H2); pure-logic tests for the Dolt service's
  ref validation and merge-outcome message formatting (no live Dolt server required).
- `frontend`: HTML rendering of the diff/change indicators used in the "pending changes" screen.
- `run`: the combined backend demo runs end-to-end against isolated in-memory H2 databases
  without throwing.

You can also run a single module's tests, e.g. `./gradlew :backend:test`.
