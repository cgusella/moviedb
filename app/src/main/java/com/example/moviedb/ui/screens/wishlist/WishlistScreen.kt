package com.example.moviedb.ui.screens.wishlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviedb.data.model.WishlistMovie
import com.example.moviedb.di.AppModule
import com.example.moviedb.ui.components.MoviePosterThumbnail
import com.example.moviedb.ui.components.MovieTypeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen() {
    val repository = AppModule.provideRepository(LocalContext.current)
    val viewModel: WishlistViewModel = viewModel(factory = WishlistViewModel.factory(repository))

    val movies by viewModel.wishlistMovies.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wishlist") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.height(48.dp),
                windowInsets = WindowInsets(top = 10.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (movies.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No movies in wishlist yet.\nAdd one from 'Add Movie'!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(movies, key = { it.id }) { movie ->
                    WishlistItem(
                        movie = movie,
                        onPromote = { viewModel.promoteToCollection(movie) },
                        onRemove = { viewModel.removeFromWishlist(movie) }
                    )
                }
            }
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
private fun WishlistItem(
    movie: WishlistMovie,
    onPromote: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove from wishlist") },
            text = { Text("Remove \"${movie.title}\" from your wishlist?") },
            confirmButton = {
                TextButton(onClick = { showRemoveDialog = false; onRemove() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
            }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoviePosterThumbnail(posterUrl = movie.posterUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${movie.director} · ${movie.year} · ${movie.format}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (movie.durationMinutes != null) {
                    Text(
                        text = formatDuration(movie.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (movie.seriesName != null) {
                    Text(
                        text = "Series: ${movie.seriesName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                MovieTypeBadge(movie.type)
            }
            IconButton(onClick = onPromote) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Move to collection",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { showRemoveDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
