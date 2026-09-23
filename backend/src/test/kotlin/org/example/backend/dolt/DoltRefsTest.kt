package org.example.backend.dolt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DoltRefsTest : FunSpec({

    test("sanitize passes through a plain branch or tag name") {
        DoltRefs.sanitize("development") shouldBe "development"
        DoltRefs.sanitize("v1.0") shouldBe "v1.0"
    }

    test("sanitize rejects blank refs") {
        shouldThrow<IllegalArgumentException> { DoltRefs.sanitize("") }
        shouldThrow<IllegalArgumentException> { DoltRefs.sanitize("   ") }
    }

    test("sanitize rejects refs that could break out of the qualified database name") {
        shouldThrow<IllegalArgumentException> { DoltRefs.sanitize("main`; DROP TABLE model_configs; --") }
        shouldThrow<IllegalArgumentException> { DoltRefs.sanitize("has space") }
    }
})
