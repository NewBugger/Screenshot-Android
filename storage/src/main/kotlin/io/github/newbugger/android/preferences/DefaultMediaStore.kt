package io.github.newbugger.android.preferences

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


/**
 * from https://github.com/topjohnwu/Magisk/commit/9e81db8
 */

class DefaultMediaStore(ctx: Context) {

    @Throws(FileNotFoundException::class)
    fun displayName(uri: Uri): String {
        require(uri.scheme == "content") { "Uri lacks 'content' scheme: $uri" }
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val displayNameColumn = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                return cursor.getString(displayNameColumn)
            }
        }
        return uri.toString()
    }

    @Throws(FileNotFoundException::class)
    fun inputStream(uri: Uri) = contentResolver.openInputStream(uri) ?: throw FileNotFoundException()

    @Throws(FileNotFoundException::class)
    fun outputStream(uri: Uri) = contentResolver.openOutputStream(uri) ?: throw FileNotFoundException()

    @RequiresApi(29)
    inner class Images {
        fun getFile(appName: String, displayName: String): MediaStoreFile? {
            return getFile(tableUri, relativePath(appName), displayName)
        }

        fun newFile(appName: String, displayName: String, imgType: String): MediaStoreFile {
            return newFile(tableUri, relativePath(appName), displayName, imgType)
        }

        private fun relativePath(appName: String): String = Environment.DIRECTORY_PICTURES + File.separator + appName

        private val tableUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    @RequiresApi(29)
    inner class Downloads {
        fun getFile(appName: String, displayName: String): MediaStoreFile? {
            return getFile(tableUri, relativePath(appName), displayName)
        }

        fun newFile(appName: String, displayName: String): MediaStoreFile {
            return newFile(tableUri, relativePath(appName), displayName, null)
        }

        private fun relativePath(appName: String): String = Environment.DIRECTORY_DOWNLOADS + File.separator + appName

        private val tableUri: Uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    }

    @RequiresApi(29)
    @Throws(IOException::class)
    private fun getFile(tableUri: Uri, relativePath: String, displayName: String): MediaStoreFile? {
        return queryFile(tableUri, relativePath, displayName)
    }

    @RequiresApi(29)
    @Throws(IOException::class)
    private fun newFile(tableUri: Uri, relativePath: String, displayName: String, mimeType: String?): MediaStoreFile {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insertFile(tableUri, relativePath, displayName, mimeType)
        } else {
            queryFile(tableUri, relativePath, displayName)?.delete()
            insertFile(tableUri, relativePath, displayName, mimeType)
        }
    }

    @RequiresApi(29)
    @Throws(IOException::class)
    private fun queryFile(tableUri: Uri, relativePath: String, displayName: String): MediaStoreFile? {
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.RELATIVE_PATH)

        // Before Android 10, we wrote the DISPLAY_NAME field when insert, so it can be used.
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} == ?"
        val selectionArgs = arrayOf(displayName)
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        contentResolver.query(tableUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val data = cursor.getString(dataColumn)
                if (data.endsWith(relativePath + File.separator + displayName)) {
                    return MediaStoreFile(id, data, tableUri, contentResolver)
                }
            }
        }

        return null
    }

    @RequiresApi(29)
    @Throws(IOException::class)
    private fun insertFile(tableUri: Uri, relativePath: String, displayName: String, mimeType: String?): MediaStoreFile {
        val fileUri = insertValue(tableUri, relativePath, displayName, mimeType)

        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.RELATIVE_PATH)

        contentResolver.query(fileUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idIndex)
                val data = cursor.getString(dataColumn)
                return MediaStoreFile(id, data, tableUri, contentResolver)
            }
        }

        throw IOException("can not query $displayName")
    }

    @RequiresApi(29)
    @Throws(IOException::class)
    private fun insertValue(tableUri: Uri, relativePath: String, displayName: String, mimeType: String?): Uri {
        return ContentValues(3).apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            if (mimeType != null) {
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            }
        }.let {
            // before Android 11, MediaStore can not rename new file when file exists,
            // insert will return null. use newFile() instead.
            contentResolver.insert(tableUri, it)
        } ?: throw IOException("can not insert $displayName")
    }

    private val contentResolver: ContentResolver by lazy { ctx.contentResolver }

    data class MediaStoreFile(private val id: Long,
                              private val data: String,
                              private val tableUri: Uri,
                              private val contentResolver: ContentResolver) {
        val uri: Uri = ContentUris.withAppendedId(tableUri, id)
        override fun toString() = data

        @Throws(IOException::class)
        fun delete(): Boolean {
            val selection = "${MediaStore.MediaColumns._ID} == ?"
            val selectionArgs = arrayOf(id.toString())
            return contentResolver.delete(uri, selection, selectionArgs) == 1
        }
    }

    companion object {
        val Context.defaultMediaStore: DefaultMediaStore
            get() = DefaultMediaStore(this)
    }

}
