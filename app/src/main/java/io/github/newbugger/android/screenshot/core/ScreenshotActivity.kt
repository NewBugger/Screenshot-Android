/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Bundle
import io.github.newbugger.android.screenshot.util.BuildUtil
import io.github.newbugger.android.screenshot.util.MediaUtil
import io.github.newbugger.android.screenshot.util.PreferenceUtil
import io.github.newbugger.android.screenshot.util.ProjectionUtil


class ScreenshotActivity : Activity() {

    private fun mediaIntent() {
        if (PreferenceUtil.checkTileMode() && !ScreenshotService.Companion.Val.checkForeground()) {
            ScreenshotService.startForeground(context())
        }
        startActivityForResult(  // request NotificationUtil allowed with each tap
            MediaUtil.mediaProjectionManager(context()).createScreenCaptureIntent(),
            BuildUtil.Constant.Code.projectionRequestCode
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                BuildUtil.Constant.Code.projectionRequestCode -> {
                    val mMediaProjection: MediaProjection = MediaUtil.mediaProjectionManager(context()).getMediaProjection(resultCode, data)
                    ProjectionUtil.receiveMediaProjection(mMediaProjection)
                    ScreenshotService.startCapture(context())
                    finish()  // kill this activity as soon
                }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        mediaIntent()  // start Intent on only once
    }

    private fun context(): Context = context
    private lateinit var context: Context

}