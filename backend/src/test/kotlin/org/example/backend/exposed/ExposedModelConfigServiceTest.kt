package org.example.backend.exposed

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.example.models.ModelConfigRow

class ExposedModelConfigServiceTest : FunSpec({

    fun newService() = ExposedModelConfigService(
        "jdbc:h2:mem:test-exposed-${System.nanoTime()};DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
    )

    test("save then findAll and findById round-trip") {
        val service = newService()
        service.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))

        service.findById("qa-model") shouldBe ModelConfigRow("qa-model", "azure", 0.3, 2048)
        service.findAll() shouldBe listOf(ModelConfigRow("qa-model", "azure", 0.3, 2048))
    }

    test("save upserts an existing row instead of duplicating it") {
        val service = newService()
        service.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))
        service.save(ModelConfigRow("qa-model", "azure-updated", 0.4, 4096))

        service.findAll() shouldBe listOf(ModelConfigRow("qa-model", "azure-updated", 0.4, 4096))
    }

    test("delete removes the row") {
        val service = newService()
        service.save(ModelConfigRow("qa-model", "azure", 0.3, 2048))

        service.delete("qa-model") shouldBe true
        service.findById("qa-model").shouldBeNull()
        service.delete("qa-model") shouldBe false
    }
})
