package com.example.moviedb.ui.screens.collection

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviedb.data.backup.BackupManager
import com.example.moviedb.data.model.Movie
import com.example.moviedb.di.AppModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(onNavigateToSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = AppModule.provideRepository(context)
    val viewModel: CollectionViewModel = viewModel(factory = CollectionViewModel.factory(repository))

    val movies by viewModel.filteredMovies.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val allSelected = movies.isNotEmpty() && selectedIds.size == movies.size

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { BackupManager.export(context, uri) }
            snackbarHostState.showSnackbar(
                if (result.isSuccess) "Backup saved successfully."
                else "Export failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    val mergeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { BackupManager.merge(context, uri) }
            if (result.isSuccess) {
                val (imported, skipped) = result.getOrThrow()
                snackbarHostState.showSnackbar("Imported $imported, $skipped already present.")
            } else {
                snackbarHostState.showSnackbar("Merge failed. Make sure you selected a valid backup file.")
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { BackupManager.import(context, uri) }
            if (result.isSuccess) {
                withContext(Dispatchers.Main) {
                    val intent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)!!
                        .apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            } else {
                snackbarHostState.showSnackbar("Restore failed. Make sure you selected a valid backup file.")
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import backup") },
            text = { Text("Merge adds only missing movies without touching existing data.\nFull Restore replaces everything and cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    mergeLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                }) { Text("Merge") }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = {
                        showImportDialog = false
                        restoreLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    }) { Text("Full Restore") }
                    TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove movies") },
            text = { Text("Remove ${selectedIds.size} movies from your collection?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSelected()
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.height(48.dp),
                    windowInsets = WindowInsets(top = 10.dp),
                    actions = {
                        TextButton(onClick = {
                            if (allSelected) viewModel.deselectAll() else viewModel.selectAll()
                        }) {
                            Text(if (allSelected) "None" else "All")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("My Shelf") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.height(48.dp),
                    windowInsets = WindowInsets(top = 10.dp),
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export backup") },
                                onClick = {
                                    showMenu = false
                                    exportLauncher.launch("moviedb_backup.db")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import backup") },
                                onClick = {
                                    showMenu = false
                                    showImportDialog = true
                                }
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (movies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Your collection is empty.\nTap 'Add Movie' to get started.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movies, key = { it.id }) { movie ->
                        MovieListItem(
                            movie = movie,
                            selected = movie.id in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onDelete = { viewModel.deleteMovie(movie) },
                            onLongClick = { viewModel.toggleSelection(movie.id) },
                            onToggleSelect = { viewModel.toggleSelection(movie.id) }
                        )
                    }
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
private fun MoviePoster(posterUrl: String?) {
    val shape = RoundedCornerShape(4.dp)
    if (posterUrl != null) {
        AsyncImage(
            model = posterUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 56.dp, height = 80.dp).clip(shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MovieListItem(
    movie: Movie,
    selected: Boolean,
    isSelectionMode: Boolean,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Remove movie") },
            text = { Text("Remove \"${movie.title}\" from your collection?") },
            confirmButton = {
                TextButton(onClick = { showDialog = false; onDelete() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() },
                onLongClick = { if (!isSelectionMode) onLongClick() }
            ),
        colors = if (selected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else
            CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoviePoster(posterUrl = movie.posterUrl)
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
                if (!movie.genres.isNullOrBlank()) {
                    Text(
                        text = movie.genres,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (movie.seriesName != null) {
                    Text(
                        text = "Series: ${movie.seriesName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val badgeLabel = if (movie.type == "TV Series") "TV Series" else "Film"
                SuggestionChip(
                    onClick = {},
                    label = { Text(badgeLabel, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(24.dp)
                )
            }
            if (isSelectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelect() }
                )
            } else {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
