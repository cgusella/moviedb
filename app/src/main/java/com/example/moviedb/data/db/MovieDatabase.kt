package com.example.moviedb.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.model.WishlistMovie

@Database(
    entities = [Movie::class, WishlistMovie::class],
    version = 1,
    exportSchema = true
)
abstract class MovieDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        @Volatile private var INSTANCE: MovieDatabase? = null

        fun getInstance(context: Context): MovieDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MovieDatabase::class.java,
                    "moviedb.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}
