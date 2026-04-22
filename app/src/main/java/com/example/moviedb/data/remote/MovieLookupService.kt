package com.example.moviedb.data.remote

import com.example.moviedb.BuildConfig
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

data class MovieLookupResult(
    val title: String,
    val director: String,
    val year: String,
    val type: String = "Movie",
    val posterUrl: String? = null,
    val durationMinutes: Int? = null,
    val genres: List<String> = emptyList()
)

data class TmdbSearchResult(
    val id: Int,
    val title: String,
    val year: String,
    val type: String = "Movie",
    val posterUrl: String? = null
)

class MovieLookupService {

    private val client = OkHttpClient()

    suspend fun searchByTitle(title: String, language: String = "it-IT"): List<TmdbSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val url = "https://api.themoviedb.org/3/search/movie?query=$encoded&language=$language&api_key=${BuildConfig.TMDB_API_KEY}"
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val body = response.body?.string() ?: return@withContext emptyList()
                JsonParser.parseString(body).asJsonObject
                    .getAsJsonArray("results")
                    ?.take(8)
                    ?.mapNotNull { el ->
                        val obj = el.asJsonObject
                        val id = obj.get("id")?.asInt ?: return@mapNotNull null
                        val t = obj.get("title")?.asString ?: return@mapNotNull null
                        val year = obj.get("release_date")?.asString?.take(4) ?: ""
                        val posterUrl = obj.get("poster_path")?.takeIf { !it.isJsonNull }?.asString
                            ?.let { "https://image.tmdb.org/t/p/w92$it" }
                        TmdbSearchResult(id, t, year, "Movie", posterUrl)
                    } ?: emptyList()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun searchTvByTitle(title: String, language: String = "it-IT"): List<TmdbSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val url = "https://api.themoviedb.org/3/search/tv?query=$encoded&language=$language&api_key=${BuildConfig.TMDB_API_KEY}"
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val body = response.body?.string() ?: return@withContext emptyList()
                JsonParser.parseString(body).asJsonObject
                    .getAsJsonArray("results")
                    ?.take(8)
                    ?.mapNotNull { el ->
                        val obj = el.asJsonObject
                        val id = obj.get("id")?.asInt ?: return@mapNotNull null
                        val t = obj.get("name")?.asString ?: return@mapNotNull null
                        val year = obj.get("first_air_date")?.asString?.take(4) ?: ""
                        val posterUrl = obj.get("poster_path")?.takeIf { !it.isJsonNull }?.asString
                            ?.let { "https://image.tmdb.org/t/p/w92$it" }
                        TmdbSearchResult(id, t, year, "TV Series", posterUrl)
                    } ?: emptyList()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchMovieById(tmdbId: Int, language: String = "it-IT"): MovieLookupResult? = withContext(Dispatchers.IO) {
        val url = "https://api.themoviedb.org/3/movie/$tmdbId?append_to_response=credits&language=$language&api_key=${BuildConfig.TMDB_API_KEY}"
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val body = response.body?.string() ?: return@withContext null
                val json = JsonParser.parseString(body).asJsonObject
                val title = json.get("title")?.asString ?: return@withContext null
                val year = json.get("release_date")?.asString?.take(4) ?: ""
                val director = json.getAsJsonObject("credits")
                    ?.getAsJsonArray("crew")
                    ?.firstOrNull { it.asJsonObject.get("job")?.asString == "Director" }
                    ?.asJsonObject?.get("name")?.asString ?: ""
                val posterUrl = json.get("poster_path")?.takeIf { !it.isJsonNull }?.asString
                    ?.let { "https://image.tmdb.org/t/p/w185$it" }
                val durationMinutes = json.get("runtime")?.takeIf { !it.isJsonNull }?.asInt
                    ?.takeIf { it > 0 }
                val genres = json.getAsJsonArray("genres")
                    ?.mapNotNull { it.asJsonObject.get("name")?.asString } ?: emptyList()
                MovieLookupResult(title, director, year, "Movie", posterUrl, durationMinutes, genres)
            }
        }.getOrNull()
    }

    suspend fun fetchTvById(tmdbId: Int, language: String = "it-IT"): MovieLookupResult? = withContext(Dispatchers.IO) {
        val url = "https://api.themoviedb.org/3/tv/$tmdbId?append_to_response=credits&language=$language&api_key=${BuildConfig.TMDB_API_KEY}"
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val body = response.body?.string() ?: return@withContext null
                val json = JsonParser.parseString(body).asJsonObject
                val title = json.get("name")?.asString ?: return@withContext null
                val year = json.get("first_air_date")?.asString?.take(4) ?: ""
                val creator = json.getAsJsonArray("created_by")
                    ?.firstOrNull()?.asJsonObject?.get("name")?.asString ?: ""
                val posterUrl = json.get("poster_path")?.takeIf { !it.isJsonNull }?.asString
                    ?.let { "https://image.tmdb.org/t/p/w185$it" }
                val durationMinutes = json.getAsJsonArray("episode_run_time")
                    ?.firstOrNull()?.asInt?.takeIf { it > 0 }
                val genres = json.getAsJsonArray("genres")
                    ?.mapNotNull { it.asJsonObject.get("name")?.asString } ?: emptyList()
                MovieLookupResult(title, creator, year, "TV Series", posterUrl, durationMinutes, genres)
            }
        }.getOrNull()
    }
}
