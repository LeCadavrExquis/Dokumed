package pl.fzar.dokumed.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import pl.fzar.dokumed.MainActivity // Assuming MainActivity is your app's entry point
import pl.fzar.dokumed.R

const val MEDICATION_REMINDER_CHANNEL_ID = "medication_reminder_channel"
const val MEDICATION_REMINDER_NOTIFICATION_ID = 1001

fun createMedicationReminderNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.getString(R.string.notification_channel_medication_reminder_name)
        val descriptionText = context.getString(R.string.notification_channel_medication_reminder_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(MEDICATION_REMINDER_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableLights(true)
            lightColor = android.graphics.Color.RED
            enableVibration(true)
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun showMedicationReminderNotification(context: Context, medicationName: String, dosage: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val notification = NotificationCompat.Builder(context, MEDICATION_REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_bell) // Replace with your notification icon
        .setContentTitle(context.getString(R.string.medication_reminder_notification_title))
        .setContentText(context.getString(R.string.medication_reminder_notification_text, medicationName, dosage))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(MEDICATION_REMINDER_NOTIFICATION_ID, notification)
}
