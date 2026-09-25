package com.jamesmosquera.moderncsstoolkit

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.ModernCssToolkitBundle"

/**
 * Delegates to a DynamicBundle instance (subclassing is deprecated).
 * https://plugins.jetbrains.com/docs/intellij/internationalization.html
 */
object ModernCssToolkitBundle {
    private val INSTANCE = DynamicBundle(ModernCssToolkitBundle::class.java, BUNDLE)

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): @Nls String =
        INSTANCE.getMessage(key, *params)
}
