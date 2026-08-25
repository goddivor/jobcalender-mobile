package tg.goddivor.jobcalender.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import tg.goddivor.jobcalender.MainActivity
import tg.goddivor.jobcalender.R

/**
 * Posts the reminder as a system notification, which is the whole point: it has to reach the user
 * when the app is closed and the phone is in a pocket. That is what was missing the day a meeting
 * was lost.
 */
object ReminderNotifier {

    const val CHANNEL_ID = "appointments"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun post(context: Context, title: String, body: String, notificationId: Int) {
        if (!canPost(context)) return
        ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(notificationId, notification) }
    }
}
