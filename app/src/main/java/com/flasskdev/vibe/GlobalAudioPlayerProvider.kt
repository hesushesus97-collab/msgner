package com.flasskdev.vibe

import androidx.compose.runtime.compositionLocalOf
import com.flasskdev.vibe.ui.viewmodels.GlobalAudioPlayerViewModel

val LocalGlobalAudioPlayer = compositionLocalOf<GlobalAudioPlayerViewModel> {
    error("No GlobalAudioPlayerViewModel provided")
}