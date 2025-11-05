package com.markdownbinder.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.util.concurrent.TimeUnit

/**
 * StatisticsManager - Usage statistics management
 *  *
 *  * Tracked metrics:
 * * - Binds inserted (total number of inserts)
 * * - Keystrokes saved (keystrokes saved)
 * * - Days active (days of active use)
 * *
 * * Statistics are used to display in cards on the home screen
 */
class StatisticsManager(private val context: Context) {

    private val prefs: SharedPreferences = 
        PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val PREF_BINDS_INSERTED = "stats_binds_inserted"
        private const val PREF_KEYSTROKES_SAVED = "stats_keystrokes_saved"
        private const val PREF_FIRST_USE_DATE = "stats_first_use_date"
        private const val PREF_LAST_USE_DATE = "stats_last_use_date"
        private const val PREF_ACTIVE_DAYS = "stats_active_days"
    }

    // ==================== Binds Inserted ====================

    fun getTotalBindsInserted(): Int {
        return prefs.getInt(PREF_BINDS_INSERTED, 0)
    }

    fun incrementBindsInserted() {
        val current = getTotalBindsInserted()
        prefs.edit()
            .putInt(PREF_BINDS_INSERTED, current + 1)
            .apply()
        
        updateActiveDay()
    }

    // ==================== Keystrokes Saved ====================

    fun getTotalKeystrokesSaved(): Int {
        return prefs.getInt(PREF_KEYSTROKES_SAVED, 0)
    }

    fun addKeystrokesSaved(count: Int) {
        val current = getTotalKeystrokesSaved()
        val savedCount = maxOf(0, count - 2)
        prefs.edit()
            .putInt(PREF_KEYSTROKES_SAVED, current + count)
            .apply()
    }

    // ==================== Days Active ====================

    fun getDaysActive(): Int {
        return prefs.getInt(PREF_ACTIVE_DAYS, 0)
    }

    private fun updateActiveDay() {
        val today = System.currentTimeMillis()
        val lastUseDate = prefs.getLong(PREF_LAST_USE_DATE, 0)

        if (!isSameDay(today, lastUseDate)) {
            val activeDays = getDaysActive()
            prefs.edit()
                .putInt(PREF_ACTIVE_DAYS, activeDays + 1)
                .putLong(PREF_LAST_USE_DATE, today)
                .apply()

            if (prefs.getLong(PREF_FIRST_USE_DATE, 0) == 0L) {
                prefs.edit()
                    .putLong(PREF_FIRST_USE_DATE, today)
                    .apply()
            }
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        if (date2 == 0L) return false
        
        val day1 = TimeUnit.MILLISECONDS.toDays(date1)
        val day2 = TimeUnit.MILLISECONDS.toDays(date2)
        
        return day1 == day2
    }

    fun getFirstUseDate(): Long {
        return prefs.getLong(PREF_FIRST_USE_DATE, 0)
    }

    fun getLastUseDate(): Long {
        return prefs.getLong(PREF_LAST_USE_DATE, 0)
    }

    fun clearStatistics() {
        prefs.edit()
            .putInt(PREF_BINDS_INSERTED, 0)
            .putInt(PREF_KEYSTROKES_SAVED, 0)
            .putInt(PREF_ACTIVE_DAYS, 0)
            .putLong(PREF_FIRST_USE_DATE, 0)
            .putLong(PREF_LAST_USE_DATE, 0)
            .apply()
    }

    fun getAverageBindsPerDay(): Double {
        val days = getDaysActive()
        if (days == 0) return 0.0
        
        val total = getTotalBindsInserted()
        return total.toDouble() / days
    }

    /**
     * Exporting statistics to Map for JSON
     */
    fun exportStatistics(): Map<String, Any> {
        return mapOf(
            "bindsInserted" to getTotalBindsInserted(),
            "keystrokesSaved" to getTotalKeystrokesSaved(),
            "daysActive" to getDaysActive(),
            "firstUseDate" to getFirstUseDate(),
            "lastUseDate" to getLastUseDate(),
            "averageBindsPerDay" to getAverageBindsPerDay()
        )
    }
}
