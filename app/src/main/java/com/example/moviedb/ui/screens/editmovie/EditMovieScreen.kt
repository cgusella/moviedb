package com.example.moviedb.ui.screens.editmovie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviedb.di.AppModule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMovieScreen(movieId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = AppModule.provideRepository(context)
    val viewModel: EditMovieViewModel = viewModel(
        key = "edit_$movieId",
        factory = EditMovieViewModel.factory(repository, movieId)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val director by viewModel.director.collectAsStateWithLifecycle()
    val year by viewModel.year.collectAsStateWithLifecycle()
    val format by viewModel.format.collectAsStateWithLifecycle()
    val belongsToSeries by viewModel.belongsToSeries.collectAsStateWithLifecycle()
    val seriesName by viewModel.seriesName.collectAsStateWithLifecycle()
    val titleSearchQuery by viewModel.titleSearchQuery.collectAsStateWithLifecycle()
    val titleSearchState by viewModel.titleSearchState.collectAsStateWithLifecycle()
    val searchType by viewModel.searchType.collectAsStateWithLifecycle()

    val validationError = uiState as? EditMovieUiState.ValidationError
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var formatExpanded by remember { mutableStateOf(false) }
    val formats = listOf("DVD", "Blu-ray", "4K", "Video tape")

    LaunchedEffect(uiState) {
        if (uiState is EditMovieUiState.Saved) onBack()
        if (uiState is EditMovieUiState.NotFound) {
            snackbarHostState.showSnackbar("Movie not found.")
            onBack()
        }
    }

    if (titleSearchState is TitleSearchState.Results) {
        val results = (titleSearchState as TitleSearchState.Results).items
        AlertDialog(
            onDismissRequest = viewModel::dismissTitleSearch,
            title = { Text("Select a movie") },
            text = {
                LazyColumn {
                    items(results) { movie ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onTitleSearchResultSelected(movie.id, movie.type) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val shape = RoundedCornerShape(4.dp)
                            if (movie.posterUrl != null) {
                                AsyncImage(
                                    model = movie.posterUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(width = 40.dp, height = 56.dp).clip(shape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 56.dp)
                                        .clip(shape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(movie.title, style = MaterialTheme.typography.bodyLarge)
                                if (movie.year.isNotBlank()) {
                                    Text(movie.year, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissTitleSearch) { Text("Cancel") }
            }
        )
    }

    if (titleSearchState is TitleSearchState.Error) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTitleSearch,
            title = { Text("Not found") },
            text = { Text((titleSearchState as TitleSearchState.Error).message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissTitleSearch) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Movie") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.height(48.dp),
                windowInsets = WindowInsets(top = 10.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState is EditMovieUiState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = titleSearchQuery,
                    onValueChange = viewModel::onTitleSearchQueryChange,
                    label = { Text("Search by title") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        viewModel.onTitleSearch()
                    })
                )
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onTitleSearch()
                    },
                    enabled = titleSearchState !is TitleSearchState.Loading
                ) {
                    if (titleSearchState is TitleSearchState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = searchType == com.example.moviedb.ui.screens.addmovie.SearchType.MOVIE,
                    onClick = { viewModel.onSearchTypeChange(com.example.moviedb.ui.screens.addmovie.SearchType.MOVIE) },
                    label = { Text("Film") }
                )
                FilterChip(
                    selected = searchType == com.example.moviedb.ui.screens.addmovie.SearchType.TV,
                    onClick = { viewModel.onSearchTypeChange(com.example.moviedb.ui.screens.addmovie.SearchType.TV) },
                    label = { Text("TV Series") }
                )
            }

            HorizontalDivider()

            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                isError = validationError?.titleError != null,
                supportingText = validationError?.titleError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = director,
                onValueChange = viewModel::onDirectorChange,
                label = { Text("Director") },
                isError = validationError?.directorError != null,
                supportingText = validationError?.directorError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = year,
                onValueChange = viewModel::onYearChange,
                label = { Text("Year") },
                isError = validationError?.yearError != null,
                supportingText = validationError?.yearError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = it }
            ) {
                OutlinedTextField(
                    value = format,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Format") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false }
                ) {
                    formats.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { viewModel.onFormatChange(option); formatExpanded = false }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = belongsToSeries,
                    onCheckedChange = viewModel::onBelongsToSeriesChange
                )
                Text("Belongs to a series", style = MaterialTheme.typography.bodyLarge)
            }

            AnimatedVisibility(visible = belongsToSeries) {
                OutlinedTextField(
                    value = seriesName,
                    onValueChange = viewModel::onSeriesNameChange,
                    label = { Text("Series name") },
                    isError = validationError?.seriesNameError != null,
                    supportingText = validationError?.seriesNameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onSaveClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is EditMovieUiState.Loading
            ) {
                Text("Save")
            }
        }
    }
}
