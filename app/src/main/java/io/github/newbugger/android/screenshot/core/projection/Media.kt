/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core.projection

import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import io.github.newbugger.android.screenshot.util.PreferenceUtil
import io.github.newbugger.android.screenshot.util.Singleton


class Media(ctx: Context) {

    fun displayMetrics(): DisplayMetrics = mDisplayMetrics(context).get()
    private fun mDisplayMetrics(context: Context) = object: Singleton<DisplayMetrics>() {
        override fun create(): DisplayMetrics {
            return context.resources.displayMetrics as DisplayMetrics
        }
    }

    // need its getter each time for latest screen width
    fun display(): Display = mDisplay(context)
    private fun mDisplay(context: Context): Display =
        if (PreferenceUtil.checkSdkVersion(
                Build.VERSION_CODES.R
            )
        ) {
            context.display as Display
        } else {
            windowManager().defaultDisplay as Display
        }

    // need its getter each time for latest screen width
    fun windowManager(): WindowManager = mWindowManager(context)
    private fun mWindowManager(context: Context): WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun mediaProjectionManager(): MediaProjectionManager = mMediaProjectionManager(context).get()
    private fun mMediaProjectionManager(context: Context) = object: Singleton<MediaProjectionManager>() {
        override fun create(): MediaProjectionManager {
            return context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }
    }

    private val context: Context = ctx

    companion object {
        fun getInstance(ctx: Context): Media {
            return Media(
                ctx
            )
        }
    }

}
