package com.speedcheck.android.util

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Configuration and secure dispatcher for sending in-app feedback
 * directly to a private Telegram bot/channel without opening external apps.
 */
object TelegramFeedbackConfig {
    // Configured Telegram Bot Token and Chat ID
    var BOT_TOKEN: String = "9033341874:AAFAtjqhOjkAuC_xCiLMb-jR0TPP6lt-ynI"
    var CHAT_ID: String = "572260323"
}

object TelegramFeedbackDispatcher {

    /**
     * Dispatches feedback to the configured Telegram bot.
     * Runs on Dispatchers.IO. Returns Result.success or Result.failure.
     */
    suspend fun sendFeedback(
        type: String,
        message: String,
        contact: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val botToken = TelegramFeedbackConfig.BOT_TOKEN.trim()
            val chatId = TelegramFeedbackConfig.CHAT_ID.trim()

            if (botToken.isEmpty() || botToken == "YOUR_TELEGRAM_BOT_TOKEN" ||
                chatId.isEmpty() || chatId == "YOUR_TELEGRAM_CHAT_ID"
            ) {
                return@withContext Result.failure(
                    IllegalStateException("Telegram bot credentials not configured.")
                )
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }.format(Date())

            val textMessage = buildString {
                appendLine("🚗 *SpeedCheck In-App Feedback*")
                appendLine()
                appendLine("📌 *Type:* $type")
                if (contact.isNotBlank()) {
                    appendLine("👤 *Contact:* $contact")
                } else {
                    appendLine("👤 *Contact:* _Not provided_")
                }
                appendLine("🕒 *Time:* $timestamp GMT")
                appendLine()
                appendLine("💬 *Message:*")
                appendLine(message)
                appendLine()
                appendLine("📱 *Diagnostics:*")
                appendLine("• App: SpeedCheck v1.0.3 (Build 15)")
                appendLine("• Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("• OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            }

            val requestUrl = URL("https://api.telegram.org/bot$botToken/sendMessage")
            val connection = (requestUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val jsonBody = JSONObject().apply {
                put("chat_id", chatId)
                put("text", textMessage)
                put("parse_mode", "Markdown")
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Telegram API error ($responseCode): $errorStream"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
