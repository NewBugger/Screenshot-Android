/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core.projection

import android.media.projection.MediaProjection


object ReceiveUtil {

    fun receiveTileMode(tTileMode: Boolean) {
        checkTileMode = tTileMode
    }
    fun checkTileMode(): Boolean = checkTileMode
    private var checkTileMode: Boolean = false

    fun receiveMediaProjection(tMediaProjection: MediaProjection) {
        mMediaProjection = tMediaProjection
    }
    fun getMediaProjection(): MediaProjection = mMediaProjection
    private lateinit var mMediaProjection: MediaProjection

}
