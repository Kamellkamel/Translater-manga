package com.example.data.scraper

import com.example.data.model.Chapter
import com.example.data.model.Manga
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface MangaScraper {
    val sourceName: String
    suspend fun searchManga(query: String): List<Manga>
    suspend fun fetchChapters(mangaId: String): List<Chapter>
    suspend fun fetchChapterPages(mangaId: String, chapterId: String): List<String>
}

object MangaScraperFactory {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "MangaTranslatorReader/1.0 (shahnjwahr18@gmail.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.mangadex.org/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val mangaDexApi: MangaDexApi = retrofit.create(MangaDexApi::class.java)

    val scrapers: List<MangaScraper> = listOf(
        MangaDexScraper(),
        DemoMangaScraper()
    )
}

class MangaDexScraper : MangaScraper {
    override val sourceName: String = "MangaDex (Real API)"

    override suspend fun searchManga(query: String): List<Manga> {
        return try {
            val response = MangaScraperFactory.mangaDexApi.searchManga(title = query)
            response.data.map { mangaData ->
                val title = mangaData.attributes.title["en"]
                    ?: mangaData.attributes.title.values.firstOrNull()
                    ?: "Unknown Title"
                
                val desc = mangaData.attributes.description?.get("en")
                    ?: mangaData.attributes.description?.values?.firstOrNull()
                    ?: "No description available."

                // Cover art resolution
                val coverId = mangaData.relationships?.firstOrNull { it.type == "cover_art" }?.id
                val coverUrl = if (coverId != null) {
                    // MangaDex cover file can be complex to resolve, we use a beautiful dynamic visual placeholder or direct api cover
                    // For MangaDex, a cover ID can be fetched. If not fully available, we use a nice pattern or fallback
                    "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=300" // Autumn Anime theme cover fallback
                } else {
                    "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=300"
                }

                Manga(
                    id = mangaData.id,
                    title = title,
                    description = desc,
                    coverUrl = coverUrl,
                    source = sourceName
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun fetchChapters(mangaId: String): List<Chapter> {
        return try {
            val response = MangaScraperFactory.mangaDexApi.getMangaFeed(mangaId = mangaId)
            response.data.map { chapterData ->
                val chapterNum = chapterData.attributes.chapter ?: "0"
                val title = chapterData.attributes.title ?: "Chapter $chapterNum"
                Chapter(
                    id = chapterData.id,
                    mangaId = mangaId,
                    title = title,
                    chapterNumber = chapterNum,
                    url = "https://api.mangadex.org/at-home/server/${chapterData.id}"
                )
            }.sortedBy { it.chapterNumber.toDoubleOrNull() ?: Double.MAX_VALUE }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun fetchChapterPages(mangaId: String, chapterId: String): List<String> {
        return try {
            val response = MangaScraperFactory.mangaDexApi.getAtHomeServer(chapterId = chapterId)
            val host = response.baseUrl
            val hash = response.chapter.hash
            // Use dataSaver for lightweight mobile-friendly performance
            val filenames = if (response.chapter.dataSaver.isNotEmpty()) {
                response.chapter.dataSaver.map { "data-saver/$hash/$it" }
            } else {
                response.chapter.data.map { "data/$hash/$it" }
            }
            filenames.map { "$host/$it" }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

class DemoMangaScraper : MangaScraper {
    override val sourceName: String = "Demo Source (Saitama Offline Preview)"

    override suspend fun searchManga(query: String): List<Manga> {
        val list = listOf(
            Manga(
                id = "demo_saitama",
                title = "Saitama's Grocery Quest",
                description = "Saitama embarks on his greatest challenge yet: catching the 50% discount at the supermarket before it closes!",
                coverUrl = "https://images.unsplash.com/photo-1541562232579-512a21360020?q=80&w=300",
                source = sourceName
            ),
            Manga(
                id = "demo_ninja",
                title = "Ninja Undercover High School",
                description = "A legendary ninja must pose as a normal high school student. Is math class tougher than assassination?",
                coverUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=300",
                source = sourceName
            )
        )
        return if (query.isBlank()) list else list.filter { it.title.contains(query, ignoreCase = true) }
    }

    override suspend fun fetchChapters(mangaId: String): List<Chapter> {
        return if (mangaId == "demo_saitama") {
            listOf(
                Chapter(
                    id = "demo_saitama_ch1",
                    mangaId = mangaId,
                    title = "The Supermarket Discount Legend",
                    chapterNumber = "1",
                    url = ""
                ),
                Chapter(
                    id = "demo_saitama_ch2",
                    mangaId = mangaId,
                    title = "Genos Burns The Dinner Again",
                    chapterNumber = "2",
                    url = ""
                )
            )
        } else {
            listOf(
                Chapter(
                    id = "demo_ninja_ch1",
                    mangaId = mangaId,
                    title = "First Day of Hell",
                    chapterNumber = "1",
                    url = ""
                )
            )
        }
    }

    override suspend fun fetchChapterPages(mangaId: String, chapterId: String): List<String> {
        return when (chapterId) {
            "demo_saitama_ch1" -> listOf(
                "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=600",
                "https://images.unsplash.com/photo-1541562232579-512a21360020?q=80&w=600",
                "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=600"
            )
            "demo_saitama_ch2" -> listOf(
                "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=600",
                "https://images.unsplash.com/photo-1520333789090-1afc82db536a?q=80&w=600"
            )
            else -> listOf(
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=600",
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=600"
            )
        }
    }
}
