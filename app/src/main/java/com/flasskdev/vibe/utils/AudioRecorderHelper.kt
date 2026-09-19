package com.flasskdev.vibe.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingDuration = MutableStateFlow(0L) // In milliseconds
    val recordingDuration: StateFlow<Long> = _recordingDuration

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startRecording(): File? {
        try {
            outputFile = File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            _isRecording.value = true
            _recordingDuration.value = 0L
            startTimer()
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            return null
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        return try {
            mediaRecorder?.stop()
            val result = outputFile
            cleanup()
            result
        } catch (e: RuntimeException) {
            // Can happen if stop() is called immediately after start()
            e.printStackTrace()
            outputFile?.delete()
            cleanup()
            null
        }
    }

    fun cancelRecording() {
        if (!_isRecording.value) return
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputFile?.delete()
            cleanup()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (_isRecording.value) {
                _recordingDuration.value = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    private fun cleanup() {
        _isRecording.value = false
        timerJob?.cancel()
        timerJob = null
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
    }
}