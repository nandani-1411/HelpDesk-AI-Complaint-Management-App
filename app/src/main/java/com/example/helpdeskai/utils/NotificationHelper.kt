package com.example.helpdeskai.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.helpdeskai.MainActivity
import com.example.helpdeskai.R

/**
 * Utility to manage real-time grievance notification alerts.
 * Works seamlessly for single-phone demos and viva presentations.
 */
object NotificationHelper {

    const val CHANNEL_ID = "helpdesk_complaint_alerts"
    private const val CHANNEL_NAME = "HelpDesk Complaint Updates"
    private const val CHANNEL_DESC = "Notifications for complaint status updates and support replies"

    /**
     * Initializes the High-Priority notification channel required on Android 8.0+.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Triggered when a student files a new complaint.
     */
    fun sendComplaintSubmittedNotification(context: Context, complaintId: Long, title: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            complaintId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📋 Complaint Submitted")
            .setContentText("\"$title\" has been recorded and routed to the Support Team.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("\"$title\" has been recorded successfully. The Support Team has been notified.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(complaintId.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission check for Android 13+
        }
    }

    /**
     * Triggered when Support Team changes status or enters a reply.
     * Appears immediately as a heads-up system banner on the phone!
     */
    fun sendStatusUpdateNotification(
        context: Context,
        complaintId: Long,
        title: String,
        newStatus: String,
        officerReply: String?
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (complaintId + 1000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusEmoji = when (newStatus.lowercase()) {
            "resolved" -> "✅"
            "in progress" -> "🔄"
            else -> "⏳"
        }

        val bigTextContent = buildString {
            append("Status: ").append(newStatus).append("\n")
            append("Complaint: \"").append(title).append("\"\n")
            if (!officerReply.isNullOrBlank()) {
                append("Support Team: ").append(officerReply)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$statusEmoji Your complaint is now $newStatus.")
            .setContentText(if (!officerReply.isNullOrBlank()) "Support Team: $officerReply" else "Status updated for: $title")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify((complaintId + 1000).toInt(), notification)
        } catch (_: SecurityException) {
            // Permission check for Android 13+
        }
    }
}
