/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.storage.storageaccessframework

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.io.FileNotFoundException
import java.io.IOException


object SAFUtil {

    // todo: reduce the uri permission level ?
    fun Context.takePersistableUriPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri.also { preferencesPersistableUriPut(it) },
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun Context.intentActionOpenDocumentTree(requestCode: Int) {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_PICTURES)
            ActivityCompat.startActivityForResult(this as Activity, intent, requestCode, null)
        }
    }

    @Throws(SecurityException::class, IOException::class, FileNotFoundException::class)
    fun Context.preferencesPersistableUriGet(fileName: String, mimeType: String): Uri =
        (preferencesPersistableUriGet().let {
            Uri.parse(it)
        }.let {
            DocumentFile.fromTreeUri(this, it)
        }.let {
            if (it?.exists() == true) it.delete()
            it?.createFile(mimeType, fileName)
        }?.uri) ?: throw FileNotFoundException()

    fun Context.preferencesPersistableUriGet(): String? =
        PreferenceManager.getDefaultSharedPreferences(this).getString("saf", null)

    private fun Context.preferencesPersistableUriPut(uri: Uri) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("saf", uri.toString()).apply()
    }

}
