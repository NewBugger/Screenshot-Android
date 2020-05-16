/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder


class ScreenshotActivity : Activity() {

    private val projectionRequestCode = 1000

    private lateinit var mMediaProjectionManager: MediaProjectionManager

    private lateinit var screenshotService: ScreenshotService  // binder

    private fun mediaManager() {
        mMediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun mediaIntent() {
        startActivityForResult(  // request Projection allowed with each tap
            mMediaProjectionManager.createScreenCaptureIntent(),
            projectionRequestCode
        )
    }

    private val screenshotConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binderName: IBinder) {
            val binder = binderName as ScreenshotService.ServiceBinder
            screenshotService = binder.service  // mService = binder.getService()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            // empty
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                projectionRequestCode -> {
                    val mMediaProjection: MediaProjection =
                        mMediaProjectionManager.getMediaProjection(resultCode, data)
                    screenshotService.createMediaValues(mMediaProjection)
                    ScreenshotService.startCapture(this)
                }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaManager()
        mediaIntent()  // start Intent only once
    }

    // https://developer.android.com/guide/components/bound-services.html#kotlin
    override fun onStart() {
        super.onStart()
        Intent(this, ScreenshotService::class.java).also { intent ->
            bindService(intent, screenshotConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(screenshotConnection)
    }

}
