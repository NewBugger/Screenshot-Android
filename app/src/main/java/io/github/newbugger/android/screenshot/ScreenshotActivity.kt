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
import android.graphics.PixelFormat.RGBA_8888
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates


class ScreenshotActivity : Activity() {

    private val projectionRequestCode = 1000
    // private val REQUEST_ON = true

    private lateinit var fileLocation: String
    private lateinit var fileName: String
    private lateinit var fileNewDocument: Uri

    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mVirtualDisplay: VirtualDisplay
    private lateinit var mDisplayMetrics: DisplayMetrics
    private lateinit var mWindowManager: WindowManager
    private lateinit var mImageReader: ImageReader
    private lateinit var mStopProjectionEventListener: ScreenshotService.StopProjectionEventListener
    private lateinit var mHandler: Handler

    private var mViewWidth by Delegates.notNull<Int>()
    private var mViewHeight by Delegates.notNull<Int>()
    private var mDensity by Delegates.notNull<Int>()

    // Service Binder
    private lateinit var screenshotService: ScreenshotService
    private var screenshotBound: Boolean = false

    private fun getSAFPreference(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        return preferences.getBoolean("saf", true)
    }

    // https://stackoverflow.com/a/37486214
    private fun getFiles() {  // regenerate filename
        val fileDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).toString()
        fileName = "Screenshot-$fileDate.png"
        if (getSAFPreference()) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val dir = preferences.getString("directory", null).toString()
            val uri = Uri.parse(dir)
            val documentFile: DocumentFile = DocumentFile.fromTreeUri(this, uri)!!
            val newDocumentFile = documentFile.createFile("image/png", fileName)!!
            fileNewDocument = newDocumentFile.uri
        } else {
            fileLocation = getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString() + File.separator + "Screenshot"
        }
    }

    private fun createObjectThread() {
        object : Thread() {  // start capture handling thread
            override fun run() {
                Looper.prepare()
                val handlerThread = HandlerThread("HandlerThread")
                handlerThread.start()
                val loop = handlerThread.looper
                mHandler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Handler.createAsync(loop)
                } else {
                    Handler(loop)
                }
                Looper.loop()
            }
        }.start()
    }

    private fun createViewValues() {
        mWindowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplayMetrics = applicationContext.resources.displayMetrics
        // Android 10 cannot install app include Android 11 apis
        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // https://developer.android.com/reference/android/view/Display#getSize(android.graphics.Point)
            // https://developer.android.com/reference/android/view/WindowManager#getCurrentWindowMetrics()
            // https://developer.android.com/reference/android/view/WindowMetrics#getSize()
            mViewWidth = mWindowManager.currentWindowMetrics.size.width
            mViewHeight = mWindowManager.currentWindowMetrics.size.height
        } else { */
            // https://developer.android.com/reference/android/view/Display#getRealSize(android.graphics.Point)
            // https://developer.android.com/reference/android/view/WindowManager#getDefaultDisplay()
            val mDisplay = mWindowManager.defaultDisplay
            val size = Point()
            mDisplay.getRealSize(size)
            mViewWidth = size.x
            mViewHeight = size.y
        /* } */
        mDensity = mDisplayMetrics.densityDpi
        getFiles()
    }

    private fun createVirtualDisplay() {
        mImageReader = ImageReader.newInstance(
            mViewWidth,
            mViewHeight,
            RGBA_8888,
            1
        )
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
            "screenshot",
            mViewWidth,
            mViewHeight,
            mDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            mImageReader.surface,
            null,
            mHandler
        )
    }

    private fun createTransferValues() {
        val tFileDirectory = if (getSAFPreference()) {
            fileNewDocument
        } else {
            fileLocation
        }
        screenshotService.createPublicValues(mDisplayMetrics, mImageReader, mViewWidth, mViewHeight, fileName, tFileDirectory)
    }

    private fun createOverListeners() {
        createTransferValues()
        screenshotService.createImageListener()
        mMediaProjection.registerCallback(MediaProjectionStopCallback(), mHandler)  // register media projection stop callback
        mStopProjectionEventListener = ScreenshotService.StopProjectionEventListener()
        mStopProjectionEventListener.setStopProjectionEventListener(object:
            ScreenshotService.StopProjectionEventListener.OnStopProjectionEventListener {
            override fun onStopProjection() {
                stopProjection()
            }
        })
        // TODO: bugfix:: not allowed stop projection when in setPixel mode ?
        // stopProjection()
    }

    private fun startProjection() {
        startActivityForResult(  // request Projection allowed with each tap
            mMediaProjectionManager.createScreenCaptureIntent(),
            projectionRequestCode
        )
    }

    private fun stopProjection() {
        mHandler.post {  // after image saved, stop MediaFunction intent
            mMediaProjection.stop()
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            mHandler.post {
                mVirtualDisplay.release()
                mImageReader.setOnImageAvailableListener(null, null)
                mMediaProjection.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }

    private val screenshotConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binderName: IBinder) {
            val binder = binderName as ScreenshotService.ServiceBinder
            screenshotService = binder.service  // mService = binder.getService()
            screenshotBound = true
        }
        override fun onServiceDisconnected(name: ComponentName) {
            screenshotBound = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                projectionRequestCode -> {  // call MediaFunction to capture the current screen display
                    // TimeUnit.SECONDS.sleep(5L)  // its freezes the UI
                    // just write it the foreground, don't need it to run Media Projection
                    /* if (screenshotService == null) {
                        Toast.makeText(this, "wait for screenshotService binder ..", Toast.LENGTH_LONG).show()
                        return
                    }
                    Toast.makeText(this, "screenshotService binder is found.", Toast.LENGTH_LONG).show()
                    mMediaProjection = screenshotService!!.createMediaProjection(mMediaProjectionManager, requestCode, data) */
                    mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data)
                    createViewValues()
                    mHandler.postDelayed({
                        // https://stackoverflow.com/a/54352394
                        createVirtualDisplay()
                        createOverListeners()
                    }, 3000)  // 5000ms == 5s
                }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMediaProjectionManager = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createObjectThread()
        startProjection() // default start capture, so opener is needed
    }

    // https://developer.android.com/guide/components/bound-services.html#kotlin
    override fun onStart() {
        super.onStart()
        if (!screenshotBound) Intent(this, ScreenshotService::class.java).also { intent ->
            bindService(intent, screenshotConnection, Context.BIND_AUTO_CREATE)
        }  // bind to the Service
        screenshotBound = true
    }

    override fun onStop() {
        super.onStop()
        if (screenshotBound) unbindService(screenshotConnection)
        screenshotBound = false
    }

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
