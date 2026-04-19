package com.example.moviedb.data.backup

import android.content.Context
import android.net.Uri
import com.example.moviedb.MovieDbApplication
import com.example.moviedb.data.db.MovieDatabase
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
