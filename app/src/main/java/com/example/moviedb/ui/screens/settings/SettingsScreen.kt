package com.example.moviedb.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviedb.data.backup.BackupManager
import com.example.moviedb.di.AppModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = AppModule.provideRepository(context)
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repository))

    val languageCode by viewModel.languageCode.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val sortOwned by viewModel.sortOwned.collectAsStateWithLifecycle()
    val collectionView by viewModel.collectionView.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showDefaultViewDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { BackupManager.export(context, uri) }
            snackbarHostState.showSnackbar(
                if (result.isSuccess) "Backup saved successfully." else "Export failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    val mergeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { BackupManager.import(context, uri) }
            if (result.isSuccess) {
                withContext(Dispatchers.Main) {
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            } else {
                snackbarHostState.showSnackbar("Restore failed. Make sure you selected a valid backup file.")
            }
        }
    }

    if (showLanguageDialog) {
        RadioPickerDialog(
            title = "Language",
            options = listOf("it-IT" to "Italiano", "en-US" to "English"),
            selected = languageCode,
            onSelect = { viewModel.setLanguage(it); showLanguageDialog = false },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showAppearanceDialog) {
        RadioPickerDialog(
            title = "Appearance",
            options = listOf("system" to "System default", "dark" to "Dark", "light" to "Light"),
            selected = appearance,
            onSelect = { viewModel.setAppearance(it); showAppearanceDialog = false },
            onDismiss = { showAppearanceDialog = false }
        )
    }

    if (showDefaultViewDialog) {
        RadioPickerDialog(
            title = "Default View",
            options = listOf("grid" to "Grid", "list" to "List"),
            selected = collectionView,
            onSelect = { viewModel.setDefaultView(it); showDefaultViewDialog = false },
            onDismiss = { showDefaultViewDialog = false }
        )
    }

    if (showSortDialog) {
        RadioPickerDialog(
            title = "Sort Owned By",
            options = listOf(
                "recently_added" to "Recently Added",
                "title" to "Title",
                "director" to "Director",
                "year" to "Year"
            ),
            selected = sortOwned,
            onSelect = { viewModel.setSortOwned(it); showSortDialog = false },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Local Data") },
            text = { Text("This will permanently delete all movies from your collection and wishlist. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAllData {
                        scope.launch { snackbarHostState.showSnackbar("All data cleared.") }
                    }
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } }
        )
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
                    TextButton(onClick = { showImportDialog = false; restoreLauncher.launch(arrayOf("application/octet-stream", "*/*")) }) { Text("Full Restore") }
                    TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.height(48.dp),
                windowInsets = WindowInsets(top = 10.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            val langLabel = if (languageCode == "it-IT") "Italiano" else "English"
            val appearLabel = when (appearance) { "dark" -> "Dark"; "light" -> "Light"; else -> "System" }
            val viewLabel = if (collectionView == "grid") "Grid" else "List"
            val sortLabel = when (sortOwned) { "title" -> "Title"; "director" -> "Director"; "year" -> "Year"; else -> "Recently Added" }

            SettingsSectionHeader("PREFERENCES")
            SettingsRow(title = "Appearance", value = appearLabel, icon = Icons.Default.Brightness4) { showAppearanceDialog = true }
            SettingsRow(title = "Default View", value = viewLabel, icon = Icons.Default.GridView) { showDefaultViewDialog = true }
            SettingsRow(title = "Language", value = langLabel, icon = Icons.Default.Language) { showLanguageDialog = true }
            SettingsRow(title = "Sort Owned By", value = sortLabel, icon = Icons.Default.Sort) { showSortDialog = true }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader("DATA")
            SettingsRow(title = "Backup / Export Data", icon = Icons.Default.Upload) { exportLauncher.launch("moviedb_backup.db") }
            SettingsRow(title = "Import Data", icon = Icons.Default.Download) { showImportDialog = true }
            SettingsRow(title = "Clear Local Data", icon = Icons.Default.DeleteForever, titleColor = MaterialTheme.colorScheme.error) { showClearConfirm = true }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader("ABOUT")
            SettingsRow(title = "About", icon = Icons.Default.Info) {}
            SettingsRow(title = "Privacy Policy", icon = Icons.Default.PrivacyTip) {}
            SettingsRow(title = "Rate this app", icon = Icons.Default.Star) {}
        }
    }
}

@Composable
private fun RadioPickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(value) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == value, onClick = { onSelect(value) })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    value: String? = null,
    icon: ImageVector,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
