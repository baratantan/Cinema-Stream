package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    
    // Glowing effect pulse
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val glowBlurRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_blur"
    )

    LaunchedEffect(Unit) {
        // Parallel scale-up and fade-in
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = LinearOutSlowInEasing)
        )
        // Keep splash active for exactly 2 seconds (2000ms total)
        delay(2000)
        onSplashComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)) // Deep rich dark cinematic background
            .testTag("splash_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Elegant Cinematically Glowing Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MovieFilter,
                    contentDescription = "Cinema Logo",
                    tint = Color.Red,
                    modifier = Modifier.size(72.dp)
                )
            }

            // Glowing Typography
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CINEMA",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 3.sp,
                    style = MaterialTheme.typography.displayMedium.copy(
                        shadow = Shadow(
                            color = Color.Red.copy(alpha = 0.8f),
                            offset = Offset(0f, 0f),
                            blurRadius = glowBlurRadius
                        )
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STREAM",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Red,
                    letterSpacing = 3.sp,
                    style = MaterialTheme.typography.displayMedium.copy(
                        shadow = Shadow(
                            color = Color.Red.copy(alpha = 0.9f),
                            offset = Offset(0f, 0f),
                            blurRadius = glowBlurRadius
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "بوابتك لعالم السينما اللامحدود",
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}
