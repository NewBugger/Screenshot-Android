/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.widget.Toast
import io.github.newbugger.android.screenshot.core.ScreenshotService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


object ProjectionUtil {

    private class ImageAvailableListener: ImageReader.OnImageAvailableListener {
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
            context().contentResolver.let { ct ->
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

    private fun virtualDisplays(): VirtualDisplay = mVirtualDisplay()
    private fun virtualDisplay(): VirtualDisplay = mVirtualDisplay
    private fun mVirtualDisplay(): VirtualDisplay {
        mVirtualDisplay = mediaProjection().createVirtualDisplay(
            "screenshot",
            AttributeUtil.getViewWidth(true),
            AttributeUtil.getViewWidth(false),
            MediaUtil.displayMetrics().densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            imageReader().surface,
            null,
            null
        )
        return mVirtualDisplay
    }
    private lateinit var mVirtualDisplay: VirtualDisplay

    private fun imageReaders(): ImageReader = mImageReader()
    private fun imageReader(): ImageReader = mImageReader
    @SuppressLint("WrongConstant")
    private fun mImageReader(): ImageReader {
        mImageReader =
            ImageReader.newInstance(
                AttributeUtil.getViewWidth(true),
                AttributeUtil.getViewWidth(false),
                PixelFormat.RGBA_8888,
                1
            ).apply {
                setOnImageAvailableListener(
                    ImageAvailableListener(),
                    null
                )
            }
        return mImageReader
    }
    private lateinit var mImageReader: ImageReader

    fun receiveMediaProjection(tMediaProjection: MediaProjection) {
        mMediaProjection = tMediaProjection
    }
    private fun mediaProjection(): MediaProjection = mMediaProjection
    private lateinit var mMediaProjection: MediaProjection

    private fun createStopCallback() {
        imageReader().setOnImageAvailableListener(null, null)
        virtualDisplay().release()
        mediaProjection().stop()
    }

    private fun createMediaWorkers() {
        imageReaders()
        virtualDisplays()
    }

    private fun createFinishToast() {
        Toast.makeText(context(), "Screenshot saved.", Toast.LENGTH_LONG).show()
    }

    fun createWorkerTasks() {
        val delay = PreferenceUtil.getString("delay", "3000").toLong()
        GlobalScope.launch(context = Dispatchers.Main) {
            delay(delay)
            createMediaWorkers()
            if (PreferenceUtil.checkTileMode()) ScreenshotService.stop(context())
        }
    }

    private fun context(): Context = ScreenshotService.Companion.Val.context()

}
