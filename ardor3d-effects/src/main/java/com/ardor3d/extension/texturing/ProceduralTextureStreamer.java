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

import com.ardor3d.image.util.GeneratedImageFactory;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.function.FbmFunction3D;
import com.ardor3d.math.function.Function3D;
import com.ardor3d.math.function.Functions;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

public class ProceduralTextureStreamer implements TextureStreamer {
    private final int sourceSize;
    private final int textureSize;
    private final int validLevels;

    Function3D terrain;

    public ProceduralTextureStreamer(final int sourceSize, final int textureSize) {
        this.sourceSize = sourceSize;
        this.textureSize = textureSize;
        validLevels = powersUpTo(textureSize, sourceSize);

        createFunction();
    }

    public void updateLevel(final int unit, final int sX, final int sY) {

    }

    public void copyImage(final int unit, final ByteBuffer sliceData, final int dX, final int dY, final int sX,
            final int sY, final int w, final int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int destX = MathUtils.moduloPositive(dX + x, textureSize);
                final int destY = MathUtils.moduloPositive(dY + y, textureSize);
                final int indexDest = (destY * textureSize + destX) * 3;

                double val = terrain.eval((sX + x) * Math.pow(2, unit) / 2048.0, (sY + y) * Math.pow(2, unit) / 2048.0,
                        0);
                val = val * 128 + 128;
                final byte colIndex = (byte) MathUtils.floor(val);
                final ReadOnlyColorRGBA c = terrainColors[colIndex & 0xFF];

                sliceData.put(indexDest, (byte) (c.getRed() * 255));
                sliceData.put(indexDest + 1, (byte) (c.getGreen() * 255));
                sliceData.put(indexDest + 2, (byte) (c.getBlue() * 255));
            }
        }
    }

    ReadOnlyColorRGBA[] terrainColors;

    private void createFunction() {
        terrain = Functions.scaleInput(Functions.simplexNoise(), 2, 2, 1);
        terrainColors = new ReadOnlyColorRGBA[256];
        terrainColors[0] = new ColorRGBA(0, 0, .5f, 1);
        terrainColors[95] = new ColorRGBA(0, 0, 1, 1);
        terrainColors[127] = new ColorRGBA(0, .5f, 1, 1);
        terrainColors[137] = new ColorRGBA(240 / 255f, 240 / 255f, 64 / 255f, 1);
        terrainColors[143] = new ColorRGBA(32 / 255f, 160 / 255f, 0, 1);
        terrainColors[175] = new ColorRGBA(224 / 255f, 224 / 255f, 0, 1);
        terrainColors[223] = new ColorRGBA(128 / 255f, 128 / 255f, 128 / 255f, 1);
        terrainColors[255] = ColorRGBA.WHITE;
        GeneratedImageFactory.fillInColorTable(terrainColors);

        terrain = new FbmFunction3D(terrain, 5, 0.5, 0.5, 3.14);
    }

    private int powersUpTo(int value, final int max) {
        int powers = 0;
        for (; value <= max; value *= 2, powers++) {
            System.out.println("value:" + value + " powers: " + powers);
        }
        return powers;
    }

    public int getValidLevels() {
        return validLevels;
    }

    public boolean isReady(final int unit) {
        return true;
    }
}
