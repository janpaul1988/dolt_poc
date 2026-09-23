package org.example.frontend

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.html.div
import kotlinx.html.stream.createHTML

class DiffCellTest : FunSpec({

    fun render(from: String?, to: String?, diffType: String): String =
        createHTML().div { diffCell(from, to, diffType) }.trim()

    test("added row shows only the new value") {
        val html = render(from = null, to = "openai", diffType = "added")
        html shouldBe """<div><span class="new">openai</span></div>"""
    }

    test("removed row shows only the old value") {
        val html = render(from = "openai", to = null, diffType = "removed")
        html shouldBe """<div><span class="old">openai</span></div>"""
    }

    test("modified row shows old and new values with an arrow") {
        val html = render(from = "openai", to = "azure", diffType = "modified")
        html shouldBe """<div><span class="old">openai</span><span class="arrow"> -&gt; </span><span class="new">azure</span></div>"""
    }

    test("unchanged value renders as plain text") {
        val html = render(from = "openai", to = "openai", diffType = "modified")
        html shouldBe "<div>openai</div>"
    }
})
