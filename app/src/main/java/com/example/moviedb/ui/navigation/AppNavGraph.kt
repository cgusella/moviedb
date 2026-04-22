package com.example.moviedb.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.moviedb.ui.screens.addmovie.AddMovieScreen
import com.example.moviedb.ui.screens.collection.CollectionScreen
import com.example.moviedb.ui.screens.search.SearchScreen
import com.example.moviedb.ui.screens.settings.SettingsScreen
import com.example.moviedb.ui.screens.wishlist.WishlistScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Collection : Screen("collection", "Collection", Icons.Default.MovieFilter)
    object AddMovie : Screen("add_movie", "Add Movie", Icons.Default.Add)
    object Wishlist : Screen("wishlist", "Wishlist", Icons.Default.Bookmarks)
    object Search : Screen("search", "Check", Icons.Default.Search)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavScreens = listOf(Screen.Collection, Screen.AddMovie, Screen.Wishlist, Screen.Search)

@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Collection.route, modifier = modifier) {
        composable(Screen.Collection.route) {
            CollectionScreen(onNavigateToSettings = { navController.navigate(Screen.Settings.route) })
        }
        composable(Screen.AddMovie.route) { AddMovieScreen() }
        composable(Screen.Wishlist.route) { WishlistScreen() }
        composable(Screen.Search.route) { SearchScreen() }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
