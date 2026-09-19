package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.components.VibeToast
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import com.flasskdev.vibe.ui.theme.luminanceIsDark

private enum class ProPlan {
    YEARLY,
    MONTHLY;

    fun label(strings: VibeStrings): String = when (this) {
        YEARLY -> strings.vibeProPlanYearly
        MONTHLY -> strings.vibeProPlanMonthly
    }

    fun price(strings: VibeStrings): String = when (this) {
        YEARLY -> strings.vibeProPlanYearlyPrice
        MONTHLY -> strings.vibeProPlanMonthlyPrice
    }

    fun discount(strings: VibeStrings): String? = when (this) {
        YEARLY -> strings.vibeProPlanYearlyDiscount
        MONTHLY -> null
    }
}

@Composable
fun VibeProScreenContent(
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val scrollState = rememberScrollState()
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    var selectedPlan by remember { mutableStateOf(ProPlan.YEARLY) }
    var showToast by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        VibeTopGlow(height = 380.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            // ─── TOP BAR ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.backBtn,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = strings.settingsVibePro,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // ─── SCROLLABLE BODY ───
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                // ══ HERO PROMO CARD ══
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFC107).copy(alpha = if (isDark) 0.24f else 0.20f),
                                        Color(0xFFFF6B6B).copy(alpha = if (isDark) 0.18f else 0.14f)
                                    )
                                )
                            )
                            .border(
                                width = 0.9.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFC107).copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFFFFB300).copy(alpha = 0.35f),
                                            Color(0xFFFFC107).copy(alpha = 0.12f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = strings.settingsVibePro,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = strings.vibeProHeroDescription,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ══ FEATURES LIST ══
                Text(
                    text = strings.vibeProSectionFeatures,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 8.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
                    border = BorderStroke(
                        width = 0.7.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        ProFeatureRow(
                            icon = Icons.Rounded.CloudUpload,
                            iconTint = Color(0xFF2196F3),
                            title = strings.vibeProFeatureLimitsTitle,
                            subtitle = strings.vibeProFeatureLimitsSubtitle
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp),
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                        )
                        ProFeatureRow(
                            icon = Icons.Rounded.GraphicEq,
                            iconTint = Color(0xFF9C27B0),
                            title = strings.vibeProFeatureVoiceToTextTitle,
                            subtitle = strings.vibeProFeatureVoiceToTextSubtitle
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp),
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                        )
                        ProFeatureRow(
                            icon = Icons.Rounded.AutoAwesome,
                            iconTint = Color(0xFFFF9800),
                            title = strings.vibeProFeatureReactionsTitle,
                            subtitle = strings.vibeProFeatureReactionsSubtitle
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp),
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                        )
                        ProFeatureRow(
                            icon = Icons.Rounded.WorkspacePremium,
                            iconTint = Color(0xFFFFC107),
                            title = strings.vibeProFeatureBadgeTitle,
                            subtitle = strings.vibeProFeatureBadgeSubtitle
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp),
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                        )
                        ProFeatureRow(
                            icon = Icons.Rounded.Speed,
                            iconTint = Color(0xFF00BCD4),
                            title = strings.vibeProFeatureSpeedTitle,
                            subtitle = strings.vibeProFeatureSpeedSubtitle
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp),
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                        )
                        ProFeatureRow(
                            icon = Icons.Rounded.Shield,
                            iconTint = Color(0xFF4CAF50),
                            title = strings.vibeProFeatureNoAdsTitle,
                            subtitle = strings.vibeProFeatureNoAdsSubtitle
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ══ PLAN SELECTOR ══
                Text(
                    text = strings.vibeProSectionPlans,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 8.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProPlan.entries.forEach { plan ->
                        val isSelected = selectedPlan == plan
                        val planDiscount = plan.discount(strings)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedPlan = plan },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) {
                                Color(0xFFFFB300).copy(alpha = if (isDark) 0.18f else 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.60f else 0.90f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.7.dp,
                                color = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = plan.label(strings),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (planDiscount != null) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(percent = 50),
                                            color = Color(0xFFFF3B30)
                                        ) {
                                            Text(
                                                text = planDiscount,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = plan.price(strings),
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ══ CTA BUTTON ══
                Button(
                    onClick = { showToast = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = strings.vibeProSubscribeCta(selectedPlan.price(strings)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = strings.vibeProAutoRenewalDisclaimer,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        // ══ TOAST NOTIFICATION ══
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            VibeToast(
                message = strings.vibeProComingSoonToast,
                isVisible = showToast,
                onDismiss = { showToast = false },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun ProFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                fontSize = 12.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}
