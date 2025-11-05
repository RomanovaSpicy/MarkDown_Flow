package com.markdownbinder.database

import android.content.Context
import com.markdownbinder.models.Bind
import kotlinx.coroutines.flow.Flow

/**
 * BindRepository is an abstraction layer between UI and Database
 * 
 * Provides a clean API for working with bindings
 * All operations are asynchronous via Kotlin Coroutines
 */
class BindRepository(context: Context) {

    private val bindDao: BindDao = AppDatabase.getDatabase(context).bindDao()

    /**
     * Getting all the bindings (Flow for reactive updates)
     */
    fun getAllBinds(): Flow<List<Bind>> {
        return bindDao.getAllBinds()
    }

    /**
     * Getting a link by ID
     */
    suspend fun getBindById(id: Long): Bind? {
        return bindDao.getBindById(id)
    }

    /**
     * Inserting a new bind
     */
    suspend fun insertBind(bind: Bind): Long {
        return bindDao.insert(bind)
    }

    /**
     * Inserting multiple binds (for presets)
     */
    suspend fun insertBinds(binds: List<Bind>) {
        bindDao.insertAll(binds)
    }

    /**
     * Updating the binder
     */
    suspend fun updateBind(bind: Bind) {
        bindDao.update(bind)
    }

    /**
     * Updating the order of the binds (after drag & drop)
     */
    suspend fun updateBindsOrder(binds: List<Bind>) {
        bindDao.updateAll(binds)
    }

    /**
     * Deleting a binder
     */
    suspend fun deleteBind(bind: Bind) {
        bindDao.delete(bind)
    }

    /**
     * Deleting all bindings
     */
    suspend fun deleteAllBinds() {
        bindDao.deleteAll()
    }

    /**
     * Search for binds
     */
    fun searchBinds(query: String): Flow<List<Bind>> {
        return bindDao.searchBinds(query)
    }

    /**
     * Getting the number of bindings
     */
    suspend fun getBindsCount(): Int {
        return bindDao.getBindsCount()
    }

    /**
     * Getting the most used binder
     */
    suspend fun getMostUsedBind(): Bind? {
        return bindDao.getMostUsedBind()
    }

    /**
     * Getting the top used bindings
     */
    suspend fun getTopUsedBinds(limit: Int = 5): List<Bind> {
        return bindDao.getTopUsedBinds(limit)
    }

    /**
     * Increasing the usage counter
     */
    suspend fun incrementBindUsage(bindId: Long) {
        bindDao.incrementUsageCount(bindId)
    }
}
