package com.example.moviedb.data.backup

import android.content.Context
import android.net.Uri
import com.example.moviedb.MovieDbApplication
import com.example.moviedb.data.db.MovieDatabase

object BackupManager {

    fun export(context: Context, outputUri: Uri): Result<Unit> = runCatching {
        val db = MovieDatabase.getInstance(context)
        // Flush WAL into the main db file so the backup is complete
        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
        val dbFile = context.getDatabasePath("moviedb.db")
        context.contentResolver.openOutputStream(outputUri)!!.use { out ->
            dbFile.inputStream().use { it.copyTo(out) }
        }
    }

    fun import(context: Context, inputUri: Uri): Result<Unit> = runCatching {
        // Release all file locks before overwriting
        (context.applicationContext as MovieDbApplication).closeDatabase()

        val dbFile = context.getDatabasePath("moviedb.db")
        context.contentResolver.openInputStream(inputUri)!!.use { input ->
            dbFile.outputStream().use { input.copyTo(it) }
        }
        // Delete WAL/SHM sidecars — they belong to the old DB, not the imported one
        context.getDatabasePath("moviedb.db-wal").delete()
        context.getDatabasePath("moviedb.db-shm").delete()
    }
}
