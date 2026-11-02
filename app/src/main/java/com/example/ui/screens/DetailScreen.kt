package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MovieItem
import com.example.ui.MovieViewModel
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.AdsterraAdBanner

@Composable
fun DetailScreen(
    viewModel: MovieViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val movie by viewModel.selectedMovie.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val currentSeason by viewModel.currentSeason.collectAsState()
    val currentEpisode by viewModel.currentEpisode.collectAsState()
    
    val tvSeasonsCount by viewModel.tvSeasonsCount.collectAsState()
    val tvEpisodesInSeasons by viewModel.tvEpisodesInSeasons.collectAsState()

    val activeMovie = movie ?: return

    val context = androidx.compose.ui.platform.LocalContext.current
    val decryptString = { encoded: String ->
        val trimmed = encoded.trim()
        if (trimmed.isEmpty()) "" else {
            try {
                val decodedBytes = android.util.Base64.decode(trimmed, android.util.Base64.DEFAULT)
                val decodedStr = String(decodedBytes, Charsets.UTF_8).trim()
                if (decodedStr.isNotEmpty() && decodedStr.all { it.code in 32..126 }) {
                    decodedStr
                } else {
                    trimmed
                }
            } catch (e: Exception) {
                trimmed
            }
        }
    }

    val triggerSmartAd = {
        val domain = viewModel.getAdsterraBannerDomain()
        val smartLink = viewModel.getAdsterraSmartLink()
        
        val targetLink = when {
            domain.isNotBlank() && domain != "YOUR_ADSTERRA_KEY_HERE" && domain != "embargotechniquebattle.com" -> domain
            smartLink.isNotBlank() && smartLink != "YOUR_ADSTERRA_SMART_LINK_HERE" -> smartLink
            else -> ""
        }
        
        if (targetLink.isNotBlank()) {
            val finalUrl = if (targetLink.startsWith("http://") || targetLink.startsWith("https://")) {
                targetLink
            } else {
                "https://$targetLink"
            }
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(finalUrl)).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val isBookmarkedFlow = remember(activeMovie) { viewModel.isBookmarked(activeMovie) }
    val isBookmarked by isBookmarkedFlow.collectAsState(initial = false)

    // Build the correct automated streaming URL
    val videoUrl = remember(activeMovie, selectedServer, currentSeason, currentEpisode) {
        if (activeMovie.isTvShow) {
            selectedServer.getTvUrl(activeMovie.tmdbId, currentSeason, currentEpisode)
        } else {
            selectedServer.getMovieUrl(activeMovie.tmdbId)
        }
    }

    if (isPlaying) {
        // Run full-screen sandboxed web scraper player
        VideoPlayerView(
            url = videoUrl,
            onClose = { viewModel.stopPlaying() },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Beautiful movie details dashboard
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF070708))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // 1. Movie Backdrop Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    val backdropUrl = if (activeMovie.posterPath.startsWith("/")) {
                        "https://image.tmdb.org/t/p/w1280${activeMovie.posterPath}"
                    } else {
                        "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200"
                    }

                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = activeMovie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Gradients
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        Color(0xFF070708)
                                    )
                                )
                            )
                    )

                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                // 2. Info Card Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = activeMovie.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 34.sp
                    )
                    
                    if (activeMovie.originalTitle.isNotBlank() && activeMovie.originalTitle != activeMovie.title) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeMovie.originalTitle,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badges row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Red, shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (activeMovie.isArabic) "عربي" else "مترجم عربي",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E1E22), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = activeMovie.releaseDate,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeMovie.rating.toString().take(3),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Genres scrolling pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeMovie.genres) { genre ->
                            Box(
                                modifier = Modifier
                                    .border(BorderStroke(1.dp, Color.DarkGray), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(genre, color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interactive Action Buttons (Play / Add to watchlist)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                triggerSmartAd()
                                viewModel.startPlaying()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("play_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("شاهد الآن مجاناً", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        IconButton(
                            onClick = { viewModel.toggleWatchlist(activeMovie) },
                            modifier = Modifier
                                .background(Color(0xFF1B1B1E), shape = RoundedCornerShape(12.dp))
                                .size(50.dp)
                                .testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) Color.Red else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Movie Overview / Plot summary
                    Text("قصة العمل", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeMovie.overview,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = if (activeMovie.isArabic) TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val adKey = viewModel.getAdsterraBannerKey()
                    val adDomain = viewModel.getAdsterraBannerDomain()
                    if (adKey.isNotBlank() && adKey != "YOUR_ADSTERRA_KEY_HERE") {
                        val adWidth = viewModel.getAdsterraBannerWidth()
                        val adHeight = viewModel.getAdsterraBannerHeight()
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        AdsterraAdBanner(
                            adKey = adKey,
                            adDomain = adDomain,
                            width = adWidth,
                            height = adHeight,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Automated Scraper Server List
                    Text("خوادم وسيرفرات البث", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.streamingServers.forEach { server ->
                            val isSelected = server == selectedServer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.Red.copy(alpha = 0.15f) else Color(0xFF131315))
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) Color.Red else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        triggerSmartAd()
                                        viewModel.selectServer(server)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = "Server",
                                        tint = if (isSelected) Color.Red else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = server.name,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                
                                if (isSelected) {
                                    Text("نشط حالياً", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("اتصال سريع", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Series & Anime Episode grid (only shown if isTvShow is true)
                    if (activeMovie.isTvShow) {
                        Text("مواسم وحلقات العمل", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Season selector tabs
                        if (tvSeasonsCount > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items((1..tvSeasonsCount).toList()) { s ->
                                    val isSelectedSeason = s == currentSeason
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelectedSeason) Color.Red else Color(0xFF1C1C1F))
                                            .clickable { viewModel.setSeason(s) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "الموسم $s",
                                            color = if (isSelectedSeason) Color.White else Color.LightGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Episode grid
                        val episodeCount = tvEpisodesInSeasons.getOrNull(currentSeason - 1) ?: 10
                        
                        Text(
                            text = "الموسم $currentSeason - ($episodeCount حلقة متوفرة للبث)",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Compose Vertical grid doesn't scroll inside vertical Scroll, so we compile a dynamic layout of Rows or use a FlowRow equivalent
                        val rowsCount = (episodeCount + 3) / 4 // 4 episodes per row
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            for (rowIndex in 0 until rowsCount) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    for (colIndex in 0..3) {
                                        val epNumber = rowIndex * 4 + colIndex + 1
                                        if (epNumber <= episodeCount) {
                                            val isSelectedEp = epNumber == currentEpisode
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1.2f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelectedEp) Color.Red else Color(0xFF131315))
                                                    .border(
                                                        width = if (isSelectedEp) 1.dp else 0.dp,
                                                        color = if (isSelectedEp) Color.Red else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        triggerSmartAd()
                                                        viewModel.setEpisode(epNumber)
                                                        viewModel.startPlaying()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "حلقة",
                                                        color = if (isSelectedEp) Color.White else Color.Gray,
                                                        fontSize = 10.sp
                                                    )
                                                    Text(
                                                        text = epNumber.toString(),
                                                        color = Color.White,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
