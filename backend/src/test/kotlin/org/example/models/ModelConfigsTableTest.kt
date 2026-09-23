package org.example.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ModelConfigsTableTest : FunSpec({

    test("table name and columns match the shared schema") {
        ModelConfigsTable.tableName shouldBe "model_configs"
        val columnNames = ModelConfigsTable.columns.map { it.name }
        columnNames shouldBe listOf("id", "provider", "temperature", "max_tokens")
    }

    test("id is the primary key") {
        ModelConfigsTable.primaryKey?.columns?.toList() shouldBe listOf(ModelConfigsTable.id)
    }

    test("id table shares the same underlying table name") {
        ModelConfigsIdTable.tableName shouldBe ModelConfigsTable.tableName
    }
})
