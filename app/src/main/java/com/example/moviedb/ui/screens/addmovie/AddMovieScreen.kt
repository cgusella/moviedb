package com.example.moviedb.ui.screens.addmovie

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.moviedb.di.AppModule

private val PosterShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieScreen() {
    val context = LocalContext.current
    val repository = AppModule.provideRepository(context)
    val viewModel: AddMovieViewModel = viewModel(factory = AddMovieViewModel.factory(repository))

    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is AddMovieUiState.Success) {
            val dest = (uiState as AddMovieUiState.Success).destination
            val label = if (dest == Destination.COLLECTION) "collection" else "wishlist"
            viewModel.resetForm()
            snackbarHostState.showSnackbar("Movie saved to $label!")
            viewModel.clearUiState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Film") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.height(48.dp),
                windowInsets = WindowInsets(top = 10.dp),
                actions = {
                    if (currentStep > 0) {
                        IconButton(onClick = viewModel::resetForm) {
                            Icon(Icons.Default.Close, contentDescription = "Reset")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Step indicator
            StepIndicator(currentStep = currentStep)

            HorizontalDivider()

            AnimatedContent(targetState = currentStep, label = "step") { step ->
                when (step) {
                    0 -> SearchStep(viewModel)
                    1 -> DetailsStep(viewModel)
                    2 -> ConfirmStep(viewModel, uiState)
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    val steps = listOf("Search", "Details", "Confirm")
    Row(modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, label ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (currentStep == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (currentStep == index) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            if (currentStep == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun SearchStep(viewModel: AddMovieViewModel) {
    val titleSearchQuery by viewModel.titleSearchQuery.collectAsStateWithLifecycle()
    val titleSearchState by viewModel.titleSearchState.collectAsStateWithLifecycle()
    val searchType by viewModel.searchType.collectAsStateWithLifecycle()
    val isLoadingDetails by viewModel.isLoadingDetails.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    if (isLoadingDetails) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = titleSearchQuery,
                onValueChange = viewModel::onTitleSearchQueryChange,
                placeholder = { Text("Search for a film, director...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus(); viewModel.onTitleSearch()
                }),
                trailingIcon = {
                    if (titleSearchState is TitleSearchState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { focusManager.clearFocus(); viewModel.onTitleSearch() }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        }

        // Film/TV toggle
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = searchType == SearchType.MOVIE, onClick = { viewModel.onSearchTypeChange(SearchType.MOVIE) }, label = { Text("Film") })
            FilterChip(selected = searchType == SearchType.TV, onClick = { viewModel.onSearchTypeChange(SearchType.TV) }, label = { Text("TV Series") })
        }

        Spacer(Modifier.height(8.dp))

        when (val state = titleSearchState) {
            is TitleSearchState.Results -> {
                Text("Search Results", style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.items, key = { it.id }) { movie ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onSearchResultViewDetails(movie.id, movie.type) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(width = 48.dp, height = 68.dp).clip(PosterShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (movie.posterUrl != null) {
                                    AsyncImage(model = movie.posterUrl, contentDescription = null,
                                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.Movie, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(movie.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                                Text("${movie.year} · ${movie.type}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.onSearchResultViewDetails(movie.id, movie.type) }) {
                                Icon(Icons.Default.Add, contentDescription = "Select",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    item {
                        AddManuallyRow(onClick = { viewModel.goToStep(1) })
                    }
                }
            }
            is TitleSearchState.Error -> {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.weight(1f))
                AddManuallyRow(onClick = { viewModel.goToStep(1) })
            }
            else -> {
                Spacer(Modifier.weight(1f))
                AddManuallyRow(onClick = { viewModel.goToStep(1) })
            }
        }
    }
}

@Composable
private fun AddManuallyRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Can't find the film?", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onClick) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add Manually")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsStep(viewModel: AddMovieViewModel) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val director by viewModel.director.collectAsStateWithLifecycle()
    val year by viewModel.year.collectAsStateWithLifecycle()
    val format by viewModel.format.collectAsStateWithLifecycle()
    val belongsToSeries by viewModel.belongsToSeries.collectAsStateWithLifecycle()
    val seriesName by viewModel.seriesName.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val validationError = uiState as? AddMovieUiState.ValidationError
    var formatExpanded by remember { mutableStateOf(false) }
    val formats = listOf("DVD", "Blu-ray", "4K", "Video tape")

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(value = title, onValueChange = viewModel::onTitleChange, label = { Text("Title") },
            isError = validationError?.titleError != null,
            supportingText = validationError?.titleError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(), singleLine = true)

        OutlinedTextField(value = director, onValueChange = viewModel::onDirectorChange, label = { Text("Director / Creator") },
            isError = validationError?.directorError != null,
            supportingText = validationError?.directorError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(), singleLine = true)

        OutlinedTextField(value = year, onValueChange = viewModel::onYearChange, label = { Text("Year") },
            isError = validationError?.yearError != null,
            supportingText = validationError?.yearError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        ExposedDropdownMenuBox(expanded = formatExpanded, onExpandedChange = { formatExpanded = it }) {
            OutlinedTextField(value = format, onValueChange = {}, readOnly = true, label = { Text("Format") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                formats.forEach { option ->
                    DropdownMenuItem(text = { Text(option) },
                        onClick = { viewModel.onFormatChange(option); formatExpanded = false })
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = belongsToSeries, onCheckedChange = viewModel::onBelongsToSeriesChange)
            Text("Belongs to a series", style = MaterialTheme.typography.bodyLarge)
        }

        if (belongsToSeries) {
            OutlinedTextField(value = seriesName, onValueChange = viewModel::onSeriesNameChange,
                label = { Text("Series name") },
                isError = validationError?.seriesNameError != null,
                supportingText = validationError?.seriesNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { focusManager.clearFocus(); viewModel.goToStep(2) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Next →") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmStep(viewModel: AddMovieViewModel, uiState: AddMovieUiState) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val director by viewModel.director.collectAsStateWithLifecycle()
    val year by viewModel.year.collectAsStateWithLifecycle()
    val format by viewModel.format.collectAsStateWithLifecycle()
    val posterUrl by viewModel.posterUrl.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val overview by viewModel.overview.collectAsStateWithLifecycle()

    if (uiState is AddMovieUiState.DuplicateWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.resetForm() },
            title = { Text("Already in collection?") },
            text = { Text("\"$title ($year)\" appears to already be in your collection. Add it anyway?") },
            confirmButton = { TextButton(onClick = viewModel::onConfirmDuplicate) { Text("Add anyway") } },
            dismissButton = { TextButton(onClick = viewModel::resetForm) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Summary card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.width(80.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (posterUrl != null) {
                        AsyncImage(model = posterUrl, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Default.Movie, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title.ifBlank { "No title" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (year.isNotBlank()) Text(year, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (director.isNotBlank()) Text("Director: $director", style = MaterialTheme.typography.bodySmall)
                    Text("Format: $format", style = MaterialTheme.typography.bodySmall)
                    val genresVal = genres
                    if (!genresVal.isNullOrBlank()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
                            genresVal.split(",").forEach { genre ->
                                val g = genre.trim()
                                if (g.isNotBlank()) SuggestionChip(onClick = {}, label = { Text(g, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                    val overviewVal = overview
                    if (!overviewVal.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(overviewVal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.onSaveClick(Destination.COLLECTION) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is AddMovieUiState.Loading
        ) { Text("Save to Collection") }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.onSaveClick(Destination.WISHLIST) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is AddMovieUiState.Loading
        ) { Text("Save to Wishlist") }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { viewModel.goToStep(1) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("← Edit Details") }
    }
}
