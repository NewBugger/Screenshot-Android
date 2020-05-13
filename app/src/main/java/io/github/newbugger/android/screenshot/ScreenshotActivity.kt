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
// import android.content.ComponentName
import android.content.Context
import android.content.Intent
// import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
// import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager


class ScreenshotActivity : Activity() {

    private val projectionRequestCode = 1000

    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mDisplayMetrics: DisplayMetrics
    private lateinit var mWindowManager: WindowManager

    // Service Binder
    /* private lateinit var screenshotService: ScreenshotService
    private var screenshotBound: Boolean = false */

    private fun startProjection() {
        startActivityForResult(  // request Projection allowed with each tap
            mMediaProjectionManager.createScreenCaptureIntent(),
            projectionRequestCode
        )
    }

    /* private val screenshotConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binderName: IBinder) {
            val binder = binderName as ScreenshotService.ServiceBinder
            screenshotService = binder.service  // mService = binder.getService()
            screenshotBound = true
        }
        override fun onServiceDisconnected(name: ComponentName) {
            screenshotBound = false
        }
    } */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                projectionRequestCode -> {  // call MediaFunction to capture the current screen display
                    // just write it the foreground, don't need it to run Media Projection
                    /* if (screenshotService == null) {
                        Toast.makeText(this, "wait for screenshotService binder ..", Toast.LENGTH_LONG).show()
                        return
                    }
                    Toast.makeText(this, "screenshotService binder is found.", Toast.LENGTH_LONG).show()
                    mMediaProjection = screenshotService!!.createMediaProjection(mMediaProjectionManager, requestCode, data) */
                    mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data)
                    mWindowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    mDisplayMetrics = applicationContext.resources.displayMetrics
                    screenshotService.createMediaValues(mMediaProjection, mWindowManager, mDisplayMetrics)
                    ScreenshotService.startCapture(this, "capture")
                }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMediaProjectionManager = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startProjection() // default start capture, so opener is needed
    }

    // https://developer.android.com/guide/components/bound-services.html#kotlin
    /* override fun onStart() {
        super.onStart()
        if (!screenshotBound) Intent(this, ScreenshotService::class.java).also { intent ->
            bindService(intent, screenshotConnection, Context.BIND_AUTO_CREATE)
            screenshotBound = true
        }  // bind to the Service
    }

    override fun onStop() {
        super.onStop()
        if (screenshotBound) unbindService(screenshotConnection)
    } */

    /* override fun onResume() {
        super.onResume()
        if (screenshotService == null) Intent(this, ScreenshotService::class.java).also { intent ->
            bindService(intent, screenshotConnection, Context.BIND_AUTO_CREATE)
        }  // bind to the Service
    }

    override fun onPause() {
        if (screenshotService != null) unbindService(screenshotConnection)
        super.onPause()
    }

    override fun onDestroy() {
        if (screenshotService != null) unbindService(screenshotConnection)
        super.onDestroy()
    } */

}

// https://stackoverflow.com/a/48474529
// https://stackoverflow.com/a/14296609
// https://stackoverflow.com/a/49208513
// private infix fun Byte.shl(that: Int): Int = this.toInt().shl(that)
// private infix fun Byte.and(that: Int): Int = this.toInt().and(that)
