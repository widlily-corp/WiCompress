package com.widlily.wicompress.ui.screens

import android.app.Activity
import android.app.PendingIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widlily.wicompress.ui.components.CustomCard
import com.widlily.wicompress.ui.theme.MintAccent
import com.widlily.wicompress.ui.theme.OrangeAccent
import com.widlily.wicompress.ui.theme.PurpleAccent
import com.widlily.wicompress.ui.theme.Shapes
import com.widlily.wicompress.ui.viewmodel.DuplicateFinderViewModel

@Composable
fun DuplicateFinderScreen(
    viewModel: DuplicateFinderViewModel
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()

    // Activity Result Launcher to prompt Scoped Storage deletion approval
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Delete approved, refresh scan
            viewModel.startScan()
        }
    }

    val selectedCount = selectedUris.filter { it.value }.size

    // Auto-trigger scan on enter if empty
    LaunchedEffect(Unit) {
        if (duplicateGroups.isEmpty() && !isScanning) {
            viewModel.startScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Duplicate Video Scanner",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isScanning) {
            // Scanning State Layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = scanProgress,
                        color = PurpleAccent,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = String.format("Scanning frame hashes... %.0f%%", scanProgress * 100),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analyzing video pixel maps to identify copies",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    )
                }
            }
        } else {
            if (duplicateGroups.isNotEmpty()) {
                // Results list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(duplicateGroups) { group ->
                        CustomCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DUPLICATE GROUP",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = OrangeAccent
                                        )
                                    )
                                    Text(
                                        text = "${group.files.size} identical files",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                // List files in group
                                group.files.forEachIndexed { index, file ->
                                    val isOriginal = index == group.bestIndex
                                    val uriStr = file.uri.toString()
                                    val isSelectedToDelete = selectedUris[uriStr] ?: false

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Visual thumbnail mock
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(Shapes.small)
                                                .background(Color.DarkGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "🎬", fontSize = 16.sp)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Title and details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = file.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = String.format("%.1f MB", file.sizeMb),
                                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                                )
                                                Text(
                                                    text = file.durationText,
                                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                                )
                                                if (isOriginal) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(Shapes.small)
                                                            .background(MintAccent.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "KEEP ORIGINAL",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MintAccent
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Checkbox for deletion selection
                                        if (!isOriginal) {
                                            Checkbox(
                                                checked = isSelectedToDelete,
                                                onCheckedChange = { viewModel.toggleSelection(file.uri) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = PurpleAccent,
                                                    checkmarkColor = Color.Black
                                                )
                                            )
                                        } else {
                                            // Keep blank spacing to balance checkbox
                                            Spacer(modifier = Modifier.width(48.dp))
                                        }
                                    }
                                    
                                    if (index < group.files.size - 1) {
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty duplicate scan result state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No duplicate videos found",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your device storage is clean!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.startScan() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = Shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Re-scan")
                }

                Button(
                    onClick = {
                        viewModel.deleteSelected(
                            onPendingIntentReady = { pendingIntent ->
                                val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                deleteLauncher.launch(request)
                            },
                            onDirectDeleted = {
                                // Deletion successful (API < 30)
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.Black
                    ),
                    shape = Shapes.medium,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Delete Selected ($selectedCount)")
                }
            }
        }
    }
}
