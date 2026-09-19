package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.flasskdev.vibe.data.VibeMessage
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import com.flasskdev.vibe.data.ChatInfo
import com.flasskdev.vibe.data.MessageInfo
import com.flasskdev.vibe.data.UserSearchResult
import com.flasskdev.vibe.ui.components.*
import com.flasskdev.vibe.ui.theme.*
import kotlinx.coroutines.launch

private const val NICKNAME_MAX_LENGTH = 32

@Composable
fun NicknameScreen(email: String, userId: Int, webSocket: VibeWebSocket, onSuccess: () -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val strings = LocalVibeStrings.current

    val listener = remember(strings) {
        object : VibeWebSocketListener {
            override fun onAuthResponse(message: VibeMessage) {
                scope.launch {
                    if (message.type == "set_nickname_result") {
                        isLoading = false
                        if (message.success == true) {
                            onSuccess()
                        } else {
                            errorMessage = message.message ?: strings.errorSaving
                        }
                    }
                }
            }

            override fun onConnected() {}
            override fun onDisconnected() {
                scope.launch { isLoading = false }
            }
            override fun onError(error: String) {
                scope.launch {
                    errorMessage = error
                    isLoading = false
                }
            }
        }
    }

    DisposableEffect(webSocket) {
        webSocket.addListener(listener)
        onDispose {
            webSocket.removeListener(listener)
        }
    }

    val isValid = nickname.trim().length in 1..NICKNAME_MAX_LENGTH
    val isAtLimit = nickname.length >= NICKNAME_MAX_LENGTH

    Box(modifier = Modifier.fillMaxSize()) {
        VibeBackgroundMesh()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Hero badge: gradient ring + glow, same visual language as the profile avatar ──
            Box(
                modifier = Modifier.size(112.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = strings.nicknameTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.nicknameDescription,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            VibeTextField(
                value = nickname,
                onValueChange = {
                    // Hard-capping here keeps the counter honest and stops the server from
                    // being the first place a too-long name gets rejected.
                    if (it.length <= NICKNAME_MAX_LENGTH) {
                        nickname = it
                        errorMessage = null
                    }
                },
                label = strings.nicknameLabel,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Counter / limit hint row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAtLimit) strings.editFieldLimitReached else strings.nicknameHint,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = if (isAtLimit)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.editFieldCounter(nickname.length, NICKNAME_MAX_LENGTH),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isAtLimit)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }

            // ── Error banner ──
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(tween(180)) + expandVertically(spring()),
                exit = fadeOut(tween(120)) + shrinkVertically(spring())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            VibeButton(
                text = if (isLoading) strings.saveLoading else strings.saveBtn,
                onClick = {
                    isLoading = true
                    webSocket.setNickname(email, nickname.trim(), if (userId != 0) userId else null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isValid
            )
        }
    }
}