/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import java.nio.ByteBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.geom.BufferUtils;

public abstract class GeneratedTextureFactory {

    /**
     * Generates a checkboard pattern with transparent and opaque alternating blocks. A useful blend state for use with
     * this Texture is:
     * 
     * <pre>
     * BlendState bs = new BlendState();
     * bs.setBlendEnabled(false);
     * bs.setTestEnabled(true);
     * bs.setTestFunction(BlendState.TestFunction.GreaterThan);
     * bs.setReference(0.5f);
     * </pre>
     * 
     * @param texSz
     * @return
     */
    public static TextureState createCheckerTextureState(final int texSz) {
        final int bufSz = texSz * texSz * 2; // format is luminance alpha
        final byte[] texData = new byte[bufSz];
        for (int i = 0; i < texSz; i++) {
            for (int j = 0; j < texSz; j++) {
                final int idx = 2 * (i * texSz + j);
                texData[idx + 0] = (byte) (100 + ((i + j) % 2 * 155));
                texData[idx + 1] = (byte) ((i == j) ? 0x00 : 0xFF);
            }
        }
        final Image img = new Image(Image.Format.Luminance8Alpha8, texSz, texSz, ByteBuffer.wrap(texData));
        final Texture2D tex = new Texture2D();
        tex.setImage(img);
        tex.setMagnificationFilter(MagnificationFilter.NearestNeighbor);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(tex);

        return ts;
    }

    /**
     * Generates a color ramp pattern with a transparent border. A useful blend state for use with this Texture is:
     * 
     * <pre>
     * BlendState bs = new BlendState();
     * bs.setBlendEnabled(false);
     * bs.setTestEnabled(true);
     * bs.setTestFunction(BlendState.TestFunction.GreaterThan);
     * bs.setReference(0.5f);
     * </pre>
     * 
     * @param texSz
     * @return
     */
    public static TextureState createColorRampTextureState(final int texSz) {
        final int bufSz = texSz * texSz * 4; // format is standard RGBA
        final byte[] texData = new byte[bufSz];
        for (int i = 0; i < texSz; i++) {
            for (int j = 0; j < texSz; j++) {
                final int idx = 4 * (i * texSz + j);
                texData[idx + 0] = (byte) (255 * ((float) (i + 0) / (float) texSz));
                texData[idx + 1] = (byte) (255 * ((float) (j + 0) / (float) texSz));
                texData[idx + 2] = (byte) (255);
                texData[idx + 3] = (byte) ((i % (texSz - 1) == 0 || j % (texSz - 1) == 0) ? 0x00 : 0xFF);
            }
        }
        final Image img = new Image(Image.Format.RGBA8, texSz, texSz, ByteBuffer.wrap(texData));
        final Texture2D tex = new Texture2D();
        tex.setImage(img);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(tex);

        return ts;
    }

    private static final Image blackImagex3;
    private static final Image blackImagex4;
    static {
        final ByteBuffer blackDatax4 = BufferUtils.createByteBuffer(32 * 32 * 4);
        for (int i = 32 * 32 * 4; i > 0; i--) {
            blackDatax4.put((byte) 0);
        }
        blackDatax4.rewind();
        blackImagex4 = new Image(Image.Format.RGBA8, 32, 32, blackDatax4);

        final ByteBuffer blackDatax3 = BufferUtils.createByteBuffer(32 * 32 * 3);
        for (int i = 32 * 32 * 3; i > 0; i--) {
            blackDatax3.put((byte) 0);
        }
        blackDatax3.rewind();
        blackImagex3 = new Image(Image.Format.RGBA8, 32, 32, blackDatax3);
    }

    /**
     * Creates a 32x32 plain color Texture, of data type byte.
     * 
     * The image data is static and shared.
     */
    public static Texture2D createColorRGBBlack(final int components) {
        final Image.Format fmt = (components == 4) ? Image.Format.RGBA8 : Image.Format.RGB8;
        final TextureKey tkey = TextureKey.getKey(null, false, fmt, Texture.MinificationFilter.BilinearNoMipMaps);

        final Texture2D texture = new Texture2D();
        texture.setTextureKey(tkey);
        texture.setMinificationFilter(tkey.getMinificationFilter());
        texture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        texture.setImage((components == 4) ? blackImagex4 : blackImagex3);
        return texture;
    }

    /**
     * Creates a 32x32 plain color Texture, of data type byte.
     * 
     * @param color
     * @param components
     * @return Texture
     */
    public static Texture2D createColorRGBPlain(final ReadOnlyColorRGBA color, final int components) {
        final Image.Format fmt = (components == 4) ? Image.Format.RGBA8 : Image.Format.RGB8;
        final TextureKey tkey = TextureKey.getKey(null, false, fmt, Texture.MinificationFilter.BilinearNoMipMaps);

        final ByteBuffer data = BufferUtils.createByteBuffer(32 * 32 * components);
        for (int i = 0; i < 32 * 32 * components;) {
            data.put((byte) (color.getRed() * 255f));
            i++;
            data.put((byte) (color.getGreen() * 255f));
            i++;
            data.put((byte) (color.getBlue() * 255f));
            i++;
            if (components > 3) {
                data.put((byte) (color.getAlpha() * 255f));
                i++;
            }
        }
        data.rewind();
        final Image image = new Image(fmt, 32, 32, data);

        final Texture2D texture = new Texture2D();
        texture.setTextureKey(tkey);
        texture.setMinificationFilter(tkey.getMinificationFilter());
        texture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        texture.setImage(image);
        return texture;
    }
}
