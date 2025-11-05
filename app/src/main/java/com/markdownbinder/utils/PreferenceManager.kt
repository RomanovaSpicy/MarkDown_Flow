package com.markdownbinder.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager as AndroidXPreferenceManager

/**
 * PreferenceManager - Managing SharedPreferences
 *  *
 * * Saved settings:
 * * - First launch flag
 *  * - Language selection
 *  * - Overlay width & height
 *  * - Statistics data
 *  *
 * * All methods are thread-safe and use apply() for asynchronous writes
 */
object PreferenceManager {

    private const val PREF_FIRST_LAUNCH = "first_launch"
    private const val PREF_LANGUAGE = "language"
    private const val PREF_OVERLAY_WIDTH = "overlay_width"
    private const val PREF_OVERLAY_HEIGHT = "overlay_height"
    private const val PREF_TEMP_EXPORT_DATA = "temp_export_data"

    private const val DEFAULT_OVERLAY_WIDTH = 250
    private const val DEFAULT_OVERLAY_HEIGHT = 5

    /**
     * Getting SharedPreferences
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return AndroidXPreferenceManager.getDefaultSharedPreferences(context)
    }

    // ==================== First Launch ====================

    /**
     * Checking the first launch
     */
    fun isFirstLaunch(context: Context): Boolean {
        return getPreferences(context).getBoolean(PREF_FIRST_LAUNCH, true)
    }

    /**
     * Setting the flag for completing the first run
     */
    fun setFirstLaunchComplete(context: Context) {
        getPreferences(context)
            .edit()
            .putBoolean(PREF_FIRST_LAUNCH, false)
            .apply()
    }

    // ==================== Language ====================

    /**
     * Getting the selected language.
     */
    fun getLanguage(context: Context, defaultValue: String = "en"): String {
        return getPreferences(context).getString(PREF_LANGUAGE, defaultValue) ?: defaultValue
    }

    /**
     * Setting the language
     */
    fun setLanguage(context: Context, language: String) {
        getPreferences(context)
            .edit()
            .putString(PREF_LANGUAGE, language)
            .apply()
    }

    // ==================== Overlay Settings ====================

    /**
     * Getting the width of the overlay
     */
    fun getOverlayWidth(context: Context): Int {
        return getPreferences(context).getInt(PREF_OVERLAY_WIDTH, DEFAULT_OVERLAY_WIDTH)
    }

    /**
     * Setting the overlay width
     */
    fun setOverlayWidth(context: Context, width: Int) {
        getPreferences(context)
            .edit()
            .putInt(PREF_OVERLAY_WIDTH, width)
            .apply()
    }

    /**
     * Getting the height of the overlay (number of lines)
     */
    fun getOverlayHeight(context: Context): Int {
        return getPreferences(context).getInt(PREF_OVERLAY_HEIGHT, DEFAULT_OVERLAY_HEIGHT)
    }

    /**
     * Setting the height of the overlay
     */
    fun setOverlayHeight(context: Context, height: Int) {
        getPreferences(context)
            .edit()
            .putInt(PREF_OVERLAY_HEIGHT, height)
            .apply()
    }

    // ==================== Export/Import Temp Data ====================

    /**
     * Saving temporary export data
     */
    fun setTempExportData(context: Context, json: String) {
        getPreferences(context)
            .edit()
            .putString(PREF_TEMP_EXPORT_DATA, json)
            .apply()
    }

    /**
     * Getting temporary export data
     */
    fun getTempExportData(context: Context): String {
        return getPreferences(context).getString(PREF_TEMP_EXPORT_DATA, "") ?: ""
    }

    /**
     * Clearing temporary export data
     */
    fun clearTempExportData(context: Context) {
        getPreferences(context)
            .edit()
            .remove(PREF_TEMP_EXPORT_DATA)
            .apply()
    }

    // ==================== Clear All ====================

    /**
     * Clearing all settings (except the first run)
     */
    fun clearAll(context: Context) {
        getPreferences(context)
            .edit()
            .clear()
            .apply()
    }
}