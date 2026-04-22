package com.example.moviedb.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.moviedb.MovieDbApplication
import com.example.moviedb.data.db.MovieDatabase
import com.example.moviedb.data.model.Movie
import com.example.moviedb.data.model.WishlistMovie
import java.io.File

object BackupManager {

    fun export(context: Context, outputUri: Uri): Result<Unit> = runCatching {
        val db = MovieDatabase.getInstance(context)
        val sdb = db.openHelper.writableDatabase

        val cursor = sdb.query("PRAGMA wal_checkpoint(TRUNCATE)")
        cursor.moveToFirst()
        cursor.close()

        // Usa il path reale dal PRAGMA invece di getDatabasePath
        val pathCursor = sdb.query("PRAGMA database_list")
        pathCursor.moveToFirst()
        val dbPath = pathCursor.getString(2)
        pathCursor.close()

        val dbFile = File(dbPath)
        context.contentResolver.openOutputStream(outputUri)!!.use { out ->
            dbFile.inputStream().use { it.copyTo(out) }
        }
    }

    suspend fun merge(context: Context, inputUri: Uri): Result<Pair<Int, Int>> = runCatching {
        val tempFile = File(context.cacheDir, "import_merge_temp.db")
        context.contentResolver.openInputStream(inputUri)!!.use { input ->
            tempFile.outputStream().use { input.copyTo(it) }
        }

        val database = MovieDatabase.getInstance(context)
        val movieDao = database.movieDao()
        val wishlistDao = database.wishlistDao()

        var imported = 0
        var skipped = 0

        val sdb = SQLiteDatabase.openDatabase(tempFile.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            sdb.rawQuery("SELECT * FROM movies", null).use { cursor ->
                while (cursor.moveToNext()) {
                    fun str(col: String) = cursor.getColumnIndex(col).takeIf { it >= 0 }?.let { cursor.getString(it) }
                    fun int(col: String) = cursor.getColumnIndex(col).takeIf { it >= 0 }?.let { cursor.getInt(it) }
                    val title = str("title") ?: continue
                    val year = int("year") ?: continue
                    if (movieDao.movieExists(title, year)) {
                        skipped++
                    } else {
                        movieDao.insertMovie(
                            Movie(
                                title = title,
                                director = str("director") ?: "",
                                year = year,
                                format = str("format") ?: "DVD",
                                type = str("type") ?: "Movie",
                                seriesName = str("seriesName"),
                                posterUrl = str("posterUrl"),
                                durationMinutes = int("durationMinutes"),
                                genres = str("genres"),
                                addedAt = cursor.getColumnIndex("addedAt").takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: System.currentTimeMillis()
                            )
                        )
                        imported++
                    }
                }
            }

            sdb.rawQuery("SELECT * FROM wishlist", null).use { cursor ->
                while (cursor.moveToNext()) {
                    fun str(col: String) = cursor.getColumnIndex(col).takeIf { it >= 0 }?.let { cursor.getString(it) }
                    fun int(col: String) = cursor.getColumnIndex(col).takeIf { it >= 0 }?.let { cursor.getInt(it) }
                    val title = str("title") ?: continue
                    val year = int("year") ?: continue
                    if (wishlistDao.wishlistExists(title, year)) {
                        skipped++
                    } else {
                        wishlistDao.insertWishlistMovie(
                            WishlistMovie(
                                title = title,
                                director = str("director") ?: "",
                                year = year,
                                format = str("format") ?: "DVD",
                                type = str("type") ?: "Movie",
                                seriesName = str("seriesName"),
                                posterUrl = str("posterUrl"),
                                durationMinutes = int("durationMinutes"),
                                genres = str("genres"),
                                addedAt = cursor.getColumnIndex("addedAt").takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: System.currentTimeMillis()
                            )
                        )
                        imported++
                    }
                }
            }
        } finally {
            sdb.close()
            tempFile.delete()
        }

        imported to skipped
    }

    fun import(context: Context, inputUri: Uri): Result<Unit> = runCatching {
        (context.applicationContext as MovieDbApplication).closeDatabase()

        val dbFile = context.getDatabasePath("moviedb.db")
        context.contentResolver.openInputStream(inputUri)!!.use { input ->
            dbFile.outputStream().use { input.copyTo(it) }
        }
        context.getDatabasePath("moviedb.db-wal").delete()
        context.getDatabasePath("moviedb.db-shm").delete()
    }
}
