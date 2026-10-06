package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.math.max

data class TranscriptionHistoryEntry(
    val id: String,
    val timestampMs: Long,
    val text: String,
    val audioFileName: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val model: String,
    val mode: String
)

object TranscriptionHistory {
    const val RETENTION_DAYS = 30L
    private const val DIRECTORY_NAME = "transcription_history"
    private const val INDEX_FILE_NAME = "index.json"

    @Volatile
    private var historyDirectory: File? = null

    @Synchronized
    fun initialize(context: Context) {
        val directory = File(context.applicationContext.filesDir, DIRECTORY_NAME)
        if (!directory.exists()) directory.mkdirs()
        historyDirectory = directory
        cleanupExpired()
    }

    @Synchronized
    fun save(
        context: Context,
        sourceAudioFile: File,
        text: String,
        durationMs: Long,
        model: String,
        mode: String,
        timestampMs: Long = System.currentTimeMillis()
    ): TranscriptionHistoryEntry? {
        initializeIfNeeded(context)

        if (!sourceAudioFile.exists() || sourceAudioFile.length() <= 0L || text.isBlank()) return null

        val directory = requireNotNull(historyDirectory)
        cleanupExpired(timestampMs)

        val id = UUID.randomUUID().toString()
        val audioFileName = "$id.m4a"
        val destination = File(directory, audioFileName)
        val entry = TranscriptionHistoryEntry(
            id = id,
            timestampMs = timestampMs,
            text = text.trim(),
            audioFileName = audioFileName,
            durationMs = max(0L, durationMs),
            fileSizeBytes = sourceAudioFile.length(),
            model = model.removePrefix("models/"),
            mode = mode.lowercase()
        )

        try {
            sourceAudioFile.copyTo(destination, overwrite = false)
            val updatedEntries = readEntries().filter { it.timestampMs > timestampMs - retentionWindowMs() } + entry
            writeEntries(updatedEntries.sortedByDescending { it.timestampMs })
            sourceAudioFile.delete()
            return entry
        } catch (e: Exception) {
            destination.delete()
            return null
        }
    }

    @Synchronized
    fun getAll(context: Context): List<TranscriptionHistoryEntry> {
        initializeIfNeeded(context)
        cleanupExpired()
        return readEntries().sortedByDescending { it.timestampMs }
    }

    @Synchronized
    fun delete(context: Context, id: String): Boolean {
        initializeIfNeeded(context)
        val entries = readEntries()
        val entry = entries.firstOrNull { it.id == id } ?: return false
        val directory = requireNotNull(historyDirectory)
        File(directory, entry.audioFileName).delete()
        writeEntries(entries.filterNot { it.id == id })
        return true
    }

    @Synchronized
    fun clear(context: Context) {
        initializeIfNeeded(context)
        val directory = requireNotNull(historyDirectory)
        readEntries().forEach { File(directory, it.audioFileName).delete() }
        directory.listFiles()
            ?.filter { it.isFile && it.name != INDEX_FILE_NAME }
            ?.forEach { it.delete() }
        writeEntries(emptyList())
    }

    @Synchronized
    fun cleanupExpired(nowMs: Long = System.currentTimeMillis()) {
        val directory = historyDirectory ?: return
        val cutoff = nowMs - retentionWindowMs()
        val entries = readEntries()
        val retained = entries.filter { it.timestampMs > cutoff }

        entries.filter { it.timestampMs <= cutoff }
            .forEach { File(directory, it.audioFileName).delete() }

        val referencedAudio = retained.mapTo(HashSet()) { it.audioFileName }
        directory.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".m4a", ignoreCase = true) && it.name !in referencedAudio }
            ?.forEach { it.delete() }

        if (retained.size != entries.size) {
            writeEntries(retained.sortedByDescending { it.timestampMs })
        }
    }

    private fun initializeIfNeeded(context: Context) {
        if (historyDirectory == null) initialize(context)
    }

    private fun retentionWindowMs(): Long = RETENTION_DAYS * 24L * 60L * 60L * 1000L

    private fun indexFile(): File = File(requireNotNull(historyDirectory), INDEX_FILE_NAME)

    private fun readEntries(): List<TranscriptionHistoryEntry> {
        val file = indexFile()
        if (!file.exists()) return emptyList()

        return try {
            val array = JSONArray(file.readText())
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val objectJson = array.optJSONObject(i) ?: continue
                    val entry = objectJson.toEntry() ?: continue
                    if (File(requireNotNull(historyDirectory), entry.audioFileName).exists()) {
                        add(entry)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeEntries(entries: List<TranscriptionHistoryEntry>) {
        val directory = requireNotNull(historyDirectory)
        val tempFile = File(directory, "$INDEX_FILE_NAME.tmp")
        val array = JSONArray()

        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("timestampMs", entry.timestampMs)
                put("text", entry.text)
                put("audioFileName", entry.audioFileName)
                put("durationMs", entry.durationMs)
                put("fileSizeBytes", entry.fileSizeBytes)
                put("model", entry.model)
                put("mode", entry.mode)
            })
        }

        tempFile.writeText(array.toString())
        try {
            Files.move(
                tempFile.toPath(),
                indexFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            if (!tempFile.renameTo(indexFile())) {
                tempFile.delete()
                throw IllegalStateException("Could not update transcription history index.")
            }
        }
    }

    private fun JSONObject.toEntry(): TranscriptionHistoryEntry? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val audioFileName = optString("audioFileName").takeIf { it.endsWith(".m4a", ignoreCase = true) } ?: return null
        val text = optString("text").takeIf { it.isNotBlank() } ?: return null
        return TranscriptionHistoryEntry(
            id = id,
            timestampMs = optLong("timestampMs"),
            text = text,
            audioFileName = audioFileName,
            durationMs = optLong("durationMs"),
            fileSizeBytes = optLong("fileSizeBytes"),
            model = optString("model", "unknown"),
            mode = optString("mode", "smart")
        )
    }
}
