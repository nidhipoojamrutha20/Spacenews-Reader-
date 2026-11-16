package com.example.spacenews.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class ApiPage<T>(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T>? = emptyList()
)

data class SpaceflightNewsDto(
    val id: Int? = null,
    val title: String? = null,
    val summary: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("news_site") val newsSite: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    val url: String? = null
)

interface SpaceflightNewsApi {
    @GET("articles/")
    suspend fun getArticles(
        @Query("limit") limit: Int = 20,
        @Query("ordering") ordering: String = "-published_at",
        @Query("search") search: String? = null
    ): ApiPage<SpaceflightNewsDto>

    /** The API's `next` response link contains the next limit/offset page and current filters. */
    @GET
    suspend fun getNextPage(@Url nextUrl: String): ApiPage<SpaceflightNewsDto>

    @GET("articles/{id}/")
    suspend fun getArticle(@Path("id") id: Int): SpaceflightNewsDto
}

object SpaceflightNewsApiFactory {
    private const val BASE_URL = "https://api.spaceflightnewsapi.net/v4/"

    fun create(): SpaceflightNewsApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpaceflightNewsApi::class.java)
    }
}
