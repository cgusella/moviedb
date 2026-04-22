package com.example.moviedb.ui.screens.collection

enum class SortField { TITLE, DIRECTOR, YEAR }
enum class SortDirection { ASC, DESC }
data class SortOption(val field: SortField, val direction: SortDirection = SortDirection.ASC)
