package dev.madina.tickr.core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

/**
 * Browser localStorage, which survives a reload and a closed tab.
 *
 * [name] is **not** honoured here: localStorage is one flat namespace per origin and this returns
 * the whole of it. Android and iOS give each name its own store, so a second call with a different
 * name would be isolated there and shared here, and two stores using the same key would overwrite
 * each other on the web only. There is one store today, so nothing collides; adding a second one
 * means prefixing keys with [name] first.
 */
actual fun createSettings(name: String): Settings = StorageSettings()
