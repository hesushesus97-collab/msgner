package com.flasskdev.vibe.data

object PowerSavingPolicy {
    fun active(manual: Boolean, automatic: Boolean, batteryPercent: Int?, threshold: Int): Boolean =
        manual || (automatic && batteryPercent != null && batteryPercent in 0..100 && batteryPercent <= threshold.coerceIn(1, 100))
}
