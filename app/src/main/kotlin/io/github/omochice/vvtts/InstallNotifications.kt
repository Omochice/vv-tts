package io.github.omochice.vvtts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Builds the notifications that show the asset download outside the app. */
internal class InstallNotifications(private val context: Context) {
  /** Registers the channel the notifications are posted to; repeating it changes nothing. */
  fun createChannel() {
    // Low importance, because a sound on every progress update would be noise for a long download.
    val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  /** Shows a download under way, at [percent] or of unknown extent while it is null. */
  fun running(percent: Int?): Notification =
      builder()
          .setContentTitle(context.getString(R.string.notification_running))
          .setProgress(100, percent ?: 0, percent == null)
          .build()

  /** Shows that the assets are installed. */
  fun installed(): Notification =
      builder()
          .setContentTitle(context.getString(R.string.notification_done))
          .setAutoCancel(true)
          .build()

  /** Shows that the download failed for [reason], with what the user can do about it. */
  fun failed(reason: FailureReason): Notification {
    val message = context.getString(reason.messageRes())
    return builder()
        .setContentTitle(context.getString(R.string.notification_failed))
        .setContentText(message)
        .setStyle(Notification.BigTextStyle().bigText(message))
        .setAutoCancel(true)
        .build()
  }

  private fun builder(): Notification.Builder =
      Notification.Builder(context, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_download)
          .setOnlyAlertOnce(true)
          .setContentIntent(
              PendingIntent.getActivity(
                  context,
                  0,
                  Intent(context, SettingsActivity::class.java),
                  PendingIntent.FLAG_IMMUTABLE,
              )
          )

  private companion object {
    const val CHANNEL_ID = "asset-download"
  }
}
