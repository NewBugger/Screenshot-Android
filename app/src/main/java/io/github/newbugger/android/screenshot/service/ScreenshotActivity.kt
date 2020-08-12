/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.service

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Bundle
import io.github.newbugger.android.screenshot.core.projection.Attribute
import io.github.newbugger.android.screenshot.core.projection.ReceiveUtil
import io.github.newbugger.android.screenshot.util.BuildUtil
import io.github.newbugger.android.screenshot.util.PreferenceUtil


class ScreenshotActivity : Activity() {

    private fun screenshot() {
        if (PreferenceUtil.getBoolean(this, "reflection", false)) {
            ScreenshotService.startCapture(this)
            finish()
        } else {
            mediaIntent()  // start Intent on only once
        }
    }

    private fun mediaIntent() {
        if (ReceiveUtil.checkTileMode() && !ScreenshotService.Companion.Val.checkForeground()) {
            ScreenshotService.startForeground(this)
        }
        startActivityForResult(  // request NotificationUtil allowed with each tap
            attribute.getMediaProjectionManager().createScreenCaptureIntent(),
            BuildUtil.Constant.Code.projectionRequestCode
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                BuildUtil.Constant.Code.projectionRequestCode -> {
                    val mMediaProjection: MediaProjection = attribute.getMediaProjectionManager().getMediaProjection(resultCode, data)
                    ReceiveUtil.receiveMediaProjection(mMediaProjection)
                    ScreenshotService.startCapture(this)
                    finish()  // kill this activity as soon
                }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenshot()
    }

    private val attribute: Attribute by lazy { Attribute.getInstance(this) }

}
