package com.widlily.wicompress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widlily.wicompress.ui.components.CustomCard
import com.widlily.wicompress.ui.components.GradientCard
import com.widlily.wicompress.ui.theme.MintAccent
import com.widlily.wicompress.ui.theme.OrangeAccent
import com.widlily.wicompress.ui.theme.PurpleAccent
import com.widlily.wicompress.ui.theme.Shapes
import com.widlily.wicompress.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToDuplicateFinder: () -> Unit,
    onNavigateToCompare: () -> Unit
) {
    val scrollState = rememberScrollState()
    val stats by viewModel.systemStats.collectAsState()
    val totalVideosVolume by viewModel.totalVideosVolume.collectAsState()
    val recentList by viewModel.recentCompressed.collectAsState()
    val suggestionText by viewModel.smartSuggestionText.collectAsState()

    // Format saved volume to GB / MB
    val savedGbText = if (stats.totalSpaceSavedBytes >= 1024L * 1024L * 1024L) {
        String.format("%.1f GB", stats.totalSpaceSavedBytes.toFloat() / (1024f * 1024f * 1024f))
    } else {
        String.format("%.1f MB", stats.totalSpaceSavedBytes.toFloat() / (1024f * 1024f))
    }

    val deviceTotalGb = totalVideosVolume.toFloat() / (1024f * 1024f * 1024f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 1. Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WiCompress",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            IconButton(onClick = {
                viewModel.triggerHapticFeedback()
                onNavigateToSettings()
            }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 2. Statistics Card Block
        CustomCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL SPACE SAVED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = savedGbText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            color = MintAccent,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                // Memory Occupancy Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    CircularProgressIndicator(
                        progress = 0.45f, // Mock percentage
                        color = PurpleAccent,
                        trackColor = MaterialTheme.colorScheme.outline,
                        strokeWidth = 6.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "45%", // Mock space usage percentage
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }

        // Three Sub-Cards Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1
            CustomCard(
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Compressed",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    )
                    Text(
                        text = "${stats.totalCompressedCount}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
            // Card 2
            CustomCard(
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Avg Savings",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    )
                    Text(
                        text = String.format("%.0f%%", stats.averageRatio),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
            // Card 3
            CustomCard(
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Total Videos",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    )
                    Text(
                        text = String.format("%.1f GB", deviceTotalGb),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        }

        // 3. Smart Suggestion Banner
        suggestionText?.let { text ->
            GradientCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                gradient = Brush.linearGradient(
                    colors = listOf(MintAccent.copy(alpha = 0.15f), PurpleAccent.copy(alpha = 0.15f))
                ),
                onClick = {
                    viewModel.compressAllLargeVideos()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MintAccent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "COMPRESS ALL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PurpleAccent,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // 4. COMPRESS Actions Section
        Text(
            text = "COMPRESS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Grid of Compress Commands
        val cardHeight = 72.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomCard(
                modifier = Modifier
                    .weight(1f)
                    .height(cardHeight),
                onClick = {
                    viewModel.triggerHapticFeedback()
                    // Mock file picker & Quick Compress
                    viewModel.largeVideos.value.firstOrNull()?.let {
                        viewModel.compressVideo(it, "Quick")
                    }
                }
            ) {
                Text(
                    text = "⚡ Quick Compress",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            CustomCard(
                modifier = Modifier
                    .weight(1f)
                    .height(cardHeight),
                onClick = {
                    viewModel.triggerHapticFeedback()
                    // Mock platform compress WhatsApp limit
                    viewModel.largeVideos.value.firstOrNull()?.let {
                        viewModel.compressVideo(it, "WhatsApp", bitrateMbps = 0.8f, useH265 = true, width = 854, height = 480)
                    }
                }
            ) {
                Text(
                    text = "📱 Platform Limit",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomCard(
                modifier = Modifier
                    .weight(1f)
                    .height(cardHeight),
                onClick = {
                    viewModel.triggerHapticFeedback()
                    onNavigateToDuplicateFinder()
                }
            ) {
                Text(
                    text = "🔍 Smart Scan",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            CustomCard(
                modifier = Modifier
                    .weight(1f)
                    .height(cardHeight),
                onClick = {
                    viewModel.triggerHapticFeedback()
                    // Mock Custom Size compress to 16MB
                    viewModel.largeVideos.value.firstOrNull()?.let {
                        viewModel.compressVideo(it, "Custom Size", bitrateMbps = 0.6f)
                    }
                }
            ) {
                Text(
                    text = "⚙️ Custom Size",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // 5. CLEANING & UTILITIES
        Text(
            text = "CLEANING & UTILITIES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CustomCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            onClick = {
                viewModel.triggerHapticFeedback()
                onNavigateToDuplicateFinder()
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "👯 Find Duplicate Videos",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = OrangeAccent
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Clean storage",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // 6. VIDEO TOOLS
        Text(
            text = "VIDEO TOOLS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("✂️ Trim", "📐 Resize", "🔁 Convert", "🎧 Extract Audio", "🖼️ To GIF", "⏩ Speed", "⚖️ Compare").forEach { tool ->
                Box(
                    modifier = Modifier
                        .clip(Shapes.small)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            viewModel.triggerHapticFeedback()
                            if (tool.contains("Compare")) {
                                onNavigateToCompare()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = tool,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground)
                    )
                }
            }
        }

        // 7. RECENT Section
        if (recentList.isNotEmpty()) {
            Text(
                text = "RECENT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentList.forEach { history ->
                    CustomCard(
                        modifier = Modifier.width(160.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(Shapes.small)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🎬",
                                    fontSize = 24.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .clip(Shapes.small)
                                        .background(MintAccent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "-${history.ratioPercent}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = history.fileName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
