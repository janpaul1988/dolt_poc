package org.example.run

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.example.backend.dolt.DoltModelConfigService
import org.example.frontend.module

/**
 * Single entry point that ties `:backend` and `:frontend` together: prepares the Dolt branches
 * the app expects, then starts the frontend Ktor web app - the whole app boots from one command,
 * `./gradlew :run:runApp`. Data access throughout is exclusively via Exposed against the Dolt
 * SQL server; there is no other data store involved.
 */
fun main() {
    val dolt = DoltModelConfigService()
    dolt.ensureBranch(dolt.developmentBranch, from = dolt.mainBranch)
    dolt.checkoutBranch(dolt.mainBranch)

    println("=== Starting Dolt CRUD web app on http://localhost:8080 ===")

    embeddedServer(Netty, port = 8080) {
        module(dolt)
    }.start(wait = true)
}
