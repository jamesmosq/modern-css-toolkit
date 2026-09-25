package com.jamesmosquera.moderncsstoolkit.context

import com.intellij.codeInsight.template.LiveTemplateContextBean
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.util.PsiUtilCore
import com.jamesmosquera.moderncsstoolkit.ModernCssToolkitBundle

/**
 * A live template context that behaves exactly like one of the IDE's CSS contexts, except in Sass files.
 *
 * The IDE's CSS contexts also match SCSS, Less and the indented Sass syntax (their languages extend CSS), but our
 * templates use braces, which are invalid in `.sass` files and in `<style lang="sass">` blocks. Instead of copying
 * the CSS context logic, this class looks up the IDE's context by id through the public `liveTemplateContext`
 * extension point and delegates to it, so it keeps working if that logic changes and needs no dependency on the
 * CSS plugin (where it is missing, the context is simply never active).
 * https://plugins.jetbrains.com/docs/intellij/providing-live-templates.html
 */
abstract class CssExceptSassContextType(presentableName: String, private val cssContextId: String) :
    TemplateContextType(presentableName) {

    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val file = templateActionContext.file
        if (PsiUtilCore.getLanguageAtOffset(file, templateActionContext.startOffset).id == SASS_LANGUAGE_ID) return false
        val css = LIVE_TEMPLATE_CONTEXTS.extensionList.firstOrNull { it.contextId == cssContextId } ?: return false
        return css.templateContextType.isInContext(templateActionContext)
    }

    companion object {
        /** Id of the indented Sass syntax (org.jetbrains.plugins.sass.SASSLanguage); SCSS is "SCSS" and stays enabled. */
        const val SASS_LANGUAGE_ID = "SASS"

        private val LIVE_TEMPLATE_CONTEXTS = ExtensionPointName<LiveTemplateContextBean>("com.intellij.liveTemplateContext")
    }
}

/** Top level of a stylesheet: rules and at-rules. */
class ModernCssRulesContextType :
    CssExceptSassContextType(ModernCssToolkitBundle.message("context.rules"), "CSS_RULESET_LIST")

/** Inside a `{ }` block: declarations. */
class ModernCssDeclarationsContextType :
    CssExceptSassContextType(ModernCssToolkitBundle.message("context.declarations"), "CSS_DECLARATION_BLOCK")

/** After `property:`: a value. */
class ModernCssValueContextType :
    CssExceptSassContextType(ModernCssToolkitBundle.message("context.value"), "CSS_PROPERTY_VALUE")
