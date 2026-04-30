# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.moviedb.YourTestClass"

# Clean build
./gradlew clean assembleDebug
```

## API Key Setup

The TMDB API key must be present in `local.properties` (not committed):
```
TMDB_API_KEY=your_key_here
```
It is injected into `BuildConfig.TMDB_API_KEY` at build time.

## Architecture

**MVVM** with manual dependency injection — no Hilt/Koin. The `Application` class (`MovieDbApplication`) owns the Room database singleton and vends a fresh `MovieRepository` each call. `AppModule.provideRepository(context)` is the DI entry point used in every screen.

**Data layer**
- `Movie` and `WishlistMovie` are separate Room entities in `MovieDatabase`. Owned films and wishlist films are kept in distinct tables with distinct DAOs (`MovieDao`, `WishlistDao`).
- `MovieRepository` wraps both DAOs and exposes `Flow`-based queries plus suspend functions for mutations. The `promoteToCollection` / (implied) `moveToWishlist` operations atomically delete from one table and insert into the other.
- `MovieLookupService` calls the TMDB REST API (OkHttp + Gson) to search and fetch movie/TV metadata. Language defaults to `it-IT`; callers can override.
- `SettingsRepository` uses DataStore Preferences for the language code setting.
- `BackupManager` handles export/import/merge of the Room database file.

**UI layer**
- Pure Jetpack Compose with Material3. Navigation is handled by `AppNavGraph` using Navigation Compose.
- Bottom nav contains four tabs: **Collection** (`/collection`), **Add Movie** (`/add_movie`), **Wishlist** (`/wishlist`), **Search** (`/search`). **Settings** is a non-tab destination reached from Collection's top bar.
- Each screen creates its ViewModel inline via `viewModel(factory = …)` passing the repository; no shared ViewModel across screens.
- `ui/components/` holds reusable composables (`MoviePosterThumbnail`, `MovieTypeBadge`).

**Theme**
- `MovieDbTheme` in `ui/theme/` wraps Material3 with a custom palette: Amber primary, Indigo secondary, Rose tertiary, Midnight Navy dark surfaces.

**Add-movie flow (multi-step)**
`AddMovieScreen` drives a 3-step flow: Search → Details → Confirm. `AddMovieViewModel` calls `MovieLookupService` for both movies and TV series searches.

**Genres storage**
Genres are stored as a comma-separated string in the `genres` column of both `Movie` and `WishlistMovie` (no separate table).
