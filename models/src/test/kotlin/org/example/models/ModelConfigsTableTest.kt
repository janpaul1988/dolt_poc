package org.example.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelConfigsTableTest {

    @Test
    fun `table name and columns match the shared schema`() {
        assertEquals("model_configs", ModelConfigsTable.tableName)
        val columnNames = ModelConfigsTable.columns.map { it.name }
        assertEquals(listOf("id", "provider", "temperature", "max_tokens"), columnNames)
    }

    @Test
    fun `id is the primary key`() {
        assertTrue(ModelConfigsTable.primaryKey?.columns?.toList() == listOf(ModelConfigsTable.id))
    }

    @Test
    fun `id table shares the same underlying table name`() {
        assertEquals(ModelConfigsTable.tableName, ModelConfigsIdTable.tableName)
    }
}
