package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.theme.VibeStrings
import kotlinx.coroutines.delay

/* ------------------------------------------------------------------ */
/*  Model                                                             */
/* ------------------------------------------------------------------ */

private const val REPORT_COMMENT_LIMIT = 512
private const val REPORT_COMMENT_WARN_AT = 460

/** Severity drives the accent color and the priority-review notice. */
enum class ReportSeverity { NORMAL, HIGH, CRITICAL }

/**
 * Reasons carry a stable [id] and that id is what goes to the backend, so the report
 * payload no longer depends on the UI language (previously the localized label was sent).
 */
enum class ReportReason(val id: String, val severity: ReportSeverity) {
    SPAM("spam", ReportSeverity.NORMAL),
    FRAUD("fraud", ReportSeverity.HIGH),
    FAKE_ACCOUNT("fake_account", ReportSeverity.NORMAL),
    MISINFORMATION("misinformation", ReportSeverity.NORMAL),
    HARASSMENT("harassment", ReportSeverity.HIGH),
    HATE_SPEECH("hate_speech", ReportSeverity.HIGH),
    PORNOGRAPHY("pornography", ReportSeverity.HIGH),
    DRUGS("drugs", ReportSeverity.HIGH),
    WEAPONS("weapons", ReportSeverity.HIGH),
    VIOLENCE("violence", ReportSeverity.CRITICAL),
    CSAM("csam", ReportSeverity.CRITICAL);

    val icon: ImageVector
        get() = when (this) {
            SPAM -> Icons.Rounded.Campaign
            FRAUD -> Icons.Rounded.MoneyOff
            FAKE_ACCOUNT -> Icons.Rounded.PersonOff
            MISINFORMATION -> Icons.Rounded.FactCheck
            HARASSMENT -> Icons.Rounded.SentimentVeryDissatisfied
            HATE_SPEECH -> Icons.Rounded.RecordVoiceOver
            PORNOGRAPHY -> Icons.Rounded.Explicit
            DRUGS -> Icons.Rounded.Medication
            WEAPONS -> Icons.Rounded.Gavel
            VIOLENCE -> Icons.Rounded.Dangerous
            CSAM -> Icons.Rounded.ChildCare
        }

    fun title(strings: VibeStrings): String = when (this) {
        SPAM -> strings.reportReasonSpam
        FRAUD -> strings.reportReasonFraud
        FAKE_ACCOUNT -> strings.reportReasonFakeAccount
        MISINFORMATION -> strings.reportReasonMisinfo
        HARASSMENT -> strings.reportReasonHarassment
        HATE_SPEECH -> strings.reportReasonHate
        PORNOGRAPHY -> strings.reportReasonPorn
        DRUGS -> strings.reportReasonDrugs
        WEAPONS -> strings.reportReasonWeapons
        VIOLENCE -> strings.reportReasonViolence
        CSAM -> strings.reportReasonCsam
    }

    fun description(strings: VibeStrings): String = when (this) {
        SPAM -> strings.reportReasonSpamDesc
        FRAUD -> strings.reportReasonFraudDesc
        FAKE_ACCOUNT -> strings.reportReasonFakeAccountDesc
        MISINFORMATION -> strings.reportReasonMisinfoDesc
        HARASSMENT -> strings.reportReasonHarassmentDesc
        HATE_SPEECH -> strings.reportReasonHateDesc
        PORNOGRAPHY -> strings.reportReasonPornDesc
        DRUGS -> strings.reportReasonDrugsDesc
        WEAPONS -> strings.reportReasonWeaponsDesc
        VIOLENCE -> strings.reportReasonViolenceDesc
        CSAM -> strings.reportReasonCsamDesc
    }

    companion object {
        fun fromId(id: String): ReportReason? = entries.firstOrNull { it.id == id }
    }
}

private enum class ReportStep { REASON, DETAILS, SENT }

@Composable
private fun ReportSeverity.accent(): Color = when (this) {
    ReportSeverity.NORMAL -> VibePrimary
    ReportSeverity.HIGH -> Color(0xFFFF9800)
    ReportSeverity.CRITICAL -> MaterialTheme.colorScheme.error
}

