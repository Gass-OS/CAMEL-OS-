package com.example.camel.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CamelNotificationSeverity {
    INFO,
    SUCCESS,
    WARNING
}

data class CamelNotification(
    val id: String = UUID.randomUUID().toString(),
    val sourceApp: CamelAppId?,
    val sourceLabel: String,
    val title: String,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val severity: CamelNotificationSeverity = CamelNotificationSeverity.INFO,
    val isUnread: Boolean = true
) {
    fun formattedTime(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestampMillis))
    }
}
