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
import android.view.Menu
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.newbugger.android.screenshot.R
import io.github.newbugger.android.screenshot.service.ScreenshotService
import io.github.newbugger.android.screenshot.util.BuildUtil
import io.github.newbugger.android.screenshot.util.PreferenceUtil
import io.github.newbugger.android.storage.storageaccessframework.SAFUtil.intentActionOpenDocumentTree
import io.github.newbugger.android.storage.storageaccessframework.SAFUtil.takePersistableUriPermission


class MainActivity : AppCompatActivity() {

    private fun startForeService() {
        ScreenshotService.startForeground(this)
    }

    private fun stopForeService() {
        ScreenshotService.stop(this)
    }

    // https://github.com/android/storage-samples/blob/master/ActionOpenDocumentTree/app/src/main/
    // java/com/example/android/ktfiles/MainActivity.kt
    // https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
    private fun setDocumentAccess() {
        if (!PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
            if (PreferenceUtil.checkDirectory(this)) {
                Toast.makeText(this, "Storage Access requesting..", Toast.LENGTH_LONG).show()
                startActivityForResult(intentActionOpenDocumentTree(), BuildUtil.Constant.Code.documentRequestCode)
            } else {
                Toast.makeText(this, "Storage Access requested.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                BuildUtil.Constant.Code.documentRequestCode -> {
                    val directoryUri = data.data ?: return
                    this.takePersistableUriPermission(directoryUri)
                }
                else -> return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar_main))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.action_sync)
            .setActionView(R.layout.activity_toggle)
            .actionView.findViewById<Switch>(R.id.main_toggle)
            .apply {
                isChecked = ScreenshotService.Companion.Val.checkForeground()
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked)
                        startForeService()
                    else
                        stopForeService()
                }
            }
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_settings,
                SettingsFragment()
            )
            .commit()
        setDocumentAccess()
        return true
    }

}
