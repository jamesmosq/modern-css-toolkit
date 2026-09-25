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

    /** The plugin's own contexts (CSS minus indented Sass), registered in plugin.xml. */
    private val cssContexts = setOf("MODERN_CSS_RULES", "MODERN_CSS_DECLARATIONS", "MODERN_CSS_VALUE")

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
            "MODERN_CSS_DECLARATIONS" -> assertTrue("${it.name} must not open a block", '{' !in it.value)
            "MODERN_CSS_RULES" -> assertTrue("${it.name} must contain a rule or at-rule block", '{' in it.value)
        }
    }

    @Test
    fun `braces are balanced`() = templates.forEach {
        assertEquals("${it.name} has unbalanced braces", it.value.count { c -> c == '{' }, it.value.count { c -> c == '}' })
    }

    @Test
    fun `every context used by a template is registered in plugin xml`() {
        val pluginXml = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use { it.readBytes().decodeToString() }
        val registered = Regex("""<liveTemplateContext\s+contextId="([^"]+)"""").findAll(pluginXml).map { it.groupValues[1] }.toSet()
        assertEquals(cssContexts, registered)
        templates.forEach { assertTrue("${it.name}: ${it.contexts} not registered", it.contexts.single() in registered) }
    }

    @Test
    fun `english and spanish messages have the same keys`() {
        fun keys(resource: String): Set<String> {
            val stream = LiveTemplatesTest::class.java.getResourceAsStream(resource) ?: error("missing $resource")
            return java.util.Properties().apply { stream.reader(Charsets.UTF_8).use { load(it) } }.stringPropertyNames()
        }
        assertEquals(keys("/messages/ModernCssToolkitBundle.properties"), keys("/messages/ModernCssToolkitBundle_es.properties"))
    }

    @Test
    fun `plugin description lists every template`() {
        val pluginXml = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use { it.readBytes().decodeToString() }
        val description = pluginXml.substringAfter("<description>").substringBefore("</description>")
        val missing = templates.map { it.name }.filter { "<code>$it</code>" !in description }
        assertTrue("add to the plugin.xml description: $missing", missing.isEmpty())
    }
}
