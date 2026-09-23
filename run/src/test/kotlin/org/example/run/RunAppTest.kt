package org.example.run

import org.example.backend.exposed.ExposedDaoModelConfigService
import org.example.backend.exposed.ExposedModelConfigService
import kotlin.test.Test
import kotlin.test.assertEquals

class RunAppTest {

    @Test
    fun `backend demo exercises both Exposed services end to end without throwing`() {
        val dsl = ExposedModelConfigService(
            "jdbc:h2:mem:test-run-dsl-${System.nanoTime()};DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
        )
        val dao = ExposedDaoModelConfigService(
            "jdbc:h2:mem:test-run-dao-${System.nanoTime()};DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
        )

        runBackendDemo(dsl, dao)

        // The demo creates then deletes the same row on each service, so both end up empty.
        assertEquals(emptyList(), dsl.findAll())
        assertEquals(emptyList(), dao.findAll())
    }
}
