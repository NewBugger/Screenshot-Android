/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.preference.PreferenceManager
import java.io.File


class MainActivity : Activity() {  // temporarily a fake and null activity

    private val documentRequestCode = 1001

    private fun setStorageAccess() {
        val requestCode = 232
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PermissionChecker.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission requested.", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        }
        // while System Screenshot dir is in /Screenshots
        // use target 28 to temporarily access external storage
        // and use DIRECTORY_PICTURES constant for better experience
        // via: https://developer.android.com/reference/android/os/Environment.html#getExternalStorageDirectory()
        // and: https://developer.android.com/reference/android/os/Environment.html#DIRECTORY_PICTURES
        val fileLocation = getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString() + File.separator + "Screenshot"
        val fileFile = File(fileLocation)
        if (!fileFile.exists()) fileFile.mkdirs()
    }

    // https://github.com/android/storage-samples/blob/master/ActionOpenDocumentTree/app/src/main/java/com/example/android/ktfiles/MainActivity.kt
    // https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
    private fun setDocumentAccess() {
        Toast.makeText(this, "Storage Access requested.", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION and
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            putExtra(EXTRA_INITIAL_URI, DIRECTORY_PICTURES)
        }
        startActivityForResult(intent, documentRequestCode)
    }

    private fun setFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setDocumentAccess()
        } else {
            setStorageAccess()
        }
    }

    private fun startForeService() {
        ScreenshotService.start(this)
    }

    // requires a button to stop Service
    /* private fun stopForeService() {
        ScreenshotService.stop(this)
    } */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                documentRequestCode -> {
                    val directoryUri = data.data ?: return
                    contentResolver.takePersistableUriPermission(
                        directoryUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION and Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                        preferences.edit().putString("directory", directoryUri.toString()).apply()
                    }
                }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setFiles()
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
