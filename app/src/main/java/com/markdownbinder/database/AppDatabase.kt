package com.markdownbinder.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.markdownbinder.models.Bind

@Database(
    entities = [Bind::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bindDao(): BindDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "markdown_binder.db"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // Beta implementation, implement migration classes
                    .build()
                
                INSTANCE = instance
                instance
            }
        }


        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
