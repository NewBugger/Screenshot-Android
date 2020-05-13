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
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
// import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mVirtualDisplay: VirtualDisplay
    private lateinit var mDisplayMetrics: DisplayMetrics
    private lateinit var mWindowManager: WindowManager
    private lateinit var mImageReader: ImageReader
    private lateinit var mHandler: Handler

    private var mViewWidth by Delegates.notNull<Int>()
    private var mViewHeight by Delegates.notNull<Int>()
    private var mDensity by Delegates.notNull<Int>()

    private var getSAFPreference by Delegates.notNull<Boolean>()

    private fun getPreferences() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        getSAFPreference = preferences.getBoolean("saf", true)
    }

    // https://stackoverflow.com/a/37486214
    private fun getFiles() {  // regenerate filename
        val fileDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).toString()
        fileName = "Screenshot-$fileDate.png"
        if (getSAFPreference) {
            val dir = preferences.getString("directory", "null")!!
            val uri = Uri.parse(dir)
            val documentFile: DocumentFile = DocumentFile.fromTreeUri(this, uri)!!
            val newDocumentFile = documentFile.createFile("image/png", fileName)!!
            fileNewDocument = newDocumentFile.uri
        } else {
            fileLocation = getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString() + File.separator + "Screenshot"
        }
    }

    private fun createObjectThread() {
        object : HandlerThread("HandlerThread") {  // start capture handling thread
            override fun run() {
                Looper.prepare()
                // https://stackoverflow.com/a/42179437
                /* val handlerThread = HandlerThread("HandlerThread")
                handlerThread.start()
                val loop = handlerThread.looper
                mHandler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Handler.createAsync(loop)
                } else {
                    Handler(loop)
                } */
                // TODO: Handler() is deprecated in Android 11
                // TODO: bug:: too many works on your main thread when on AVD but not mobiles
                mHandler = Handler()
                Looper.loop()
            }
        }.start()
    }

    private fun createMediaValues() {
        val activity = ScreenshotActivity()
        mMediaProjection = activity.mMediaProjection
        mWindowManager = activity.mWindowManager
        mDisplayMetrics = activity.mDisplayMetrics
    }

    private fun createViewValues() {
        // TODO: full Android 11 (R) support
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
    }

    private fun createVirtualDisplay() {
        mImageReader = ImageReader.newInstance(
            mViewWidth,
            mViewHeight,
            PixelFormat.RGBA_8888,
            1
        )
        val delay = preferences.getString("delay", "1000")!!.toLong()
        mHandler.postDelayed({
            // https://stackoverflow.com/a/54352394
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "screenshot",
                mViewWidth,
                mViewHeight,
                mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mImageReader.surface,
                null,
                null  // mHandler
            )
            // TODO: replaced the method of time wait
        }, delay)
    }

    private fun createWorkListeners() {
        mImageReader.setOnImageAvailableListener(
            ImageAvailableListener(),
            mHandler
        )
        mMediaProjection.registerCallback(MediaProjectionStopCallback(), mHandler)  // register media projection stop callback
    }

    private fun createWorkerTasks() {
        getPreferences()
        getFiles()
        createMediaValues()
        createObjectThread()
        createViewValues()
        createVirtualDisplay()
        createWorkListeners()
    }

    private fun stopProjection() {
        mHandler.post {  // after image saved, stop MediaFunction intent
            mMediaProjection.stop()
            createFileBroadcast()
            createFinishToast()
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
            val onViewWidth = mViewWidth
            val onViewHeight = mViewHeight
            val image: Image = reader.acquireNextImage()  // https://stackoverflow.com/a/38786747
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val bitmap = Bitmap.createBitmap(  // https://developer.android.com/reference/android/graphics/Bitmap#createBitmap(android.util.DisplayMetrics,%20int,%20int,%20android.graphics.Bitmap.Config,%20boolean)
                    mDisplayMetrics,  // Its initial density is determined from the given DisplayMetrics
                    onViewWidth,
                    onViewHeight,
                    Bitmap.Config.ARGB_8888,
                    false
                )
            if (preferences.getBoolean("setPixel", true)) {
                // logcat:: I/Choreographer: Skipped 120 frames!  The application may be doing too much work on its main thread.
                // TODO: find a method more efficient
                // https://stackoverflow.com/questions/26673127/android-imagereader-acquirelatestimage-returns-invalid-jpg
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * onViewWidth
                Utils.getColor(bitmap, buffer, onViewHeight, onViewWidth, pixelStride, rowPadding)
            } else {
                // TODO: bug on this method -> picture not available
                bitmap.copyPixelsFromBuffer(buffer)
            }
            if (getSAFPreference) {
                // https://stackoverflow.com/a/49998139
                // https://developer.android.com/reference/android/graphics/Bitmap#compress(android.graphics.Bitmap.CompressFormat,%20int,%20java.io.OutputStream)
                val fileOutputStream: OutputStream = contentResolver.openOutputStream(fileNewDocument, "rw")!!
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
            } else {
                val onFileLocation = fileLocation
                val onFileName = fileName
                val onFileTarget = File(onFileLocation + File.separator + onFileName)
                val fileOutputStream = FileOutputStream(onFileTarget)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
            }
            buffer.clear()
            bitmap.recycle()
            image.close()
            stopProjection()
        }
    }

    private fun createFileBroadcast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // https://stackoverflow.com/a/59196277
            // https://developer.android.com/reference/android/content/ContentResolver#insert(android.net.Uri,%20android.content.ContentValues)
            val values = ContentValues(3)
            values.put(MediaStore.Images.Media.TITLE, fileName)
            // TODO: express "Screenshot" dir using SAF uri (1)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, DIRECTORY_PICTURES.toString() + File.separator + "Screenshot")
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            // https://developer.android.com/training/camera/photobasics#TaskGallery
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                // TODO: express "Screenshot" dir using SAF uri (2)
                val fileDir = if (getSAFPreference) {
                    fileNewDocument.toString()
                } else {
                    fileLocation
                }
                val file = File(fileDir + File.separator  + fileName)
                mediaScanIntent.data = Uri.fromFile(file)
                sendBroadcast(mediaScanIntent)
            }
        }
    }

    private fun createFinishToast() {
        // TODO: bug on this toast -> the toast shows too many times
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
    /* override fun onBind(intent: Intent): IBinder {
        return ServiceBinder()
    }
    internal inner class ServiceBinder : Binder() {
        val service: ScreenshotService
            get() = this@ScreenshotService
    } */
    // https://developer.android.com/guide/components/services#ExtendingService
    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startInForeground()
        if (intent.getStringExtra("flag") == "capture") createWorkerTasks()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopInForeground()
        super.onDestroy()
    }

    companion object {
        fun startForeground(context: Context) {
            Intent(context, ScreenshotService::class.java).also { intent ->
                context.startService(intent)
            }
        }

        fun startCapture(context: Context, flag: String) {
            Intent(context, ScreenshotService::class.java).also { intent ->
                intent.putExtra("flag", flag)
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenshotService::class.java)
            context.stopService(intent)
        }
    }

}
