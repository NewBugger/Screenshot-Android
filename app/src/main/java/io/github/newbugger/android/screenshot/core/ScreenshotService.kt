/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import io.github.newbugger.android.screenshot.util.NotificationUtil
import io.github.newbugger.android.screenshot.util.PreferenceUtil
import io.github.newbugger.android.screenshot.util.ProjectionUtil


class ScreenshotService : Service() {

    private fun startInForeground() {
        // https://developer.android.com/guide/components/services#Foreground
        // https://stackoverflow.com/questions/61276730/media-projections-require-
        // a-foreground-service-of-type-serviceinfo-foreground-se
        NotificationUtil.createNotificationChannel()
        if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
            startForeground(
                NotificationUtil.notificationsNotificationId,
                NotificationUtil.notificationBuilder(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(
                NotificationUtil.notificationsNotificationId,
                NotificationUtil.notificationBuilder()
            )
        }
    }

    private fun stopInForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopInForeground()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Val.context = this
        if (intent.getBooleanExtra("capture", true)) {  // if run Capture intent
            ProjectionUtil.createWorkerTasks()
        } else {
            startInForeground()
        }
        return START_NOT_STICKY
    }

    companion object {
        fun startForeground(context: Context) {
            Intent(context, ScreenshotService::class.java).also { intent ->
                intent.putExtra("capture", false)
                context.startService(intent)
            }
        }

        fun startCapture(context: Context) {
            Intent(context, ScreenshotService::class.java).also { intent ->
                intent.putExtra("capture", true)
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            Intent(context, ScreenshotService::class.java).also { intent ->
                context.stopService(intent)
            }
        }

        object Val {
            lateinit var context: Context
            fun context(): Context = context
        }
    }

}
