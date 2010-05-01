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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.effect.water.ImprovedNoise;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.google.common.collect.Lists;

public class SimpleFileTextureStreamer implements TextureStreamer {
    private static final Logger logger = Logger.getLogger(SimpleFileTextureStreamer.class.getName());

    private final int sourceSize;
    private final int textureSize;
    private final int validLevels;

    private static class MemData {
        public int sizeX, sizeY;
        public ByteBuffer imageSource;
    }

    private final List<MemData> memDataList = Lists.newArrayList();

    public SimpleFileTextureStreamer(final int sourceSize, final int textureSize) {
        this.sourceSize = sourceSize;
        this.textureSize = textureSize;

        validLevels = powersUpTo(textureSize, sourceSize);
        for (int l = 0; l < validLevels; l++) {
            memDataList.add(new MemData());
        }

        if (!new File("texture0").exists()) {
            createMipmaps();
        }

        // write down mipmaps as files. presplit into tiles?
        int currentSize = sourceSize;
        for (int l = 0; l < validLevels; l++) {
            final MemData memData = memDataList.get(l);

            memData.sizeX = currentSize;
            memData.sizeY = currentSize;

            try {
                // Create a read-only memory-mapped file
                final FileChannel roChannel = new RandomAccessFile("texture" + l, "r").getChannel();
                final ByteBuffer roBuf = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
                memData.imageSource = roBuf;
            } catch (final IOException e) {
                e.printStackTrace();
            }

            currentSize /= 2;
        }

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

    ReadOnlyColorRGBA[] terrainColors;

    private void createMipmaps() {
        int currentSize = sourceSize;

        int diff = 1;
        for (int l = 0; l < validLevels; l++) {
            ByteBuffer destBuffer = null;
            try {
                destBuffer = new RandomAccessFile("texture" + l, "rw").getChannel().map(FileChannel.MapMode.READ_WRITE,
                        0, currentSize * currentSize * 3);
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Exception creating texture file", e);
                return;
            }

            for (int y = 0; y < currentSize; y++) {
                for (int x = 0; x < currentSize; x++) {
                    // final int sourceIndex = (x * diff + diff / 2 + (y * diff + diff / 2) * SOURCE_SIZE) * 3;
                    final int destIndex = (y * currentSize + x) * 3;

                    final byte fish = (byte) ((int) (ImprovedNoise.noise(x * diff * 0.01, y * diff * 0.01, 0) * 255) & 0xff);
                    destBuffer.put(destIndex, fish);
                    destBuffer.put(destIndex + 1, fish);
                    destBuffer.put(destIndex + 2, fish);
                    // destBuffer.put(destIndex, sourceBuffer.get(sourceIndex));
                    // destBuffer.put(destIndex + 1, sourceBuffer.get(sourceIndex + 1));
                    // destBuffer.put(destIndex + 2, sourceBuffer.get(sourceIndex + 2));
                }
            }

            currentSize /= 2;
            diff *= 2;
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
