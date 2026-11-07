package com.gityou.app.ui.components

import io.noties.prism4j.Prism4j
import io.noties.prism4j.annotations.PrismBundle

/**
 * Anchor for the prism4j-bundler annotation processor: kapt generates
 * [GrammarLocatorDef] in this package with ALL bundled prism4j languages
 * (java, kotlin, python, json, yaml, js, c/cpp/csharp, go, sql, swift,
 * dart, css, markup, markdown, ...). Unknown languages (bash, rust, ...)
 * simply render as plain monospace, exactly as before (F-011).
 */
@PrismBundle(includeAll = true)
private object PrismBundleAnchor {
    val prism: Prism4j by lazy { Prism4j(GrammarLocatorDef()) }
}

/** Shared [Prism4j] engine for syntax highlighting; the locator is generated. */
internal val markdownPrism: Prism4j
    get() = PrismBundleAnchor.prism
