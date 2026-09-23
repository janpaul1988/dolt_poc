package org.example.frontend

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import kotlin.test.Test
import kotlin.test.assertEquals

class DiffCellTest {

    private fun render(from: String?, to: String?, diffType: String): String =
        createHTML().div { diffCell(from, to, diffType) }.trim()

    @Test
    fun `added row shows only the new value`() {
        val html = render(from = null, to = "openai", diffType = "added")
        assertEquals("""<div><span class="new">openai</span></div>""", html)
    }

    @Test
    fun `removed row shows only the old value`() {
        val html = render(from = "openai", to = null, diffType = "removed")
        assertEquals("""<div><span class="old">openai</span></div>""", html)
    }

    @Test
    fun `modified row shows old and new values with an arrow`() {
        val html = render(from = "openai", to = "azure", diffType = "modified")
        assertEquals(
            """<div><span class="old">openai</span><span class="arrow"> -&gt; </span><span class="new">azure</span></div>""",
            html
        )
    }

    @Test
    fun `unchanged value renders as plain text`() {
        val html = render(from = "openai", to = "openai", diffType = "modified")
        assertEquals("<div>openai</div>", html)
    }
}
