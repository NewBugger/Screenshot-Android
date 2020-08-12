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
import io.github.newbugger.android.screenshot.core.projection.Projection
import io.github.newbugger.android.screenshot.core.reflection.Reflection
import io.github.newbugger.android.screenshot.util.PreferenceUtil


class Choose(ctx: Context) {

    fun screenshot() {
        if (PreferenceUtil.getBoolean(context, "reflection", false)) {
            reflection()
        } else {
            projection()
        }
    }

    private fun reflection() {
        reflection.screenshot()
    }

    private fun projection() {
        projection.createWorkerTasks()
    }

    private val reflection: Reflection by lazy {
        Reflection.getInstance(ctx)
    }

    private val projection: Projection by lazy {
        Projection.getInstance(ctx)
    }

    private val context: Context = ctx

    companion object {
        fun getInstance(ctx: Context): Choose {
            return Choose(ctx)
        }
    }

}
