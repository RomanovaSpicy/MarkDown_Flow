package com.markdownbinder.database

import androidx.room.*
import com.markdownbinder.models.Bind
import kotlinx.coroutines.flow.Flow

/**
 * BindDao - Data Access Object for working with bindings
 * 
 * All requests are asynchronous via Kotlin Coroutines
 * Flow is used for reactive UI updates.
 */
@Dao
interface BindDao {

    /**
     * Getting all the bindings sorted by order
     * Flow is automatically updated when data changes
     */
    @Query("SELECT * FROM binds ORDER BY `order` ASC")
    fun getAllBinds(): Flow<List<Bind>>

    /**
     * Getting a link by ID
     */
    @Query("SELECT * FROM binds WHERE id = :bindId")
    suspend fun getBindById(bindId: Long): Bind?

    /**
     * Inserting a new bind
     * @return ID of the inserted bind
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bind: Bind): Long

    /**
     * Inserting multiple binds (for presets)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(binds: List<Bind>)

    /**
     * Updating an existing binder
     */
    @Update
    suspend fun update(bind: Bind)

    /**
     * Updating multiple bindings (to change the order)
     */
    @Update
    suspend fun updateAll(binds: List<Bind>)

    /**
     * Deleting a bind
     */
    @Delete
    suspend fun delete(bind: Bind)

    /**
     * Deleting all binds
     */
    @Query("DELETE FROM binds")
    suspend fun deleteAll()

    /**
     * Search for binds by name or content
     */
    @Query("""
        SELECT * FROM binds 
        WHERE name LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%'
        ORDER BY `order` ASC
    """)
    fun searchBinds(query: String): Flow<List<Bind>>

    /**
     * Getting the number of bindings
     */
    @Query("SELECT COUNT(*) FROM binds")
    suspend fun getBindsCount(): Int

    /**
     * Getting the most used binder
     */
    @Query("SELECT * FROM binds ORDER BY usageCount DESC LIMIT 1")
    suspend fun getMostUsedBind(): Bind?

    /**
     * Getting the top N most used bindings
     */
    @Query("SELECT * FROM binds ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getTopUsedBinds(limit: Int): List<Bind>

    /**
     * Updating the usage counter
     */
    @Query("UPDATE binds SET usageCount = usageCount + 1, updatedAt = :timestamp WHERE id = :bindId")
    suspend fun incrementUsageCount(bindId: Long, timestamp: Long = System.currentTimeMillis())
}
