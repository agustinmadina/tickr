package dev.madina.tickr.core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

/**
 * Browser localStorage, which survives a reload and a closed tab. The [name] is used as a key
 * prefix rather than a separate store, since localStorage is one flat namespace per origin.
 */
actual fun createSettings(name: String): Settings = StorageSettings()
