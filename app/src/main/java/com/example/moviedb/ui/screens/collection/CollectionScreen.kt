package com.example.moviedb.ui.screens.collection

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import coil3.compose.AsyncImage
import com.example.moviedb.data.backup.BackupManager
import com.example.moviedb.data.model.Movie
import com.example.moviedb.di.AppModule
import com.example.moviedb.ui.components.MoviePosterThumbnail
import com.example.moviedb.ui.components.MovieTypeBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DialogPosterShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(onNavigateToSettings: () -> Unit = {}, onNavigateToEdit: (Int) -> Unit = {}) {
    val context = LocalContext.current
    val repository = AppModule.provideRepository(context)
    val viewModel: CollectionViewModel = viewModel(factory = CollectionViewModel.factory(repository))

    val movies by viewModel.filteredMovies.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val allSelected = movies.isNotEmpty() && selectedIds.size == movies.size

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(sortOption) {
        listState.scrollToItem(0)
    }

    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
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
                    title = {
                        Text(if (movies.isNotEmpty()) "My Shelf (${movies.size})" else "My Shelf")
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.height(48.dp),
                    windowInsets = WindowInsets(top = 10.dp),
                    actions = {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(
                                SortField.TITLE to "Title",
                                SortField.DIRECTOR to "Director",
                                SortField.YEAR to "Year",
                            ).forEach { (field, label) ->
                                val isActive = sortOption.field == field
                                val arrow = if (isActive) if (sortOption.direction == SortDirection.ASC) " ↑" else " ↓" else ""
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$label$arrow",
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { viewModel.toggleSort(field); showSortMenu = false }
                                )
                            }
                        }
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
        val indexLabels = remember(movies, sortOption.field) {
            indexLabels(movies, sortOption.field)
        }

        Box(
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
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 40.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(movies, key = { it.id }) { movie ->
                        MovieListItem(
                            movie = movie,
                            selected = movie.id in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onDelete = { viewModel.deleteMovie(movie) },
                            onLongClick = { viewModel.toggleSelection(movie.id) },
                            onToggleSelect = { viewModel.toggleSelection(movie.id) },
                            onEdit = { onNavigateToEdit(movie.id) }
                        )
                    }
                }

                AlphabetIndexBar(
                    labels = indexLabels,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    onLabelSelected = { label ->
                        val idx = firstIndexForLabel(label, movies, sortOption.field)
                        scope.launch { listState.scrollToItem(idx) }
                    }
                )
            }
        }
    }
}

@Composable
private fun AlphabetIndexBar(
    labels: List<String>,
    modifier: Modifier = Modifier,
    onLabelSelected: (String) -> Unit
) {
    if (labels.isEmpty()) return
    var activeLabel by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        activeLabel?.let { label ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-36).dp)
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(24.dp)
                .pointerInput(labels) {
                    val count = labels.size.coerceAtLeast(1)
                    fun labelAt(y: Float): String {
                        val idx = (y / (size.height.toFloat() / count))
                            .toInt()
                            .coerceIn(0, labels.lastIndex)
                        return labels[idx]
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val first = labelAt(down.position.y)
                        activeLabel = first
                        onLabelSelected(first)
                        drag(down.id) { change ->
                            val l = labelAt(change.position.y)
                            if (l != activeLabel) {
                                activeLabel = l
                                onLabelSelected(l)
                            }
                        }
                        activeLabel = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Movie.sortKey(field: SortField): String = when (field) {
    SortField.TITLE -> title.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"
    SortField.DIRECTOR -> director.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"
    SortField.YEAR -> "${(year / 10) * 10}s"
}

private fun indexLabels(movies: List<Movie>, field: SortField): List<String> =
    movies.map { it.sortKey(field) }.distinct()

private fun firstIndexForLabel(label: String, movies: List<Movie>, field: SortField): Int =
    movies.indexOfFirst { it.sortKey(field) == label }.coerceAtLeast(0)

@Composable
private fun MovieDetailsDialog(
    movie: Movie,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    var showFullscreen by remember { mutableStateOf(false) }

    if (showFullscreen && movie.posterUrl != null) {
        PosterFullscreenDialog(url = movie.posterUrl, onDismiss = { showFullscreen = false })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (movie.posterUrl != null) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 100.dp, height = 140.dp)
                                .clip(DialogPosterShape)
                                .clickable { showFullscreen = true },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 140.dp)
                                .clip(DialogPosterShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
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
                if (!movie.overview.isNullOrBlank()) {
                    HorizontalDivider()
                    Text(
                        text = movie.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onEdit() }) { Text("Edit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PosterFullscreenDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url.replace("/w185/", "/w500/"),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MovieListItem(
    movie: Movie,
    selected: Boolean,
    isSelectionMode: Boolean,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    if (showDetailsDialog) {
        MovieDetailsDialog(
            movie = movie,
            onDismiss = { showDetailsDialog = false },
            onEdit = onEdit
        )
    }

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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else showDetailsDialog = true },
                onLongClick = { if (!isSelectionMode) onLongClick() }
            ),
        colors = if (selected)
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else
            CardDefaults.elevatedCardColors()
    ) {
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
                MovieTypeBadge(movie.type)
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
