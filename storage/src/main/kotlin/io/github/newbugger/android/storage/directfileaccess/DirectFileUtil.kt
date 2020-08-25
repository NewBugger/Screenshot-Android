/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.storage.directfileaccess

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


object DirectFileUtil {

    @Throws(SecurityException::class, IOException::class, FileNotFoundException::class)
    fun Context.getExternalPath(path: String): String =
            if (Build.VERSION.SDK_INT >= 29) {
                this.getExternalFilesDirPath(path)
            } else {
                this.getExternalStorageDirectoryPath(path)
            }

    @Throws(SecurityException::class, IOException::class, FileNotFoundException::class)
    private fun Context.getExternalFilesDirPath(path: String): String =
            (this.getExternalFilesDir() + File.separator + path).also {
                if (!File(it).exists()) File(it).mkdir()
            }

    @Throws(SecurityException::class, IOException::class, FileNotFoundException::class)
    private fun Context.getExternalFilesDir(): String =
            (this.getExternalFilesDir(null)!!.absolutePath).also {
                if (!File(it).exists()) File(it).mkdir()
            }

    @TargetApi(28)
    @Throws(SecurityException::class, IOException::class, FileNotFoundException::class)
    private fun Context.getExternalStorageDirectoryPath(path: String): String =
            (this.getExternalStorageDirectory() + File.separator + path).also {
                if (!File(it).exists()) File(it).mkdir()
            }

    @TargetApi(28)
    @Throws(SecurityException::class, IOException::class, FileNotFoundException::class)
    private fun Context.getExternalStorageDirectory(): String =
            this.let {
                it.requestStorageReadPermission()
                it.requestStorageWritePermission()
                Environment.getExternalStorageDirectory().absolutePath
            }

    @TargetApi(28)
    private fun Context.requestStorageWritePermission(): Boolean {
        val requestCode = 1
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this as Activity, arrayOf(permission), requestCode)
        }
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun Context.requestStorageReadPermission(): Boolean {
        val requestCode = 1
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this as Activity, arrayOf(permission), requestCode)
        }
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

}
