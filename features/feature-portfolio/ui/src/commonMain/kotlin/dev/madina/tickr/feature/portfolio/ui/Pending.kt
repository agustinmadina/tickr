package dev.madina.tickr.feature.portfolio.ui

/**
 * Shown wherever a figure is not known yet, rather than a zero standing in for one.
 *
 * ASCII, not an em dash: the wasm build bundles its own font through Skia and renders anything
 * outside it as an empty box, and this string appears on every row before the first quote lands.
 */
internal const val Pending = "--"
