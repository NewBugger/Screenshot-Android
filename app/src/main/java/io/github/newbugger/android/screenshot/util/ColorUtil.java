/*
 * Copyright (c) 2018-2020 : NewBugger (https://github.com/NewBugger)
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.newbugger.android.screenshot.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.nio.ByteBuffer;


public final class ColorUtil {

    public static void getColor(Bitmap bitmap, ByteBuffer buffer,
                                int height, int width,
                                int pixelStride, int rowPadding) {
        int colorR;
        int colorG;
        int colorB;
        int colorA;
        int colorT;
        int offset = 0;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                /* colorR = buffer.get(offset) & 0xff << 16;     // R
                colorG = buffer.get(offset + 1) & 0xff << 8;  // G
                colorB = buffer.get(offset + 2) & 0xff;       // B
                colorA = buffer.get(offset + 3) & 0xff << 24; // A */
                // separately get ARGB pixels, while do not need a shift
                colorR = buffer.get(offset) & 0xff;     // R
                colorG = buffer.get(offset + 1) & 0xff; // G
                colorB = buffer.get(offset + 2) & 0xff; // B
                colorA = buffer.get(offset + 3) & 0xff; // A
                colorT = Color.argb(colorA, colorR, colorG, colorB);
                bitmap.setPixel(j, i, colorT);
                offset += pixelStride;
            }
            offset += rowPadding;
        }
    }

}
