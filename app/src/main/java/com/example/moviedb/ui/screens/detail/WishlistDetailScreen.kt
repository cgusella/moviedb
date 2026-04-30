package com.example.moviedb.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.example.moviedb.data.model.WishlistMovie
import com.example.moviedb.data.repository.MovieRepository
import com.example.moviedb.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private class WishlistDetailViewModel(
    private val repository: MovieRepository,
    private val movieId: Int
) : ViewModel() {

    private val _movie = MutableStateFlow<WishlistMovie?>(null)
    val movie: StateFlow<WishlistMovie?> = _movie.asStateFlow()

    init {
        viewModelScope.launch { _movie.value = repository.getWishlistMovieById(movieId) }
    }

    fun promoteToCollection(movie: WishlistMovie, onDone: () -> Unit) {
        viewModelScope.launch { repository.promoteToCollection(movie); onDone() }
    }

    fun removeFromWishlist(movie: WishlistMovie, onDone: () -> Unit) {
        viewModelScope.launch { repository.removeFromWishlist(movie); onDone() }
    }

    companion object {
        fun factory(repository: MovieRepository, movieId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer { WishlistDetailViewModel(repository, movieId) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WishlistDetailScreen(
    movieId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = AppModule.provideRepository(context)
    val viewModel: WishlistDetailViewModel = viewModel(factory = WishlistDetailViewModel.factory(repository, movieId))
    val movie by viewModel.movie.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var showMoveConfirm by remember { mutableStateOf(false) }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove from wishlist") },
            text = { Text("Remove \"${movie?.title}\" from your wishlist?") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    movie?.let { viewModel.removeFromWishlist(it) { onBack() } }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showMoveConfirm) {
        AlertDialog(
            onDismissRequest = { showMoveConfirm = false },
            title = { Text("Move to Owned") },
            text = { Text("Move \"${movie?.title}\" to your collection?") },
            confirmButton = {
                TextButton(onClick = {
                    showMoveConfirm = false
                    movie?.let { viewModel.promoteToCollection(it) { onBack() } }
                }) { Text("Move") }
            },
            dismissButton = {
                TextButton(onClick = { showMoveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Remove from wishlist", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showRemoveConfirm = true }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = { showMoveConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Move to Owned")
                    }
                }
            }
        }
    ) { padding ->
        val m = movie
        if (m == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Poster + metadata
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (m.posterUrl != null) {
                        AsyncImage(
                            model = m.posterUrl.replace("/w185/", "/w342/"),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = m.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    val meta = buildList {
                        add(m.year.toString())
                        if (m.durationMinutes != null) add(formatWishlistDuration(m.durationMinutes))
                        if (!m.genres.isNullOrBlank()) add(m.genres.split(",").firstOrNull()?.trim() ?: "")
                    }.filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Director: ${m.director}", style = MaterialTheme.typography.bodyMedium)
                    if (m.seriesName != null) {
                        Spacer(Modifier.height(4.dp))
                        Text("Series: ${m.seriesName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Genre chips
            if (!m.genres.isNullOrBlank()) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    m.genres.split(",").forEach { genre ->
                        val g = genre.trim()
                        if (g.isNotBlank()) {
                            SuggestionChip(onClick = {}, label = { Text(g) })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Synopsis
            if (!m.overview.isNullOrBlank()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Synopsis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(m.overview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Production date
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Production Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(m.year.toString(), style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatWishlistDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
