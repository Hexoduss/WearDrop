package com.weardrop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ClothingItem::class, SavedOutfit::class, WearLog::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): WardrobeDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "weardrop.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
