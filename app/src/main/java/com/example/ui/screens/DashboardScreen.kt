package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.WatchHistoryEntity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.model.MovieItem
import com.example.ui.MovieUiState
import com.example.ui.MovieViewModel

@Composable
fun DashboardScreen(
    viewModel: MovieViewModel,
    onMovieClick: (MovieItem) -> Unit,
    onWatchlistNavigate: () -> Unit,
    onSettingsNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCategory by viewModel.currentCategory.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50
        }
    }
    val topBarBgColor by animateColorAsState(
        targetValue = if (isScrolled) Color(0xF2070708) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "TopBarBg"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070708)) // Pitch Black Cinematic background
    ) {
        // 1. Full-Width Main Content
        Column(modifier = Modifier.fillMaxSize()) {
            // Content Grid / Banner / Lists
            when (val state = uiState) {
                is MovieUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Red)
                    }
                }
                is MovieUiState.Success -> {
                    if (state.items.isEmpty()) {
                        EmptyState(message = "لا توجد نتائج مطابقة لطلبك حالياً.")
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val isCarouselVisible = selectedGenre == null && searchQuery.isBlank() && state.items.isNotEmpty()
                            val topPadding = if (isCarouselVisible) 0.dp else 80.dp

                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(top = topPadding, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // 1. Header: Featured Movie Carousel (only if not searching/filtering)
                                if (isCarouselVisible) {
                                    item {
                                        val featuredCount = minOf(state.items.size, 5)
                                        val featuredMovies = state.items.take(featuredCount)
                                        MovieCarousel(
                                            movies = featuredMovies,
                                            onPlayClick = { movie ->
                                                scope.launch {
                                                    kotlinx.coroutines.delay(180)
                                                    onMovieClick(movie)
                                                }
                                            },
                                            onInfoClick = onMovieClick,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // 2. Horizontal Categories Navigation (M3 Pill style)
                                item {
                                    CategorySelector(
                                        categories = viewModel.categories,
                                        selectedCategory = currentCategory,
                                        onCategorySelect = { viewModel.setCategory(it) }
                                    )
                                }

                                // 3. Continue Watching Row (only show on home page when active progress exists)
                                if (selectedGenre == null && searchQuery.isBlank() && watchHistory.isNotEmpty()) {
                                    item {
                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            ContinueWatchingRow(
                                                historyItems = watchHistory,
                                                onItemClick = { history: com.example.data.local.WatchHistoryEntity ->
                                                    scope.launch {
                                                        kotlinx.coroutines.delay(180)
                                                        
                                                        // Resolve into full movie from fallback or query
                                                        val catalogItem = viewModel.getOfflineCatalog().find { it.tmdbId == history.tmdbId }
                                                        if (catalogItem != null) {
                                                            viewModel.selectMovie(catalogItem)
                                                            viewModel.startPlaying()
                                                        } else {
                                                            // For TMDB items, create virtual movie item and play
                                                            val virtualMovie = MovieItem(
                                                                id = history.id,
                                                                tmdbId = history.tmdbId,
                                                                title = history.title,
                                                                originalTitle = "",
                                                                overview = "متابعة مشاهدة المسلسل المفضل لديك.",
                                                                posterPath = history.posterPath,
                                                                backdropPath = history.posterPath,
                                                                category = history.category,
                                                                genres = listOf("Drama"),
                                                                rating = 8.0,
                                                                releaseDate = "",
                                                                isTvShow = history.isTvShow,
                                                                isArabic = history.category.contains("Arabic")
                                                            )
                                                            viewModel.selectMovie(virtualMovie)
                                                            viewModel.setSeason(history.lastWatchedSeason)
                                                            viewModel.setEpisode(history.lastWatchedEpisode)
                                                            viewModel.startPlaying()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                // 4. Movies Grid List (Chunked)
                                val featuredCount = if (selectedGenre == null && searchQuery.isBlank() && state.items.isNotEmpty()) {
                                    minOf(state.items.size, 5)
                                } else {
                                    0
                                }
                                val gridItems = if (featuredCount > 0) {
                                    state.items.subList(featuredCount, state.items.size)
                                } else {
                                    state.items
                                }

                                val columnsCount = if (isTablet) 4 else 3
                                val chunkedItems = gridItems.chunked(columnsCount)

                                items(chunkedItems.size) { rowIndex ->
                                    val rowItems = chunkedItems[rowIndex]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { movie ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                MovieCard(
                                                    movie = movie,
                                                    onClick = { onMovieClick(movie) }
                                                )
                                            }
                                        }
                                        // Spacer placeholders for partial rows
                                        if (rowItems.size < columnsCount) {
                                            repeat(columnsCount - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is MovieUiState.Error -> {
                    EmptyState(message = "حدث خطأ أثناء تحميل البيانات: ${state.message}")
                }
            }
        }

        // 2. Premium FLOATING Search Bar and Library Nav Header (Overlay)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBgColor)
                .align(Alignment.TopCenter)
                .zIndex(2f)
        ) {
            HeaderBar(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onWatchlistClick = onWatchlistNavigate,
                onSettingsClick = onSettingsNavigate
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onWatchlistClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Modern Search Field taking full remaining width
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("بحث عن فيلم...", fontSize = 12.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(16.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Red,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color(0xFF131315).copy(alpha = 0.85f),
                unfocusedContainerColor = Color(0xFF131315).copy(alpha = 0.85f)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("search_input")
        )

        // Watchlist navigation button
        IconButton(
            onClick = onWatchlistClick,
            modifier = Modifier
                .background(Color(0xFF131315).copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp))
                .size(44.dp)
                .testTag("watchlist_button")
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = "Watchlist",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Settings navigation button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .background(Color(0xFF131315).copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp))
                .size(44.dp)
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CategorySelector(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    val categoryArabicMap = mapOf(
        "Arabic Movies" to "أفلام عربية",
        "Foreign Movies" to "أفلام أجنبية",
        "Arabic Series" to "مسلسلات عربية",
        "Foreign Series" to "مسلسلات أجنبية",
        "Anime" to "أنمي وكرتون"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            val arName = categoryArabicMap[category] ?: category
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color.Red else Color(0xFF161619))
                    .clickable { onCategorySelect(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("category_tab_$category")
            ) {
                Text(
                    text = arName,
                    color = if (isSelected) Color.White else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun GenreSidebar(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelect: (String?) -> Unit,
    onCollapse: () -> Unit
) {
    val genreArabicMap = mapOf(
        "Action" to "أكشن وحركة",
        "Romance" to "رومانسي وعاطفي",
        "History" to "تاريخي ودراما",
        "Comedy" to "كوميدي وضاحك",
        "Sci-Fi" to "خيال علمي وفضاء",
        "Drama" to "دراما واجتماعي",
        "Mystery" to "غموض وتشويق"
    )

    Column(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(Color(0xFF0F0F11))
            .drawBehind {
                // Add right glow border
                drawLine(
                    color = Color.DarkGray,
                    start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الفئات والمود",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            IconButton(onClick = onCollapse, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Collapse", tint = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // All Genres Reset Pill
        val isAllSelected = selectedGenre == null
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAllSelected) Color.Red.copy(alpha = 0.15f) else Color.Transparent)
                .clickable { onGenreSelect(null) }
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "أحدث الإضافات (الكل)",
                color = if (isAllSelected) Color.Red else Color.LightGray,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        genres.forEach { genre ->
            val isSelected = genre == selectedGenre
            val arGenreName = genreArabicMap[genre] ?: genre
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color.Red.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onGenreSelect(genre) }
                    .padding(vertical = 10.dp, horizontal = 12.dp)
                    .testTag("genre_sidebar_$genre")
            ) {
                Text(
                    text = arGenreName,
                    color = if (isSelected) Color.Red else Color.LightGray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun HeroBanner(
    movie: MovieItem,
    onPlayClick: (MovieItem) -> Unit,
    onInfoClick: (MovieItem) -> Unit
) {
    val posterUrl = if (movie.posterPath.startsWith("/")) {
        "https://image.tmdb.org/t/p/w1280${movie.posterPath}"
    } else {
        "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clickable { onInfoClick(movie) }
            .testTag("hero_banner")
    ) {
        AsyncImage(
            model = posterUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay for visual aesthetics and readable text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color(0xFF070708)
                        ),
                        startY = 100f
                    )
                )
        )

        // Action controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Red, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("مميز حصرياً", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = movie.releaseDate,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "★ ${movie.rating}",
                    color = Color.Yellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = movie.overview,
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPlayClick(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("شاهد الآن", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = { onInfoClick(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B30)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("التفاصيل", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MovieCarousel(
    movies: List<MovieItem>,
    onPlayClick: (MovieItem) -> Unit,
    onInfoClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })

    // Auto-scroll effect
    LaunchedEffect(key1 = movies) {
        while (movies.size > 1) {
            delay(4000) // Auto-slide every 4 seconds
            try {
                val nextPage = (pagerState.currentPage + 1) % movies.size
                pagerState.animateScrollToPage(nextPage)
            } catch (e: Exception) {
                // Ignore any interrupted animation errors
            }
        }
    }

    // Dynamic app logo animation trigger on slide change
    val logoAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(pagerState.currentPage) {
        // Smoothly fade in the cinematic logo
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
        // Keep it beautifully visible for 2 seconds
        delay(2000)
        // Gracefully fade it out to enjoy full movie art
        logoAlpha.animateTo(
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutLinearInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            HeroBanner(
                movie = movies[page],
                onPlayClick = onPlayClick,
                onInfoClick = onInfoClick
            )
        }

        // Elegant floating cinematic app logo overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = 1f + (1f - logoAlpha.value) * 0.15f
                    scaleY = 1f + (1f - logoAlpha.value) * 0.15f
                }
                .background(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 28.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CINEMA",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 3.sp,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STREAM",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Red,
                    letterSpacing = 3.sp,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        // Dot indicators
        if (movies.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(movies.size) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (active) 16.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (active) Color.Red else Color.LightGray.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: MovieItem,
    onClick: () -> Unit
) {
    val posterUrl = if (movie.posterPath.startsWith("/")) {
        "https://image.tmdb.org/t/p/w342${movie.posterPath}"
    } else {
        "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=342"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("movie_card_${movie.tmdbId}")
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131315))
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Subtle top shadow for rating tag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            )

            // Rating tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color(0xE6000000), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "★ ${movie.rating.toString().take(3)}",
                    color = Color.Yellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Arabic or Multi sub Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Red.copy(alpha = 0.9f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (movie.isArabic) "عربي" else "مترجم",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = movie.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = movie.releaseDate,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ContinueWatchingRow(
    historyItems: List<WatchHistoryEntity>,
    onItemClick: (WatchHistoryEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "متابعة المشاهدة (سجل العرض)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Icon(Icons.Default.History, contentDescription = "History", tint = Color.Red, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(historyItems.take(5)) { history ->
                val posterUrl = if (history.posterPath.startsWith("/")) {
                    "https://image.tmdb.org/t/p/w185${history.posterPath}"
                } else {
                    "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=185"
                }

                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onItemClick(history) }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .height(170.dp)
                                .fillMaxWidth()
                                .background(Color(0xFF131315))
                        ) {
                            AsyncImage(
                                model = posterUrl,
                                contentDescription = history.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Play overlay icon
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Resume",
                                    tint = Color.Red,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Episode label for series/anime
                            if (history.isTvShow) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(Color.Red)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ح ${history.lastWatchedEpisode} / م ${history.lastWatchedSeason}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = history.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = "No results",
            tint = Color.DarkGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
