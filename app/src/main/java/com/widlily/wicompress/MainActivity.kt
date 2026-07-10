package com.widlily.wicompress

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.widlily.wicompress.ui.screens.*
import com.widlily.wicompress.ui.theme.WiCompressTheme
import com.widlily.wicompress.ui.viewmodel.*

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val activityViewModel: ActivityViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val duplicateFinderViewModel: DuplicateFinderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initial storage permissions check
        checkAndRequestStoragePermissions()

        setContent {
            val themeMode by settingsViewModel.theme.collectAsState()
            
            // Map theme choices dynamically
            val isDarkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            WiCompressTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                
                // Screen Navigation Routes
                val items = listOf(
                    NavigationItem("home", "Home", Icons.Default.Home),
                    NavigationItem("activity", "Activity", Icons.Default.Share),
                    NavigationItem("history", "History", Icons.Default.List),
                    NavigationItem("settings", "Settings", Icons.Default.Settings)
                )

                var selectedRoute by remember { mutableStateOf("home") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            items.forEach { item ->
                                NavigationBarItem(
                                    selected = selectedRoute == item.route,
                                    onClick = {
                                        selectedRoute = item.route
                                        homeViewModel.triggerHapticFeedback()
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    label = { Text(item.title) },
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToSettings = {
                                    selectedRoute = "settings"
                                    navController.navigate("settings")
                                },
                                onNavigateToDuplicateFinder = {
                                    navController.navigate("duplicate_finder")
                                },
                                onNavigateToCompare = {
                                    navController.navigate("compare")
                                }
                            )
                        }
                        composable("activity") {
                            ActivityScreen(
                                viewModel = activityViewModel,
                                onNavigateToHome = {
                                    selectedRoute = "home"
                                    navController.navigate("home")
                                }
                            )
                        }
                        composable("history") {
                            HistoryScreen(viewModel = historyViewModel)
                        }
                        composable("settings") {
                            SettingsScreen(viewModel = settingsViewModel)
                        }
                        composable("duplicate_finder") {
                            DuplicateFinderScreen(viewModel = duplicateFinderViewModel)
                        }
                        composable("compare") {
                            CompareScreen()
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
                Toast.makeText(this, "WiCompress requires storage manager permissions to scan video files.", Toast.LENGTH_LONG).show()
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val neededPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (neededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 2026
    }
}

data class NavigationItem(val route: String, val title: String, val icon: ImageVector)
