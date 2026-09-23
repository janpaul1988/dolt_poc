package org.example.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CheckoutStateTest : FunSpec({

    test("attached branch label is just the ref") {
        val state = CheckoutState(ref = "development", detached = false, resolvedHash = "abc123")
        state.label shouldBe "development"
    }

    test("detached label shows a short hash prefix") {
        val state = CheckoutState(ref = "v1.0", detached = true, resolvedHash = "0123456789abcdef")
        state.label shouldBe "detached HEAD (01234567)"
    }

    test("detached label truncates hashes shorter than eight chars without failing") {
        val state = CheckoutState(ref = "v1.0", detached = true, resolvedHash = "abc")
        state.label shouldBe "detached HEAD (abc)"
    }
})
