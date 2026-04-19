package com.example.moviedb

import android.app.Application
import com.example.moviedb.data.db.MovieDatabase
import com.example.moviedb.data.repository.MovieRepository

class MovieDbApplication : Application() {

    private var _db: MovieDatabase? = null

    val database: MovieDatabase
        get() = _db ?: MovieDatabase.getInstance(this).also { _db = it }

    val repository: MovieRepository
        get() = MovieRepository(database.movieDao(), database.wishlistDao())

    fun closeDatabase() {
        _db?.close()
        MovieDatabase.clearInstance()
        _db = null
    }
}
