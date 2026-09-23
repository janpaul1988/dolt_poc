package org.example.run

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.example.backend.dolt.DoltModelConfigService
import org.example.backend.exposed.ExposedDaoModelConfigService
import org.example.backend.exposed.ExposedModelConfigService
import org.example.frontend.module
import org.example.models.ModelConfigRow

/**
 * Single entry point that ties `:backend` and `:frontend` together: exercises the backend's
 * H2 Exposed demo (DSL + DAO CRUD smoke test) so both API styles are demonstrated, prepares the
 * Dolt branches the app expects, and then starts the frontend Ktor web app - all from one
 * command, `./gradlew :run:runApp`.
 */
fun main() {
    runBackendDemo(ExposedModelConfigService(), ExposedDaoModelConfigService())

    val dolt = DoltModelConfigService()
    dolt.ensureBranch(dolt.developmentBranch, from = dolt.mainBranch)
    dolt.checkoutBranch(dolt.mainBranch)

    embeddedServer(Netty, port = 8080) {
        module(dolt)
    }.start(wait = true)
}

/**
 * Runs the backend's Exposed DSL/DAO CRUD smoke test against H2, printing its output. Takes the
 * two services as parameters (rather than constructing its own file-backed H2 instances) so it
 * can be exercised in a unit test against isolated in-memory databases.
 */
internal fun runBackendDemo(dsl: ExposedModelConfigService, dao: ExposedDaoModelConfigService) {
    println("=== Backend: Exposed DSL demo ===")
    dsl.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))
    println("After create: ${dsl.findAll()}")
    dsl.delete("qa-model")

    println()
    println("=== Backend: Exposed DAO demo ===")
    dao.save("qa-model", "azure", 0.3, 2048)
    println("After create: ${dao.findAll()}")
    dao.delete("qa-model")

    println()
    println("=== Frontend: starting Dolt web app on http://localhost:8080 ===")
}
