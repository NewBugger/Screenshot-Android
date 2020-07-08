/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.github.newbugger.android.screenshot.R
import io.github.newbugger.android.screenshot.core.ScreenshotService
import io.github.newbugger.android.screenshot.util.BuildUtil
import io.github.newbugger.android.screenshot.util.PreferenceUtil


class MainActivity : AppCompatActivity() {

    // https://github.com/android/storage-samples/blob/master/ActionOpenDocumentTree/app/src/main/
    // java/com/example/android/ktfiles/MainActivity.kt
    // https://developer.android.com/training/data-storage/shared/documents-files
    // #grant-access-directory
    private fun setDocumentAccess() {
        if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q) || PreferenceUtil.checkDirectory()) return
        Toast.makeText(this, "Storage Access requested.", Toast.LENGTH_LONG).show()
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            intent.putExtra(EXTRA_INITIAL_URI, DIRECTORY_PICTURES)
            startActivityForResult(intent, BuildUtil.Constant.Code.documentRequestCode)
        }
    }

    private fun startForeService() {
        ScreenshotService.startForeground(this)
    }

    private fun stopForeService() {
        ScreenshotService.stop(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                BuildUtil.Constant.Code.documentRequestCode -> {
                    val directoryUri = data.data ?: return
                    // reduce the uri permission level,
                    // use mediaStore instead already.
                    contentResolver.takePersistableUriPermission(
                        directoryUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    PreferenceUtil.putString("directory", directoryUri.toString())
                }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // TODO: Activity() + setActionBar(), AppCompatActivity() + setSupportActionBar()
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
        setSupportActionBar(toolbar)  // setActionBar(toolbar)
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_settings,
                SettingsFragment()
            )
            .commit()
        startForeService()
        setDocumentAccess()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_stopper) stopForeService()
        return super.onOptionsItemSelected(item)
    }

}
