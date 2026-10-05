package com.example.data

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import android.os.Process
import android.os.ThermalManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import rikka.shizuku.Shizuku

/** Lightweight, opt-in performance diagnostics. */
object DiagnosticsCollector {
    private const val SAMPLE_INTERVAL_MS = 500L
    private var session: Session? = null
    private var samplingJob: Job? = null

    private data class Sample(val cpuPercent: Double, val pssKb: Int)
    private data class Session(
        val label: String,
        val startedAtMs: Long,
        val startCpuMs: Long,
        val startPssKb: Int,
        val samples: MutableList<Sample> = mutableListOf()
    )

    @Synchronized
    fun start(context: Context, label: String) {
        stop()
        session = Session(label, SystemClock.elapsedRealtime(), Process.getElapsedCpuTime(), Debug.getPss().toInt())
        DiagnosticLog.add("Diagnostics started: " + label)
        DiagnosticLog.add(deviceSnapshot(context))
        samplingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) { sample(context); delay(SAMPLE_INTERVAL_MS) }
        }
    }

    @Synchronized
    fun stop() {
        samplingJob?.cancel()
        samplingJob = null
        val active = session ?: return
        val durationMs = (SystemClock.elapsedRealtime() - active.startedAtMs).coerceAtLeast(1L)
        val cpuMs = Process.getElapsedCpuTime() - active.startCpuMs
        val avgCpu = cpuMs.toDouble() / durationMs * 100.0
        val peakCpu = active.samples.maxOfOrNull { it.cpuPercent } ?: avgCpu
        val endPss = Debug.getPss().toInt()
        val peakPss = active.samples.maxOfOrNull { it.pssKb } ?: endPss
        DiagnosticLog.add("Diagnostics finished: " + active.label + "; duration=" + durationMs + "ms; cpu=" + format(avgCpu) + "% avg/" + format(peakCpu) + "% peak; PSS=" + endPss + "KB (start=" + active.startPssKb + "KB, peak=" + peakPss + "KB, delta=" + (endPss - active.startPssKb) + "KB)")
        session = null
    }

    fun mark(label: String) { DiagnosticLog.add("Diagnostic checkpoint: " + label) }

    fun logAudioFile(file: File) { DiagnosticLog.add("Audio file: " + file.length() + " bytes") }

    private fun sample(context: Context) {
        val active = synchronized(this) { session } ?: return
        val elapsedMs = (SystemClock.elapsedRealtime() - active.startedAtMs).coerceAtLeast(1L)
        val cpuPercent = (Process.getElapsedCpuTime() - active.startCpuMs).toDouble() / elapsedMs * 100.0
        synchronized(this) { session?.samples?.add(Sample(cpuPercent, Debug.getPss().toInt())) }
    }

    private fun deviceSnapshot(context: Context): String {
        val battery = context.getSystemService(BatteryManager::class.java)
        val level = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val temp = if (rawTemp != Int.MIN_VALUE) rawTemp / 10.0 else null
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getSystemService(ThermalManager::class.java)?.currentThermalStatus else null
        val activity = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also { activity?.getMemoryInfo(it) }
        return "Device: Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "); battery=" + (level?.let { "$it%" } ?: "n/a") + "; temp=" + (temp?.let { format(it) + "C" } ?: "n/a") + "; thermal=" + (thermal?.toString() ?: "n/a") + "; availMem=" + (memory.availMem / 1024 / 1024) + "MB; Shizuku=" + shizukuStatus()
    }

    private fun shizukuStatus(): String = try {
        if (!Shizuku.pingBinder()) return "not-running"
        if (Shizuku.isPreV11()) return "pre-v11"
        val granted = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        val uid = runCatching { Shizuku.getUid() }.getOrDefault(-1)
        val selinux = runCatching { Shizuku.getSELinuxContext() }.getOrNull()
        "running,permission=" + if (granted) "granted" else "not-granted" + ",uid=" + uid + ",context=" + (selinux ?: "n/a")
    } catch (_: Exception) { "unavailable" }

    private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)
}