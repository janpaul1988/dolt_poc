package org.example.backend.exposed

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ExposedDaoModelConfigServiceTest : FunSpec({

    fun newService() = ExposedDaoModelConfigService(
        "jdbc:h2:mem:test-exposed-dao-${System.nanoTime()};DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'"
    )

    test("save then findAll and findById round-trip") {
        val service = newService()
        service.save("qa-model", "azure", 0.3, 2048)

        val found = service.findById("qa-model")
        found?.provider shouldBe "azure"
        found?.temperature shouldBe 0.3
        found?.maxTokens shouldBe 2048
        service.findAll().map { it.id.value } shouldBe listOf("qa-model")
    }

    test("save upserts an existing row instead of duplicating it") {
        val service = newService()
        service.save("qa-model", "azure", 0.3, 2048)
        service.save("qa-model", "azure-updated", 0.4, 4096)

        service.findAll().map { it.id.value } shouldBe listOf("qa-model")
        service.findById("qa-model")?.provider shouldBe "azure-updated"
    }

    test("delete removes the row") {
        val service = newService()
        service.save("qa-model", "azure", 0.3, 2048)

        service.delete("qa-model") shouldBe true
        service.findById("qa-model").shouldBeNull()
        service.delete("qa-model") shouldBe false
    }
})
