/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import io.github.newbugger.android.screenshot.MainActivity
import io.github.newbugger.android.screenshot.R
import io.github.newbugger.android.screenshot.ScreenshotActivity


object NotificationUtil {

    fun notificationBuilder(context: Context): Notification {
        val notificationIntentMain: PendingIntent = notificationIntent(context, true)
        val notificationIntentScreenshot: PendingIntent = notificationIntent(context, false)
        // https://developer.android.com/training/notify-user/build-notification#Actions
        // https://stackoverflow.com/a/37134139
        val notificationAction: Notification.Action = Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_snooze),
            context.getString(R.string.notification_button),
            notificationIntentScreenshot
        ).build()
        return Notification.Builder(context, notificationsCHANNELID)
            .setSmallIcon(notificationsSmallIcon)
            .setContentTitle(notificationsTextTitle)
            .setContentText(notificationsTextContent)
            .setContentIntent(notificationIntentMain)
            .addAction(notificationAction)
            .build()
    }

    private fun notificationIntent(context: Context, yes: Boolean): PendingIntent {
        return if (yes) {
            Intent(context, MainActivity::class.java)
                .let { notificationPendingIntent ->
                    PendingIntent.getActivity(
                        context,
                        0,
                        notificationPendingIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
        } else {
            Intent(context, ScreenshotActivity::class.java)
                .let { notificationPendingIntent ->
                    PendingIntent.getActivity(
                        context,
                        0,
                        notificationPendingIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT
                        // the current one should be canceled before generating a new one
                    )
                }
        }
    }

    fun notificationChannel(): NotificationChannel {
        return NotificationChannel(
            notificationsCHANNELID,
            notificationsChannelName,
            notificationsImportance
        ).apply {
            description = notificationsChannelDescription
        }
    }

    private const val notificationsCHANNELID = "Foreground"
    private const val notificationsTextTitle = "Screenshot Service"
    private const val notificationsTextContent = "tap to take a Screenshot"
    private const val notificationsChannelName = "Foreground"
    private const val notificationsChannelDescription = "Foreground Service"
    private const val notificationsSmallIcon = R.mipmap.ic_launcher_round
    private const val notificationsImportance = NotificationManager.IMPORTANCE_LOW
    const val notificationsNotificationId = 1038

}
