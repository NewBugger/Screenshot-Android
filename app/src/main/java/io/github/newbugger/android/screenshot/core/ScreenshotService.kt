/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.*
import android.widget.Toast
import io.github.newbugger.android.screenshot.util.*
import java.nio.ByteBuffer


class ScreenshotService : Service() {

    private lateinit var handler: Handler

    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader

    private fun createMediaWorks() {
        val onViewWidth = AttributeUtil.getViewWidth(true)
        val onViewHeight = AttributeUtil.getViewWidth(false)
        mediaProjection = MediaUtil.mediaProjection()
        imageReader = MediaUtil.imageReader(onViewWidth, onViewHeight).apply {
            setOnImageAvailableListener(
                ImageAvailableListener(),
                null
            )
        }
        virtualDisplay = MediaUtil.virtualDisplay(onViewWidth, onViewHeight, mediaProjection, imageReader)
    }

    // use inner class as a implicit reference
    private inner class ImageAvailableListener: ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            createStopCallback()  // instantly stop callbacks as avoiding over-produce 2 pictures
            val onViewWidth = AttributeUtil.getViewWidth(true)
            val onViewHeight = AttributeUtil.getViewWidth(false)
            val image: Image = reader.acquireNextImage()  // https://stackoverflow.com/a/38786747
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val bitmap = Bitmap.createBitmap(
                MediaUtil.displayMetrics(),
                onViewWidth,
                onViewHeight,
                Bitmap.Config.ARGB_8888,
                false
            )
            if (PreferenceUtil.getBoolean("setPixel")) {
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
            contentResolver.let { ct ->
                val fileDocument = if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
                    AttributeUtil.getFileResolver()
                } else {
                    AttributeUtil.getFileDocument()
                }
                ct.openOutputStream(fileDocument, "rw")!!.also { fileOutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.flush()
                    fileOutputStream.close()
                }
            }
            buffer.clear()
            bitmap.recycle()
            image.close()
            createFinishToast()
        }
    }

    private fun createFinishToast() {
        Toast.makeText(Val.context, "Screenshot saved.", Toast.LENGTH_LONG).show()
    }

    private fun createStopCallback() {
        imageReader.setOnImageAvailableListener(null, null)
        virtualDisplay.release()
        mediaProjection.stop()
    }

    private fun createWorkerTasks() {
        val delay = PreferenceUtil.getString("delay", "1000").toLong()
        handler.postDelayed({
            createMediaWorks()
        }, delay)
    }

    private fun createObjectThread() {
        // start capture handling thread
        object : HandlerThread("Thread") {
            override fun run() {
                Looper.prepare()
                handler = Handler(Looper.getMainLooper())
                Looper.loop()
            }
        }.start()
    }

    private fun startInForeground() {
        // https://developer.android.com/guide/components/services#Foreground
        // https://stackoverflow.com/questions/61276730/media-projections-require-
        // a-foreground-service-of-type-serviceinfo-foreground-se
        if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
            startForeground(
                NotificationUtil.notificationsNotificationId,
                NotificationUtil.notificationBuilder(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(
                NotificationUtil.notificationsNotificationId,
                NotificationUtil.notificationBuilder()
            )
        }
    }

    private fun stopInForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Val.context = this
        if (intent.getBooleanExtra("capture", true)) {  // if run Capture intent
            createWorkerTasks()
        } else {
            startInForeground()
            createObjectThread()  // Handler generate for only once
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopInForeground()
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

        object Val {
            lateinit var context: Context
            fun context(): Context = context
        }
    }

}
