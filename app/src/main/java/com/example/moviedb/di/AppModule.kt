package com.example.moviedb.di

import android.content.Context
import com.example.moviedb.MovieDbApplication
import com.example.moviedb.data.repository.MovieRepository

object AppModule {
    fun provideRepository(context: Context): MovieRepository =
        (context.applicationContext as MovieDbApplication).repository
}
