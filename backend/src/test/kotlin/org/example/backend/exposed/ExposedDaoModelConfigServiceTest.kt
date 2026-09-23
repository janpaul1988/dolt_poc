package org.example.backend.exposed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExposedDaoModelConfigServiceTest {

    private val service = ExposedDaoModelConfigService(
        "jdbc:h2:mem:test-exposed-dao-${System.nanoTime()};DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
    )

    @Test
    fun `save then findAll and findById round-trip`() {
        service.save("qa-model", "azure", 0.3, 2048)

        val found = service.findById("qa-model")
        assertEquals("azure", found?.provider)
        assertEquals(0.3, found?.temperature)
        assertEquals(2048, found?.maxTokens)
        assertEquals(1, service.findAll().size)
    }

    @Test
    fun `save upserts an existing row instead of duplicating it`() {
        service.save("qa-model", "azure", 0.3, 2048)
        service.save("qa-model", "azure-updated", 0.4, 4096)

        assertEquals(1, service.findAll().size)
        assertEquals("azure-updated", service.findById("qa-model")?.provider)
    }

    @Test
    fun `delete removes the row`() {
        service.save("qa-model", "azure", 0.3, 2048)

        assertEquals(true, service.delete("qa-model"))
        assertNull(service.findById("qa-model"))
        assertEquals(false, service.delete("qa-model"))
    }
}
