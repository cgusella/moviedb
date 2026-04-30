package com.example.moviedb.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.moviedb.ui.screens.addmovie.AddMovieScreen
import com.example.moviedb.ui.screens.collection.CollectionScreen
import com.example.moviedb.ui.screens.detail.MovieDetailScreen
import com.example.moviedb.ui.screens.detail.WishlistDetailScreen
import com.example.moviedb.ui.screens.editmovie.EditMovieScreen
import com.example.moviedb.ui.screens.settings.SettingsScreen
import com.example.moviedb.ui.screens.wishlist.WishlistScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Collection : Screen("collection", "Owned", Icons.Default.Folder)
    object AddMovie : Screen("add_movie", "Add", Icons.Default.Add)
    object Wishlist : Screen("wishlist", "Wishlist", Icons.Default.Favorite)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavScreens = listOf(Screen.Collection, Screen.AddMovie, Screen.Wishlist, Screen.Settings)

@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Collection.route, modifier = modifier) {
        composable(Screen.Collection.route) {
            CollectionScreen(
                onNavigateToDetail = { movieId -> navController.navigate("movie_detail/$movieId") },
                onNavigateToEdit = { movieId -> navController.navigate("edit_movie/$movieId") }
            )
        }
        composable(Screen.AddMovie.route) { AddMovieScreen() }
        composable(Screen.Wishlist.route) {
            WishlistScreen(onNavigateToDetail = { movieId -> navController.navigate("wishlist_detail/$movieId") })
        }
        composable(Screen.Settings.route) { SettingsScreen() }
        composable(
            route = "movie_detail/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments!!.getInt("movieId")
            MovieDetailScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("edit_movie/$movieId") }
            )
        }
        composable(
            route = "wishlist_detail/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments!!.getInt("movieId")
            WishlistDetailScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "edit_movie/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments!!.getInt("movieId")
            EditMovieScreen(movieId = movieId, onBack = { navController.popBackStack() })
        }
    }
}
