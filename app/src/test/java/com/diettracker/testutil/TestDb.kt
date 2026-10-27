package com.diettracker.testutil

import android.content.Context
import androidx.room.Room
import com.diettracker.data.local.db.AppDatabase

object TestDb {

    fun inMemory(context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
