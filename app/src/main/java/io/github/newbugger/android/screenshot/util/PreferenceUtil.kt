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


object PreferenceUtil {

    private fun setContextPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun checkSdkVersion(num: Int): Boolean {
        return Build.VERSION.SDK_INT >= num
    }

    fun checkDirectory(context: Context): Boolean {
        return setContextPreferences(context).getString("directory", null) != null
    }

    fun putString(context: Context, id: String, value: String) {
        setContextPreferences(context).edit().putString(id, value).apply()
    }

    fun getString(context: Context, id: String, default: String): String {
        return setContextPreferences(context).getString(id, default)!!
    }

    fun getString(pref: SharedPreferences, id: String, default: String): String {
        return pref.getString(id, default)!!
    }

    fun getBoolean(context: Context, id: String): Boolean {
        return setContextPreferences(context).getBoolean(id, true)
    }

    fun getBoolean(pref: SharedPreferences, id: String): Boolean {
        return pref.getBoolean(id, true)
    }

}
