package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        TaskEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MiyaDatabase : RoomDatabase() {

    abstract fun miyaDao(): MiyaDao

    companion object {
        @Volatile
        private var INSTANCE: MiyaDatabase? = null

        fun getDatabase(context: Context): MiyaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MiyaDatabase::class.java,
                    "miya_assistant_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
