package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.CachedPage
import com.example.data.model.Chapter
import com.example.data.model.Manga
import com.example.data.scraper.MangaScraper
import com.example.data.scraper.MangaScraperFactory
import com.example.data.ocr.OcrManager
import com.example.data.translation.GeminiTranslationClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MangaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.mangaDao()
    private val ocrManager = OcrManager(application)
    private val translationClient = GeminiTranslationClient()

    // Scrapers
    val scrapers: List<MangaScraper> = MangaScraperFactory.scrapers
    private val _currentScraper = MutableStateFlow<MangaScraper>(scrapers.first())
    val currentScraper: StateFlow<MangaScraper> = _currentScraper.asStateFlow()

    // Search & Manga States
    private val _mangas = MutableStateFlow<List<Manga>>(emptyList())
    val mangas: StateFlow<List<Manga>> = _mangas.asStateFlow()

    private val _selectedManga = MutableStateFlow<Manga?>(null)
    val selectedManga: StateFlow<Manga?> = _selectedManga.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _selectedChapter = MutableStateFlow<Chapter?>(null)
    val selectedChapter: StateFlow<Chapter?> = _selectedChapter.asStateFlow()

    // Reader UI State
    private val _activePages = MutableStateFlow<List<CachedPage>>(emptyList())
    val activePages: StateFlow<List<CachedPage>> = _activePages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _translatingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val translatingMap: StateFlow<Map<String, Boolean>> = _translatingMap.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Settings
    private val _autoTranslate = MutableStateFlow(true) // Automatically detect and translate when page is loaded
    val autoTranslate: StateFlow<Boolean> = _autoTranslate.asStateFlow()

    init {
        // Initial search for default search list
        searchManga("Saitama")
    }

    fun setScraper(scraper: MangaScraper) {
        _currentScraper.value = scraper
        _mangas.value = emptyList()
        _chapters.value = emptyList()
        _selectedManga.value = null
        _selectedChapter.value = null
        _activePages.value = emptyList()
        searchManga("")
    }

    fun toggleAutoTranslate() {
        _autoTranslate.value = !_autoTranslate.value
    }

    fun searchManga(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val results = withContext(Dispatchers.IO) {
                    _currentScraper.value.searchManga(query)
                }
                _mangas.value = results
                if (results.isEmpty()) {
                    _error.value = "No manga found for \"$query\""
                }
            } catch (e: Exception) {
                _error.value = "Failed to search manga: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectManga(manga: Manga) {
        _selectedManga.value = manga
        _selectedChapter.value = null
        _activePages.value = emptyList()
        fetchChapters(manga.id)
    }

    fun deselectManga() {
        _selectedManga.value = null
        _selectedChapter.value = null
        _chapters.value = emptyList()
        _activePages.value = emptyList()
    }

    fun fetchChapters(mangaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val results = withContext(Dispatchers.IO) {
                    _currentScraper.value.fetchChapters(mangaId)
                }
                _chapters.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load chapters: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectChapter(chapter: Chapter) {
        _selectedChapter.value = chapter
        _activePages.value = emptyList()
        _translatingMap.value = emptyMap()
        loadChapterPages(chapter)
    }

    fun deselectChapter() {
        _selectedChapter.value = null
        _activePages.value = emptyList()
        _translatingMap.value = emptyMap()
    }

    private fun loadChapterPages(chapter: Chapter) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Check if we already have these pages cached in the database
                val cached = withContext(Dispatchers.IO) {
                    dao.getPagesForChapterSync(chapter.mangaId, chapter.id)
                }

                if (cached.isNotEmpty()) {
                    _activePages.value = cached
                    _isLoading.value = false
                    
                    // Trigger auto translate on load if enabled
                    if (_autoTranslate.value) {
                        cached.forEach { page ->
                            if (page.translationArabic == null) {
                                translatePage(page)
                            }
                        }
                    }
                    return@launch
                }

                // If not cached, scrape page URLs
                val pageUrls = withContext(Dispatchers.IO) {
                    _currentScraper.value.fetchChapterPages(chapter.mangaId, chapter.id)
                }

                if (pageUrls.isEmpty()) {
                    _error.value = "No pages found for this chapter."
                    return@launch
                }

                val newPages = pageUrls.mapIndexed { index, url ->
                    CachedPage(
                        id = "${chapter.mangaId}_${chapter.id}_$index",
                        mangaId = chapter.mangaId,
                        chapterId = chapter.id,
                        pageIndex = index,
                        imageUrl = url,
                        ocrText = null,
                        translationArabic = null
                    )
                }

                // Cache them in local database
                withContext(Dispatchers.IO) {
                    dao.insertPages(newPages)
                }

                // Observe from database or set directly to trigger flow
                _activePages.value = newPages

                // If auto-translate is on, translate the pages
                if (_autoTranslate.value) {
                    newPages.forEach { page ->
                        translatePage(page)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to select chapter pages: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun translatePage(page: CachedPage) {
        // Prevent duplicate concurrent translations
        if (_translatingMap.value[page.id] == true) return

        viewModelScope.launch {
            _translatingMap.value = _translatingMap.value + (page.id to true)
            try {
                // Step 1: Handle OCR
                var textToTranslate = page.ocrText

                if (textToTranslate == null) {
                    // Check if it's a demo page with pre-cooked English texts
                    val demoText = ocrManager.getDemoTextForPage(page.imageUrl)
                    if (demoText != null) {
                        textToTranslate = demoText
                    } else {
                        // Standard cloud image OCR
                        val bitmap = ocrManager.loadBitmapFromUrl(page.imageUrl)
                        if (bitmap != null) {
                            textToTranslate = ocrManager.recognizeText(bitmap)
                        }
                    }
                }

                // If no text was found, set a small notice
                val finalOcrText = if (textToTranslate.isNullOrBlank()) "No text found on page." else textToTranslate
                page.ocrText = finalOcrText

                // Step 2: Translate via Gemini
                val translated = if (!finalOcrText.contains("No text found")) {
                    withContext(Dispatchers.IO) {
                        translationClient.translateToArabic(finalOcrText)
                    }
                } else {
                    ""
                }

                page.translationArabic = translated

                // Step 3: Update local database cache
                withContext(Dispatchers.IO) {
                    dao.updatePage(page)
                }

                // Step 4: Refresh active pages stream in UI
                _activePages.value = _activePages.value.map {
                    if (it.id == page.id) page.copy() else it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _translatingMap.value = _translatingMap.value + (page.id to false)
            }
        }
    }

    fun forceRefreshChapter(chapter: Chapter) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    dao.clearPages(chapter.mangaId, chapter.id)
                }
                loadChapterPages(chapter)
            } catch (e: Exception) {
                _error.value = "Failed to refresh chapter: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