/* ------------------------------------------------------------------ */
/*  Dialog                                                            */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val strings = LocalVibeStrings.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var step by remember { mutableStateOf(ReportStep.REASON) }
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { ReportDragHandle() }
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(260)) { full -> dir * full / 3 } +
                    fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(260)) { full -> -dir * full / 3 } +
                        fadeOut(tween(160)))
            },
            label = "report-step"
        ) { current ->
            when (current) {
                ReportStep.REASON -> ReasonStep(
                    strings = strings,
                    onClose = onDismiss,
                    onPick = { reason ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedReason = reason
                        step = ReportStep.DETAILS
                    }
                )

                ReportStep.DETAILS -> selectedReason?.let { reason ->
                    DetailsStep(
                        strings = strings,
                        reason = reason,
                        comment = comment,
                        onCommentChange = { if (it.length <= REPORT_COMMENT_LIMIT) comment = it },
                        onBack = { step = ReportStep.REASON },
                        onSubmit = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSubmit(reason.id, comment.trim())
                            step = ReportStep.SENT
                        }
                    )
                }

                ReportStep.SENT -> SentStep(strings = strings, onDone = onDismiss)
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Step 1 - reason picker                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun ReasonStep(
    strings: VibeStrings,
    onClose: () -> Unit,
    onPick: (ReportReason) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        StepHeader(
            icon = Icons.Rounded.Flag,
            accent = MaterialTheme.colorScheme.error,
            stepLabel = strings.reportStepLabel(1, 2),
            title = strings.reportTitle,
            subtitle = strings.reportSubtitle,
            trailing = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = strings.a11yReportClose,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        )

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .verticalScroll(rememberScrollState())
        ) {
            ReportReason.entries.forEachIndexed { index, reason ->
                if (index > 0) RowSeparator()
                ReasonRow(reason = reason, strings = strings, onClick = { onPick(reason) })
            }
        }
    }
}

@Composable
private fun ReasonRow(
    reason: ReportReason,
    strings: VibeStrings,
    onClick: () -> Unit
) {
    val accent = reason.severity.accent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = reason.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reason.title(strings),
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = reason.description(strings),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Step 2 - details                                                  */
/* ------------------------------------------------------------------ */

@Composable
private fun DetailsStep(
    strings: VibeStrings,
    reason: ReportReason,
    comment: String,
    onCommentChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val accent = reason.severity.accent()
    val nearLimit = comment.length >= REPORT_COMMENT_WARN_AT

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.backBtn,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.reportStepLabel(2, 2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = accent
                )
                Text(
                    text = strings.reportDetailsTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Recap of the chosen reason; tapping it goes back to the list.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(accent.copy(alpha = 0.10f))
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(reason.icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = reason.title(strings),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = strings.reportChangeReason,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }

        if (reason.severity == ReportSeverity.CRITICAL) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f))
                    .padding(14.dp)
            ) {
                Icon(
                    Icons.Rounded.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = strings.reportCriticalNotice,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = strings.reportDetailsHint,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            placeholder = {
                Text(
                    text = strings.reportCommentPlaceholder,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp),
            maxLines = 6,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                cursorColor = accent
            )
        )

        Text(
            text = strings.reportCommentCounter(comment.length, REPORT_COMMENT_LIMIT),
            fontSize = 11.sp,
            fontWeight = if (nearLimit) FontWeight.SemiBold else FontWeight.Normal,
            color = if (nearLimit) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 6.dp)
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
        ) {
            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(text = strings.reportSubmitBtn, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Step 3 - confirmation                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun SentStep(
    strings: VibeStrings,
    onDone: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(2400)
        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 12.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(VibePrimary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = VibePrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = strings.reportSentTitle,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = strings.reportSentDesc,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onDone) {
            Text(
                text = strings.reportDoneBtn,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = VibePrimary
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Shared pieces                                                     */
/* ------------------------------------------------------------------ */

@Composable
private fun StepHeader(
    icon: ImageVector,
    accent: Color,
    stepLabel: String,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stepLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = accent
                )
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun RowSeparator() {
    Box(
        modifier = Modifier
            .padding(start = 68.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    )
}

@Composable
private fun ReportDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
        )
    }
}