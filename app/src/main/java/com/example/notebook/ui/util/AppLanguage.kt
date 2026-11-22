package com.example.notebook.ui.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/**
 * App language (v1.1): follow the system, or force Russian / English.
 *
 * Android 13+ has a built-in per-app language (LocaleManager): Android stores the choice,
 * applies it on every start and shows it in system settings ("Языки приложений").
 * Setting it restarts the screen automatically in the new language.
 * On older Android versions the app simply follows the system language.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    RUSSIAN("ru"),
    ENGLISH("en"),
    ;

    companion object {
        val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        fun current(context: Context): AppLanguage {
            if (!isSupported) return SYSTEM
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) return SYSTEM
            return entries.firstOrNull { it.tag == locales[0].language } ?: SYSTEM
        }

        fun set(context: Context, language: AppLanguage) {
            if (!isSupported) return
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        }
    }
}
