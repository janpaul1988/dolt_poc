package org.example.backend.dolt

import kotlin.test.Test
import kotlin.test.assertEquals

class MergeResultTest {

    @Test
    fun `fast forward merge is reported as such`() {
        val message = describeMergeResult("development", "main", fastForward = true, conflictingTables = emptyList())
        assertEquals("Fast-forwarded 'main' to 'development'.", message)
    }

    @Test
    fun `three way merge without conflicts`() {
        val message = describeMergeResult("development", "main", fastForward = false, conflictingTables = emptyList())
        assertEquals("Merged 'development' into 'main'.", message)
    }

    @Test
    fun `conflicting merge lists the affected tables and does not claim a fast-forward`() {
        val message = describeMergeResult(
            "development",
            "main",
            fastForward = false,
            conflictingTables = listOf("model_configs")
        )
        assertEquals(
            "Merge aborted: conflicts in model_configs. Resolve manually with the Dolt CLI, then retry.",
            message
        )
    }
}
