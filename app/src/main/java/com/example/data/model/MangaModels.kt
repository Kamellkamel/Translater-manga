package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Manga(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val source: String
)

data class Chapter(
    val id: String,
    val mangaId: String,
    val title: String,
    val chapterNumber: String,
    val url: String
)

@Entity(tableName = "cached_pages")
data class CachedPage(
    @PrimaryKey val id: String, // mangaId_chapterId_pageIndex
    val mangaId: String,
    val chapterId: String,
    val pageIndex: Int,
    val imageUrl: String,
    var ocrText: String? = null,
    var translationArabic: String? = null
)
