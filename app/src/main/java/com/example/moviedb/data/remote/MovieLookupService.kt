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
    val year: String
)

data class TmdbSearchResult(
    val id: Int,
    val title: String,
    val year: String
)

class MovieLookupService {

    private val client = OkHttpClient()

    suspend fun searchByTitle(title: String): List<TmdbSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val url = "https://api.themoviedb.org/3/search/movie?query=$encoded&api_key=${BuildConfig.TMDB_API_KEY}"
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
                        TmdbSearchResult(id, t, year)
                    } ?: emptyList()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchMovieById(tmdbId: Int): MovieLookupResult? = withContext(Dispatchers.IO) {
        val url = "https://api.themoviedb.org/3/movie/$tmdbId?append_to_response=credits&api_key=${BuildConfig.TMDB_API_KEY}"
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
                MovieLookupResult(title, director, year)
            }
        }.getOrNull()
    }
}
