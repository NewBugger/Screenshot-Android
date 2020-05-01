/*
 * Copyright (c) 2018-2020.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
// import android.media.projection.MediaProjection
// import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder


class ScreenshotService : Service() {

    // prepare a method for clients ScreenshotActivity
    /* fun createMediaProjection(mMediaProjectionManager: MediaProjectionManager, resultCode: Int, data: Intent): MediaProjection {
        return mMediaProjectionManager.getMediaProjection(resultCode, data)
    } */

    private fun startInForeground() {
        val notificationsCHANNELID = "Foreground"
        val notificationsTextTitle = "Screenshot Service"
        val notificationsTextContent = "tap to take a Screenshot"
        val notificationsChannelName = "Foreground"
        val notificationsChannelDescription = "Foreground Service"
        val notificationsSmallIcon = R.mipmap.ic_launcher_round
        val notificationsImportance = NotificationManager.IMPORTANCE_LOW
        val notificationsNotificationId = 1038

        val notificationIntentScreenshot: PendingIntent =
            Intent(this, ScreenshotActivity::class.java)
                .let { notificationPendingIntent ->
                    PendingIntent.getActivity(
                        this,
                        0,
                        notificationPendingIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT  // the current one should be canceled before generating a new one
                    )
                }

        val notificationIntentMain: PendingIntent =
            Intent(this, MainActivity::class.java)
                .let { notificationPendingIntent ->
                    PendingIntent.getActivity(
                        this,
                        0,
                        notificationPendingIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

        // https://developer.android.com/training/notify-user/build-notification#Actions
        // https://stackoverflow.com/a/37134139
        val notificationAction: Notification.Action = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_snooze),
            getString(R.string.notification_button),
            notificationIntentScreenshot)
            .build()

        val notificationBuilder: Notification = Notification.Builder(this, notificationsCHANNELID)
            .setSmallIcon(notificationsSmallIcon)
            .setContentTitle(notificationsTextTitle)
            .setContentText(notificationsTextContent)
            .setContentIntent(notificationIntentMain)
            .addAction(notificationAction)
            .build()

        val channel: NotificationChannel = NotificationChannel(
            notificationsCHANNELID,
            notificationsChannelName,
            notificationsImportance
        ).apply {
            description = notificationsChannelDescription
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)

        // https://developer.android.com/guide/components/services#Foreground
        // https://stackoverflow.com/questions/61276730/media-projections-require-a-foreground-service-of-type-serviceinfo-foreground-se
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationsNotificationId, notificationBuilder, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(notificationsNotificationId, notificationBuilder)
        }
    }

    private fun stopInForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // https://developer.android.com/guide/components/bound-services.html#Binder
    override fun onBind(intent: Intent): IBinder {
        return ServiceBinder()
    }
    internal inner class ServiceBinder : Binder() {
        val service: ScreenshotService
            get() = this@ScreenshotService
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startInForeground()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopInForeground()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ScreenshotService::class.java)
            context.startService(intent)
        }

        /* fun stop(context: Context) {
            val intent = Intent(context, ScreenshotService::class.java)
            context.stopService(intent)
        } */  // need stop Foreground opener
    }

}
