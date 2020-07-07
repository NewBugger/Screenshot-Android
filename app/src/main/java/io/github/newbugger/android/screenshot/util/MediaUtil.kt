/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
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

    fun display(): Display = mDisplay()
    private fun mDisplay(): Display =
        if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.R)) {
            context().display as Display
        } else {
            mWindowManager().defaultDisplay as Display
        }

    fun windowManager(): WindowManager = mWindowManager()
    private fun mWindowManager(): WindowManager =
        context().getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun virtualDisplay(onViewWidth: Int, onViewHeight:Int,
                       mMediaProjection: MediaProjection, mImageReader: ImageReader): VirtualDisplay =
        mVirtualDisplay(onViewWidth, onViewHeight, mMediaProjection, mImageReader)
    private fun mVirtualDisplay(onViewWidth: Int, onViewHeight:Int,
                                mMediaProjection: MediaProjection, mImageReader: ImageReader): VirtualDisplay =
        mMediaProjection.createVirtualDisplay(
            "screenshot",
            onViewWidth,
            onViewHeight,
            displayMetrics().densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            mImageReader.surface,
            null,
            null
        )

    fun imageReader(onViewWidth: Int, onViewHeight:Int): ImageReader =
        mImageReader(onViewWidth, onViewHeight)
    @SuppressLint("WrongConstant")
    private fun mImageReader(onViewWidth: Int, onViewHeight:Int): ImageReader =
        ImageReader.newInstance(
            onViewWidth,
            onViewHeight,
            PixelFormat.RGBA_8888,
            1
        )

    fun receiveMediaProjection(tMediaProjection: MediaProjection) {
        mMediaProjection = tMediaProjection
    }
    fun mediaProjection(): MediaProjection = mMediaProjection
    private lateinit var mMediaProjection: MediaProjection

    fun mediaProjectionManager(): MediaProjectionManager = mMediaProjectionManager().get()
    private fun mMediaProjectionManager() = object: Singleton<MediaProjectionManager>() {
        override fun create(): MediaProjectionManager {
            return context().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }
    }

    private fun context(): Context = ScreenshotService.Companion.Val.context()

}
