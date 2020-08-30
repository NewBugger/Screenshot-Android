/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.newbugger.android.screenshot.core.choose.projection.Projection


@RequiresApi(30)
class ScreenshotAccessibilityService: AccessibilityService() {

    private fun printScreenshot(bitmap: Bitmap) {
        Projection.getInstance(this).output(bitmap)
    }

    private fun takeScreenshot() {
        this.takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, takeScreenshotCallback)
    }

    private val takeScreenshotCallback = object: TakeScreenshotCallback {
        override fun onSuccess(screenshotResult: ScreenshotResult) {
            val bitmap = Bitmap.wrapHardwareBuffer(screenshotResult.hardwareBuffer, screenshotResult.colorSpace) ?: return
            printScreenshot(bitmap)
            onToast(false)
        }

        override fun onFailure(errorCode: Int) {
            onToast(true)
            this@ScreenshotAccessibilityService.disableSelf()
        }

        private fun onToast(failure: Boolean) {
            val toast = if (failure) { "Screenshot failure." } else { "Screenshot success." }
            Toast.makeText(this@ScreenshotAccessibilityService, toast, Toast.LENGTH_LONG).show()
        }
    }

    private val listener = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.extras?.getString(const_action) == const_takeScreenshot) {
                takeScreenshot()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // AccessibilityServiceInfo cannot dynamically change the CAPABILITY configuration
        /*AccessibilityServiceInfo().also {
            it.packageNames = arrayOf(BuildConfig.APPLICATION_ID)
            it.flags = AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT
            serviceInfo = it
        }*/
        localBroadcastManager.registerReceiver(listener, IntentFilter(const_takeScreenshot))
    }

    override fun onUnbind(intent: Intent?): Boolean {
        localBroadcastManager.unregisterReceiver(listener)
        return super.onUnbind(intent)
    }

    private val localBroadcastManager: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(this) }

    companion object {
        const val const_takeScreenshot = "takeScreenshot()"
        const val const_action = "action"
    }

}
