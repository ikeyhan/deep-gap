package ir.atom313.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.atom313.app.MainActivity
import ir.atom313.app.R
import ir.atom313.app.domain.model.AppNotification
import ir.atom313.app.domain.model.NotificationType
import ir.atom313.app.navigation.DeepLinks
import javax.inject.Inject
import javax.inject.Singleton

/**
 * نمایش اعلان سیستمی. مستقل از منبع اعلان است: همگام‌سازی دوره‌ای امروز و پوش FCM در آینده
 * هر دو از همین کلاس استفاده می‌کنند. لمس اعلان صفحهٔ مربوط را با پیوند داخلی باز می‌کند.
 */
@Singleton
class NotificationPublisher @Inject constructor(@ApplicationContext private val context: Context) {

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_ORDERS, context.getString(R.string.channel_orders), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = context.getString(R.string.channel_orders_desc) },
                NotificationChannel(CHANNEL_SUPPORT, context.getString(R.string.channel_support), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = context.getString(R.string.channel_support_desc) },
            ),
        )
    }

    fun canPost(): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(n: AppNotification) {
        if (!canPost()) return
        val channel = if (n.type == NotificationType.SUPPORT_REPLY || n.type == NotificationType.OFFICE_REPLY) CHANNEL_SUPPORT else CHANNEL_ORDERS
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.forNotification(n)), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(context, n.id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_green))
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setCategory(if (channel == CHANNEL_ORDERS) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_MESSAGE)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(n.id.toInt(), notification)
        } catch (_: SecurityException) { /* مجوز در لحظه لغو شد */ }
    }

    companion object {
        const val CHANNEL_ORDERS = "orders"
        const val CHANNEL_SUPPORT = "support"
    }
}
