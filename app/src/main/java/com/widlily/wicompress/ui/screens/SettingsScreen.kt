package com.widlily.wicompress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widlily.wicompress.ui.components.CustomCard
import com.widlily.wicompress.ui.theme.MintAccent
import com.widlily.wicompress.ui.theme.PurpleAccent
import com.widlily.wicompress.ui.theme.Shapes
import com.widlily.wicompress.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val scrollState = rememberScrollState()

    val currentTheme by viewModel.theme.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val autoDeleteEnabled by viewModel.autoDeleteEnabled.collectAsState()
    val outputDirectory by viewModel.outputDirectory.collectAsState()

    var outputDirInput by remember { mutableStateOf(outputDirectory) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Appearance Section
        Text(
            text = "APPEARANCE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CustomCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Theme mode",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto", "Light", "Dark").forEach { themeMode ->
                        val selected = currentTheme == themeMode
                        val color = if (selected) PurpleAccent else MaterialTheme.colorScheme.outline
                        val textColor = if (selected) Color.Black else MaterialTheme.colorScheme.onBackground
                        
                        Button(
                            onClick = { viewModel.setTheme(themeMode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color,
                                contentColor = textColor
                            ),
                            shape = Shapes.small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = themeMode, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // 2. Output Configurations
        Text(
            text = "OUTPUT STORAGE (SCOPED)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CustomCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Save Folder Name",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Files will be saved in Movies/[Folder Name]",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = outputDirInput,
                    onValueChange = {
                        outputDirInput = it
                        viewModel.setOutputDirectory(it)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = PurpleAccent
                    ),
                    shape = Shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // 3. User Preferences
        Text(
            text = "PREFERENCES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CustomCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Switch 1: Haptic Vibrations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Feedback",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Calibrates click vibration for Huawei X-axis haptic motor",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        )
                    }
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = { viewModel.setHapticEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = MintAccent
                        )
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline)

                // Switch 2: Auto delete original
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-delete original",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Automatically move original video to trash after verifying compressed file integrity",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        )
                    }
                    Switch(
                        checked = autoDeleteEnabled,
                        onCheckedChange = { viewModel.setAutoDeleteEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = MintAccent
                        )
                    )
                }
            }
        }
    }
}
