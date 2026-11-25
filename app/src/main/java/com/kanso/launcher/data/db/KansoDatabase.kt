package com.kanso.launcher.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HomeItemEntity::class,
        DockSlotEntity::class,
        FolderEntity::class,
        FolderAppEntity::class,
        HiddenAppEntity::class,
        IconOverrideEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KansoDatabase : RoomDatabase() {
    abstract fun kansoDao(): KansoDao

    companion object {
        @Volatile
        private var instance: KansoDatabase? = null

        fun getInstance(context: Context): KansoDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KansoDatabase::class.java,
                    "kanso_launcher.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
