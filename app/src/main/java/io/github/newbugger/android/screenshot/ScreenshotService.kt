/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import kotlin.properties.Delegates


class ScreenshotService : Service() {

    // prepare a method for clients ScreenshotActivity
    /* fun createMediaProjection(mMediaProjectionManager: MediaProjectionManager, resultCode: Int, data: Intent): MediaProjection {
        return mMediaProjectionManager.getMediaProjection(resultCode, data)
    } */

    private lateinit var fileLocation: String
    private lateinit var fileName: String
    private lateinit var fileNewDocument: Uri
    private lateinit var preferences: SharedPreferences
    private lateinit var mDisplayMetrics: DisplayMetrics
    private lateinit var mImageReader: ImageReader
    private lateinit var mHandler: Handler
    private var mViewWidth by Delegates.notNull<Int>()
    private var mViewHeight by Delegates.notNull<Int>()

    private fun getSDKLimit(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    private fun getSAFPreference(): Boolean {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        return preferences.getBoolean("saf", true)
    }

    private fun createObjectThread() {
        object : Thread() {  // start capture handling thread
            override fun run() {
                Looper.prepare()
                // https://stackoverflow.com/a/42179437
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

    fun createPublicValues(tDisplayMetrics: DisplayMetrics,
                           tImageReader :ImageReader,
                           tViewWidth: Int, tViewHeight: Int,
                           tFileName: String, tFileDirectory: Any) {
        mDisplayMetrics = tDisplayMetrics
        mImageReader = tImageReader
        mViewWidth = tViewWidth
        mViewHeight = tViewHeight
        fileName = tFileName
        if (getSAFPreference()) {
            fileNewDocument = tFileDirectory as Uri
        } else {
            fileLocation = tFileDirectory as String
        }
    }

    fun createImageListener() {
        mImageReader.setOnImageAvailableListener(
            ImageAvailableListener(WeakReference(this)),
            mHandler
        )
    }

    private fun createListenerTrigger() {
        val listener = StopProjectionEventListener().listener
        listener?.onStopProjection()
    }

    // https://github.com/codepath/android_guides/wiki/Creating-Custom-Listeners
    class StopProjectionEventListener {
        interface OnStopProjectionEventListener {
            fun onStopProjection()
        }
        var listener: OnStopProjectionEventListener? = null
        fun setStopProjectionEventListener(listener: OnStopProjectionEventListener?) {
            this.listener = listener
        }
    }

    private class ImageAvailableListener(private val outerClass: WeakReference<ScreenshotService>) : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            val onViewWidth = outerClass.get()!!.mViewWidth
            val onViewHeight = outerClass.get()!!.mViewHeight
            val image: Image = reader.acquireNextImage()  // https://stackoverflow.com/a/38786747
            val planes: Array<Image.Plane> = image.planes
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * onViewWidth
            val buffer: ByteBuffer = planes[0].buffer
            // logcat:: W/roid.screensho: Core platform API violation:
            // Ljava/nio/Buffer;->address:J from Landroid/graphics/Bitmap; using JNI
            // TODO: replace Bitmap.copyPixelsFromBuffer() method
            val bitmap =
                Bitmap.createBitmap(  // https://developer.android.com/reference/android/graphics/Bitmap#createBitmap(android.util.DisplayMetrics,%20int,%20int,%20android.graphics.Bitmap.Config,%20boolean)
                    outerClass.get()!!.mDisplayMetrics,  // Its initial density is determined from the given DisplayMetrics
                    onViewWidth,
                    onViewHeight,
                    Bitmap.Config.ARGB_8888,
                    false
                )
            if (outerClass.get()!!.preferences.getBoolean("setPixel", true)) {
                // logcat:: I/Choreographer: Skipped 120 frames!  The application may be doing too much work on its main thread.
                // TODO: find a method more efficient
                // https://stackoverflow.com/questions/26673127/android-imagereader-acquirelatestimage-returns-invalid-jpg
                Utils.getColor(bitmap, buffer, onViewHeight, onViewWidth, pixelStride, rowPadding)
            } else {
                bitmap.copyPixelsFromBuffer(buffer)
            }
            if (outerClass.get()!!.getSAFPreference()) {
                // https://stackoverflow.com/a/49998139
                // https://developer.android.com/reference/android/graphics/Bitmap#compress(android.graphics.Bitmap.CompressFormat,%20int,%20java.io.OutputStream)
                val fileOutputStream: OutputStream =
                    outerClass.get()!!.contentResolver.openOutputStream(
                        outerClass.get()!!.fileNewDocument,
                        "rw"
                    )!!
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
            } else {
                val onFileLocation = outerClass.get()!!.fileLocation
                val onFileName = outerClass.get()!!.fileName
                val onFileTarget = File(onFileLocation + File.separator + onFileName)
                val fileOutputStream = FileOutputStream(onFileTarget)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
            }
            buffer.clear()
            bitmap.recycle()
            image.close()
            outerClass.get()!!.createListenerTrigger()
            outerClass.get()!!.createFileBroadcast()
            outerClass.get()!!.createFinishToast()
        }
    }

    private fun createFileBroadcast() {
        if (getSDKLimit()) {
            // https://stackoverflow.com/a/59196277
            // https://developer.android.com/reference/android/content/ContentResolver#insert(android.net.Uri,%20android.content.ContentValues)
            val values = ContentValues(3)
            values.put(MediaStore.Images.Media.TITLE, fileName)
            // TODO: express "Screenshot" dir using SAF uri
            values.put(MediaStore.Images.Media.RELATIVE_PATH, DIRECTORY_PICTURES.toString() + File.separator + "Screenshot")
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            // https://developer.android.com/training/camera/photobasics#TaskGallery
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                val file = File(fileLocation + File.separator  + fileName)
                mediaScanIntent.data = Uri.fromFile(file)
                sendBroadcast(mediaScanIntent)
            }
        }
    }

    private fun createFinishToast() {
        Toast.makeText(this, "Screenshot saved.", Toast.LENGTH_LONG).show()
    }

    private fun startInForeground() {
        val notificationsCHANNELID = "Foreground"
        val notificationsTextTitle = "Screenshot Service"
        val notificationsTextContent = "tap to take a Screenshot"
        val notificationsChannelName = "Foreground"
        val notificationsChannelDescription = "Foreground Service"
        val notificationsSmallIcon = R.mipmap.ic_launcher_round
        val notificationsImportance = NotificationManager.IMPORTANCE_LOW
        val notificationsNotificationId = 1038

        val notificationIntentScreenshot: PendingIntent =
            Intent(this, ScreenshotActivity::class.java)
                .let { notificationPendingIntent ->
                    PendingIntent.getActivity(
                        this,
                        0,
                        notificationPendingIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT  // the current one should be canceled before generating a new one
                    )
                }

        val notificationIntentMain: PendingIntent =
            Intent(this, MainActivity::class.java)
                .let { notificationPendingIntent ->
                    PendingIntent.getActivity(
                        this,
                        0,
                        notificationPendingIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

        // https://developer.android.com/training/notify-user/build-notification#Actions
        // https://stackoverflow.com/a/37134139
        val notificationAction: Notification.Action = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_snooze),
            getString(R.string.notification_button),
            notificationIntentScreenshot)
            .build()

        val notificationBuilder: Notification = Notification.Builder(this, notificationsCHANNELID)
            .setSmallIcon(notificationsSmallIcon)
            .setContentTitle(notificationsTextTitle)
            .setContentText(notificationsTextContent)
            .setContentIntent(notificationIntentMain)
            .addAction(notificationAction)
            .build()

        val channel: NotificationChannel = NotificationChannel(
            notificationsCHANNELID,
            notificationsChannelName,
            notificationsImportance
        ).apply {
            description = notificationsChannelDescription
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)

        // https://developer.android.com/guide/components/services#Foreground
        // https://stackoverflow.com/questions/61276730/media-projections-require-a-foreground-service-of-type-serviceinfo-foreground-se
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationsNotificationId, notificationBuilder, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(notificationsNotificationId, notificationBuilder)
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
        createObjectThread()
        startInForeground()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopInForeground()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ScreenshotService::class.java)
            context.startService(intent)
        }

        /* fun stop(context: Context) {
            val intent = Intent(context, ScreenshotService::class.java)
            context.stopService(intent)
        } */  // need stop Foreground opener
    }

}
