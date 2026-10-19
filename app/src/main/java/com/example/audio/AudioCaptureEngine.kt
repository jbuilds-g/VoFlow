package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.data.DiagnosticLog
import com.example.data.SecurePreferences
import java.io.File

class AudioCaptureEngine(private val context: Context) {

    companion object {
        private const val TAG = "AudioCaptureEngine"
        const val SAMPLE_RATE_HZ = 16000
    }

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingStartTimeMs: Long = 0L

    var isRecording: Boolean = false
        private set

    var lastRecordingDurationMs: Long = 0L
        private set

    /**
     * Starts 16kHz mono recording. Opus-in-Ogg is experimental on Android 10+;
     * AAC/m4a remains the default and is used as a fallback if Opus setup fails.
     */
    @Synchronized
    fun startRecording(): Result<File> {
        if (isRecording) stopRecording()

        val requestOpus = SecurePreferences(context).isExperimentalOpusEnabled() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        if (requestOpus) {
            try {
                return startWithFormat(useOpus = true)
            } catch (e: Exception) {
                Log.w(TAG, "Experimental Opus recording unavailable; falling back to AAC", e)
                DiagnosticLog.add("Opus recorder unavailable; falling back to AAC")
                cleanup()
            }
        }

        return try {
            startWithFormat(useOpus = false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            cleanup()
            Result.failure(Exception("Could not initialize microphone: ${e.message}"))
        }
    }

    private fun startWithFormat(useOpus: Boolean): Result<File> {
        val extension = if (useOpus) "ogg" else "m4a"
        val file = File(context.cacheDir, "voflow_audio_${System.currentTimeMillis()}.$extension")
        file.delete()
        outputFile = file

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                if (useOpus) {
                    setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                    setAudioSamplingRate(SAMPLE_RATE_HZ)
                    setAudioChannels(1)
                    setAudioEncodingBitRate(24000)
                } else {
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(SAMPLE_RATE_HZ)
                    setAudioChannels(1)
                    setAudioEncodingBitRate(64000)
                }
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            try { recorder.release() } catch (_: Exception) {}
            file.delete()
            outputFile = null
            throw e
        }

        mediaRecorder = recorder
        isRecording = true
        recordingStartTimeMs = SystemClock.elapsedRealtime()
        val format = if (useOpus) "experimental Opus/Ogg" else "AAC/m4a"
        Log.d(TAG, "$format recording started -> ${file.absolutePath}")
        DiagnosticLog.add("Recording format: $format")
        return Result.success(file)
    }

    /**
     * Stops audio capture and returns the generated m4a or Ogg file.
     * Keeps a minimum recording duration so MediaRecorder can flush encoded frames.
     */
    @Synchronized
    fun stopRecording(): File? {
        if (!isRecording) return null

        return try {
            val elapsedMs = SystemClock.elapsedRealtime() - recordingStartTimeMs
            // MediaRecorder requires at least ~500ms of recorded data, otherwise stop() throws
            if (elapsedMs < 600) {
                val sleepNeeded = 600 - elapsedMs
                try {
                    Thread.sleep(sleepNeeded)
                } catch (ignored: InterruptedException) {
                }
            }

            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    Log.w(TAG, "MediaRecorder stop called prematurely or failed", e)
                }
                release()
            }
            isRecording = false
            mediaRecorder = null

            lastRecordingDurationMs = elapsedMs.coerceAtLeast(600L)
            val file = outputFile
            if (file != null && file.exists() && file.length() > 0) {
                Log.d(TAG, "Recording completed. File: ${file.name}, Size: ${file.length()} bytes")
                file
            } else {
                Log.w(TAG, "Recording stopped but output file is empty (0 bytes) or missing")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recorder", e)
            cleanup()
            null
        }
    }

    @Synchronized
    fun cancelRecording() {
        cleanup()
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) {
                mediaRecorder?.maxAmplitude ?: 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaRecorder = null
        isRecording = false
        outputFile?.let {
            if (it.exists()) it.delete()
        }
        outputFile = null
    }
}
