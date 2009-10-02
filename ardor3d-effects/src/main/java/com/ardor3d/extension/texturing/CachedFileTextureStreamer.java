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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.effect.water.ImprovedNoise;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;

public class CachedFileTextureStreamer implements TextureStreamer {
    private final int sourceSize;
    private final int textureSize;
    private final int validLevels;

    private final int tileSize;
    private final int cacheSize;

    class MemData {
        public int sizeX, sizeY;
        public ByteBuffer imageSource;

        public ByteBuffer cache;
        public int tileX, tileY;

        public AtomicBoolean ready = new AtomicBoolean(true);

        public MemData() {
            cache = BufferUtils.createByteBuffer(cacheSize * cacheSize * 3);
        }
    }

    private final List<MemData> memDataList = Lists.newArrayList();

    public CachedFileTextureStreamer(final int sourceSize, final int textureSize) {
        this.sourceSize = sourceSize;
        this.textureSize = textureSize;

        tileSize = textureSize;
        cacheSize = tileSize * 3;

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
        final MemData memData = memDataList.get(unit);

        if (!memData.ready.get()) {
            return;
        }

        int tileX = (int) Math.floor(sX / tileSize);
        int tileY = (int) Math.floor(sY / tileSize);

        final int diffX = memData.tileX - tileX;
        final int diffY = memData.tileY - tileY;
        if (diffX == 0 && diffY == 0) {
            return;
        }

        // memData.ready.set(false);

        memData.tileX = tileX;
        memData.tileY = tileY;

        tileX += diffX;
        tileY += diffY;
        final int startX = tileX * tileSize;
        final int startY = tileY * tileSize;

        tileX = MathUtils.moduloPositive(tileX, 3);
        tileY = MathUtils.moduloPositive(tileY, 3);

        // System.out.println(tileX + ", " + tileY);

        updateCache(startX, startY, memData, tileX, tileY);

    }

    private void updateCache(final int sX, final int sY, final MemData memData, final int tileX, final int tileY) {
        new Thread(new Runnable() {
            public void run() {
                final int sizeX = memData.sizeX;
                final int sizeY = memData.sizeY;
                final ByteBuffer imageSource = memData.imageSource;
                final ByteBuffer cacheData = memData.cache;

                for (int y = 0; y < sizeY; y++) {
                    for (int x = 0; x < sizeX; x++) {
                        final int sourceX = MathUtils.moduloPositive(sX + x, sizeX);
                        final int sourceY = MathUtils.moduloPositive(sY + y, sizeY);
                        final int indexSource = (sourceY * sizeX + sourceX) * 3;

                        final int destX = MathUtils.moduloPositive(x + tileX * tileSize, cacheSize);
                        final int destY = MathUtils.moduloPositive(y + tileY * tileSize, cacheSize);
                        final int indexDest = (destY * cacheSize + destX) * 3;

                        cacheData.put(indexDest, imageSource.get(indexSource));
                        cacheData.put(indexDest + 1, imageSource.get(indexSource + 1));
                        cacheData.put(indexDest + 2, imageSource.get(indexSource + 2));
                    }
                }

                // memData.ready.set(true);
            }
        }, "updateCache").start();
    }

    public void copyImage(final int unit, final ByteBuffer sliceData, final int dX, final int dY, final int sX,
            final int sY, final int w, final int h) {
        final MemData memData = memDataList.get(unit);
        final ByteBuffer imageSource = memData.cache;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int sourceX = MathUtils.moduloPositive(sX + x, cacheSize);
                final int sourceY = MathUtils.moduloPositive(sY + y, cacheSize);
                final int indexSource = (sourceY * cacheSize + sourceX) * 3;

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
            } catch (final FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (final IOException ex) {
                ex.printStackTrace();
            }

            for (int y = 0; y < currentSize; y++) {
                for (int x = 0; x < currentSize; x++) {
                    final int destIndex = (y * currentSize + x) * 3;

                    final byte noise = (byte) ((int) (ImprovedNoise.noise(x * diff * 0.01, y * diff * 0.01, 0) * 255) & 0xff);
                    destBuffer.put(destIndex, noise);
                    destBuffer.put(destIndex + 1, noise);
                    destBuffer.put(destIndex + 2, noise);
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
        return memDataList.get(unit).ready.get();
    }
}
