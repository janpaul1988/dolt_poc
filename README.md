# aitester

Two CRUD demos against the same flat `model_configs` table, showing two different Kotlin
data-access styles:

| Demo | Package | Storage | Run with |
|---|---|---|---|
| Exposed (Kotlin SQL DSL + DAO) | `org.example.db.exposed` | H2, file-based | `./gradlew runExposedDemo` |
| Dolt (versioned SQL, HTML CRUD) | `org.example.dolt` | [Dolt](https://www.dolthub.com/) (MySQL wire-compatible) | `./gradlew runDoltWeb` |

The Dolt demo is the interesting one: it's a small Ktor web app that lets you edit rows, review
the change as a diff before committing, browse history, switch between `main`/`development`
branches, tag `main`, and merge `development` into `main` - all backed by Dolt's version-control
system tables (`dolt_status`, `dolt_diff_*`, `dolt_log`, `dolt_branches`, `dolt_tags`) and stored
procedures (`DOLT_COMMIT`, `DOLT_MERGE`, `DOLT_TAG`, ...).

## Prerequisites

- JDK 24 (the Gradle toolchain will provision this automatically if it's missing)
- [Dolt](https://docs.dolthub.com/introduction/installation) CLI, for the Dolt demo only
- Optionally, a MySQL client, to poke at the running Dolt server by hand

Nothing else needs installing - the Exposed/H2 demo is fully embedded (no server process), and
Dolt's SQL server speaks the plain MySQL wire protocol, so the app talks to it with an ordinary
MySQL JDBC driver.

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

The webapp connects to a `dolt sql-server` over JDBC at `127.0.0.1:3307`, database `doltdb` - it
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

## Running the demos

With the Dolt server running (see above):

```bash
./gradlew runDoltWeb
```

Then open http://localhost:8080. From there you can:
- edit rows and review the pending diff before committing,
- browse full commit history and check out any commit (detached HEAD, read-only),
- switch between the `main` (protected, read-only) and `development` (writable) branches,
- tag `main` with a custom label (e.g. `1.0`),
- merge `development` into `main`.

The Exposed/H2 demo needs no separate server:

```bash
./gradlew runExposedDemo   # Exposed DSL + DAO CRUD smoke test
```
