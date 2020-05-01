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
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.OrientationEventListener
import android.widget.Toast
// import com.muddzdev.quickshot.QuickShot
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates


class ScreenshotActivity : Activity() {

    private val projectionRequestCode = 1000
    // private val documentRequestCode = 1001
    // private val REQUEST_ON = true

    private lateinit var fileLocation: String

    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mVirtualDisplay: VirtualDisplay
    private lateinit var mDisplayMetrics: DisplayMetrics
    private lateinit var mDisplay: Display
    private lateinit var mImageReader: ImageReader
    private lateinit var mHandler: Handler
    private lateinit var mOrientationChangeCallback: OrientationChangeCallback

    private var mRotation by Delegates.notNull<Int>()  // compare from OrientationChangeCallback
    private var mViewWidth by Delegates.notNull<Int>()
    private var mViewHeight by Delegates.notNull<Int>()
    private var mDensity by Delegates.notNull<Int>()

    // Service Binder
    private lateinit var screenshotService: ScreenshotService
    private var screenshotBound: Boolean = false

    // https://stackoverflow.com/a/37486214
    private fun getFiles(): String {  // regenerate filename
        val fileDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")).toString()
        return "Screenshot-$fileDate"
    }

    /* private fun getLocation(): String {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            Environment.getExternalStorageDirectory().toString() + File.separator + Environment.DIRECTORY_PICTURES.toString() + "/Screenshot"
        } else {
            ""
        }
    } */

    /* https://github.com/android/storage-samples/blob/master/ActionOpenDocumentTree/app/src/main/java/com/example/android/ktfiles/MainActivity.kt
    private fun showDirectoryContents(directoryUri: Uri) {
        supportFragmentManager.transaction {
            val directoryTag = directoryUri.toString()
            val directoryFragment = DirectoryFragment.newInstance(directoryUri)
            replace(R.id.fragment_container, directoryFragment, directoryTag)
            addToBackStack(directoryTag)
        }
    } */

    private fun createObjectThread() {
        object : Thread() {  // start capture handling thread
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()
    }

    private fun createViewValues() {
        mDisplayMetrics = resources.displayMetrics
        mDisplay = windowManager.defaultDisplay
        // mDisplay.getMetrics(mDisplayMetrics)  // https://stackoverflow.com/a/60378460
        // mViewWidth = mDisplayMetrics.widthPixels  // unexpected get wrong value
        // mViewHeight = mDisplayMetrics.heightPixels  // unexpected get wrong value
        mViewWidth = 1080
        mViewHeight = 1920
        mDensity = mDisplayMetrics.densityDpi
        mRotation = mDisplay.rotation  // write down the current value
        // fileLocation = getLocation()
        fileLocation = getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString() + File.separator + "Screenshot"
    }

    private fun createVirtualDisplay() {
        mImageReader = ImageReader.newInstance(
            mViewWidth,
            mViewHeight,
            PixelFormat.RGBA_8888,
            2
        )
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
            "capture_screen",  // SCREEN_CAP_NAME
            mViewWidth,
            mViewHeight,
            mDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.surface,
            null,
            mHandler
        )
        mImageReader.setOnImageAvailableListener(
            ImageAvailableListener(WeakReference(this)),
            mHandler
        )
    }

    private fun createOverListener() {
        mOrientationChangeCallback = OrientationChangeCallback(this, WeakReference(this))  // register orientation change callback
        if (mOrientationChangeCallback.canDetectOrientation()) mOrientationChangeCallback.enable()
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
            val onFileLocation = outerClass.get()!!.fileLocation
            val onFileName = outerClass.get()!!.getFiles()
            val image: Image = reader.acquireLatestImage()
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride: Int = planes[0].pixelStride
            val rowStride: Int = planes[0].rowStride
            val rowPadding: Int = rowStride - pixelStride * onViewWidth
            // logcat:: W/roid.screensho: Core platform API violation: Ljava/nio/Buffer;->address:J from Landroid/graphics/Bitmap; using JNI
            val bitmap = Bitmap.createBitmap(  // create bitmap
                onViewWidth + rowPadding / pixelStride,
                onViewHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            val fos = FileOutputStream(onFileLocation + File.separator  + onFileName +".png")  // write bitmap to a file
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
            bitmap.recycle()
            image.close()
            outerClass.get()!!.stopProjection()
        }
    }

    private class OrientationChangeCallback internal constructor(context: Context, private val outerClass: WeakReference<ScreenshotActivity>) : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation: Int = outerClass.get()!!.mDisplay.rotation  // if NEW rotation state
            if (rotation != outerClass.get()!!.mRotation) {
                outerClass.get()!!.mRotation = rotation
                outerClass.get()!!.mVirtualDisplay.release()  // clean up the old display
                outerClass.get()!!.mImageReader.setOnImageAvailableListener(null, null)
                outerClass.get()!!.createVirtualDisplay()  // recreate a new display
            }
        }
    }

    private class MediaProjectionStopCallback(private val outerClass: WeakReference<ScreenshotActivity>) : MediaProjection.Callback() {
        override fun onStop() {
            outerClass.get()!!.mHandler.post {
                outerClass.get()!!.mVirtualDisplay.release()
                outerClass.get()!!.mImageReader.setOnImageAvailableListener(null, null)
                outerClass.get()!!.mOrientationChangeCallback.disable()
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

/*    override fun onQuickShotSuccess(path: String) {
        Toast.makeText(this, "Screenshot saved.", Toast.LENGTH_LONG).show()
    }

    override fun onQuickShotFailed(path: String) {
        Toast.makeText(this, "Screenshot failed, please check.", Toast.LENGTH_LONG).show()
    }
*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == projectionRequestCode && resultCode == RESULT_OK && data != null) {  // if yes, call MediaFunction to capture the current screen display
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
            mHandler.postDelayed({  // https://stackoverflow.com/a/54352394
                createVirtualDisplay()  // create virtual display depending on device width / height
                createOverListener()
                Toast.makeText(this, "Screenshot saved.", Toast.LENGTH_LONG).show()
            }, 7000)  // 5000ms == 5s
        } else {
            Toast.makeText(this, "Capture behavior canceled.", Toast.LENGTH_LONG).show()
        }
        /* if (requestCode == documentRequestCode && resultCode == RESULT_OK && data != null) {
            val directoryUri = data.data ?: return
            contentResolver.takePersistableUriPermission(
                directoryUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            showDirectoryContents(directoryUri)
        } */
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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
