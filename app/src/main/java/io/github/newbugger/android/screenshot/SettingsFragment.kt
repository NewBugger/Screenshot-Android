/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import androidx.preference.SwitchPreference


class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = preferenceManager.sharedPreferences
        val saf: SwitchPreference = findPreference("saf")!!
        saf.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
            val checked = java.lang.Boolean.valueOf(value.toString())
            if (checked) {
                sharedPreferences.edit().putBoolean("saf", true).apply()
                true
            } else {
                sharedPreferences.edit().putBoolean("saf", false).apply()
                true
            }
        }
        val setPixel: SwitchPreference = findPreference("setPixel")!!
        setPixel.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
            val checked = java.lang.Boolean.valueOf(value.toString())
            if (checked) {
                sharedPreferences.edit().putBoolean("setPixel", true).apply()
                true
            } else {
                sharedPreferences.edit().putBoolean("setPixel", false).apply()
                true
            }
        }
    }

}
