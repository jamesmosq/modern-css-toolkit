package com.jamesmosquera.moderncsstoolkit.generator

/**
 * Renders the live template group in the platform's XML format.
 * https://plugins.jetbrains.com/docs/intellij/providing-live-templates.html
 */
object LiveTemplateXml {

    const val FILE_NAME = "ModernCssToolkit.xml"
    private const val GROUP = "Modern CSS Toolkit"

    fun render(templates: List<TemplateSource>): String = buildString {
        append("<!-- Generated from src/templates by the generateLiveTemplates task. Do not edit. -->\n")
        append("<templateSet group=\"${escape(GROUP)}\">\n")
        templates.forEach { template ->
            val markup = template.body.let { if ("\$END\$" in it) it else it + "\$END\$" }
            val description = "${template.description} (${template.baseline.label})"
            append("    <template name=\"${escape(template.name)}\" value=\"${escape(markup)}\"\n")
            append("              description=\"${escape(description)}\" toReformat=\"false\" toShortenFQNames=\"false\">\n")
            template.variables.forEach {
                val expression = if (it.options.isEmpty()) it.expression else it.options.joinToString(",", "enum(", ")") { o -> "\"$o\"" }
                append("        <variable name=\"${it.name}\" expression=\"${escape(expression)}\" defaultValue=\"${escape("\"${it.default}\"")}\" alwaysStopAt=\"true\"/>\n")
            }
            append("        <context>\n")
            append("            <option name=\"${template.context.contextId}\" value=\"true\"/>\n")
            append("        </context>\n")
            append("    </template>\n")
        }
        append("</templateSet>\n")
    }

    /** Attribute escaping; newlines and tabs must be character references or XML parsers turn them into spaces. */
    private fun escape(text: String) = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        .replace("\n", "&#10;").replace("\t", "&#9;")
}
