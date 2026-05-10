package com.example.moviedb.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.model.WishlistMovie

@Database(
    entities = [Movie::class, WishlistMovie::class],
    version = 6,
    exportSchema = true
)
abstract class MovieDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        @Volatile private var INSTANCE: MovieDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN overview TEXT")
                db.execSQL("ALTER TABLE wishlist ADD COLUMN overview TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN cast TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN trailerKey TEXT")
                db.execSQL("ALTER TABLE wishlist ADD COLUMN cast TEXT")
                db.execSQL("ALTER TABLE wishlist ADD COLUMN trailerKey TEXT")
            }
        }

        fun getInstance(context: Context): MovieDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MovieDatabase::class.java,
                    "moviedb.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}
