package org.example.backend.exposed

import org.example.models.ModelConfigRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExposedModelConfigServiceTest {

    private val service = ExposedModelConfigService(
        "jdbc:h2:mem:test-exposed-${System.nanoTime()};DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
    )

    @Test
    fun `save then findAll and findById round-trip`() {
        service.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))

        assertEquals(ModelConfigRow("qa-model", "azure", 0.3, 2048), service.findById("qa-model"))
        assertEquals(listOf(ModelConfigRow("qa-model", "azure", 0.3, 2048)), service.findAll())
    }

    @Test
    fun `save upserts an existing row instead of duplicating it`() {
        service.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))
        service.save(ModelConfigRow("qa-model", "azure-updated", 0.4, 4096))

        assertEquals(1, service.findAll().size)
        assertEquals(ModelConfigRow("qa-model", "azure-updated", 0.4, 4096), service.findById("qa-model"))
    }

    @Test
    fun `delete removes the row`() {
        service.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))

        assertEquals(true, service.delete("qa-model"))
        assertNull(service.findById("qa-model"))
        assertEquals(false, service.delete("qa-model"))
    }
}
