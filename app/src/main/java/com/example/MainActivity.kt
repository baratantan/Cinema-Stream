package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MovieViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MovieViewModel = viewModel(
                factory = MovieViewModel.Factory(application)
            )
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("splash") {
                        SplashScreen(
                            onSplashComplete = {
                                navController.navigate("dashboard")
                            }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onMovieClick = { movie ->
                                viewModel.selectMovie(movie)
                                navController.navigate("detail")
                            },
                            onWatchlistNavigate = {
                                navController.navigate("watchlist")
                            },
                            onSettingsNavigate = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable("detail") {
                        DetailScreen(
                            viewModel = viewModel,
                            onBack = {
                                viewModel.selectMovie(null)
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("watchlist") {
                        WatchlistScreen(
                            viewModel = viewModel,
                            onBack = {
                                navController.popBackStack()
                            },
                            onItemClick = { movie ->
                                viewModel.selectMovie(movie)
                                navController.navigate("detail")
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
