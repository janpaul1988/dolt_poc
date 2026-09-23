package org.example.models

import kotlin.test.Test
import kotlin.test.assertEquals

class CheckoutStateTest {

    @Test
    fun `attached branch label is just the ref`() {
        val state = CheckoutState(ref = "development", detached = false, resolvedHash = "abc123")
        assertEquals("development", state.label)
    }

    @Test
    fun `detached label shows a short hash prefix`() {
        val state = CheckoutState(ref = "v1.0", detached = true, resolvedHash = "0123456789abcdef")
        assertEquals("detached HEAD (01234567)", state.label)
    }

    @Test
    fun `detached label truncates hashes shorter than eight chars without failing`() {
        val state = CheckoutState(ref = "v1.0", detached = true, resolvedHash = "abc")
        assertEquals("detached HEAD (abc)", state.label)
    }
}
