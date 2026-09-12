package com.antigravity.go.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

object AppLogger {
    private const val TAG = "AntigravityGo"
    private const val MAX_LOG_LINES = 1000
    private const val PREFS_NAME = "antigravity_crash_log"
    private const val KEY_LAST_CRASH = "last_crash"

    private val logBuffer = ConcurrentLinkedDeque<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        log("INIT", "=== Antigravity Go v0.0.4-beta Log Session ===")
        log("INIT", "Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
        log("INIT", "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        log("INIT", "Build ID: ${Build.DISPLAY}")

        // Check if there was a previous crash recorded
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCrash = prefs.getString(KEY_LAST_CRASH, null)
        if (!lastCrash.isNullOrBlank()) {
            log("CRASH_LOG", "=== PREVIOUS CRASH TRACE FOUND ===\n$lastCrash")
        }

        // Install uncaught exception handler
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = Log.getStackTraceString(throwable)
            val crashDetails = "[${dateFormat.format(Date())}] FATAL CRASH on thread '${thread.name}':\n$stackTrace"
            log("FATAL", crashDetails)
            try {
                prefs.edit().putString(KEY_LAST_CRASH, crashDetails).commit()
            } catch (_: Exception) {}
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun d(tag: String, msg: String) = log("DEBUG/$tag", msg)
    fun i(tag: String, msg: String) = log("INFO/$tag", msg)
    fun w(tag: String, msg: String) = log("WARN/$tag", msg)
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        val extra = if (tr != null) "\n" + Log.getStackTraceString(tr) else ""
        log("ERROR/$tag", "$msg$extra")
    }

    fun log(tag: String, msg: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp] [$tag] $msg"
        Log.d(TAG, entry)
        logBuffer.add(entry)
        while (logBuffer.size > MAX_LOG_LINES) {
            logBuffer.pollFirst()
        }
    }

    fun getLogs(): String = logBuffer.joinToString("\n")

    fun getLogCount(): Int = logBuffer.size

    fun getCrashLog(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_CRASH, null)
    }

    fun clearLogs(context: Context) {
        logBuffer.clear()
        log("SYS", "Logs cleared by user")
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply()
        } catch (_: Exception) {}
    }

    fun copyLogsToClipboard(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Antigravity Go Logs", getLogs())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Logs copied to clipboard (${logBuffer.size} lines)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy logs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareLogs(context: Context, extraHeader: String = "") {
        try {
            val logText = if (extraHeader.isNotBlank()) "$extraHeader\n\n${getLogs()}" else getLogs()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Antigravity Go Diagnostics Log")
                putExtra(Intent.EXTRA_TEXT, logText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Antigravity Go Logs").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share logs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun collectSystemLogcat(maxLines: Int = 400): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "-t", maxLines.toString()))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sb = StringBuilder()
            sb.append("=== SYSTEM LOGCAT DUMP (Last $maxLines lines) ===\n")
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            reader.close()
            sb.toString()
        } catch (e: Exception) {
            "Failed to collect logcat: ${e.message}"
        }
    }
}
