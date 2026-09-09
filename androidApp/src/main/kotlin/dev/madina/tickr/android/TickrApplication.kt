package dev.madina.tickr.android

import android.app.Application
import dev.madina.tickr.core.storage.AndroidStorage

/**
 * Exists for one reason: Android's key-value store needs a `Context`, and common code has none to
 * give. Handing it over once here keeps that requirement from leaking into every constructor
 * between the storage layer and the composition root.
 */
class TickrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidStorage.initialise(this)
    }
}
