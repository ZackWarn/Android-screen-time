package com.example.screentime.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.screentime.data.entities.AppLimit
import com.example.screentime.domain.managers.AppInfo

@Composable
fun AppLimitSetterCard(
    appInfo: AppInfo,
    currentLimit: AppLimit?,
    currentUsageMinutes: Int,
    onSetLimit: (Int) -> Unit,
    onDeleteLimit: () -> Unit,
    modifier: Modifier = Modifier,
    pastUsageData: Map<String, Int> = emptyMap() // Past 7 days usage data (date -> minutes)
) {
    var showDialog by remember { mutableStateOf(false) }
    var limitInput by remember { mutableStateOf(currentLimit?.limitMinutes?.toString() ?: "") }
    var validationError by remember { mutableStateOf("") }

    // Minimum allowed limit = current usage for today (never lower than already used)
    val minimumLimit = maxOf(currentUsageMinutes, currentLimit?.usedTodayMinutes ?: 0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currentLimit?.isBlocked == true) {
                Color(0xFFFF6B6B).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Image(
                bitmap = appInfo.appIcon.toBitmap().asImageBitmap(),
                contentDescription = "${appInfo.appName} icon",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (currentLimit != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Usage progress
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentLimit.usedTodayMinutes}/${currentLimit.limitMinutes} min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        if (currentLimit.isBlocked) {
                            Text(
                                text = "🚫 Blocked",
                                fontSize = 12.sp,
                                color = Color(0xFFFF6B6B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = (currentLimit.usedTodayMinutes.toFloat() / currentLimit.limitMinutes.toFloat()).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(6.dp),
                        color = if (currentLimit.usedTodayMinutes >= currentLimit.limitMinutes) {
                            Color(0xFFFF6B6B)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    if (currentUsageMinutes > 0) {
                        Text(
                            text = "Used today: $currentUsageMinutes min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = "No limit set",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentLimit != null) {
                    IconButton(
                        onClick = onDeleteLimit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove limit",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (currentLimit != null) "Edit" else "Set Limit",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Dialog for setting limit
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(text = "Set Time Limit for ${appInfo.appName}")
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {

                    Text(
                        text = "Set daily limit in minutes:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    // Show minimum limit requirement
                    if (minimumLimit > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Minimum: $minimumLimit min (already used today)",
                            fontSize = 12.sp,
                            color = Color(0xFFFFA500),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = {
                            limitInput = it
                            // Clear validation error when user starts typing
                            validationError = ""
                        },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = validationError.isNotEmpty(),
                        supportingText = {
                            if (validationError.isNotEmpty()) {
                                Text(
                                    text = validationError,
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = limitInput.toIntOrNull()

                        // Validate the limit
                        when {
                            minutes == null -> {
                                validationError = "Please enter a valid number"
                            }
                            minutes <= 0 -> {
                                validationError = "Limit must be greater than 0"
                            }
                            minutes < minimumLimit -> {
                                validationError = "Limit must be at least $minimumLimit min (already used today)"
                            }
                            else -> {
                                // Validation passed
                                onSetLimit(minutes)
                                showDialog = false
                                validationError = ""
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
