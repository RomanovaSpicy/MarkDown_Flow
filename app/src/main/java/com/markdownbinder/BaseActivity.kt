package com.markdownbinder

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.markdownbinder.utils.PreferenceManager

/**
 * BaseActivity is the base class for all Activities in the application.
 *
 * Is responsible for the centralized installation of the correct locale (language)
 * when creating any Activity.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = PreferenceManager.getLanguage(newBase, "en")
        super.attachBaseContext(LocaleManager.updateResources(newBase, lang))
    }
}

object LocaleManager {
    fun updateResources(context: Context, language: String): Context {
        val locale = java.util.Locale(language)
        java.util.Locale.setDefault(locale)

        val configuration: Configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }
}