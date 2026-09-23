package org.example.backend.dolt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoltRefsTest {

    @Test
    fun `sanitize passes through a plain branch or tag name`() {
        assertEquals("development", DoltRefs.sanitize("development"))
        assertEquals("v1.0", DoltRefs.sanitize("v1.0"))
    }

    @Test
    fun `sanitize rejects blank refs`() {
        assertFailsWith<IllegalArgumentException> { DoltRefs.sanitize("") }
        assertFailsWith<IllegalArgumentException> { DoltRefs.sanitize("   ") }
    }

    @Test
    fun `sanitize rejects refs that could break out of the qualified database name`() {
        assertFailsWith<IllegalArgumentException> { DoltRefs.sanitize("main`; DROP TABLE model_configs; --") }
        assertFailsWith<IllegalArgumentException> { DoltRefs.sanitize("has space") }
    }
}
