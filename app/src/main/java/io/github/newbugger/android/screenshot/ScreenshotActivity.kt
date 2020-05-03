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
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
// import androidx.annotation.RequiresApi
// import com.muddzdev.quickshot.QuickShot
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
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
    private lateinit var mHandler: Handler

    private var mViewWidth by Delegates.notNull<Int>()
    private var mViewHeight by Delegates.notNull<Int>()
    private var mDensity by Delegates.notNull<Int>()

    // Service Binder
    private lateinit var screenshotService: ScreenshotService
    private var screenshotBound: Boolean = false

    // https://stackoverflow.com/a/37486214
    private fun getFiles() {  // regenerate filename
        val fileDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).toString()
        fileName = "Screenshot-$fileDate.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                /* mHandler = if (Looper.myLooper() != null) {
                    Handler(Looper.myLooper()!!)  // https://developer.android.com/reference/android/os/Handler#Handler()
                } else {
                    Handler()  // deprecated in api 30.
                } */
                mHandler = Handler()
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
            PixelFormat.RGBA_8888,
            1
        )
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
            "capture_screen",
            mViewWidth,
            mViewHeight,
            mDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.surface,
            null,
            mHandler
        )
    }

    private fun createOverListener() {
        mImageReader.setOnImageAvailableListener(
            ImageAvailableListener(WeakReference(this)),
            mHandler
        )
        mMediaProjection.registerCallback(MediaProjectionStopCallback(WeakReference(this)), mHandler)  // register media projection stop callback
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

    private class ImageAvailableListener(private val outerClass: WeakReference<ScreenshotActivity>) : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            val onViewWidth = outerClass.get()!!.mViewWidth
            val onViewHeight = outerClass.get()!!.mViewHeight
            val image: Image = reader.acquireNextImage()  // https://stackoverflow.com/a/38786747
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val bitmap = Bitmap.createBitmap(  // https://developer.android.com/reference/android/graphics/Bitmap#createBitmap(android.util.DisplayMetrics,%20int,%20int,%20android.graphics.Bitmap.Config,%20boolean)
                outerClass.get()!!.mDisplayMetrics,  // Its initial density is determined from the given DisplayMetrics
                onViewWidth,
                onViewHeight,
                Bitmap.Config.ARGB_8888,
                false
            )
            // logcat:: W/roid.screensho: Core platform API violation:
            // Ljava/nio/Buffer;->address:J from Landroid/graphics/Bitmap; using JNI
            // TODO: replace Bitmap.copyPixelsFromBuffer() method
            bitmap.copyPixelsFromBuffer(buffer)
            buffer.rewind()
            buffer.clear()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // https://stackoverflow.com/a/49998139
                val fileOutputStream: OutputStream = outerClass.get()!!.contentResolver.openOutputStream(outerClass.get()!!.fileNewDocument, "rwt")!!
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                fileOutputStream.write(byteArray)
                fileOutputStream.close()
            } else {
                val onFileLocation = outerClass.get()!!.fileLocation
                val onFileName  = outerClass.get()!!.fileName
                val onFileTarget = File(onFileLocation + File.separator  + onFileName)
                val fileOutputStream = FileOutputStream(onFileTarget)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()
            }
            bitmap.recycle()
            image.close()
            outerClass.get()!!.stopProjection()
        }
    }

    private class MediaProjectionStopCallback(private val outerClass: WeakReference<ScreenshotActivity>) : MediaProjection.Callback() {
        override fun onStop() {
            outerClass.get()!!.mHandler.post {
                outerClass.get()!!.mVirtualDisplay.release()
                outerClass.get()!!.mImageReader.setOnImageAvailableListener(null, null)
                outerClass.get()!!.mMediaProjection.unregisterCallback(this@MediaProjectionStopCallback)
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

    private fun createFileBroadcast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

/*    override fun onQuickShotSuccess(path: String) {
        Toast.makeText(this, "Screenshot saved.", Toast.LENGTH_LONG).show()
    }

    override fun onQuickShotFailed(path: String) {
        Toast.makeText(this, "Screenshot failed, please check.", Toast.LENGTH_LONG).show()
    }
*/

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
                        createOverListener()
                        createFileBroadcast()
                        Toast.makeText(this, "Screenshot saved.", Toast.LENGTH_LONG).show()
                    }, 3000)  // 5000ms == 5s}
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
