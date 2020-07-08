/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import io.github.newbugger.android.screenshot.core.ScreenshotService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object AttributeUtil {

    private fun getFileName(): String {
        val fileDate = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")).toString()
        return "Screenshot-$fileDate.png"  // regenerate filename
    }

    // https://stackoverflow.com/a/59196277
    // https://developer.android.com/reference/android/content/ContentResolver
    // https://developer.android.com/reference/android/content/ContentValues#ContentValues(int)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getFileResolver(): Uri  =
        ContentValues(4)
            .apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, getFileName())
                put(MediaStore.Images.Media.TITLE, getFileName())
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Screenshot"
                )
            }
            .let {
                context().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it)!!
            }

    fun getFileDocument(): Uri =
        PreferenceUtil.getString("directory", "null")
            .let {
                Uri.parse(it)
            }
            .let {
                DocumentFile.fromTreeUri(context(), it)!!
            }
            .let {
                it.createFile("image/png", getFileName())!!
            }.uri

    fun getViewWidth(yes: Boolean): Int =
        if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.R)) {
            getViewWidthBounds(yes)
        } else {
            getViewWidthSize(yes)
        }

    // Android 10 cannot install application on Android 11 sdk
    // https://developer.android.com/reference/android/view/Display
    // #getSize(android.graphics.Point)
    // https://developer.android.com/reference/android/view/WindowManager
    // #getCurrentWindowMetrics()
    // https://developer.android.com/reference/android/view/WindowMetrics#getBounds()
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getViewWidthBounds(yes: Boolean): Int =
        MediaUtil.windowManager().currentWindowMetrics.bounds.let {
            if (yes) {
                it.width()
            } else {
                it.height()
            }
        }

    // https://developer.android.com/reference/android/view/Display
    // #getRealSize(android.graphics.Point)
    // https://developer.android.com/reference/android/view/WindowManager
    // #getDefaultDisplay()
    @RequiresApi(Build.VERSION_CODES.P)
    private fun getViewWidthSize(yes: Boolean): Int =
        Point().also { MediaUtil.display().getRealSize(it) }.let {
            if (yes) {
                it.x
            } else {
                it.y
            }
        }

    private fun context(): Context = ScreenshotService.Companion.Val.context()

}
