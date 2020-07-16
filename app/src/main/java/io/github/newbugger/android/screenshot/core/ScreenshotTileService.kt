/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core

import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import io.github.newbugger.android.screenshot.util.PreferenceUtil


class ScreenshotTileService: TileService() {

    private fun tileScreenShot(context: Context) {
        Intent(context, ScreenshotActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            .also { intent ->
                startActivity(intent)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceUtil.receiveTileMode(false)
    }

    override fun onCreate() {
        super.onCreate()
        PreferenceUtil.receiveTileMode(true)
    }

    override fun onClick() {
        super.onClick()
        tileScreenShot(this)
    }

}
