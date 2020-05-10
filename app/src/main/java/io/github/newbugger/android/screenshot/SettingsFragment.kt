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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreference


class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    private lateinit var sharedPreferences: SharedPreferences

    private fun setSwitch(preference: SwitchPreference) {
        val key = preference.key
        preference.onPreferenceChangeListener = OnPreferenceChangeListener { _, value ->
            val checked = java.lang.Boolean.valueOf(value.toString())
            if (checked) {
                sharedPreferences.edit().putBoolean(key, true).apply()
                true
            } else {
                sharedPreferences.edit().putBoolean(key, false).apply()
                true
            }
        }
    }

    private fun setEditText(preference: EditTextPreference) {
        val text = preference.text  // never be null
        preference.summary = "$text ms"  // set View
    }

    private fun setSummary(preference: Preference) {
        when (preference) {
            is SwitchPreference -> setSwitch(preference)
            is EditTextPreference -> setEditText(preference)
            else -> return
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val preference: Preference = findPreference(key)!!
        setSummary(preference)  // smart cast
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = preferenceManager.sharedPreferences
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

}
