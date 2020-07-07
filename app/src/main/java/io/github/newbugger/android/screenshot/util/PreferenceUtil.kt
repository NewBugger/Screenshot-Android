/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import io.github.newbugger.android.screenshot.core.ScreenshotService


object PreferenceUtil {

    private fun setContextPreferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun checkSdkVersion(num: Int): Boolean =
        Build.VERSION.SDK_INT >= num

    fun checkDirectory(context: Context): Boolean =
        setContextPreferences(context).getString("directory", null) != null

    fun putString(context: Context, id: String, value: String) {
        setContextPreferences(context).edit().putString(id, value).apply()
    }

    fun getString(id: String, default: String): String =
        setContextPreferences(context()).getString(id, default)!!

    fun getBoolean(id: String): Boolean =
        setContextPreferences(context()).getBoolean(id, true)

    private fun context(): Context = ScreenshotService.Companion.Val.context()

}
