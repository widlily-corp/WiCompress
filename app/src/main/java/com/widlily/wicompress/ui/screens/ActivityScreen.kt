package com.widlily.wicompress.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widlily.wicompress.ui.components.CustomCard
import com.widlily.wicompress.ui.theme.MintAccent
import com.widlily.wicompress.ui.theme.OrangeAccent
import com.widlily.wicompress.ui.theme.PurpleAccent
import com.widlily.wicompress.ui.theme.Shapes
import com.widlily.wicompress.ui.viewmodel.ActivityViewModel

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onNavigateToHome: () -> Unit
) {
    val scrollState = rememberScrollState()

    val currentTask by viewModel.currentTask.collectAsState()
    val queueList by viewModel.queueList.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val etaSeconds by viewModel.etaSeconds.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val sessionSavedBytes by viewModel.sessionSavedBytes.collectAsState()

    val totalSavedText = if (sessionSavedBytes >= 1024L * 1024L * 1024L) {
        String.format("%.1f GB Saved", sessionSavedBytes.toFloat() / (1024f * 1024f * 1024f))
    } else {
        String.format("%.1f MB Saved", sessionSavedBytes.toFloat() / (1024f * 1024f))
    }

    val etaText = if (etaSeconds > 0) {
        val minutes = etaSeconds / 60
        val seconds = etaSeconds % 60
        String.format("ETA: %02d:%02d", minutes, seconds)
    } else {
        "ETA: --:--"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Active Queue",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Session counters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compressed Count
            CustomCard(
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    )
                    Text(
                        text = "$completedCount Done",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }

            // Session Space Saved
            CustomCard(
                modifier = Modifier.weight(1.2f),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Text(
                        text = "This Session",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    )
                    Text(
                        text = totalSavedText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MintAccent
                        )
                    )
                }
            }
        }

        // 2. Now Processing
        if (currentTask != null) {
            val task = currentTask!!
            Text(
                text = "NOW PROCESSING",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CustomCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = task.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Original size: ${String.format("%.1f MB", task.originalSize.toFloat() / (1024f * 1024f))}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        color = PurpleAccent,
                        trackColor = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(Shapes.small)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%.1f%%", progress),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                        )
                        Text(
                            text = String.format("Speed: %.1fx", speed),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = OrangeAccent
                            )
                        )
                        Text(
                            text = etaText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isProcessing) {
                                    viewModel.pauseQueue()
                                } else {
                                    viewModel.resumeQueue()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isProcessing) OrangeAccent else PurpleAccent,
                                contentColor = Color.Black
                            ),
                            shape = Shapes.small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (isProcessing) "Pause" else "Resume")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.cancelCurrentTask()
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = Shapes.small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Cancel", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        } else {
            // Idle State Card
            CustomCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No active compression tasks",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add tasks from the Home screen",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    )
                }
            }
        }

        // 3. Up Next List
        val upcomingTasks = if (queueList.isNotEmpty()) queueList.drop(1) else emptyList()
        
        Text(
            text = "UP NEXT (${upcomingTasks.size})",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (upcomingTasks.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                upcomingTasks.forEach { task ->
                    CustomCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Target: ${task.type}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                )
                            }
                            IconButton(onClick = {
                                // For mock simplicity: can cancel active / remove queue
                                viewModel.cancelAll() // Reset service queue
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Remove task",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Queue empty text
            Text(
                text = "Queue is empty",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // 4. Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.pauseQueue() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = Shapes.medium,
                modifier = Modifier.weight(1f)
            ) {
                Text("Pause All")
            }

            Button(
                onClick = onNavigateToHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleAccent,
                    contentColor = Color.Black
                ),
                shape = Shapes.medium,
                modifier = Modifier.weight(1f)
            ) {
                Text("Add More")
            }
        }
    }
}
