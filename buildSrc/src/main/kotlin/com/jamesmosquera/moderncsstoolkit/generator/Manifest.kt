package com.jamesmosquera.moderncsstoolkit.generator

/**
 * JSON description of every template for tools/css-check (parsing and Baseline verification):
 * `[{"name", "context", "baseline", "features": [], "variables": [{"name", "default", "options": []}], "body"}]`.
 */
object Manifest {

    fun render(templates: List<TemplateSource>): String = templates.joinToString(",\n", "[\n", "\n]\n") { t ->
        val variables = t.variables.joinToString(", ", "[", "]") { v ->
            """{"name": ${str(v.name)}, "default": ${str(v.default)}, "options": ${list(v.options)}}"""
        }
        """  {"name": ${str(t.name)}, "context": ${str(t.context.key)}, "baseline": ${str(t.baseline.key)}, """ +
            """"features": ${list(t.features)}, "variables": $variables, "body": ${str(t.body)}}"""
    }

    private fun list(values: List<String>) = values.joinToString(", ", "[", "]") { str(it) }

    private fun str(value: String) = buildString {
        append('"')
        value.forEach { c ->
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\t' -> append("\\t")
                c < ' ' -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
        append('"')
    }
}
