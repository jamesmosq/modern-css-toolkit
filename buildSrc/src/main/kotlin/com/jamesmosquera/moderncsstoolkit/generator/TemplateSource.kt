package com.jamesmosquera.moderncsstoolkit.generator

/**
 * A live template variable. Declaration order is the Tab order in the editor.
 * [options] become an `enum(...)` expression: a completion list shown when the template expands.
 * [expression] is a raw live template expression such as `date("yyyy")`
 * (https://www.jetbrains.com/help/idea/template-variables.html#predefined_functions).
 */
data class Variable(
    val name: String,
    val default: String,
    val options: List<String> = emptyList(),
    val expression: String = "",
)

/**
 * Where a template may be expanded. Context ids verified in IDEA 2025.2 (css-impl plugin.xml).
 * They also apply in SCSS, Sass and Less (their Language has CSS as base) and in Vue/HTML `<style>`.
 */
enum class Context(val key: String, val contextId: String) {
    /** Top level of a stylesheet, where rules and at-rules go (`@container`, `@layer`, `.card { }`). */
    RULES("rules", "CSS_RULESET_LIST"),

    /** Inside a `{ }` block, where declarations go (`font-size: clamp(...)`). */
    DECLARATIONS("declarations", "CSS_DECLARATION_BLOCK"),

    /** After `property:`, where a value goes. */
    VALUE("value", "CSS_PROPERTY_VALUE"),
}

/**
 * Baseline status of the newest feature a template relies on (https://web.dev/baseline).
 * WIDELY: interoperable for 30+ months. NEWLY: works in all current core browsers, not in older versions.
 * Features that are not Baseline at all are not accepted as templates.
 */
enum class Baseline(val key: String, val label: String) {
    WIDELY("widely", "Baseline: widely available"),
    NEWLY("newly", "Baseline: newly available"),
}

/**
 * One source template: a CSS file under src/templates whose name is the abbreviation
 * (css-fluid-type.css -> css-fluid-type) and which starts with a CSS header comment whose lines are:
 *
 * ```
 * description: Fluid font size that scales with the viewport
 * context: declarations
 * baseline: widely
 * features: min-max-clamp
 * var MIN: 1rem
 * ```
 * followed by the body, e.g. `font-size: clamp($MIN$, 0.9rem + 1vw, 2rem);`. See src/templates for real files.
 *
 * `features` are web-features ids (https://github.com/web-platform-dx/web-features); the declared `baseline`
 * is checked against them automatically (see CLAUDE.md). Variables: `var NAME: default`,
 * `options NAME: a, b, c` (completion list, must contain the default), `expr NAME: date("yyyy")`.
 */
data class TemplateSource(
    val name: String,
    val description: String,
    val context: Context,
    val baseline: Baseline,
    val features: List<String>,
    val variables: List<Variable>,
    val body: String,
) {
    companion object {
        private val NAME = Regex("""css-[a-z0-9]+(-[a-z0-9]+)*""")
        private val FEATURE_ID = Regex("""[a-z0-9]+(-[a-z0-9]+)*""")
        private val VARIABLE_NAME = Regex("""[A-Z][A-Z0-9_]*""")
        private val VARIABLE_USE = Regex("""\$([A-Za-z_][A-Za-z0-9_]*)\$""")
        private val HEADER = Regex("""\A\s*/\*(.*?)\*/""", RegexOption.DOT_MATCHES_ALL)
        private val PREDEFINED = setOf("END", "SELECTION")

        fun parse(name: String, text: String): TemplateSource {
            require(NAME.matches(name)) { "file name must be the abbreviation, like css-fluid-type (got '$name')" }
            val source = text.replace("\r\n", "\n")
            val header = HEADER.find(source) ?: error("must start with a /* ... */ header")

            var description = ""
            var context: Context? = null
            var baseline: Baseline? = null
            var features = emptyList<String>()
            val variables = mutableListOf<Variable>()
            header.groupValues[1].lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                require(':' in line) { "header line must be 'key: value' (got '$line')" }
                val key = line.substringBefore(':').trim()
                val value = line.substringAfter(':').trim()
                when {
                    key == "description" -> description = value
                    key == "context" -> context = Context.entries.firstOrNull { it.key == value }
                        ?: error("context must be one of ${Context.entries.map { it.key }} (got '$value')")
                    key == "baseline" -> baseline = Baseline.entries.firstOrNull { it.key == value }
                        ?: error("baseline must be one of ${Baseline.entries.map { it.key }} (got '$value')")
                    key == "features" -> features = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    key.startsWith("var ") -> variables += Variable(key.removePrefix("var ").trim(), value)
                    key.startsWith("options ") -> {
                        val index = indexOfDeclared(variables, key.removePrefix("options ").trim(), "options")
                        val options = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                        require(options.isNotEmpty()) { "options for '${variables[index].name}' are empty" }
                        variables[index] = variables[index].copy(options = options)
                    }
                    key.startsWith("expr ") -> {
                        val index = indexOfDeclared(variables, key.removePrefix("expr ").trim(), "expr")
                        require(value.isNotEmpty()) { "expr for '${variables[index].name}' is empty" }
                        variables[index] = variables[index].copy(expression = value)
                    }
                    else -> error("unknown header key '$key' (expected description, context, baseline, features, var, options or expr)")
                }
            }

            val body = source.substring(header.range.last + 1)
                .lines().dropWhile { it.isBlank() }.joinToString("\n").trimEnd()

            require(description.isNotBlank()) { "header needs a 'description'" }
            requireNotNull(context) { "header needs a 'context' (${Context.entries.map { it.key }})" }
            requireNotNull(baseline) { "header needs a 'baseline' (${Baseline.entries.map { it.key }})" }
            require(features.isNotEmpty()) { "header needs 'features': the web-features ids the template relies on" }
            features.forEach { require(FEATURE_ID.matches(it)) { "'$it' is not a web-features id" } }
            require(body.isNotBlank()) { "template body is empty" }
            variables.forEach {
                require(VARIABLE_NAME.matches(it.name)) { "variable '${it.name}' must be UPPER_SNAKE_CASE" }
                require(it.name !in PREDEFINED) { "\$${it.name}\$ is predefined, do not declare it" }
                require('"' !in it.default && '\\' !in it.default) { "default of ${it.name} must not contain \" or \\" }
                require(it.options.none { o -> '"' in o || '\\' in o }) { "options of ${it.name} must not contain \" or \\" }
                require(it.options.size == it.options.toSet().size) { "options of ${it.name} have duplicates" }
                require(it.options.isEmpty() || it.default in it.options) { "default of ${it.name} must be one of its options" }
                require(it.options.isEmpty() || it.expression.isEmpty()) { "${it.name} cannot have both options and expr" }
            }
            val declared = variables.map { it.name }
            require(declared.size == declared.toSet().size) { "duplicate variable declaration" }
            val used = VARIABLE_USE.findAll(body).map { it.groupValues[1] }.toSet() - PREDEFINED
            (used - declared.toSet()).let { require(it.isEmpty()) { "used but not declared: $it" } }
            (declared.toSet() - used).let { require(it.isEmpty()) { "declared but not used: $it" } }

            return TemplateSource(name, description, context!!, baseline!!, features, variables, body)
        }

        private fun indexOfDeclared(variables: List<Variable>, target: String, key: String): Int =
            variables.indexOfFirst { it.name == target }.also { require(it >= 0) { "$key for '$target' must come after 'var $target'" } }
    }
}
