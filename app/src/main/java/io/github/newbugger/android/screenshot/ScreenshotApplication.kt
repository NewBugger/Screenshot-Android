package io.github.newbugger.android.screenshot

import android.annotation.SuppressLint
import android.app.Application
import io.github.newbugger.android.screenshot.util.NotificationUtil


@SuppressLint("unused")
class ScreenshotApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannel(this)
    }

}
