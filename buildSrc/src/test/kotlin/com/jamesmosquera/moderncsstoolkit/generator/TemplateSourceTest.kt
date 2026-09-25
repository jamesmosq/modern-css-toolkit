package com.jamesmosquera.moderncsstoolkit.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TemplateSourceTest {

    private val valid = "/*\r\ndescription: Fluid font size\r\ncontext: declarations\r\nbaseline: widely\r\n" +
        "features: min-max-clamp\r\nvar MIN: 1rem\r\nvar MAX: 2rem\r\n*/\r\n\r\nfont-size: clamp(\$MIN\$, 1rem + 1vw, \$MAX\$);\r\n"

    @Test
    fun `parses header and body, normalising line endings`() {
        val t = TemplateSource.parse("css-fluid-type", valid)
        assertEquals("Fluid font size", t.description)
        assertEquals(Context.DECLARATIONS, t.context)
        assertEquals(Baseline.WIDELY, t.baseline)
        assertEquals(listOf("min-max-clamp"), t.features)
        assertEquals(listOf(Variable("MIN", "1rem"), Variable("MAX", "2rem")), t.variables)
        assertEquals("font-size: clamp(\$MIN\$, 1rem + 1vw, \$MAX\$);", t.body)
    }

    @Test
    fun `rejects invalid sources`() {
        fun fails(name: String, text: String) =
            assertThrows(IllegalArgumentException::class.java) { TemplateSource.parse(name, text) }

        fails("fluid-type", valid) // missing css- prefix
        fails("css-fluid-type", valid.replace("context: declarations\r\n", "")) // no context
        fails("css-fluid-type", valid.replace("baseline: widely\r\n", "")) // no baseline
        fails("css-fluid-type", valid.replace("features: min-max-clamp\r\n", "")) // no features
        fails("css-fluid-type", valid.replace("features: min-max-clamp", "features: Min_Max")) // not a web-features id
        fails("css-fluid-type", valid.replace("var MAX: 2rem\r\n", "")) // used but not declared
        fails("css-fluid-type", valid.replace("\$MAX\$", "2rem")) // declared but not used
    }

    @Test
    fun `rejects unknown values and keys`() {
        fun fails(text: String) = assertThrows(IllegalStateException::class.java) { TemplateSource.parse("css-fluid-type", text) }
        fails(valid.replace("context: declarations", "context: body"))
        fails(valid.replace("baseline: widely", "baseline: limited")) // non-Baseline features are not accepted
        fails(valid.replace("description", "desc"))
        fails("font-size: 1rem;") // no header
    }

    @Test
    fun `options and expr attach to declared variables`() {
        val t = TemplateSource.parse("css-fluid-type", valid.replace("var MAX: 2rem\r\n", "var MAX: 2rem\r\noptions MAX: 2rem, 3rem\r\n"))
        assertEquals(listOf("2rem", "3rem"), t.variables.last().options)
        assertThrows(IllegalArgumentException::class.java) {
            TemplateSource.parse("css-fluid-type", valid.replace("var MAX: 2rem\r\n", "options MAX: 2rem\r\nvar MAX: 2rem\r\n"))
        }
    }
}
