package com.example.data.database

import androidx.room.*
import com.example.data.model.CachedPage
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM cached_pages WHERE mangaId = :mangaId AND chapterId = :chapterId ORDER BY pageIndex ASC")
    fun getPagesForChapter(mangaId: String, chapterId: String): Flow<List<CachedPage>>

    @Query("SELECT * FROM cached_pages WHERE mangaId = :mangaId AND chapterId = :chapterId ORDER BY pageIndex ASC")
    suspend fun getPagesForChapterSync(mangaId: String, chapterId: String): List<CachedPage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<CachedPage>)

    @Update
    suspend fun updatePage(page: CachedPage)

    @Query("DELETE FROM cached_pages WHERE mangaId = :mangaId AND chapterId = :chapterId")
    suspend fun clearPages(mangaId: String, chapterId: String)
}
