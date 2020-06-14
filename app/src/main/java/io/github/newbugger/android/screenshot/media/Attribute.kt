/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.media

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import io.github.newbugger.android.screenshot.util.PreferenceUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object Attribute {

    private fun getFileName(): String {
        val fileDate = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")).toString()
        return "Screenshot-$fileDate.png"  // regenerate filename
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getFileDocument(ct: ContentResolver): Uri {
        val fileName = getFileName()
        // https://stackoverflow.com/a/59196277
        // https://developer.android.com/reference/android/content/ContentResolver
        // https://developer.android.com/reference/android/content/ContentValues#ContentValues(int)
        return ContentValues(4)
            .apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.TITLE, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Screenshot"
                )
            }
            .let {
                ct.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it)!!
            }
    }

    fun getFileDocument(context: Context): Uri {
        val fileName = getFileName()
        return PreferenceUtil.getString(context, "directory", "null")
            .let {
                Uri.parse(it)
            }
            .let {
                DocumentFile.fromTreeUri(context, it)!!
            }
            .let {
                it.createFile("image/png", fileName)!!
            }.uri
    }

    // Android 10 cannot install application on Android 11 sdk
    /*@RequiresApi(Build.VERSION_CODES.R)
    fun getViewWidth(mWindowManager: WindowManager, yes: Boolean): Int {
        // https://developer.android.com/reference/android/view/Display
        // #getSize(android.graphics.Point)
        // https://developer.android.com/reference/android/view/WindowManager
        // #getCurrentWindowMetrics()
        // https://developer.android.com/reference/android/view/WindowMetrics#getBounds()
        return mWindowManager.currentWindowMetrics.bounds.let {
            if (yes) {
                it.width()
            } else {
                it.height()
            }
        }
    }*/

    fun getViewWidth(point: Point, yes: Boolean): Int {
        // https://developer.android.com/reference/android/view/Display
        // #getRealSize(android.graphics.Point)
        // https://developer.android.com/reference/android/view/WindowManager
        // #getDefaultDisplay()
        return point.let {
            if (yes) {
                it.x
            } else {
                it.y
            }
        }
    }

}
