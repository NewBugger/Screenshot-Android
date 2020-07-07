/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.util

import io.github.newbugger.android.screenshot.BuildConfig
import java.util.Locale


object BuildUtil {

    object BuildProperty {
        fun isBuildDebug(): Boolean = BuildConfig.BUILD_TYPE == "debug"
        fun isTimeOut(): Boolean = isTimeOut
        private const val isTimeOut = true
        val APP_NAME = BuildConfig.APPLICATION_ID.split(".").last().toUpperCase(Locale.US)
        const val APP_URI = "https://github.com/NewBugger/Screenshot-Android"
    }

    object Constant {
        object Code {
            const val documentRequestCode = 1001
            const val projectionRequestCode = 1000
        }
    }

}
