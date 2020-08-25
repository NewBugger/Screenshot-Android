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
import io.github.newbugger.android.storage.storageaccessframework.SAFUtil.preferencesPersistableUriGet


object PreferenceUtil {

    private fun preferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun checkDirectory(context: Context): Boolean =
        context.preferencesPersistableUriGet() == null

    fun putString(context: Context, id: String, value: String) {
        preferences(context).edit().putString(id, value).apply()
    }

    fun getString(context: Context, id: String, default: String): String =
        preferences(context).getString(id, default)!!

    fun getBoolean(context: Context, id: String, default: Boolean): Boolean =
        preferences(context).getBoolean(id, default)

    fun checkSdkVersion(num: Int): Boolean =
        Build.VERSION.SDK_INT >= num

}
