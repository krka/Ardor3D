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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.ImageDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;

public class TextureClipmap {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TextureClipmap.class.getName());

    private final int textureSize;
    private final int textureLevels;
    private final int validLevels;

    private float scale = 32f;

    private Texture3D textureClipmap;
    private final TextureState textureClipmapState;
    private GLSLShaderObjectsState textureClipmapShader;

    private final List<LevelData> levelDataList = Lists.newArrayList();

    private final FloatBuffer sliceDataBuffer;

    private final Vector3 eyePosition = new Vector3();

    private boolean showDebug = false;

    private final TextureStreamer streamer;

    public TextureClipmap(final TextureStreamer streamer, final int textureSize, final int levels, final float scale) {
        this.streamer = streamer;
        this.textureSize = textureSize;
        validLevels = levels;

        textureLevels = roundUpPowerTwo(validLevels);
        this.scale = scale;

        logger.info("Texture size: " + textureSize);
        logger.info("ValidLevels: " + validLevels);
        logger.info("Total number of levels: " + textureLevels);

        sliceDataBuffer = BufferUtils.createFloatBuffer(textureLevels * 2);

        for (int i = 0; i < validLevels; i++) {
            levelDataList.add(new LevelData(i));
        }

        textureClipmapState = new TextureState();
        textureClipmapState.setTexture(createTexture());
    }

    public void update(final Renderer renderer, final ReadOnlyVector3 position) {
        eyePosition.set(position);
        textureClipmapShader.setUniform("eyePosition", eyePosition);
        eyePosition.multiplyLocal(textureSize / (scale * 4f));

        // final long t = System.nanoTime();
        for (int unit = validLevels - 1; unit >= 0; unit--) {
            float x = eyePosition.getXf();
            float y = eyePosition.getZf();

            final int exp2 = (int) Math.pow(2, unit);
            x /= exp2;
            y /= exp2;

            final int offX = (int) Math.floor(x);
            final int offY = (int) Math.floor(y);

            final LevelData levelData = levelDataList.get(unit);

            if (levelData.x != offX || levelData.y != offY) {
                streamer.updateLevel(levelData.unit, offX, offY);

                if (streamer.isReady(levelData.unit)) {
                    updateLevel(renderer, levelData, offX, offY);
                } else {
                    // TODO: set valid levels to this level (also in shader)
                    break;
                }
            }

            x = MathUtils.moduloPositive(x, 2);
            y = MathUtils.moduloPositive(y, 2);

            int shiftX = levelData.x;
            int shiftY = levelData.y;
            shiftX = MathUtils.moduloPositive(shiftX, 2);
            shiftY = MathUtils.moduloPositive(shiftY, 2);

            x -= shiftX;
            y -= shiftY;

            x += levelData.offsetX;
            y += levelData.offsetY;

            x /= textureSize;
            y /= textureSize;

            sliceDataBuffer.put(unit * 2, x);
            sliceDataBuffer.put(unit * 2 + 1, y);

            // if (unit == 0) {
            // textureClipmapShader.setUniform("maxLevel", (float) 4);
            // } else if (update) {
            // final long t2 = System.nanoTime() - t;
            // if (t2 > 100000) {
            // System.out.println(unit + " - " + t2);
            // textureClipmapShader.setUniform("maxLevel", (float) unit);
            // break;
            // }
            // }
        }

        sliceDataBuffer.rewind();
        textureClipmapShader.setUniform("sliceOffset", sliceDataBuffer, 2);
    }

    private void updateLevel(final Renderer renderer, final LevelData levelData, final int x, final int y) {
        final int diffX = x - levelData.x;
        final int diffY = y - levelData.y;
        levelData.x = x;
        levelData.y = y;

        final int sX = x - textureSize / 2;
        final int sY = y - textureSize / 2;
        int dX = 0;
        int dY = 0;
        final int width = textureSize;
        final int height = textureSize;

        levelData.offsetX += diffX;
        levelData.offsetY += diffY;
        levelData.offsetX = MathUtils.moduloPositive(levelData.offsetX, textureSize);
        levelData.offsetY = MathUtils.moduloPositive(levelData.offsetY, textureSize);

        dX += levelData.offsetX;
        dY += levelData.offsetY;

        // updateNormal(renderer, levelData, diffX, diffY, sX, sY, dX, dY, width, height);
        updateQuick(renderer, levelData, diffX, diffY, sX, sY, dX, dY, width, height);
    }

    private void updateNormal(final Renderer renderer, final LevelData levelData, final int diffX, final int diffY,
            int sX, int sY, int dX, int dY, int width, int height) {
        final int unit = levelData.unit;
        final ByteBuffer imageDestination = levelData.sliceData;

        if (Math.abs(diffX) > textureSize || Math.abs(diffY) > textureSize) {
            // Copy the whole slice
            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);
        } else if (diffX != 0 && diffY != 0) {
            // Copy three rectangles. Horizontal, vertical and corner

            final int tmpSX = sX;
            final int tmpDX = dX;

            // Vertical
            if (diffX > 0) {
                sX = sX + textureSize - diffX;
                dX = dX - diffX;
            }
            final int saveWidth = Math.abs(diffX);

            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, saveWidth, height);

            sX = tmpSX;
            dX = tmpDX;

            // Horizontal
            if (diffY > 0) {
                sY = sY + textureSize - diffY;
                dY = dY - diffY;
            }
            height = Math.abs(diffY);

            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);
        } else if (diffX != 0) {
            // Copy vertical only
            if (diffX > 0) {
                sX = sX + textureSize - diffX;
                dX = dX - diffX;
            }
            width = Math.abs(diffX);

            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);
        } else if (diffY != 0) {
            // Copy horizontal only
            if (diffY > 0) {
                sY = sY + textureSize - diffY;
                dY = dY - diffY;
            }
            height = Math.abs(diffY);

            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);
        }

        // drawRect(Color.red.getRGB(), imageDestination, dX, dY, textureSize - 1, textureSize - 1);

        imageDestination.rewind();
        renderer.updateTexture3DSubImage(textureClipmap, 0, 0, unit, textureSize, textureSize, 1, imageDestination, 0,
                0, 0, textureSize, textureSize);
    }

    private void updateQuick(final Renderer renderer, final LevelData levelData, final int diffX, final int diffY,
            int sX, int sY, int dX, int dY, int width, int height) {
        final int unit = levelData.unit;
        final ByteBuffer imageDestination = levelData.sliceData;

        if (Math.abs(diffX) > textureSize || Math.abs(diffY) > textureSize) {
            // Copy the whole slice
            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);

            imageDestination.rewind();
            renderer.updateTexture3DSubImage(textureClipmap, 0, 0, unit, textureSize, textureSize, 1, imageDestination,
                    0, 0, 0, textureSize, textureSize);
        } else if (diffX != 0 && diffY != 0) {
            // Copy three rectangles. Horizontal, vertical and corner

            final int tmpSX = sX;
            final int tmpDX = dX;
            final int tmpWidth = width;

            // Vertical
            if (diffX > 0) {
                sX = sX + textureSize - diffX;
                dX = dX - diffX;
            }
            width = Math.abs(diffX);

            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);

            dX = MathUtils.moduloPositive(dX, textureSize);
            if (dX + width > textureSize) {
                int dX1 = dX;
                int width1 = textureSize - dX;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX1, 0, unit, width1, textureSize, 1,
                        imageDestination, dX1, 0, 0, textureSize, textureSize);

                dX1 = 0;
                width1 = width - width1;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX1, 0, unit, width1, textureSize, 1,
                        imageDestination, dX1, 0, 0, textureSize, textureSize);
            } else {
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX, 0, unit, width, textureSize, 1, imageDestination,
                        dX, 0, 0, textureSize, textureSize);
            }

            sX = tmpSX;
            dX = tmpDX;
            width = tmpWidth;

            // Horizontal
            if (diffY > 0) {
                sY = sY + textureSize - diffY;
                dY = dY - diffY;
            }
            height = Math.abs(diffY);

            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);

            dY = MathUtils.moduloPositive(dY, textureSize);
            if (dY + height > textureSize) {
                int dY1 = dY;
                int height1 = textureSize - dY;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY1, unit, textureSize, height1, 1,
                        imageDestination, 0, dY1, 0, textureSize, textureSize);

                dY1 = 0;
                height1 = height - height1;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY1, unit, textureSize, height1, 1,
                        imageDestination, 0, dY1, 0, textureSize, textureSize);
            } else {
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY, unit, textureSize, height, 1, imageDestination,
                        0, dY, 0, textureSize, textureSize);
            }
        } else if (diffX != 0) {
            // Copy vertical only
            if (diffX > 0) {
                sX = sX + textureSize - diffX;
                dX = dX - diffX;
            }
            width = Math.abs(diffX);

            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);

            dX = MathUtils.moduloPositive(dX, textureSize);
            if (dX + width > textureSize) {
                int dX1 = dX;
                int width1 = textureSize - dX;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX1, 0, unit, width1, textureSize, 1,
                        imageDestination, dX1, 0, 0, textureSize, textureSize);

                dX1 = 0;
                width1 = width - width1;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX1, 0, unit, width1, textureSize, 1,
                        imageDestination, dX1, 0, 0, textureSize, textureSize);
            } else {
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX, 0, unit, width, textureSize, 1, imageDestination,
                        dX, 0, 0, textureSize, textureSize);
            }
        } else if (diffY != 0) {
            // Copy horizontal only
            if (diffY > 0) {
                sY = sY + textureSize - diffY;
                dY = dY - diffY;
            }
            height = Math.abs(diffY);

            streamer.copyImage(unit, imageDestination, dX, dY, sX, sY, width, height);

            dY = MathUtils.moduloPositive(dY, textureSize);
            if (dY + height > textureSize) {
                int dY1 = dY;
                int height1 = textureSize - dY;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY1, unit, textureSize, height1, 1,
                        imageDestination, 0, dY1, 0, textureSize, textureSize);

                dY1 = 0;
                height1 = height - height1;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY1, unit, textureSize, height1, 1,
                        imageDestination, 0, dY1, 0, textureSize, textureSize);
            } else {
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY, unit, textureSize, height, 1, imageDestination,
                        0, dY, 0, textureSize, textureSize);
            }
        }
    }

    public TextureState getTextureState() {
        return textureClipmapState;
    }

    public void reloadShader() {
        textureClipmapShader = new GLSLShaderObjectsState();
        try {
            textureClipmapShader.setVertexShader(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "com/ardor3d/extension/effect/texture/textureClipmapShader.vert"));
            textureClipmapShader.setFragmentShader(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "com/ardor3d/extension/effect/texture/textureClipmapShader.frag"));
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
        }
        textureClipmapShader.setUniform("texture", 0);

        textureClipmapShader.setUniform("scale", 1f / scale);
        textureClipmapShader.setUniform("textureSize", (float) textureSize);
        textureClipmapShader.setUniform("texelSize", 1f / textureSize);

        textureClipmapShader.setUniform("levels", (float) textureLevels);
        textureClipmapShader.setUniform("validLevels", (float) validLevels - 1);

        textureClipmapShader.setUniform("showDebug", showDebug ? 1.0f : 0.0f);
    }

    public GLSLShaderObjectsState getShaderState() {
        if (textureClipmapShader == null) {
            reloadShader();
        }
        return textureClipmapShader;
    }

    public void setShaderState(final GLSLShaderObjectsState textureClipmapShader) {
        this.textureClipmapShader = textureClipmapShader;
    }

    private void drawRect(final int rgb, final ByteBuffer imageBuffer, final int x, final int y, final int w,
            final int h) {
        for (int yy = y; yy < y + h; yy++) {
            int destX = MathUtils.moduloPositive(x, textureSize);
            int destY = MathUtils.moduloPositive(yy, textureSize);
            int index = (destY * textureSize + destX) * 3;

            imageBuffer.put(index, (byte) ((rgb >> 16) & 0xFF));
            imageBuffer.put(index + 1, (byte) ((rgb >> 8) & 0xFF));
            imageBuffer.put(index + 2, (byte) ((rgb) & 0xFF));

            destX = MathUtils.moduloPositive(x + w, textureSize);
            destY = MathUtils.moduloPositive(yy, textureSize);
            index = (destY * textureSize + destX) * 3;

            imageBuffer.put(index, (byte) ((rgb >> 16) & 0xFF));
            imageBuffer.put(index + 1, (byte) ((rgb >> 8) & 0xFF));
            imageBuffer.put(index + 2, (byte) ((rgb) & 0xFF));
        }

        for (int xx = x; xx < x + w; xx++) {
            int destX = MathUtils.moduloPositive(xx, textureSize);
            int destY = MathUtils.moduloPositive(y, textureSize);
            int index = (destY * textureSize + destX) * 3;

            imageBuffer.put(index, (byte) ((rgb >> 16) & 0xFF));
            imageBuffer.put(index + 1, (byte) ((rgb >> 8) & 0xFF));
            imageBuffer.put(index + 2, (byte) ((rgb) & 0xFF));

            destX = MathUtils.moduloPositive(xx, textureSize);
            destY = MathUtils.moduloPositive(y + h, textureSize);
            index = (destY * textureSize + destX) * 3;

            imageBuffer.put(index, (byte) ((rgb >> 16) & 0xFF));
            imageBuffer.put(index + 1, (byte) ((rgb >> 8) & 0xFF));
            imageBuffer.put(index + 2, (byte) ((rgb) & 0xFF));
        }
    }

    public static int clamp(final int x, final int low, final int high) {
        return (x < low) ? low : ((x > high) ? high : x);
    }

    private Texture createTexture() {
        textureClipmap = new Texture3D();
        textureClipmap.setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);
        textureClipmap.setMagnificationFilter(MagnificationFilter.NearestNeighbor);
        // textureClipmap.setMinificationFilter(MinificationFilter.BilinearNoMipMaps);
        // textureClipmap.setMagnificationFilter(MagnificationFilter.Bilinear);
        final Image img = new Image();
        img.setWidth(textureSize);
        img.setHeight(textureSize);
        img.setDepth(textureLevels);
        img.setDataFormat(ImageDataFormat.RGB);
        img.setDataType(ImageDataType.UnsignedByte);
        textureClipmap.setTextureKey(TextureKey.getKey(null, false, TextureStoreFormat.RGB8, textureClipmap
                .getMinificationFilter()));

        for (int l = 0; l < textureLevels; l++) {
            final ByteBuffer sliceData = BufferUtils.createByteBuffer(textureSize * textureSize * 3);
            img.setData(l, sliceData);
            if (l < validLevels) {
                levelDataList.get(l).sliceData = sliceData;
            }
        }
        textureClipmap.setImage(img);

        return textureClipmap;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(final float scale) {
        this.scale = scale;
    }

    private int roundUpPowerTwo(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

    public boolean isShowDebug() {
        return showDebug;
    }

    public void setShowDebug(final boolean showDebug) {
        this.showDebug = showDebug;
    }

    public int getTextureSize() {
        return textureSize;
    }

    public int getTextureLevels() {
        return textureLevels;
    }

    public int getValidLevels() {
        return validLevels;
    }

}
