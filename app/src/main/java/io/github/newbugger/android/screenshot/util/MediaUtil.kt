/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.util

import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import io.github.newbugger.android.screenshot.core.ScreenshotService


object MediaUtil {

    fun displayMetrics(): DisplayMetrics = mDisplayMetrics().get()
    private fun mDisplayMetrics() = object: Singleton<DisplayMetrics>() {
        override fun create(): DisplayMetrics {
            return context().resources.displayMetrics as DisplayMetrics
        }
    }

    // need its getter each time for latest screen width
    fun display(): Display = mDisplay()
    private fun mDisplay(): Display =
        if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.R)) {
            context().display as Display
        } else {
            mWindowManager().defaultDisplay as Display
        }

    // need its getter each time for latest screen width
    fun windowManager(): WindowManager = mWindowManager()
    private fun mWindowManager(): WindowManager =
        context().getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun mediaProjectionManager(): MediaProjectionManager = mMediaProjectionManager().get()
    private fun mMediaProjectionManager() = object: Singleton<MediaProjectionManager>() {
        override fun create(): MediaProjectionManager {
            return context().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }
    }

    private fun context(): Context = ScreenshotService.Companion.Val.context()

}
