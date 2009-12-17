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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.ardor3d.extension.terrain.TexturedGeometryClipmapTerrain;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;

public class CachedFileTextureStreamer implements TextureStreamer {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TexturedGeometryClipmapTerrain.class.getName());

    private final int textureSize;
    private final int validLevels;
    private final int cacheCount = 5;

    class MemData {
        public int sizeX, sizeY;
        public FileChannel channel;

        public ByteBuffer cache[][];
        public int tileX = Integer.MAX_VALUE, tileY = Integer.MAX_VALUE;

        public AtomicBoolean ready = new AtomicBoolean(true);

        public boolean initialized = false;

        public MemData() {
            cache = new ByteBuffer[cacheCount][cacheCount];
            for (int i = 0; i < cacheCount; i++) {
                for (int j = 0; j < cacheCount; j++) {
                    cache[i][j] = BufferUtils.createByteBuffer(textureSize * textureSize * 3);
                }
            }
        }
    }

    private final List<MemData> memDataList = Lists.newArrayList();

    public CachedFileTextureStreamer(final String fileName, final int sourceSize, final int textureSize) {
        this.textureSize = textureSize;

        validLevels = powersUpTo(textureSize, sourceSize);
        for (int l = 0; l < validLevels; l++) {
            memDataList.add(new MemData());
        }

        int currentSize = sourceSize;
        for (int l = 0; l < validLevels; l++) {
            final MemData memData = memDataList.get(l);

            memData.sizeX = currentSize;
            memData.sizeY = currentSize;

            try {
                memData.channel = new FileInputStream(fileName + l).getChannel();
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

        final int tileX = (int) Math.floor((float) sX / textureSize);
        final int tileY = (int) Math.floor((float) sY / textureSize);

        final int diffX = tileX - memData.tileX;
        final int diffY = tileY - memData.tileY;
        if (diffX == 0 && diffY == 0) {
            return;
        }

        // memData.ready.set(false);

        memData.tileX = tileX;
        memData.tileY = tileY;

        if (memData.initialized) {
            new Thread(new Runnable() {
                public void run() {
                    updateTiles(memData, tileX, tileY, diffX, diffY);

                    // memData.ready.set(true);
                }
            }, "updateCache").start();
        } else {
            updateTiles(memData, tileX, tileY, cacheCount, cacheCount);

            memData.initialized = true;
        }
    }

    private void updateTiles(final MemData memData, final int tileX, final int tileY, final int diffX, final int diffY) {
        if (diffX != 0) {
            final int sign = (int) Math.signum(diffX);
            final int diffSize = Math.abs(diffX);
            for (int i = 0; i < cacheCount; i++) {
                for (int j = 0; j < diffSize; j++) {
                    final int startX = (tileX + (2 - j) * sign);
                    final int startY = (tileY + i - 2);

                    updateCache(startX, startY, memData);
                }
            }
        }

        if (diffY != 0) {
            final int sign = (int) Math.signum(diffY);
            final int diffSize = Math.abs(diffY);
            for (int i = 0; i < cacheCount; i++) {
                for (int j = 0; j < diffSize; j++) {
                    final int startX = (tileX + i - 2);
                    final int startY = (tileY + (2 - j) * sign);

                    updateCache(startX, startY, memData);
                }
            }
        }
    }

    private void updateCache(final int tileX, final int tileY, final MemData memData) {
        final int sizeX = memData.sizeX;
        final int sizeY = memData.sizeY;
        final FileChannel channel = memData.channel;

        final int tileX2 = MathUtils.moduloPositive(tileX, cacheCount);
        final int tileY2 = MathUtils.moduloPositive(tileY, cacheCount);
        final ByteBuffer cacheData = memData.cache[tileX2][tileY2];

        try {
            final int nrTiles = sizeX / textureSize;
            final int wrappedTileX = MathUtils.moduloPositive(tileX, nrTiles);
            final int wrappedTileY = MathUtils.moduloPositive(tileY, nrTiles);
            synchronized (channel) {
                channel.position((wrappedTileY * nrTiles + wrappedTileX) * textureSize * textureSize * 3);
                cacheData.rewind();
                channel.read(cacheData);
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    public void copyImage(final int unit, final ByteBuffer sliceData, final int dX, final int dY, final int sX,
            final int sY, final int w, final int h) {
        final MemData memData = memDataList.get(unit);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int tileX = (int) Math.floor((float) (sX + x) / textureSize);
                int tileY = (int) Math.floor((float) (sY + y) / textureSize);
                tileX = MathUtils.moduloPositive(tileX, cacheCount);
                tileY = MathUtils.moduloPositive(tileY, cacheCount);
                final ByteBuffer imageSource = memData.cache[tileX][tileY];

                final int sourceX = MathUtils.moduloPositive(sX + x, textureSize);
                final int sourceY = MathUtils.moduloPositive(sY + y, textureSize);
                final int indexSource = (sourceY * textureSize + sourceX) * 3;

                final int destX = MathUtils.moduloPositive(dX + x, textureSize);
                final int destY = MathUtils.moduloPositive(dY + y, textureSize);
                final int indexDest = (destY * textureSize + destX) * 3;

                sliceData.put(indexDest, imageSource.get(indexSource));
                sliceData.put(indexDest + 1, imageSource.get(indexSource + 1));
                sliceData.put(indexDest + 2, imageSource.get(indexSource + 2));
            }
        }
    }

    private static int powersUpTo(int value, final int max) {
        int powers = 0;
        for (; value <= max; value *= 2, powers++) {
        }
        return powers;
    }

    public static void createTexture(final String fileName, final Function3D function,
            final ReadOnlyColorRGBA[] terrainColors, final int sourceSize, final int textureSize) {
        logger.info("Creating texture files...");

        final int validLevels = powersUpTo(textureSize, sourceSize);

        int currentSize = sourceSize;

        final ByteBuffer destBuffer = BufferUtils.createByteBuffer(textureSize * textureSize * 3);
        final byte[] color = new byte[3];

        int diff = 1;
        for (int l = 0; l < validLevels; l++) {
            try {
                logger.info("Writing texture file for level: " + l + ", size: " + currentSize + ", name: " + fileName
                        + l + ", expected size: " + (currentSize * currentSize * 3));
                final FileChannel out = new FileOutputStream(fileName + l).getChannel();

                final int nrTiles = currentSize / textureSize;

                for (int i = 0; i < nrTiles; i++) {
                    for (int j = 0; j < nrTiles; j++) {
                        destBuffer.rewind();
                        for (int x = 0; x < textureSize; x++) {
                            for (int y = 0; y < textureSize; y++) {
                                final int xx = i * textureSize + x;
                                final int yy = j * textureSize + y;

                                // final byte noise = (byte) ((int) (ImprovedNoise.noise(xx * diff * 0.01, yy * diff
                                // * 0.01, 0) * 255) & 0xff);

                                // eval our function at the given slice and location
                                double val = function.eval(xx * diff, yy * diff, 0);

                                // Keep us in [-1, 1]
                                val = MathUtils.clamp(val, -1, 1);

                                // Convert to [0, 255]
                                val = ((val + 1) * 0.5) * 255.0;

                                // get us a color
                                final byte colIndex = (byte) val;
                                final ReadOnlyColorRGBA c = terrainColors[colIndex & 0xFF];

                                // place color channels in byte array
                                color[0] = (byte) (c.getRed() * 255);
                                color[1] = (byte) (c.getGreen() * 255);
                                color[2] = (byte) (c.getBlue() * 255);

                                destBuffer.put(color);
                            }
                        }
                        destBuffer.rewind();
                        out.write(destBuffer);
                    }
                }

                out.close();
            } catch (final FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (final IOException ex) {
                ex.printStackTrace();
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
