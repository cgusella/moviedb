package com.example.moviedb.ui.screens.editmovie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    val type by viewModel.type.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val durationText by viewModel.durationText.collectAsStateWithLifecycle()
    val posterUrl by viewModel.posterUrl.collectAsStateWithLifecycle()

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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == "Movie",
                    onClick = { viewModel.onTypeChange("Movie") },
                    label = { Text("Film") }
                )
                FilterChip(
                    selected = type == "TV Series",
                    onClick = { viewModel.onTypeChange("TV Series") },
                    label = { Text("TV Series") }
                )
            }

            OutlinedTextField(
                value = genres,
                onValueChange = viewModel::onGenresChange,
                label = { Text("Genres (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = durationText,
                onValueChange = viewModel::onDurationChange,
                label = { Text("Duration (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = posterUrl,
                onValueChange = viewModel::onPosterUrlChange,
                label = { Text("Poster URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
