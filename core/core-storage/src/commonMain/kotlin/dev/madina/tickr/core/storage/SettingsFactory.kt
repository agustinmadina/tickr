package dev.madina.tickr.core.storage

import com.russhwolf.settings.Settings

/**
 * Key-value storage, backed by whatever each platform already provides: SharedPreferences on
 * Android, NSUserDefaults on iOS, localStorage in the browser.
 *
 * **Why not SQLDelight.** It is the usual answer for persistence in Kotlin Multiplatform and it is
 * the right one for tabular data, but it has no maintained driver for wasm, and the web target is
 * the point of this project: adding a dependency that works on two platforms out of three would
 * trade the interesting half of the demo for a nicer line in the stack table. A portfolio is also a
 * handful of rows read whole and written whole, with no queries, no joins and no migrations to
 * speak of, so a serialised document is the proportionate choice rather than a compromise.
 */
expect fun createSettings(name: String): Settings
