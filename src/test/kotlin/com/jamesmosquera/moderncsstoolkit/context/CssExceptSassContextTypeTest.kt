package com.jamesmosquera.moderncsstoolkit.context

import com.intellij.codeInsight.template.LiveTemplateContextBean
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Behaviour of the plugin's contexts in a real (test) IDE with the CSS, Sass and Less plugins:
 * same as the IDE's CSS contexts everywhere, except in the indented Sass syntax.
 */
class CssExceptSassContextTypeTest : BasePlatformTestCase() {

    private val rules = ModernCssRulesContextType()
    private val declarations = ModernCssDeclarationsContextType()

    private fun ideContext(id: String): TemplateContextType =
        ExtensionPointName<LiveTemplateContextBean>("com.intellij.liveTemplateContext").extensionList
            .single { it.contextId == id }.templateContextType

    /**
     * As in the editor: the abbreviation is already typed where `<caret>` is, and the context is asked at the
     * offset where the abbreviation starts.
     */
    private fun activeIn(fileName: String, text: String, context: TemplateContextType): Boolean {
        myFixture.configureByText(fileName, text.replace("<caret>", "$KEY<caret>"))
        val keyStart = myFixture.caretOffset - KEY.length
        return context.isInContext(TemplateActionContext.expanding(myFixture.file, keyStart))
    }

    private companion object {
        const val KEY = "css-layers"
    }

    fun `test css top level is a rules context, not a declarations context`() {
        assertTrue(activeIn("a.css", "<caret>\n.a { color: red; }", rules))
        assertFalse(activeIn("a.css", "<caret>\n.a { color: red; }", declarations))
    }

    fun `test css inside a block is a declarations context, not a rules context`() {
        assertTrue(activeIn("a.css", ".a {\n  <caret>\n}", declarations))
        assertFalse(activeIn("a.css", ".a {\n  <caret>\n}", rules))
    }

    fun `test scss and less are kept`() {
        assertTrue(activeIn("a.scss", "<caret>\n.a { color: red; }", rules))
        assertTrue(activeIn("a.scss", ".a {\n  <caret>\n}", declarations))
        assertTrue(activeIn("a.less", "<caret>\n.a { color: red; }", rules))
    }

    fun `test indented sass is excluded although the ide css context matches it`() {
        val sass = "<caret>\n.a\n  color: red\n"
        assertTrue("precondition: the IDE's CSS context is active in .sass", activeIn("a.sass", sass, ideContext("CSS_RULESET_LIST")))
        assertFalse(activeIn("a.sass", sass, rules))
        assertFalse(activeIn("a.sass", ".a\n  <caret>\n", declarations))
    }

    fun `test html style block is kept and html body is not a css context`() {
        assertTrue(activeIn("a.html", "<style>\n  <caret>\n</style>", rules))
        assertFalse(activeIn("a.html", "<body>\n  <caret>\n</body>", rules))
    }
}
