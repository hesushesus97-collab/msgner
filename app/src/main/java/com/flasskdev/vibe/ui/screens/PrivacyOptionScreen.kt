package com.flasskdev.vibe.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeTopGlow

enum class PrivacyType {
    EVERYONE, NOBODY, SELECTED
}

@Composable
fun PrivacyOptionScreen(
    title: String,
    description: String,
    savedValue: PrivacyType,
    initialValue: PrivacyType = savedValue,
    selectedUsersCount: Int,
    onValueChange: (PrivacyType) -> Unit,
    onNavigateToSelectUsers: () -> Unit,
    onSave: (PrivacyType) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val scrollState = rememberScrollState()
    var localValue by remember(savedValue, initialValue) { mutableStateOf(initialValue) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        VibeTopGlow(height = 380.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.backBtn,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            
            if (localValue != savedValue) {
                IconButton(onClick = { 
                    onSave(localValue)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = strings.saveBtn,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Options section
            SettingsSection {
                PrivacyRadioItem(
                    text = strings.privacyValueEverybody,
                    isSelected = localValue == PrivacyType.EVERYONE,
                    onClick = { 
                        localValue = PrivacyType.EVERYONE
                        onValueChange(PrivacyType.EVERYONE) 
                    }
                )
                PrivacyRadioItem(
                    text = strings.privacyValueNobody,
                    isSelected = localValue == PrivacyType.NOBODY,
                    onClick = { 
                        localValue = PrivacyType.NOBODY
                        onValueChange(PrivacyType.NOBODY) 
                    }
                )
                PrivacyRadioItem(
                    text = strings.privacyValueSelected,
                    isSelected = localValue == PrivacyType.SELECTED,
                    onClick = { 
                        localValue = PrivacyType.SELECTED
                        onValueChange(PrivacyType.SELECTED) 
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (localValue == PrivacyType.SELECTED) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = strings.privacyExceptionsTitle,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                
                SettingsSection {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToSelectUsers)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.privacySelectUsers,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedUsersCount > 0) {
                                Text(
                                    text = strings.privacyExceptionsSelected(selectedUsersCount),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.privacySelectUsersRuleDesc,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@Composable
private fun PrivacyRadioItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
