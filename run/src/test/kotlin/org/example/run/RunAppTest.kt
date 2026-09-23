package org.example.run

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.example.backend.exposed.ExposedDaoModelConfigService
import org.example.backend.exposed.ExposedModelConfigService

class RunAppTest : FunSpec({

    test("backend demo exercises both Exposed services end to end without throwing") {
        val dsl = ExposedModelConfigService(
            "jdbc:h2:mem:test-run-dsl-${System.nanoTime()};DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
        )
        val dao = ExposedDaoModelConfigService(
            "jdbc:h2:mem:test-run-dao-${System.nanoTime()};DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
        )

        runBackendDemo(dsl, dao)

        // The demo creates then deletes the same row on each service, so both end up empty.
        dsl.findAll() shouldBe emptyList()
        dao.findAll() shouldBe emptyList()
    }
})
