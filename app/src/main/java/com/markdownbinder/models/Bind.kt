package com.markdownbinder.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class for the bind (template)
 *
 * @param id Unique identifier
 * @param name The name of the bind (display name)
 * @param content The content with the cursor marker |
 * @param order Display order (for drag & drop)
 * @param usageCount Number of uses (for statistics)
 * @param createdAt Creation time (timestamp)
 * @param updatedAt Last update time
 */
@Entity(tableName = "binds")
data class Bind(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val content: String,
    val order: Int = 0,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns the content with the calculated cursor position
     * @return Pair(text without |, cursor position)
     */
    fun getProcessedContent(): Pair<String, Int> {
        val cursorPos = content.indexOf('|')
        return if (cursorPos != -1) {
            val processed = content.replace("|", "")
            Pair(processed, cursorPos)
        } else {
            Pair(content, content.length)
        }
    }

    /**
     * Creates a copy with an increased usage counter
     */
    fun incrementUsage(): Bind {
        return copy(
            usageCount = usageCount + 1,
            updatedAt = System.currentTimeMillis()
        )
    }
}
