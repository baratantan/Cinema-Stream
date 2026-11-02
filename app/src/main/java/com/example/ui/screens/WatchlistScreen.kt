package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.MovieItem
import com.example.ui.MovieViewModel

@Composable
fun WatchlistScreen(
    viewModel: MovieViewModel,
    onBack: () -> Unit,
    onItemClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val watchlist by viewModel.watchlist.collectAsState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070708))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color(0xFF131315), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "قائمتي المفضلة (Watchlist)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            if (watchlist.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Empty Watchlist",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "قائمتك فارغة تماماً.",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أضف أفلامك ومسلسلاتك المفضلة للوصول إليها لاحقاً.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(watchlist) { entity ->
                        WatchlistItemCard(
                            entity = entity,
                            onClick = {
                                // Find movie from catalog or build a virtual one
                                val item = viewModel.getOfflineCatalog().find { it.tmdbId == entity.tmdbId }
                                val finalMovie = item ?: MovieItem(
                                    id = entity.id,
                                    tmdbId = entity.tmdbId,
                                    title = entity.title,
                                    originalTitle = "",
                                    overview = "عمل سينمائي مفضل قمت بحفظه في قائمتك الخاصة.",
                                    posterPath = entity.posterPath,
                                    backdropPath = entity.posterPath,
                                    category = entity.category,
                                    genres = listOf("Drama"),
                                    rating = 8.0,
                                    releaseDate = "",
                                    isTvShow = entity.isTvShow,
                                    isArabic = entity.category.contains("Arabic")
                                )
                                onItemClick(finalMovie)
                            },
                            onRemove = { viewModel.deleteHistoryItem(entity.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistItemCard(
    entity: WatchHistoryEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val posterUrl = if (entity.posterPath.startsWith("/")) {
        "https://image.tmdb.org/t/p/w342${entity.posterPath}"
    } else {
        "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=342"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("watchlist_item_${entity.tmdbId}")
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131315))
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = entity.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Delete / Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp))
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove Bookmark",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Category badge
            val categoryLabel = when (entity.category) {
                "Arabic Movies" -> "فيلم عربي"
                "Foreign Movies" -> "فيلم أجنبي"
                "Arabic Series" -> "مسلسل عربي"
                "Foreign Series" -> "مسلسل أجنبي"
                "Anime" -> "أنمي"
                else -> "عمل فني"
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Red.copy(alpha = 0.9f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = categoryLabel,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = entity.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
