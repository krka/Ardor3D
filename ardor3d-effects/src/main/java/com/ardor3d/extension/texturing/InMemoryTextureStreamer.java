/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.texturing;

import java.nio.ByteBuffer;
import java.util.List;

import com.ardor3d.math.MathUtils;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;

public class InMemoryTextureStreamer implements TextureStreamer {
    private final int sourceSize;
    private final int textureSize;
    private final int validLevels;

    private static class MemData {
        public int sizeX, sizeY;
        public ByteBuffer imageSource;
    }

    private final List<MemData> memDataList = Lists.newArrayList();

    public InMemoryTextureStreamer(final ByteBuffer sourceBuffer, final int sourceSize, final int textureSize) {
        this.sourceSize = sourceSize;
        this.textureSize = textureSize;

        validLevels = powersUpTo(textureSize, sourceSize);
        for (int l = 0; l < validLevels; l++) {
            memDataList.add(new MemData());
        }

        createMipmaps(sourceBuffer);
    }

    public void updateLevel(final int unit, final int sX, final int sY) {

    }

    public void copyImage(final int unit, final ByteBuffer sliceData, final int dX, final int dY, final int sX,
            final int sY, final int w, final int h) {
        final MemData memData = memDataList.get(unit);
        final int sizeX = memData.sizeX;
        final int sizeY = memData.sizeY;
        final ByteBuffer imageSource = memData.imageSource;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int sourceX = MathUtils.moduloPositive(sX + x, sizeX);
                final int sourceY = MathUtils.moduloPositive(sY + y, sizeY);
                final int indexSource = (sourceY * sizeX + sourceX) * 3;

                final int destX = MathUtils.moduloPositive(dX + x, textureSize);
                final int destY = MathUtils.moduloPositive(dY + y, textureSize);
                final int indexDest = (destY * textureSize + destX) * 3;

                sliceData.put(indexDest, imageSource.get(indexSource));
                sliceData.put(indexDest + 1, imageSource.get(indexSource + 1));
                sliceData.put(indexDest + 2, imageSource.get(indexSource + 2));
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

    private void createMipmaps(ByteBuffer sourceBuffer) {
        int currentSize = sourceSize;
        int parentSize = currentSize;

        MemData memData = memDataList.get(0);
        memData.imageSource = sourceBuffer;
        memData.sizeX = currentSize;
        memData.sizeY = currentSize;

        currentSize /= 2;

        for (int l = 1; l < validLevels; l++) {
            final ByteBuffer destBuffer = BufferUtils.createByteBuffer(currentSize * currentSize * 3);

            memData = memDataList.get(l);
            memData.imageSource = destBuffer;
            memData.sizeX = currentSize;
            memData.sizeY = currentSize;

            for (int y = 0; y < currentSize - 1; y++) {
                for (int x = 0; x < currentSize - 1; x++) {
                    final int destIndex = (y * currentSize + x) * 3;

                    final int sourceIndex = (x * 2 + y * 2 * parentSize) * 3;
                    destBuffer.put(destIndex, sourceBuffer.get(sourceIndex));
                    destBuffer.put(destIndex + 1, sourceBuffer.get(sourceIndex + 1));
                    destBuffer.put(destIndex + 2, sourceBuffer.get(sourceIndex + 2));

                    // int sourceIndex = (x * 2 + y * 2 * parentSize) * 3;
                    // col.set(sourceBuffer.get(sourceIndex) + 127, sourceBuffer.get(sourceIndex + 1) + 127,
                    // sourceBuffer
                    // .get(sourceIndex + 2) + 127, 0.0f);
                    // sourceIndex += 1 * 3;
                    // col.addLocal(sourceBuffer.get(sourceIndex) + 127, sourceBuffer.get(sourceIndex + 1) + 127,
                    // sourceBuffer.get(sourceIndex + 2) + 127, 0.0f);
                    // sourceIndex += (parentSize - 1) * 3;
                    // col.addLocal(sourceBuffer.get(sourceIndex) + 127, sourceBuffer.get(sourceIndex + 1) + 127,
                    // sourceBuffer.get(sourceIndex + 2) + 127, 0.0f);
                    // sourceIndex += 1 * 3;
                    // col.addLocal(sourceBuffer.get(sourceIndex) + 127, sourceBuffer.get(sourceIndex + 1) + 127,
                    // sourceBuffer.get(sourceIndex + 2) + 127, 0.0f);
                    // col.divideLocal(4.0f);
                    // final byte red = (byte) ((int) (col.getRed() - 127) & 0xFF);
                    // final byte green = (byte) ((int) (col.getGreen() - 127) & 0xFF);
                    // final byte blue = (byte) ((int) (col.getBlue() - 127) & 0xFF);
                    //
                    // destBuffer.put(destIndex, red);
                    // destBuffer.put(destIndex + 1, green);
                    // destBuffer.put(destIndex + 2, blue);
                }
            }

            sourceBuffer = destBuffer;

            currentSize /= 2;
            parentSize /= 2;
        }
    }

    public int getTextureSize() {
        return textureSize;
    }

    public int getValidLevels() {
        return validLevels;
    }

    public boolean isReady(final int unit) {
        return true;
    }
}
