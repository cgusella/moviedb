package com.example.moviedb.ui.screens.collection

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.moviedb.data.model.Movie
import com.example.moviedb.di.AppModule
import com.example.moviedb.ui.components.MoviePosterThumbnail
import com.example.moviedb.ui.components.MovieTypeBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CollectionScreen(
    onNavigateToDetail: (Int) -> Unit = {},
    onNavigateToEdit: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = AppModule.provideRepository(context)
    val viewModel: CollectionViewModel = viewModel(factory = CollectionViewModel.factory(repository))

    val movies by viewModel.filteredMovies.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val allSelected = movies.isNotEmpty() && selectedIds.size == movies.size

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(sortOption) { listState.scrollToItem(0) }

    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove movies") },
            text = { Text("Remove ${selectedIds.size} movies from your collection?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteSelected() }) {
                    Text("Remove")
                }
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
                        TextButton(onClick = { if (allSelected) viewModel.deselectAll() else viewModel.selectAll() }) {
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
                        Text(if (movies.isNotEmpty()) "Owned Films (${movies.size})" else "Owned Films")
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.height(48.dp),
                    windowInsets = WindowInsets(top = 10.dp),
                    actions = {
                        IconButton(onClick = {
                            showSearch = !showSearch
                            if (!showSearch) viewModel.setSearchQuery("")
                        }) {
                            Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            listOf(
                                SortField.ADDED_AT to "Recently Added",
                                SortField.TITLE to "Title",
                                SortField.DIRECTOR to "Director",
                                SortField.YEAR to "Year",
                            ).forEach { (field, label) ->
                                val isActive = sortOption.field == field
                                val arrow = if (isActive && field != SortField.ADDED_AT)
                                    if (sortOption.direction == SortDirection.ASC) " ↑" else " ↓" else ""
                                DropdownMenuItem(
                                    text = {
                                        Text("$label$arrow", fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                                    },
                                    onClick = { viewModel.toggleSort(field); showSortMenu = false }
                                )
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Search bar
            AnimatedVisibility(visible = showSearch && !isSelectionMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search films...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            // Grid / List toggle
            if (!isSelectionMode) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = viewMode == "grid",
                        onClick = { viewModel.setViewMode("grid") },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("Grid") }
                    )
                    SegmentedButton(
                        selected = viewMode == "list",
                        onClick = { viewModel.setViewMode("list") },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("List") }
                    )
                }
            }

            val indexLabels = remember(movies, sortOption.field) { indexLabels(movies, sortOption.field) }

            if (movies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No results for \"$searchQuery\""
                               else "Your collection is empty.\nTap 'Add' to get started.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (viewMode == "grid") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(movies, key = { it.id }) { movie ->
                        MovieGridItem(
                            movie = movie,
                            selected = movie.id in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) viewModel.toggleSelection(movie.id)
                                else onNavigateToDetail(movie.id)
                            },
                            onLongClick = { viewModel.toggleSelection(movie.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
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
                                onClick = {
                                    if (isSelectionMode) viewModel.toggleSelection(movie.id)
                                    else onNavigateToDetail(movie.id)
                                }
                            )
                        }
                    }
                    AlphabetIndexBar(
                        labels = indexLabels,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        onLabelSelected = { label ->
                            val idx = firstIndexForLabel(label, movies, sortOption.field)
                            scope.launch { listState.scrollToItem(idx) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MovieGridItem(
    movie: Movie,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (movie.posterUrl != null) {
                AsyncImage(
                    model = movie.posterUrl,
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
            if (selected) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                )
            }
        }
        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
        )
        Text(
            text = movie.year.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )
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
                Text(label, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
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
                        val idx = (y / (size.height.toFloat() / count)).toInt().coerceIn(0, labels.lastIndex)
                        return labels[idx]
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        activeLabel = labelAt(down.position.y)
                        onLabelSelected(activeLabel!!)
                        drag(down.id) { change ->
                            val l = labelAt(change.position.y)
                            if (l != activeLabel) { activeLabel = l; onLabelSelected(l) }
                        }
                        activeLabel = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun Movie.sortKey(field: SortField): String = when (field) {
    SortField.TITLE -> title.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"
    SortField.DIRECTOR -> director.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"
    SortField.YEAR -> "${(year / 10) * 10}s"
    SortField.ADDED_AT -> ""
}

private fun indexLabels(movies: List<Movie>, field: SortField): List<String> =
    if (field == SortField.ADDED_AT) emptyList()
    else movies.map { it.sortKey(field) }.distinct()

private fun firstIndexForLabel(label: String, movies: List<Movie>, field: SortField): Int =
    movies.indexOfFirst { it.sortKey(field) == label }.coerceAtLeast(0)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MovieListItem(
    movie: Movie,
    selected: Boolean,
    isSelectionMode: Boolean,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { if (!isSelectionMode) onLongClick() }),
        colors = if (selected) CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                 else CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoviePosterThumbnail(posterUrl = movie.posterUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(movie.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${movie.director} · ${movie.year} · ${movie.format}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (movie.durationMinutes != null) {
                    Text(formatDuration(movie.durationMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!movie.genres.isNullOrBlank()) {
                    Text(movie.genres, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                MovieTypeBadge(movie.type)
            }
            if (isSelectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
            } else {
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60; val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
