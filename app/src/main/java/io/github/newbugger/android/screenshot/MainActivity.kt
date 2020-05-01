/*
 * Copyright (c) 2018-2020.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import java.io.File


class MainActivity : Activity() {  // temporarily a fake and null activity

    private fun setStorageAccess() {
        val requestCode = 232
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PermissionChecker.PERMISSION_GRANTED) {
            Toast.makeText(this, "Storage Permission requested.", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
        // while System Screenshot dir is in /Screenshots
        // use target 28 to temporarily access external storage
        // and use DIRECTORY_PICTURES constant for better experience
        // via: https://developer.android.com/reference/android/os/Environment.html#getExternalStorageDirectory()
        // and: https://developer.android.com/reference/android/os/Environment.html#DIRECTORY_PICTURES
        val fileLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "Screenshot"
        val fileFile = File(fileLocation)
        if (!fileFile.exists()) fileFile.mkdirs()
    }

    /* private fun setDocumentAccess() {
    }

    // temporarily downgrade to target 28
    // before SAF is applied here
    private fun setFiles() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            setStorageAccess()
        } else {
            setDocumentAccess()
        }
    } */

    private fun startForeService() {
        ScreenshotService.start(this)
    }

    // requires a button to stop Service
    /* private fun stopForeService() {
        ScreenshotService.stop(this)
    } */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // setFiles()
        setStorageAccess()
        startForeService()
    }

    /* override fun onResume() {
        startForeService()
        super.onResume()
    }

    override fun onPause() {
        stopForeService()
        super.onPause()
    }

    override fun onDestroy() {
        stopForeService()
        super.onDestroy()
    } */

}
