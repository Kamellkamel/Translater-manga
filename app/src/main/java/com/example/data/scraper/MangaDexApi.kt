package com.example.data.scraper

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class MangaDexSearchResponse(
    val data: List<MangaDexMangaData>
)

@JsonClass(generateAdapter = true)
data class MangaDexMangaData(
    val id: String,
    val attributes: MangaDexMangaAttributes,
    val relationships: List<MangaDexRelationship>?
)

@JsonClass(generateAdapter = true)
data class MangaDexMangaAttributes(
    val title: Map<String, String>,
    val description: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class MangaDexRelationship(
    val id: String,
    val type: String
)

@JsonClass(generateAdapter = true)
data class MangaDexFeedResponse(
    val data: List<MangaDexChapterData>
)

@JsonClass(generateAdapter = true)
data class MangaDexChapterData(
    val id: String,
    val attributes: MangaDexChapterAttributes
)

@JsonClass(generateAdapter = true)
data class MangaDexChapterAttributes(
    val title: String?,
    val chapter: String?,
    val pages: Int
)

@JsonClass(generateAdapter = true)
data class MangaDexAtHomeResponse(
    val baseUrl: String,
    val chapter: MangaDexAtHomeChapter
)

@JsonClass(generateAdapter = true)
data class MangaDexAtHomeChapter(
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>
)

interface MangaDexApi {
    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 10,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaDexSearchResponse

    @GET("manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path("id") mangaId: String,
        @Query("translatedLanguage[]") translatedLanguage: List<String> = listOf("en"),
        @Query("limit") limit: Int = 100,
        @Query("order[chapter]") order: String = "asc"
    ): MangaDexFeedResponse

    @GET("at-home/server/{chapterId}")
    suspend fun getAtHomeServer(
        @Path("chapterId") chapterId: String
    ): MangaDexAtHomeResponse
}
