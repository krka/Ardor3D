/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.texturing;

import java.nio.ByteBuffer;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

public class ProceduralTextureStreamer implements TextureStreamer {
    private final int _textureSliceSize;
    private final int _validLevels;

    private final Function3D _function;
    private final ReadOnlyColorRGBA[] _terrainColors;

    public ProceduralTextureStreamer(final int sourceSize, final int textureSliceSize, final Function3D function,
            final ReadOnlyColorRGBA[] terrainColors) {
        _textureSliceSize = textureSliceSize;
        _validLevels = powersUpTo(textureSliceSize, sourceSize);

        _function = function;
        _terrainColors = terrainColors;
    }

    public void updateLevel(final int unit, final int sX, final int sY) {
        ; // nothing to do?
    }

    public void copyImage(final int unit, final ByteBuffer sliceData, final int dX, final int dY, final int sX,
            final int sY, final int w, final int h) {
        final double scale = Math.pow(2, unit);
        final byte[] color = new byte[3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int destX = MathUtils.moduloPositive(dX + x, _textureSliceSize);
                final int destY = MathUtils.moduloPositive(dY + y, _textureSliceSize);
                final int indexDest = (destY * _textureSliceSize + destX) * 3;

                // eval our function at the given slice and location
                double val = _function.eval((sX + x) * scale, (sY + y) * scale, 0);

                // Keep us in [-1, 1]
                val = MathUtils.clamp(val, -1, 1);

                // Convert to [0, 255]
                val = ((val + 1) * 0.5) * 255.0;

                // get us a color
                final byte colIndex = (byte) val;
                final ReadOnlyColorRGBA c = _terrainColors[colIndex & 0xFF];

                // place color channels in byte array
                color[0] = (byte) (c.getRed() * 255);
                color[1] = (byte) (c.getGreen() * 255);
                color[2] = (byte) (c.getBlue() * 255);

                // Place array into byte buffer
                sliceData.position(indexDest);
                sliceData.put(color);
            }
        }
    }

    private int powersUpTo(int value, final int max) {
        int powers = 0;
        for (; value <= max; value *= 2, powers++) {
            System.out.println("value:" + value + " powers: " + powers);
        }
        return powers;
    }

    public int getValidLevels() {
        return _validLevels;
    }

    public boolean isReady(final int unit) {
        return true;
    }
}
