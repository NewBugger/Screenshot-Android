/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core.projection

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
import io.github.newbugger.android.preferences.DefaultMediaStore.Companion.defaultMediaStore
import io.github.newbugger.android.screenshot.service.ScreenshotService
import io.github.newbugger.android.screenshot.util.ColorUtil
import io.github.newbugger.android.screenshot.util.PreferenceUtil
import java.nio.ByteBuffer


class Projection(ctx: Context) {

    private inner class ImageAvailableListener: ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            createStopCallback()  // instantly stop callbacks as avoiding over-produce 2 pictures
            val onViewWidth = attribute().getViewWidth(true)
            val onViewHeight = attribute().getViewWidth(false)
            val image: Image = reader.acquireNextImage()  // https://stackoverflow.com/a/38786747
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val bitmap = Bitmap.createBitmap(
                attribute().getDisplayMetrics(),
                onViewWidth,
                onViewHeight,
                Bitmap.Config.ARGB_8888,
                false
            )
            if (PreferenceUtil.getBoolean(context, "setPixel", true)) {
                // TODO: find a method more efficient
                // TODO: Android 11 provides a context.Screenshot() method
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * onViewWidth
                ColorUtil.getColor(
                    bitmap,
                    buffer,
                    onViewHeight,
                    onViewWidth,
                    pixelStride,
                    rowPadding
                )
            } else {
                // TODO:: bug: on this method -> picture not available
                bitmap.copyPixelsFromBuffer(buffer)
            }
            output(bitmap)
            buffer.clear()
            bitmap.recycle()
            image.close()
        }
    }

    fun output(bitmap: Bitmap) {
        val fileDocument = if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
            attribute().getFileResolver()
        } else {
            attribute().getFileDocument()
        }
        context.defaultMediaStore.outputStream(fileDocument).use { fileOutputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        }
    }

    private fun mVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
            "screenshot",
            attribute().getViewWidth(true),
            attribute().getViewWidth(false),
            attribute().getDisplayMetrics().densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            mImageReader.surface,
            null,
            null
        )
    }
    private lateinit var mVirtualDisplay: VirtualDisplay

    @SuppressLint("WrongConstant")
    private fun mImageReader() {
        mImageReader =
            ImageReader.newInstance(
                attribute().getViewWidth(true),
                attribute().getViewWidth(false),
                PixelFormat.RGBA_8888,
                1
            ).apply {
                setOnImageAvailableListener(
                    ImageAvailableListener(),
                    null
                )
            }
    }
    private lateinit var mImageReader: ImageReader

    private fun mMediaProjection() {
        mMediaProjection = ReceiveUtil.getMediaProjection()
    }
    private lateinit var mMediaProjection: MediaProjection

    private fun createStopCallback() {
        mImageReader.setOnImageAvailableListener(null, null)
        mVirtualDisplay.release()
        mMediaProjection.stop()
    }

    private fun createStartCaller() {
        mMediaProjection()
        mImageReader()
        mVirtualDisplay()
    }

    fun createFinishToast() {
        Toast.makeText(context, "Screenshot saved.", Toast.LENGTH_LONG).show()
    }

    fun createWorkerTasks() {
        createStartCaller()
        createFinishToast()
        if (ReceiveUtil.checkTileMode()) ScreenshotService.stop(context)
    }

    private fun attribute(): Attribute = attribute
    private val attribute: Attribute = Attribute.getInstance(ctx)

    private val context: Context = ctx

    companion object {
        fun getInstance(ctx: Context): Projection {
            return Projection(ctx)
        }
    }

}
