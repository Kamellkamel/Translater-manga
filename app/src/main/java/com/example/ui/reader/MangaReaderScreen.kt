package com.example.ui.reader

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CachedPage
import com.example.data.model.Chapter
import com.example.data.model.Manga
import com.example.ui.MangaViewModel
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderScreen(
    viewModel: MangaViewModel,
    modifier: Modifier = Modifier
) {
    val selectedManga by viewModel.selectedManga.collectAsState()
    val selectedChapter by viewModel.selectedChapter.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MANGA TRANS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = MangaGold
                            )
                        )
                        Text(
                            text = "قارئ ومترجم المانجا",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MangaOrange,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (selectedManga != null) {
                        IconButton(
                            onClick = {
                                if (selectedChapter != null) {
                                    viewModel.deselectChapter()
                                } else {
                                    viewModel.deselectManga()
                                }
                            },
                            modifier = Modifier.testTag("back_button_nav")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MangaGold
                            )
                        }
                    }
                },
                actions = {
                    if (selectedChapter != null) {
                        IconButton(
                            onClick = { viewModel.forceRefreshChapter(selectedChapter!!) },
                            modifier = Modifier.testTag("refresh_chapter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Force Re-scrape Pages",
                                tint = MangaGold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalDark,
                    titleContentColor = MangaGold
                )
            )
        },
        containerColor = BackgroundMain
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content based on routing state
            AnimatedContent(
                targetState = Triple(selectedManga, selectedChapter, isLoading),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "NavigationTransition"
            ) { stateTriple ->
                val (manga, chapter, loading) = stateTriple

                when {
                    manga == null -> {
                        MangaSearchLayout(viewModel = viewModel)
                    }
                    chapter == null -> {
                        MangaChapterLayout(manga = manga, viewModel = viewModel)
                    }
                    else -> {
                        MangaVerticalReaderLayout(
                            manga = manga,
                            chapter = chapter,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Global Loading Indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MangaGold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Fetching Manga Data...",
                                color = WarmSand,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Global Error Banner
            error?.let { err ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = err,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaSearchLayout(
    viewModel: MangaViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val currentScraper by viewModel.currentScraper.collectAsState()
    val mangas by viewModel.mangas.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scraper Selection Segmented Row
        Text(
            text = "Select Chapter Data Source:",
            color = TextCream,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.scrapers.forEach { scraper ->
                val isSelected = scraper.sourceName == currentScraper.sourceName
                val bgColor = if (isSelected) MangaGold else SlateCard
                val textColor = if (isSelected) CharcoalDark else WarmSand

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable { viewModel.setScraper(scraper) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("scraper_select_${scraper.sourceName.split(" ")[0]}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scraper.sourceName,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Search Input Fields
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search manga / manhwa...", color = TextCream.copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MangaGold,
                    unfocusedBorderColor = SlateCard,
                    focusedContainerColor = SlateCard,
                    unfocusedContainerColor = SlateCard,
                    focusedTextColor = WarmSand,
                    unfocusedTextColor = WarmSand
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("manga_search_input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.searchManga(searchQuery)
                    keyboardController?.hide()
                }),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MangaOrange
                    )
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.searchManga(searchQuery)
                    keyboardController?.hide()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MangaOrange),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("manga_search_submit_button")
            ) {
                Text("Search", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Search Results List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mangas.size) { index ->
                val manga = mangas[index]
                MangaResultCard(manga = manga, onClick = { viewModel.selectManga(manga) })
            }
        }
    }
}

@Composable
fun MangaResultCard(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("manga_card_${manga.id}"),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image with nice fallback
            AsyncImage(
                model = manga.coverUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CharcoalDark)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = manga.title,
                    color = MangaGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = manga.description,
                    color = WarmSand.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(MangaOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = manga.source,
                        color = MangaOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MangaChapterLayout(
    manga: Manga,
    viewModel: MangaViewModel,
    modifier: Modifier = Modifier
) {
    val chapters by viewModel.chapters.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = { viewModel.deselectManga() },
                modifier = Modifier.background(SlateCard, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MangaGold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Back to Results",
                color = WarmSand,
                fontWeight = FontWeight.Medium
            )
        }

        // Manga Header Info
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 100.dp, height = 140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalDark)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = manga.title,
                        color = MangaGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = manga.description,
                        color = WarmSand.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Chapter Selection Header
        Text(
            text = "Available Chapter List:",
            color = MangaGold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No chapters",
                        tint = TextCream.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No English chapters found for this title.\nPlease check online source.",
                        color = TextCream.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Column Grid of Chapters
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(chapters.size) { index ->
                    val chapter = chapters[index]
                    ChapterItemRow(chapter = chapter, onClick = { viewModel.selectChapter(chapter) })
                }
            }
        }
    }
}

@Composable
fun ChapterItemRow(
    chapter: Chapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("chapter_item_${chapter.id}"),
        border = CardDefaults.outlinedCardBorder(true).copy(
            brush = Brush.horizontalGradient(listOf(SlateCard, MangaOrange.copy(alpha = 0.15f)))
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .background(MangaOrange, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Ch ${chapter.chapterNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = chapter.title,
                    color = WarmSand,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Read",
                tint = MangaGold
            )
        }
    }
}

@Composable
fun MangaVerticalReaderLayout(
    manga: Manga,
    chapter: Chapter,
    viewModel: MangaViewModel,
    modifier: Modifier = Modifier
) {
    val activePages by viewModel.activePages.collectAsState()
    val autoTranslate by viewModel.autoTranslate.collectAsState()
    val translatingMap by viewModel.translatingMap.collectAsState()
    val listState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Quick Settings bar
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ch ${chapter.chapterNumber}",
                        color = MangaGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = manga.title,
                        color = WarmSand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }

                // Auto-translate toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.toggleAutoTranslate() }
                        .padding(8.dp)
                        .testTag("auto_translate_toggle")
                ) {
                    Text(
                        text = "Auto-Translate",
                        color = if (autoTranslate) MangaGold else TextCream.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = autoTranslate,
                        onCheckedChange = { viewModel.toggleAutoTranslate() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CharcoalDark,
                            checkedTrackColor = MangaGold,
                            uncheckedThumbColor = WarmSand,
                            uncheckedTrackColor = CharcoalDark
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }

        // Warning if Secret Key is empty
        val context = LocalContext.current
        val hasSecret = remember {
            try {
                val clazz = Class.forName("com.example.BuildConfig")
                val field = clazz.getField("GEMINI_API_KEY")
                val value = field.get(null) as? String ?: ""
                value != "MY_GEMINI_API_KEY" && value.isNotBlank()
            } catch (e: Exception) {
                false
            }
        }

        if (!hasSecret) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MangaOrange.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder(true).copy(
                    brush = Brush.horizontalGradient(listOf(MangaOrange, RedAccent))
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "No API Key",
                        tint = MangaOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sandbox Environment Warning",
                            color = MangaGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "GEMINI_API_KEY is not configured yet. Offline demo translation will display, but live Translation requests will fail. Configure your key via the SECRETS panel in AI Studio.",
                            color = WarmSand.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Continuous Vertical Scrolling Reader list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            itemsIndexed(activePages) { index, page ->
                val isTranslating = translatingMap[page.id] ?: false

                MangaPageReaderItem(
                    page = page,
                    index = index,
                    totalPages = activePages.size,
                    isTranslating = isTranslating,
                    onTranslateClick = { viewModel.translatePage(page) }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MangaPageReaderItem(
    page: CachedPage,
    index: Int,
    totalPages: Int,
    isTranslating: Boolean,
    onTranslateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOcrDebug by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundMain)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page tracker indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(SlateCard, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Page ${index + 1} / $totalPages",
                    color = TextCream,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // OCR Debug toggle button
            if (page.ocrText != null) {
                Text(
                    text = if (showOcrDebug) "Hide OCR text" else "View OCR Text",
                    color = MangaOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showOcrDebug = !showOcrDebug }
                        .padding(4.dp)
                )
            }
        }

        // Live Graphic Image Canvas
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = page.imageUrl,
                    contentDescription = "Manga Page ${index + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )

                if (isTranslating) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MangaGold, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Processing Translation...",
                                color = MangaGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Translation Box BELOW image
        Spacer(modifier = Modifier.height(12.dp))

        // OCR Extracted Debug panel
        AnimatedVisibility(
            visible = showOcrDebug && page.ocrText != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalDark.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Extracted OCR Text (English):",
                        color = MangaGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = page.ocrText ?: "None",
                        color = WarmSand,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Main translation container
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (page.translationArabic != null && page.translationArabic!!.isNotBlank()) {
                    SlateCard
                } else {
                    SlateCard.copy(alpha = 0.4f)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = if (page.translationArabic != null) {
                CardDefaults.outlinedCardBorder(true).copy(
                    brush = Brush.horizontalGradient(listOf(SlateCard, MangaGold.copy(alpha = 0.2f)))
                )
            } else null
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (page.translationArabic != null) {
                    if (page.translationArabic!!.isNotBlank()) {
                        // Display Arabic output in standard right-to-left
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الترجمة العربية (عربي):",
                                    color = MangaGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Translated to Arabic",
                                    tint = MangaOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = page.translationArabic!!,
                                color = TextCream,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // Translation found nothing or empty
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No dialogue text detected",
                                tint = WarmSand.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No dialougue text detected on page to translate.",
                                color = WarmSand.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Manual translation trigger or pending state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Arabic translation is ready to be fetched",
                            color = WarmSand.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = onTranslateClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MangaGold),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("manual_translate_page_${index}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Translate",
                                    tint = CharcoalDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Translate into Arabic",
                                    color = CharcoalDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
