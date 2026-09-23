package org.example.backend.dolt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MergeResultTest : FunSpec({

    test("fast forward merge is reported as such") {
        val message = describeMergeResult("development", "main", fastForward = true, conflictingTables = emptyList())
        message shouldBe "Fast-forwarded 'main' to 'development'."
    }

    test("three way merge without conflicts") {
        val message = describeMergeResult("development", "main", fastForward = false, conflictingTables = emptyList())
        message shouldBe "Merged 'development' into 'main'."
    }

    test("conflicting merge lists the affected tables and does not claim a fast-forward") {
        val message = describeMergeResult(
            "development",
            "main",
            fastForward = false,
            conflictingTables = listOf("model_configs")
        )
        message shouldBe "Merge aborted: conflicts in model_configs. Resolve manually with the Dolt CLI, then retry."
    }
})
