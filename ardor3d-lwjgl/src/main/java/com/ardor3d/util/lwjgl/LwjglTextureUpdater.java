/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.lwjgl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.Util;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.scene.state.lwjgl.util.LwjglTextureUtil;
import com.ardor3d.util.geom.BufferUtils;

public abstract class LwjglTextureUpdater {
    private static IntBuffer idBuff = BufferUtils.createIntBuffer(16);
    private static boolean glTexSubImage2DSupported = true;

    public static void updateTexture(final Texture texture, final ByteBuffer data, final int w, final int h,
            final Format format) {
        final int dataFormat = LwjglTextureUtil.getGLDataFormat(format);
        final int pixelFormat = LwjglTextureUtil.getGLPixelFormat(format);

        idBuff.clear();
        GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D, idBuff);
        final int oldTex = idBuff.get();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getTextureId());
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        if (glTexSubImage2DSupported) {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, w, h, pixelFormat, GL11.GL_UNSIGNED_BYTE, data);

            try {
                Util.checkGLError();
            } catch (final OpenGLException e) {
                glTexSubImage2DSupported = false;
                updateTexture(texture, data, w, h, format);
            }
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, dataFormat, w, h, 0, pixelFormat, GL11.GL_UNSIGNED_BYTE, data);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
    }
}
