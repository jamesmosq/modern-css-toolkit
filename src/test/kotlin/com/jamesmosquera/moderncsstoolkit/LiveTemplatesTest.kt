package com.jamesmosquera.moderncsstoolkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the rules in CLAUDE.md on the XML generated from src/templates (see buildSrc).
 * Pure XML checks: they need no IDE and no network.
 */
class LiveTemplatesTest {

    private class Tpl(val name: String, val value: String, val description: String, val contexts: Set<String>)

    private val templates: List<Tpl> by lazy {
        val stream = javaClass.getResourceAsStream("/liveTemplates/ModernCssToolkit.xml") ?: error("generated XML missing")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
        val nodes = doc.getElementsByTagName("template")
        (0 until nodes.length).map { i ->
            val e = nodes.item(i) as Element
            val options = e.getElementsByTagName("option")
            Tpl(
                e.getAttribute("name"), e.getAttribute("value"), e.getAttribute("description"),
                (0 until options.length).map { j -> options.item(j) as Element }
                    .filter { it.getAttribute("value") == "true" }.map { it.getAttribute("name") }.toSet(),
            )
        }
    }

    private val cssContexts = setOf("CSS", "CSS_RULESET_LIST", "CSS_DECLARATION_BLOCK", "CSS_PROPERTY_VALUE")

    @Test
    fun `templates exist, use the css- prefix and exactly one css context`() {
        assertTrue(templates.isNotEmpty())
        templates.forEach {
            assertTrue("${it.name} must start with css-", it.name.startsWith("css-"))
            assertEquals("${it.name} needs exactly one context", 1, it.contexts.size)
            assertTrue("${it.name} uses a non-CSS context ${it.contexts}", it.contexts.single() in cssContexts)
        }
    }

    @Test
    fun `every description states the baseline status`() = templates.forEach {
        assertTrue("${it.name}: ${it.description}", Regex("""\(Baseline: (widely|newly) available\)$""").containsMatchIn(it.description))
    }

    @Test
    fun `declaration templates contain no rules and rule templates contain a block`() = templates.forEach {
        when (it.contexts.single()) {
            "CSS_DECLARATION_BLOCK" -> assertTrue("${it.name} must not open a block", '{' !in it.value)
            "CSS_RULESET_LIST" -> assertTrue("${it.name} must contain a rule or at-rule block", '{' in it.value)
        }
    }

    @Test
    fun `braces are balanced`() = templates.forEach {
        assertEquals("${it.name} has unbalanced braces", it.value.count { c -> c == '{' }, it.value.count { c -> c == '}' })
    }

    @Test
    fun `plugin description lists every template`() {
        val pluginXml = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use { it.readBytes().decodeToString() }
        val description = pluginXml.substringAfter("<description>").substringBefore("</description>")
        val missing = templates.map { it.name }.filter { "<code>$it</code>" !in description }
        assertTrue("add to the plugin.xml description: $missing", missing.isEmpty())
    }
}
