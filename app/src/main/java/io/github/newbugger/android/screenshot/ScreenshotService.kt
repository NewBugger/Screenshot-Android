/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot

import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat.RGBA_8888
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import io.github.newbugger.android.screenshot.media.Number
import io.github.newbugger.android.screenshot.util.ColorUtil
import io.github.newbugger.android.screenshot.util.NotificationUtil
import io.github.newbugger.android.screenshot.util.PreferenceUtil
import java.nio.ByteBuffer


class ScreenshotService : Service() {

    private lateinit var mContext: Context
    private lateinit var mHandler: Handler

    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mVirtualDisplay: VirtualDisplay
    private lateinit var mImageReader: ImageReader

    private fun getFileDocument(contentResolver: ContentResolver): Uri {
        val fileName = Number.getFileName()
        return if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
            Number.getFileDocument(fileName, contentResolver)
        } else {
            Number.getFileDocument(fileName, this)
        }
    }

    private fun createObjectThread() {
        // start capture handling thread
        object : HandlerThread("Thread") {
            override fun run() {
                Looper.prepare()
                mHandler = Handler(Looper.getMainLooper())
                Looper.loop()
            }
        }.start()
    }

    fun createMediaValue(tMediaProjection: MediaProjection) {
        mMediaProjection = tMediaProjection
    }

    private fun createViewValue(yes: Boolean): Int {
        val mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        /* if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.R)) {
            return Number.getViewWidth(mWindowManager, yes)
        } else { */
            val mDisplay = mWindowManager.defaultDisplay as Display
            return Number.getViewWidth(mDisplay, yes)
        /* } */
    }

    private fun createVirtualDisplay() {
        val onViewWidth = createViewValue(true)
        val onViewHeight = createViewValue(false)
        val mDisplayMetrics = resources.displayMetrics as DisplayMetrics
        val mDensity = mDisplayMetrics.densityDpi
        mImageReader = ImageReader.newInstance(
            onViewWidth,
            onViewHeight,
            RGBA_8888,
            1
        )
        val delay = PreferenceUtil.getString(mContext, "delay", "1000").toLong()
        mHandler.postDelayed({
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "screenshot",
                onViewWidth,
                onViewHeight,
                mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mImageReader.surface,
                null,
                null
            )
        }, delay)
    }

    private fun createWorkListeners() {
        mImageReader.setOnImageAvailableListener(
            ImageAvailableListener(),
            mHandler
        )
        mMediaProjection.registerCallback(MediaProjectionStopCallback(), null)
    }

    private fun createWorkerTasks() {
        createVirtualDisplay()
        createWorkListeners()
    }

    private fun createWorkerStart() {
        mContext = this  // for preferences use
        createObjectThread()
    }

    private fun stopProjection() {
        mHandler.post {
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

    // use inner class as a implicit reference
    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            val onViewWidth = createViewValue(true)
            val onViewHeight = createViewValue(false)
            val image: Image = reader.acquireNextImage()  // https://stackoverflow.com/a/38786747
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val bitmap = Bitmap.createBitmap(
                    onViewWidth,
                    onViewHeight,
                    Bitmap.Config.ARGB_8888,
                    false
                )
            if (PreferenceUtil.getBoolean(mContext, "setPixel")) {
                // TODO: find a method more efficient
                // TODO: Android 11 provides a context.Screenshot() method
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * onViewWidth
                ColorUtil.getColor(bitmap, buffer, onViewHeight, onViewWidth, pixelStride, rowPadding)
            } else {
                // TODO:: bug: on this method -> picture not available
                bitmap.copyPixelsFromBuffer(buffer)
            }
            // https://stackoverflow.com/a/49998139
            contentResolver.let {
                it.openOutputStream(getFileDocument(it), "rw")!!.also { fileOutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.flush()
                    fileOutputStream.close()
                }
            }
            buffer.clear()
            bitmap.recycle()
            image.close()
            stopProjection()
            createFinishToast()
        }
    }

    private fun createFinishToast() {
        // TODO: bug on this toast -> the toast shows too many times
        Toast.makeText(this, "Screenshot saved.", Toast.LENGTH_LONG).show()
    }

    private fun startInForeground() {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationUtil.notificationChannel())

        // https://developer.android.com/guide/components/services#Foreground
        // https://stackoverflow.com/questions/61276730/media-projections-require-
        // a-foreground-service-of-type-serviceinfo-foreground-se
        if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
            startForeground(
                NotificationUtil.notificationsNotificationId,
                NotificationUtil.notificationBuilder(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(
                NotificationUtil.notificationsNotificationId,
                NotificationUtil.notificationBuilder(this)
            )
        }
    }

    private fun stopInForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // https://developer.android.com/guide/components/bound-services.html#Binder
    override fun onBind(intent: Intent): IBinder {
        return ServiceBinder()
    }
    internal inner class ServiceBinder : Binder() {
        val service: ScreenshotService
            get() = this@ScreenshotService
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.getBooleanExtra("capture", true)) {  // if run Capture intent
            createWorkerTasks()
        } else {
            startInForeground()
            createWorkerStart()  // Handler generate for only once
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopInForeground()
        super.onDestroy()
    }

    companion object {
        fun startForeground(context: Context) {
            Intent(context, ScreenshotService::class.java).also { intent ->
                intent.putExtra("capture", false)
                context.startService(intent)
            }
        }

        fun startCapture(context: Context) {
            Intent(context, ScreenshotService::class.java).also { intent ->
                intent.putExtra("capture", true)
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            Intent(context, ScreenshotService::class.java).also { intent ->
                context.stopService(intent)
            }
        }
    }

}
