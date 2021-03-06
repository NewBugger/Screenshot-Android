/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.core.choose.reflection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.Surface
import io.github.newbugger.android.screenshot.core.choose.projection.Attribute
import io.github.newbugger.android.screenshot.core.choose.projection.Projection
import io.github.newbugger.android.screenshot.util.PreferenceUtil
import java.lang.reflect.Method


class Reflection(context: Context) {

    fun screenshot() {
        projection.output(reflection())
        projection.createFinishToast()
    }

    // https://www.jianshu.com/p/bbb82b81f2e2
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/
    // android/view/SurfaceControl.java;l=1884
    @SuppressLint("Reflection")
    @Throws(TypeCastException::class, NoSuchMethodException::class, SecurityException::class)
    private fun reflection(): Bitmap {
        val width = attribute.getViewWidth(true)
        val height = attribute.getViewWidth(false)
        val getClass: Class<*> = Class.forName("android.view.SurfaceControl")
        val getMethod: Method = if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
            getClass.getDeclaredMethod("screenshot", Rect::class.java, Int::class.java, Int::class.java, Boolean::class.java, Int::class.java)
        } else {
            getClass.getDeclaredMethod("screenshot", Rect::class.java, Int::class.java, Int::class.java, Int::class.java)
        }
        return if (PreferenceUtil.checkSdkVersion(Build.VERSION_CODES.Q)) {
            getMethod.invoke(null, Rect(), width, height, false, Surface.ROTATION_0) as Bitmap
        } else {
            getMethod.invoke(null, Rect(), width, height, Surface.ROTATION_0) as Bitmap
        }
    }

    private val projection: Projection = Projection.getInstance(context)
    private val attribute: Attribute = Attribute.getInstance(context)

    companion object {
        fun getInstance(ctx: Context): Reflection {
            return Reflection(ctx)
        }
    }

}
