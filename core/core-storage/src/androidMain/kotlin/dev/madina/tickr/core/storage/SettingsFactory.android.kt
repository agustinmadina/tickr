package dev.madina.tickr.core.storage

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Android is the one platform whose key-value store needs a `Context`, which common code has no way
 * to hand over. It is set once from the Application, rather than threaded through every constructor
 * between here and the composition root only to be ignored on the other two platforms.
 */
object AndroidStorage {
    internal var applicationContext: Context? = null

    fun initialise(context: Context) {
        applicationContext = context.applicationContext
    }
}

actual fun createSettings(name: String): Settings {
    val context =
        checkNotNull(AndroidStorage.applicationContext) {
            "AndroidStorage.initialise(context) must be called from Application.onCreate"
        }
    return SharedPreferencesSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE))
}
