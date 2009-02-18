/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBDepthTexture;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureBorderClamp;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBTextureCubeMap;
import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.ARBTextureMirroredRepeat;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.EXTTextureLODBias;
import org.lwjgl.opengl.EXTTextureMirrorClamp;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.SGISGenerateMipmap;
import org.lwjgl.opengl.Util;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.MipMap;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.Texture.CombinerFunctionAlpha;
import com.ardor3d.image.Texture.CombinerFunctionRGB;
import com.ardor3d.image.Texture.CombinerOperandAlpha;
import com.ardor3d.image.Texture.CombinerOperandRGB;
import com.ardor3d.image.Texture.CombinerSource;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.Texture.WrapAxis;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.lwjgl.LwjglRenderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.renderer.state.record.TextureUnitRecord;
import com.ardor3d.scene.state.lwjgl.util.LwjglRendererUtil;
import com.ardor3d.scene.state.lwjgl.util.LwjglTextureUtil;
import com.ardor3d.util.Debug;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public abstract class LwjglTextureStateUtil {
    private static final Logger logger = Logger.getLogger(LwjglTextureStateUtil.class.getName());

    private static final long serialVersionUID = 1L;

    /**
     * override MipMap to access helper methods
     */
    protected static class LwjglMipMap extends MipMap {
        /**
         * @see MipMap#glGetIntegerv(int)
         */
        protected static int glGetIntegerv(final int what) {
            return org.lwjgl.util.glu.Util.glGetIntegerv(what);
        }

        /**
         * @see MipMap#nearestPower(int)
         */
        protected static int nearestPower(final int value) {
            return org.lwjgl.util.glu.Util.nearestPower(value);
        }

        /**
         * @see MipMap#bytesPerPixel(int, int)
         */
        protected static int bytesPerPixel(final int format, final int type) {
            return org.lwjgl.util.glu.Util.bytesPerPixel(format, type);
        }
    }

    public static void load(final Texture texture, final int unit) {
        if (texture == null) {
            return;
        }

        // our texture type:
        final Texture.Type type = texture.getType();

        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        TextureStateRecord record = null;
        if (context != null) {
            record = (TextureStateRecord) context.getStateRecord(StateType.Texture);
        }

        // Check we are in the right unit
        if (record != null) {
            checkAndSetUnit(unit, record, caps);
        }

        // Create the texture...
        if (texture.getTextureKey() != null) {

            // First, check if we've already created this texture for our gl context (already on the card)

            // if texture key has a context already, and it is not ours, complain.
            if (texture.getTextureKey().getContextRep() != null
                    && texture.getTextureKey().getContextRep() != context.getGlContextRep()) {
                logger.warning("Texture key is morphing contexts: " + texture.getTextureKey());
            }

            // make sure our context is set.
            texture.getTextureKey().setContextRep(context.getGlContextRep());

            // Look for a texture in the cache just like ours, also in the same context.
            final Texture cached = TextureManager.findCachedTexture(texture.getTextureKey());

            if (cached == null) {
                TextureManager.addToCache(texture);
            } else if (cached.getTextureId() != 0) {
                texture.setTextureId(cached.getTextureId());
                GL11.glBindTexture(getGLType(type), cached.getTextureId());
                if (Debug.stats) {
                    StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
                }
                if (record != null) {
                    record.units[unit].boundTexture = texture.getTextureId();
                }
                return;
            }
        }

        final IntBuffer id = BufferUtils.createIntBuffer(1);
        id.clear();
        GL11.glGenTextures(id);
        texture.setTextureId(id.get(0));

        GL11.glBindTexture(getGLType(type), texture.getTextureId());
        if (Debug.stats) {
            StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
        }
        if (record != null) {
            record.units[unit].boundTexture = texture.getTextureId();
        }

        TextureManager.registerForCleanup(texture.getTextureKey(), texture.getTextureId());

        // pass image data to OpenGL
        final Image image = texture.getImage();
        final boolean hasBorder = texture.hasBorder();
        if (image == null) {
            logger.warning("Image data for texture is null.");
        }

        // set alignment to support images with width % 4 != 0, as images are
        // not aligned
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        // Get texture image data. Not all textures have image data.
        // For example, ApplyMode.Combine modes can use primary colors,
        // texture output, and constants to modify fragments via the
        // texture units.
        if (image != null) {
            if (!caps.isNonPowerOfTwoTextureSupported()
                    && (!MathUtils.isPowerOfTwo(image.getWidth()) || !MathUtils.isPowerOfTwo(image.getHeight()))) {
                logger.warning("(card unsupported) Attempted to apply texture with size that is not power of 2: "
                        + image.getWidth() + " x " + image.getHeight());

                final int maxSize = LwjglMipMap.glGetIntegerv(GL11.GL_MAX_TEXTURE_SIZE);

                final int actualWidth = image.getWidth();
                int w = LwjglMipMap.nearestPower(actualWidth);
                if (w > maxSize) {
                    w = maxSize;
                }

                final int actualHeight = image.getHeight();
                int h = LwjglMipMap.nearestPower(actualHeight);
                if (h > maxSize) {
                    h = maxSize;
                }
                logger.warning("Rescaling image to " + w + " x " + h + " !!!");

                // must rescale image to get "top" mipmap texture image
                final int format = LwjglTextureUtil.getGLPixelFormat(texture.getTextureKey().getFormat(), image
                        .getFormat(), context.getCapabilities());
                final int dType = GL11.GL_UNSIGNED_BYTE;
                final int bpp = LwjglMipMap.bytesPerPixel(format, dType);
                final ByteBuffer scaledImage = BufferUtils.createByteBuffer((w + 4) * h * bpp);
                final int error = MipMap.gluScaleImage(format, actualWidth, actualHeight, dType, image.getData(0), w,
                        h, dType, scaledImage);
                if (error != 0) {
                    Util.checkGLError();
                }

                image.setWidth(w);
                image.setHeight(h);
                image.setData(scaledImage);
            }

            if (!texture.getMinificationFilter().usesMipMapLevels()
                    && !LwjglTextureUtil.isCompressedType(image.getFormat())) {

                // Load textures which do not need mipmap auto-generating and
                // which aren't using compressed images.

                switch (texture.getType()) {
                    case TwoDimensional:
                        // ensure the buffer is ready for reading
                        image.getData(0).rewind();
                        // send top level to card
                        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, LwjglTextureUtil.getGLDataFormat(image.getFormat()),
                                image.getWidth(), image.getHeight(), hasBorder ? 1 : 0, LwjglTextureUtil
                                        .getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE, image.getData(0));
                        break;
                    case OneDimensional:
                        // ensure the buffer is ready for reading
                        image.getData(0).rewind();
                        // send top level to card
                        GL11.glTexImage1D(GL11.GL_TEXTURE_1D, 0, LwjglTextureUtil.getGLDataFormat(image.getFormat()),
                                image.getWidth(), hasBorder ? 1 : 0, LwjglTextureUtil.getGLPixelFormat(image
                                        .getFormat()), GL11.GL_UNSIGNED_BYTE, image.getData(0));
                        break;
                    case ThreeDimensional:
                        if (caps.isTexture3DSupported()) {
                            // concat data into single buffer:
                            int dSize = 0;
                            int count = 0;
                            ByteBuffer data = null;
                            for (int x = 0; x < image.getData().size(); x++) {
                                if (image.getData(x) != null) {
                                    data = image.getData(x);
                                    dSize += data.limit();
                                    count++;
                                }
                            }
                            // reuse buffer if we can.
                            if (count != 1) {
                                data = BufferUtils.createByteBuffer(dSize);
                                for (int x = 0; x < image.getData().size(); x++) {
                                    if (image.getData(x) != null) {
                                        data.put(image.getData(x));
                                    }
                                }
                                // ensure the buffer is ready for reading
                                data.flip();
                            }
                            // send top level to card
                            GL12.glTexImage3D(GL12.GL_TEXTURE_3D, 0, LwjglTextureUtil
                                    .getGLDataFormat(image.getFormat()), image.getWidth(), image.getHeight(), image
                                    .getDepth(), hasBorder ? 1 : 0, LwjglTextureUtil
                                    .getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE, data);
                        } else {
                            logger.warning("This card does not support Texture3D.");
                        }
                        break;
                    case CubeMap:
                        // NOTE: Cubemaps MUST be square, so height is ignored
                        // on purpose.
                        if (caps.isTextureCubeMapSupported()) {
                            for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
                                // ensure the buffer is ready for reading
                                image.getData(face.ordinal()).rewind();
                                // send top level to card
                                GL11.glTexImage2D(getGLCubeMapFace(face), 0, LwjglTextureUtil.getGLDataFormat(image
                                        .getFormat()), image.getWidth(), image.getWidth(), hasBorder ? 1 : 0,
                                        LwjglTextureUtil.getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE,
                                        image.getData(face.ordinal()));
                            }
                        } else {
                            logger.warning("This card does not support Cubemaps.");
                        }
                        break;
                }
            } else if (texture.getMinificationFilter().usesMipMapLevels() && !image.hasMipmaps()
                    && !LwjglTextureUtil.isCompressedType(image.getFormat())) {

                // For textures which need mipmaps auto-generating and which
                // aren't using compressed images, generate the mipmaps.
                // A new mipmap builder may be needed to build mipmaps for
                // compressed textures.

                if (caps.isAutomaticMipmapsSupported()) {
                    // Flag the card to generate mipmaps
                    GL11.glTexParameteri(getGLType(type), SGISGenerateMipmap.GL_GENERATE_MIPMAP_SGIS, GL11.GL_TRUE);
                }

                switch (type) {
                    case TwoDimensional:
                        // ensure the buffer is ready for reading
                        image.getData(0).rewind();
                        if (caps.isAutomaticMipmapsSupported()) {
                            // send top level to card
                            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, LwjglTextureUtil
                                    .getGLDataFormat(image.getFormat()), image.getWidth(), image.getHeight(),
                                    hasBorder ? 1 : 0, LwjglTextureUtil.getGLPixelFormat(image.getFormat()),
                                    GL11.GL_UNSIGNED_BYTE, image.getData(0));
                        } else {
                            // send to card
                            GLU.gluBuild2DMipmaps(GL11.GL_TEXTURE_2D, LwjglTextureUtil.getGLDataFormat(image
                                    .getFormat()), image.getWidth(), image.getHeight(), LwjglTextureUtil
                                    .getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE, image.getData(0));
                        }
                        break;
                    case OneDimensional:
                        // ensure the buffer is ready for reading
                        image.getData(0).rewind();
                        if (caps.isAutomaticMipmapsSupported()) {
                            // send top level to card
                            GL11.glTexImage1D(GL11.GL_TEXTURE_1D, 0, LwjglTextureUtil
                                    .getGLDataFormat(image.getFormat()), image.getWidth(), hasBorder ? 1 : 0,
                                    LwjglTextureUtil.getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE, image
                                            .getData(0));
                        } else {
                            // Note: LWJGL's GLU class does not support
                            // gluBuild1DMipmaps.
                            logger
                                    .warning("non-fbo 1d mipmap generation is not currently supported.  Use DDS or a non-mipmap minification filter.");
                            return;
                        }
                        break;
                    case ThreeDimensional:
                        if (caps.isTexture3DSupported()) {
                            if (caps.isAutomaticMipmapsSupported()) {
                                // concat data into single buffer:
                                int dSize = 0;
                                int count = 0;
                                ByteBuffer data = null;
                                for (int x = 0; x < image.getData().size(); x++) {
                                    if (image.getData(x) != null) {
                                        data = image.getData(x);
                                        dSize += data.limit();
                                        count++;
                                    }
                                }
                                // reuse buffer if we can.
                                if (count != 1) {
                                    data = BufferUtils.createByteBuffer(dSize);
                                    for (int x = 0; x < image.getData().size(); x++) {
                                        if (image.getData(x) != null) {
                                            data.put(image.getData(x));
                                        }
                                    }
                                    // ensure the buffer is ready for reading
                                    data.flip();
                                }
                                // send top level to card
                                GL12.glTexImage3D(GL12.GL_TEXTURE_3D, 0, LwjglTextureUtil.getGLDataFormat(image
                                        .getFormat()), image.getWidth(), image.getHeight(), image.getDepth(),
                                        hasBorder ? 1 : 0, LwjglTextureUtil.getGLPixelFormat(image.getFormat()),
                                        GL11.GL_UNSIGNED_BYTE, data);
                            } else {
                                // Note: LWJGL's GLU class does not support
                                // gluBuild3DMipmaps.
                                logger
                                        .warning("non-fbo 3d mipmap generation is not currently supported.  Use DDS or a non-mipmap minification filter.");
                                return;
                            }
                        } else {
                            logger.warning("This card does not support Texture3D.");
                            return;
                        }
                        break;
                    case CubeMap:
                        // NOTE: Cubemaps MUST be square, so height is ignored
                        // on purpose.
                        if (caps.isTextureCubeMapSupported()) {
                            if (caps.isAutomaticMipmapsSupported()) {
                                for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
                                    // ensure the buffer is ready for reading
                                    image.getData(face.ordinal()).rewind();
                                    // send top level to card
                                    GL11.glTexImage2D(getGLCubeMapFace(face), 0, LwjglTextureUtil.getGLDataFormat(image
                                            .getFormat()), image.getWidth(), image.getWidth(), hasBorder ? 1 : 0,
                                            LwjglTextureUtil.getGLPixelFormat(image.getFormat()),
                                            GL11.GL_UNSIGNED_BYTE, image.getData(face.ordinal()));
                                }
                            } else {
                                for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
                                    // ensure the buffer is ready for reading
                                    image.getData(face.ordinal()).rewind();
                                    // send to card
                                    GLU.gluBuild2DMipmaps(getGLCubeMapFace(face), LwjglTextureUtil
                                            .getGLDataFormat(image.getFormat()), image.getWidth(), image.getWidth(),
                                            LwjglTextureUtil.getGLPixelFormat(image.getFormat()),
                                            GL11.GL_UNSIGNED_BYTE, image.getData(face.ordinal()));
                                }
                            }
                        } else {
                            logger.warning("This card does not support Cubemaps.");
                            return;
                        }
                        break;
                }

            } else {
                // Here we handle textures that are either compressed or have
                // predefined mipmaps.
                // Get mipmap data sizes and amount of mipmaps to send to
                // opengl. Then loop through all mipmaps and send them.
                int[] mipSizes = image.getMipMapSizes();
                ByteBuffer data = null;

                if (type == Type.CubeMap) {
                    if (caps.isTextureCubeMapSupported()) {
                        for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
                            data = image.getData(face.ordinal());
                            int pos = 0;
                            int max = 1;

                            if (mipSizes == null) {
                                mipSizes = new int[] { data.capacity() };
                            } else if (texture.getMinificationFilter().usesMipMapLevels()) {
                                max = mipSizes.length;
                            }

                            for (int m = 0; m < max; m++) {
                                final int width = Math.max(1, image.getWidth() >> m);
                                final int height = type != Type.OneDimensional ? Math.max(1, image.getHeight() >> m)
                                        : 0;

                                data.position(pos);
                                data.limit(pos + mipSizes[m]);

                                if (LwjglTextureUtil.isCompressedType(image.getFormat())) {
                                    ARBTextureCompression.glCompressedTexImage2DARB(getGLCubeMapFace(face), m,
                                            LwjglTextureUtil.getGLDataFormat(image.getFormat()), width, height,
                                            hasBorder ? 1 : 0, data);
                                } else {
                                    GL11.glTexImage2D(getGLCubeMapFace(face), m, LwjglTextureUtil.getGLDataFormat(image
                                            .getFormat()), width, height, hasBorder ? 1 : 0, LwjglTextureUtil
                                            .getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE, data);
                                }
                                pos += mipSizes[m];
                            }
                        }
                    } else {
                        logger.warning("This card does not support CubeMaps.");
                        return;
                    }
                } else {
                    data = image.getData(0);
                    int pos = 0;
                    int max = 1;

                    if (mipSizes == null) {
                        mipSizes = new int[] { data.capacity() };
                    } else if (texture.getMinificationFilter().usesMipMapLevels()) {
                        max = mipSizes.length;
                    }

                    if (type == Type.ThreeDimensional) {
                        if (caps.isTexture3DSupported()) {
                            // concat data into single buffer:
                            int dSize = 0;
                            int count = 0;
                            for (int x = 0; x < image.getData().size(); x++) {
                                if (image.getData(x) != null) {
                                    data = image.getData(x);
                                    dSize += data.limit();
                                    count++;
                                }
                            }
                            // reuse buffer if we can.
                            if (count != 1) {
                                data = BufferUtils.createByteBuffer(dSize);
                                for (int x = 0; x < image.getData().size(); x++) {
                                    if (image.getData(x) != null) {
                                        data.put(image.getData(x));
                                    }
                                }
                                // ensure the buffer is ready for reading
                                data.flip();
                            }
                        } else {
                            logger.warning("This card does not support Texture3D.");
                            return;
                        }
                    }

                    for (int m = 0; m < max; m++) {
                        final int width = Math.max(1, image.getWidth() >> m);
                        final int height = type != Type.OneDimensional ? Math.max(1, image.getHeight() >> m) : 0;
                        final int depth = type == Type.ThreeDimensional ? Math.max(1, image.getDepth() >> m) : 0;

                        data.position(pos);
                        data.limit(pos + mipSizes[m]);

                        switch (type) {
                            case TwoDimensional:
                                if (LwjglTextureUtil.isCompressedType(image.getFormat())) {
                                    ARBTextureCompression.glCompressedTexImage2DARB(GL11.GL_TEXTURE_2D, m,
                                            LwjglTextureUtil.getGLDataFormat(image.getFormat()), width, height,
                                            hasBorder ? 1 : 0, data);
                                } else {
                                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, m, LwjglTextureUtil.getGLDataFormat(image
                                            .getFormat()), width, height, hasBorder ? 1 : 0, LwjglTextureUtil
                                            .getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE, data);
                                }
                                break;
                            case OneDimensional:
                                if (LwjglTextureUtil.isCompressedType(image.getFormat())) {
                                    ARBTextureCompression.glCompressedTexImage1DARB(GL11.GL_TEXTURE_1D, m,
                                            LwjglTextureUtil.getGLDataFormat(image.getFormat()), width, hasBorder ? 1
                                                    : 0, data);
                                } else {
                                    GL11.glTexImage1D(GL11.GL_TEXTURE_1D, m, LwjglTextureUtil.getGLDataFormat(image
                                            .getFormat()), width, hasBorder ? 1 : 0, LwjglTextureUtil
                                            .getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE, data);
                                }
                                break;
                            case ThreeDimensional:
                                // already checked for support above...
                                if (LwjglTextureUtil.isCompressedType(image.getFormat())) {
                                    ARBTextureCompression.glCompressedTexImage3DARB(GL12.GL_TEXTURE_3D, m,
                                            LwjglTextureUtil.getGLDataFormat(image.getFormat()), width, height, depth,
                                            hasBorder ? 1 : 0, data);
                                } else {
                                    GL12.glTexImage3D(GL12.GL_TEXTURE_3D, m, LwjglTextureUtil.getGLDataFormat(image
                                            .getFormat()), width, height, depth, hasBorder ? 1 : 0, LwjglTextureUtil
                                            .getGLPixelFormat(image.getFormat()), GL11.GL_UNSIGNED_BYTE, data);
                                }
                                break;
                        }
                        pos += mipSizes[m];
                    }
                }
                if (data != null) {
                    data.clear();
                }
            }
        }
    }

    public static void apply(final LwjglRenderer renderer, final TextureState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);
        context.setCurrentState(StateType.Texture, state);

        if (state.isEnabled()) {

            Texture texture;
            Texture.Type type;
            TextureUnitRecord unitRecord;
            TextureRecord texRecord;

            final int glHint = LwjglTextureUtil.getPerspHint(state.getCorrectionType());
            if (!record.isValid() || record.hint != glHint) {
                // set up correction mode
                GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, glHint);
                record.hint = glHint;
            }

            // loop through all available texture units...
            for (int i = 0; i < caps.getNumberOfTotalTextureUnits(); i++) {
                unitRecord = record.units[i];

                // grab a texture for this unit, if available
                texture = state.getTexture(i);

                // check for invalid textures - ones that have no opengl id and
                // no image data
                if (texture != null && texture.getTextureId() == 0 && texture.getImage() == null) {
                    texture = null;
                }

                // null textures above fixed limit do not need to be disabled
                // since they are not really part of the pipeline.
                if (texture == null) {
                    if (i >= caps.getNumberOfFixedTextureUnits()) {
                        continue;
                    } else {
                        // a null texture indicates no texturing at this unit
                        // Disable texturing on this unit if enabled.
                        disableTexturing(unitRecord, record, i, caps);

                        if (i < state._idCache.length) {
                            state._idCache[i] = 0;
                        }

                        // next texture!
                        continue;
                    }
                }

                type = texture.getType();

                // disable other texturing types for this unit, if enabled.
                disableTexturing(unitRecord, record, i, type, caps);

                // Time to bind the texture, so see if we need to load in image
                // data for this texture.
                if (texture.getTextureId() == 0) {
                    // texture not yet loaded.
                    // this will load and bind and set the records...
                    load(texture, i);
                    if (texture.getTextureId() == 0) {
                        continue;
                    }
                } else {
                    // texture already exists in OpenGL, just bind it if needed
                    if (!unitRecord.isValid() || unitRecord.boundTexture != texture.getTextureId()) {
                        checkAndSetUnit(i, record, caps);
                        GL11.glBindTexture(getGLType(type), texture.getTextureId());
                        if (Debug.stats) {
                            StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
                        }
                        unitRecord.boundTexture = texture.getTextureId();
                    }
                }

                // Grab our record for this texture
                texRecord = record.getTextureRecord(texture.getTextureId(), texture.getType());

                // Set the idCache value for this unit of this texture state
                // This is done so during state comparison we don't have to
                // spend a lot of time pulling out classes and finding field
                // data.
                state._idCache[i] = texture.getTextureId();

                // Some texture things only apply to fixed function pipeline
                if (i < caps.getNumberOfFixedTextureUnits()) {

                    // Enable 2D texturing on this unit if not enabled.
                    if (!unitRecord.isValid() || !unitRecord.enabled[type.ordinal()]) {
                        checkAndSetUnit(i, record, caps);
                        GL11.glEnable(getGLType(type));
                        unitRecord.enabled[type.ordinal()] = true;
                    }

                    // Set our blend color, if needed.
                    applyBlendColor(texture, unitRecord, i, record, caps);

                    // Set the texture environment mode if this unit isn't
                    // already set properly
                    applyEnvMode(texture.getApply(), unitRecord, i, record, caps);

                    // If our mode is combine, and we support multitexturing
                    // apply combine settings.
                    if (texture.getApply() == ApplyMode.Combine && caps.isMultitextureSupported()
                            && caps.isEnvCombineSupported()) {
                        applyCombineFactors(texture, unitRecord, i, record, caps);
                    }
                }

                // Other items only apply to textures below the frag unit limit
                if (i < caps.getNumberOfFragmentTextureUnits()) {

                    // texture specific params
                    applyFilter(texture, texRecord, i, record, caps);
                    applyWrap(texture, texRecord, i, record, caps);
                    applyShadow(texture, texRecord, i, record, caps);

                    // Set our border color, if needed.
                    applyBorderColor(texture, texRecord, i, record);

                    // all states have now been applied for a tex record, so we
                    // can safely make it valid
                    if (!texRecord.isValid()) {
                        texRecord.validate();
                    }

                }

                // Other items only apply to textures below the frag tex coord
                // unit limit
                if (i < caps.getNumberOfFragmentTexCoordUnits()) {

                    // Now time to play with texture matrices
                    // Determine which transforms to do.
                    applyTextureTransforms(texture, i, record, caps);

                    // Now let's look at automatic texture coordinate
                    // generation.
                    applyTexCoordGeneration(texture, unitRecord, i, record, caps);

                    // Set our texture lod bias, if needed.
                    applyLodBias(texture, unitRecord, i, record, caps);
                }

            }

        } else {
            // turn off texturing
            TextureUnitRecord unitRecord;

            if (caps.isMultitextureSupported()) {
                for (int i = 0; i < caps.getNumberOfFixedTextureUnits(); i++) {
                    unitRecord = record.units[i];
                    disableTexturing(unitRecord, record, i, caps);
                }
            } else {
                unitRecord = record.units[0];
                disableTexturing(unitRecord, record, 0, caps);
            }
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void disableTexturing(final TextureUnitRecord unitRecord, final TextureStateRecord record,
            final int unit, final Type exceptedType, final ContextCapabilities caps) {
        if (exceptedType != Type.TwoDimensional) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.TwoDimensional.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                unitRecord.enabled[Type.TwoDimensional.ordinal()] = false;
            }
        }

        if (exceptedType != Type.OneDimensional) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.OneDimensional.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                GL11.glDisable(GL11.GL_TEXTURE_1D);
                unitRecord.enabled[Type.OneDimensional.ordinal()] = false;
            }
        }

        if (caps.isTexture3DSupported() && exceptedType != Type.ThreeDimensional) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.ThreeDimensional.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                GL11.glDisable(GL12.GL_TEXTURE_3D);
                unitRecord.enabled[Type.ThreeDimensional.ordinal()] = false;
            }
        }

        if (caps.isTextureCubeMapSupported() && exceptedType != Type.CubeMap) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.CubeMap.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                GL11.glDisable(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB);
                unitRecord.enabled[Type.CubeMap.ordinal()] = false;
            }
        }

    }

    private static void disableTexturing(final TextureUnitRecord unitRecord, final TextureStateRecord record,
            final int unit, final ContextCapabilities caps) {
        if (!unitRecord.isValid() || unitRecord.enabled[Type.TwoDimensional.ordinal()]) {
            // Check we are in the right unit
            checkAndSetUnit(unit, record, caps);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            unitRecord.enabled[Type.TwoDimensional.ordinal()] = false;
        }

        if (!unitRecord.isValid() || unitRecord.enabled[Type.OneDimensional.ordinal()]) {
            // Check we are in the right unit
            checkAndSetUnit(unit, record, caps);
            GL11.glDisable(GL11.GL_TEXTURE_1D);
            unitRecord.enabled[Type.OneDimensional.ordinal()] = false;
        }

        if (caps.isTexture3DSupported()) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.ThreeDimensional.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                GL11.glDisable(GL12.GL_TEXTURE_3D);
                unitRecord.enabled[Type.ThreeDimensional.ordinal()] = false;
            }
        }

        if (caps.isTextureCubeMapSupported()) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.CubeMap.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                GL11.glDisable(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB);
                unitRecord.enabled[Type.CubeMap.ordinal()] = false;
            }
        }

    }

    public static void applyCombineFactors(final Texture texture, final TextureUnitRecord unitRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        // check that this is a valid fixed function unit. glTexEnv is only
        // supported for unit < GL_MAX_TEXTURE_UNITS
        if (unit >= caps.getNumberOfFixedTextureUnits()) {
            return;
        }

        // first thing's first... if we are doing dot3 and don't
        // support it, disable this texture.
        boolean checked = false;
        if (!caps.isEnvDot3TextureCombineSupported()
                && (texture.getCombineFuncRGB() == CombinerFunctionRGB.Dot3RGB || texture.getCombineFuncRGB() == CombinerFunctionRGB.Dot3RGBA)) {

            // disable
            disableTexturing(unitRecord, record, unit, caps);

            // No need to continue
            return;
        }

        // Okay, now let's set our scales if we need to:
        // First RGB Combine scale
        if (!unitRecord.isValid() || unitRecord.envRGBScale != texture.getCombineScaleRGB()) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_RGB_SCALE_ARB, texture.getCombineScaleRGB()
                    .floatValue());
            unitRecord.envRGBScale = texture.getCombineScaleRGB();
        }
        // Then Alpha Combine scale
        if (!unitRecord.isValid() || unitRecord.envAlphaScale != texture.getCombineScaleAlpha()) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_ALPHA_SCALE, texture.getCombineScaleAlpha().floatValue());
            unitRecord.envAlphaScale = texture.getCombineScaleAlpha();
        }

        // Time to set the RGB combines
        final CombinerFunctionRGB rgbCombineFunc = texture.getCombineFuncRGB();
        if (!unitRecord.isValid() || unitRecord.rgbCombineFunc != rgbCombineFunc) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_COMBINE_RGB_ARB, LwjglTextureUtil
                    .getGLCombineFuncRGB(rgbCombineFunc));
            unitRecord.rgbCombineFunc = rgbCombineFunc;
        }

        CombinerSource combSrcRGB = texture.getCombineSrc0RGB();
        if (!unitRecord.isValid() || unitRecord.combSrcRGB0 != combSrcRGB) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE0_RGB_ARB, LwjglTextureUtil
                    .getGLCombineSrc(combSrcRGB));
            unitRecord.combSrcRGB0 = combSrcRGB;
        }

        CombinerOperandRGB combOpRGB = texture.getCombineOp0RGB();
        if (!unitRecord.isValid() || unitRecord.combOpRGB0 != combOpRGB) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND0_RGB_ARB, LwjglTextureUtil
                    .getGLCombineOpRGB(combOpRGB));
            unitRecord.combOpRGB0 = combOpRGB;
        }

        // We only need to do Arg1 or Arg2 if we aren't in Replace mode
        if (rgbCombineFunc != CombinerFunctionRGB.Replace) {

            combSrcRGB = texture.getCombineSrc1RGB();
            if (!unitRecord.isValid() || unitRecord.combSrcRGB1 != combSrcRGB) {
                if (!checked) {
                    checkAndSetUnit(unit, record, caps);
                    checked = true;
                }
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE1_RGB_ARB, LwjglTextureUtil
                        .getGLCombineSrc(combSrcRGB));
                unitRecord.combSrcRGB1 = combSrcRGB;
            }

            combOpRGB = texture.getCombineOp1RGB();
            if (!unitRecord.isValid() || unitRecord.combOpRGB1 != combOpRGB) {
                if (!checked) {
                    checkAndSetUnit(unit, record, caps);
                    checked = true;
                }
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND1_RGB_ARB, LwjglTextureUtil
                        .getGLCombineOpRGB(combOpRGB));
                unitRecord.combOpRGB1 = combOpRGB;
            }

            // We only need to do Arg2 if we are in Interpolate mode
            if (rgbCombineFunc == CombinerFunctionRGB.Interpolate) {

                combSrcRGB = texture.getCombineSrc2RGB();
                if (!unitRecord.isValid() || unitRecord.combSrcRGB2 != combSrcRGB) {
                    if (!checked) {
                        checkAndSetUnit(unit, record, caps);
                        checked = true;
                    }
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE2_RGB_ARB, LwjglTextureUtil
                            .getGLCombineSrc(combSrcRGB));
                    unitRecord.combSrcRGB2 = combSrcRGB;
                }

                combOpRGB = texture.getCombineOp2RGB();
                if (!unitRecord.isValid() || unitRecord.combOpRGB2 != combOpRGB) {
                    if (!checked) {
                        checkAndSetUnit(unit, record, caps);
                        checked = true;
                    }
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND2_RGB_ARB, LwjglTextureUtil
                            .getGLCombineOpRGB(combOpRGB));
                    unitRecord.combOpRGB2 = combOpRGB;
                }

            }
        }

        // Now Alpha combines
        final CombinerFunctionAlpha alphaCombineFunc = texture.getCombineFuncAlpha();
        if (!unitRecord.isValid() || unitRecord.alphaCombineFunc != alphaCombineFunc) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_COMBINE_ALPHA_ARB, LwjglTextureUtil
                    .getGLCombineFuncAlpha(alphaCombineFunc));
            unitRecord.alphaCombineFunc = alphaCombineFunc;
        }

        CombinerSource combSrcAlpha = texture.getCombineSrc0Alpha();
        if (!unitRecord.isValid() || unitRecord.combSrcAlpha0 != combSrcAlpha) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE0_ALPHA_ARB, LwjglTextureUtil
                    .getGLCombineSrc(combSrcAlpha));
            unitRecord.combSrcAlpha0 = combSrcAlpha;
        }

        CombinerOperandAlpha combOpAlpha = texture.getCombineOp0Alpha();
        if (!unitRecord.isValid() || unitRecord.combOpAlpha0 != combOpAlpha) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND0_ALPHA_ARB, LwjglTextureUtil
                    .getGLCombineOpAlpha(combOpAlpha));
            unitRecord.combOpAlpha0 = combOpAlpha;
        }

        // We only need to do Arg1 or Arg2 if we aren't in Replace mode
        if (alphaCombineFunc != CombinerFunctionAlpha.Replace) {

            combSrcAlpha = texture.getCombineSrc1Alpha();
            if (!unitRecord.isValid() || unitRecord.combSrcAlpha1 != combSrcAlpha) {
                if (!checked) {
                    checkAndSetUnit(unit, record, caps);
                    checked = true;
                }
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE1_ALPHA_ARB, LwjglTextureUtil
                        .getGLCombineSrc(combSrcAlpha));
                unitRecord.combSrcAlpha1 = combSrcAlpha;
            }

            combOpAlpha = texture.getCombineOp1Alpha();
            if (!unitRecord.isValid() || unitRecord.combOpAlpha1 != combOpAlpha) {
                if (!checked) {
                    checkAndSetUnit(unit, record, caps);
                    checked = true;
                }
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND1_ALPHA_ARB, LwjglTextureUtil
                        .getGLCombineOpAlpha(combOpAlpha));
                unitRecord.combOpAlpha1 = combOpAlpha;
            }

            // We only need to do Arg2 if we are in Interpolate mode
            if (alphaCombineFunc == CombinerFunctionAlpha.Interpolate) {

                combSrcAlpha = texture.getCombineSrc2Alpha();
                if (!unitRecord.isValid() || unitRecord.combSrcAlpha2 != combSrcAlpha) {
                    if (!checked) {
                        checkAndSetUnit(unit, record, caps);
                        checked = true;
                    }
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE2_ALPHA_ARB, LwjglTextureUtil
                            .getGLCombineSrc(combSrcAlpha));
                    unitRecord.combSrcAlpha2 = combSrcAlpha;
                }

                combOpAlpha = texture.getCombineOp2Alpha();
                if (!unitRecord.isValid() || unitRecord.combOpAlpha2 != combOpAlpha) {
                    if (!checked) {
                        checkAndSetUnit(unit, record, caps);
                        checked = true;
                    }
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND2_ALPHA_ARB, LwjglTextureUtil
                            .getGLCombineOpAlpha(combOpAlpha));
                    unitRecord.combOpAlpha2 = combOpAlpha;
                }
            }
        }
    }

    public static void applyEnvMode(final ApplyMode mode, final TextureUnitRecord unitRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        if (!unitRecord.isValid() || unitRecord.envMode != mode) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, LwjglTextureUtil.getGLEnvMode(mode));
            unitRecord.envMode = mode;
        }
    }

    public static void applyBlendColor(final Texture texture, final TextureUnitRecord unitRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final ReadOnlyColorRGBA texBlend = texture.getBlendColor();
        if (!unitRecord.isValid() || !unitRecord.blendColor.equals(texBlend)) {
            checkAndSetUnit(unit, record, caps);
            TextureRecord.colorBuffer.clear();
            TextureRecord.colorBuffer.put(texBlend.getRed()).put(texBlend.getGreen()).put(texBlend.getBlue()).put(
                    texBlend.getAlpha());
            TextureRecord.colorBuffer.rewind();
            GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, TextureRecord.colorBuffer);
            unitRecord.blendColor.set(texBlend);
        }
    }

    public static void applyLodBias(final Texture texture, final TextureUnitRecord unitRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        if (caps.isTextureLodBiasSupported()) {
            final float bias = texture.getLodBias() < caps.getMaxLodBias() ? texture.getLodBias() : caps
                    .getMaxLodBias();
            if (!unitRecord.isValid() || unitRecord.lodBias != bias) {
                checkAndSetUnit(unit, record, caps);
                GL11.glTexEnvf(EXTTextureLODBias.GL_TEXTURE_FILTER_CONTROL_EXT,
                        EXTTextureLODBias.GL_TEXTURE_LOD_BIAS_EXT, bias);
                unitRecord.lodBias = bias;
            }
        }
    }

    public static void applyBorderColor(final Texture texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record) {
        final ReadOnlyColorRGBA texBorder = texture.getBorderColor();
        if (!texRecord.isValid() || !texRecord.borderColor.equals(texBorder)) {
            TextureRecord.colorBuffer.clear();
            TextureRecord.colorBuffer.put(texBorder.getRed()).put(texBorder.getGreen()).put(texBorder.getBlue()).put(
                    texBorder.getAlpha());
            TextureRecord.colorBuffer.rewind();
            GL11.glTexParameter(getGLType(texture.getType()), GL11.GL_TEXTURE_BORDER_COLOR, TextureRecord.colorBuffer);
            texRecord.borderColor.set(texBorder);
        }
    }

    public static void applyTextureTransforms(final Texture texture, final int unit, final TextureStateRecord record,
            final ContextCapabilities caps) {
        final boolean needsReset = !record.units[unit].identityMatrix;

        // Should we apply the transform?
        final boolean doTrans = !texture.getTextureMatrix().isIdentity();

        // Now do them.
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        if (doTrans) {
            checkAndSetUnit(unit, record, caps);
            LwjglRendererUtil.switchMode(matRecord, GL11.GL_TEXTURE);

            record.tmp_matrixBuffer.rewind();
            texture.getTextureMatrix().toDoubleBuffer(record.tmp_matrixBuffer, true);
            record.tmp_matrixBuffer.rewind();
            GL11.glLoadMatrix(record.tmp_matrixBuffer);

            record.units[unit].identityMatrix = false;
        } else if (needsReset) {
            checkAndSetUnit(unit, record, caps);
            LwjglRendererUtil.switchMode(matRecord, GL11.GL_TEXTURE);
            GL11.glLoadIdentity();
            record.units[unit].identityMatrix = true;
        }
        // Switch back to the modelview matrix for further operations
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
    }

    public static void applyTexCoordGeneration(final Texture texture, final TextureUnitRecord unitRecord,
            final int unit, final TextureStateRecord record, final ContextCapabilities caps) {

        switch (texture.getEnvironmentalMapMode()) {
            case None:
                // No coordinate generation
                if (!unitRecord.isValid() || unitRecord.textureGenQ) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = false;
                }
                if (!unitRecord.isValid() || unitRecord.textureGenR) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glDisable(GL11.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = false;
                }
                if (!unitRecord.isValid() || unitRecord.textureGenS) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = false;
                }
                if (!unitRecord.isValid() || unitRecord.textureGenT) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = false;
                }
                break;
            case SphereMap:
                // generate spherical texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenSMode != GL11.GL_SPHERE_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_SPHERE_MAP);
                    unitRecord.textureGenSMode = GL11.GL_SPHERE_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != GL11.GL_SPHERE_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_SPHERE_MAP);
                    unitRecord.textureGenTMode = GL11.GL_SPHERE_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenQ) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = false;
                }
                if (!unitRecord.isValid() || unitRecord.textureGenR) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glDisable(GL11.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = false;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
            case NormalMap:
                // generate spherical texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenSMode != ARBTextureCubeMap.GL_NORMAL_MAP_ARB) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_NORMAL_MAP_ARB);
                    unitRecord.textureGenSMode = ARBTextureCubeMap.GL_NORMAL_MAP_ARB;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != ARBTextureCubeMap.GL_NORMAL_MAP_ARB) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_NORMAL_MAP_ARB);
                    unitRecord.textureGenTMode = ARBTextureCubeMap.GL_NORMAL_MAP_ARB;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenRMode != ARBTextureCubeMap.GL_NORMAL_MAP_ARB) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_NORMAL_MAP_ARB);
                    unitRecord.textureGenTMode = ARBTextureCubeMap.GL_NORMAL_MAP_ARB;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenQ) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = false;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenR) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
            case ReflectionMap:
                // generate spherical texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenSMode != ARBTextureCubeMap.GL_REFLECTION_MAP_ARB) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_REFLECTION_MAP_ARB);
                    unitRecord.textureGenSMode = ARBTextureCubeMap.GL_REFLECTION_MAP_ARB;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != ARBTextureCubeMap.GL_REFLECTION_MAP_ARB) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_REFLECTION_MAP_ARB);
                    unitRecord.textureGenTMode = ARBTextureCubeMap.GL_REFLECTION_MAP_ARB;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenRMode != ARBTextureCubeMap.GL_REFLECTION_MAP_ARB) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, ARBTextureCubeMap.GL_REFLECTION_MAP_ARB);
                    unitRecord.textureGenTMode = ARBTextureCubeMap.GL_REFLECTION_MAP_ARB;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenQ) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = false;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenR) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    checkAndSetUnit(unit, record, caps);
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
            case EyeLinear:
                // do here because we don't check planes
                checkAndSetUnit(unit, record, caps);

                // generate eye linear texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenQMode != GL11.GL_EYE_LINEAR) {
                    GL11.glTexGeni(GL11.GL_Q, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
                    unitRecord.textureGenQMode = GL11.GL_EYE_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenRMode != GL11.GL_EYE_LINEAR) {
                    GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
                    unitRecord.textureGenRMode = GL11.GL_EYE_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenSMode != GL11.GL_EYE_LINEAR) {
                    GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
                    unitRecord.textureGenSMode = GL11.GL_EYE_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != GL11.GL_EYE_LINEAR) {
                    GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
                    unitRecord.textureGenTMode = GL11.GL_EYE_LINEAR;
                }

                record.eyePlaneS.rewind();
                GL11.glTexGen(GL11.GL_S, GL11.GL_EYE_PLANE, record.eyePlaneS);
                record.eyePlaneT.rewind();
                GL11.glTexGen(GL11.GL_T, GL11.GL_EYE_PLANE, record.eyePlaneT);
                record.eyePlaneR.rewind();
                GL11.glTexGen(GL11.GL_R, GL11.GL_EYE_PLANE, record.eyePlaneR);
                record.eyePlaneQ.rewind();
                GL11.glTexGen(GL11.GL_Q, GL11.GL_EYE_PLANE, record.eyePlaneQ);

                if (!unitRecord.isValid() || !unitRecord.textureGenQ) {
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenR) {
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
            case ObjectLinear:
                // do here because we don't check planes
                checkAndSetUnit(unit, record, caps);

                // generate object linear texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenQMode != GL11.GL_OBJECT_LINEAR) {
                    GL11.glTexGeni(GL11.GL_Q, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
                    unitRecord.textureGenSMode = GL11.GL_OBJECT_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenRMode != GL11.GL_OBJECT_LINEAR) {
                    GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
                    unitRecord.textureGenTMode = GL11.GL_OBJECT_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenSMode != GL11.GL_OBJECT_LINEAR) {
                    GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
                    unitRecord.textureGenSMode = GL11.GL_OBJECT_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != GL11.GL_OBJECT_LINEAR) {
                    GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
                    unitRecord.textureGenTMode = GL11.GL_OBJECT_LINEAR;
                }

                record.eyePlaneS.rewind();
                GL11.glTexGen(GL11.GL_S, GL11.GL_OBJECT_PLANE, record.eyePlaneS);
                record.eyePlaneT.rewind();
                GL11.glTexGen(GL11.GL_T, GL11.GL_OBJECT_PLANE, record.eyePlaneT);
                record.eyePlaneR.rewind();
                GL11.glTexGen(GL11.GL_R, GL11.GL_OBJECT_PLANE, record.eyePlaneR);
                record.eyePlaneQ.rewind();
                GL11.glTexGen(GL11.GL_Q, GL11.GL_OBJECT_PLANE, record.eyePlaneQ);

                if (!unitRecord.isValid() || !unitRecord.textureGenQ) {
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenR) {
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
        }
    }

    // If we support multtexturing, specify the unit we are affecting.
    public static void checkAndSetUnit(final int unit, final TextureStateRecord record, final ContextCapabilities caps) {
        // No need to worry about valid record, since invalidate sets record's
        // currentUnit to -1.
        if (record.currentUnit != unit) {
            if (unit >= caps.getNumberOfTotalTextureUnits() || !caps.isMultitextureSupported() || unit < 0) {
                // ignore this request as it is not valid for the user's hardware.
                return;
            }
            ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB + unit);
            record.currentUnit = unit;
        }
    }

    /**
     * Check if the filter settings of this particular texture have been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the texture in gl
     * @param record
     */
    public static void applyShadow(final Texture texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final Type type = texture.getType();

        if (caps.isDepthTextureSupported()) {
            final int depthMode = LwjglTextureUtil.getGLDepthTextureMode(texture.getDepthMode());
            // set up magnification filter
            if (!texRecord.isValid() || texRecord.depthTextureMode != depthMode) {
                checkAndSetUnit(unit, record, caps);
                GL11.glTexParameteri(getGLType(type), ARBDepthTexture.GL_DEPTH_TEXTURE_MODE_ARB, depthMode);
                texRecord.depthTextureMode = depthMode;
            }
        }

        if (caps.isARBShadowSupported()) {
            final int depthCompareMode = LwjglTextureUtil.getGLDepthTextureCompareMode(texture.getDepthCompareMode());
            // set up magnification filter
            if (!texRecord.isValid() || texRecord.depthTextureCompareMode != depthCompareMode) {
                checkAndSetUnit(unit, record, caps);
                GL11.glTexParameteri(getGLType(type), ARBShadow.GL_TEXTURE_COMPARE_MODE_ARB, depthCompareMode);
                texRecord.depthTextureCompareMode = depthCompareMode;
            }

            final int depthCompareFunc = LwjglTextureUtil.getGLDepthTextureCompareFunc(texture.getDepthCompareFunc());
            // set up magnification filter
            if (!texRecord.isValid() || texRecord.depthTextureCompareFunc != depthCompareFunc) {
                checkAndSetUnit(unit, record, caps);
                GL11.glTexParameteri(getGLType(type), ARBShadow.GL_TEXTURE_COMPARE_FUNC_ARB, depthCompareFunc);
                texRecord.depthTextureCompareFunc = depthCompareFunc;
            }
        }
    }

    /**
     * Check if the filter settings of this particular texture have been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the texture in gl
     * @param record
     */
    public static void applyFilter(final Texture texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final Type type = texture.getType();

        final int magFilter = LwjglTextureUtil.getGLMagFilter(texture.getMagnificationFilter());
        // set up magnification filter
        if (!texRecord.isValid() || texRecord.magFilter != magFilter) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(getGLType(type), GL11.GL_TEXTURE_MAG_FILTER, magFilter);
            texRecord.magFilter = magFilter;
        }

        final int minFilter = LwjglTextureUtil.getGLMinFilter(texture.getMinificationFilter());
        // set up mipmap filter
        if (!texRecord.isValid() || texRecord.minFilter != minFilter) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(getGLType(type), GL11.GL_TEXTURE_MIN_FILTER, minFilter);
            texRecord.minFilter = minFilter;
        }

        // set up aniso filter
        if (caps.isAnisoSupported()) {
            float aniso = texture.getAnisotropicFilterPercent() * (caps.getMaxAnisotropic() - 1.0f);
            aniso += 1.0f;
            if (!texRecord.isValid() || texRecord.anisoLevel != aniso) {
                checkAndSetUnit(unit, record, caps);
                GL11.glTexParameterf(getGLType(type), EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, aniso);
                texRecord.anisoLevel = aniso;
            }
        }
    }

    /**
     * Check if the wrap mode of this particular texture has been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record
     */
    public static void applyWrap(final Texture3D texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        if (!caps.isTexture3DSupported()) {
            return;
        }

        final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);
        final int wrapT = getGLWrap(texture.getWrap(WrapAxis.T), caps);
        final int wrapR = getGLWrap(texture.getWrap(WrapAxis.R), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
        if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, wrapT);
            texRecord.wrapT = wrapT;
        }
        if (!texRecord.isValid() || texRecord.wrapR != wrapR) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, wrapR);
            texRecord.wrapR = wrapR;
        }

    }

    /**
     * Check if the wrap mode of this particular texture has been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record
     */
    public static void applyWrap(final Texture1D texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
    }

    /**
     * Check if the wrap mode of this particular texture has been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record
     */
    public static void applyWrap(final Texture texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        if (texture instanceof Texture2D) {
            applyWrap((Texture2D) texture, texRecord, unit, record, caps);
        } else if (texture instanceof Texture1D) {
            applyWrap((Texture1D) texture, texRecord, unit, record, caps);
        } else if (texture instanceof Texture3D) {
            applyWrap((Texture3D) texture, texRecord, unit, record, caps);
        } else if (texture instanceof TextureCubeMap) {
            applyWrap((TextureCubeMap) texture, texRecord, unit, record, caps);
        }
    }

    /**
     * Check if the wrap mode of this particular texture has been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record
     */
    public static void applyWrap(final Texture2D texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);
        final int wrapT = getGLWrap(texture.getWrap(WrapAxis.T), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
        if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapT);
            texRecord.wrapT = wrapT;
        }

    }

    /**
     * Check if the wrap mode of this particular texture has been changed and apply as needed.
     * 
     * @param cubeMap
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record
     */
    public static void applyWrap(final TextureCubeMap cubeMap, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        if (!caps.isTexture3DSupported()) {
            return;
        }

        final int wrapS = getGLWrap(cubeMap.getWrap(WrapAxis.S), caps);
        final int wrapT = getGLWrap(cubeMap.getWrap(WrapAxis.T), caps);
        final int wrapR = getGLWrap(cubeMap.getWrap(WrapAxis.R), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB, GL11.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
        if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB, GL11.GL_TEXTURE_WRAP_T, wrapT);
            texRecord.wrapT = wrapT;
        }
        if (!texRecord.isValid() || texRecord.wrapR != wrapR) {
            checkAndSetUnit(unit, record, caps);
            GL11.glTexParameteri(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB, GL12.GL_TEXTURE_WRAP_R, wrapR);
            texRecord.wrapR = wrapR;
        }
    }

    public static void deleteTextureId(final int textureId) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

        final IntBuffer id = BufferUtils.createIntBuffer(1);
        id.clear();
        id.put(textureId);
        id.rewind();
        GL11.glDeleteTextures(id);
        record.removeTextureRecord(textureId);
    }

    /**
     * Useful for external lwjgl based classes that need to safely set the current texture.
     */
    public static void doTextureBind(final int textureId, final int unit, final Type type) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(RenderState.StateType.Texture);
        // Set this to null because no current state really matches anymore
        context.setCurrentState(StateType.Texture, null);
        checkAndSetUnit(unit, record, context.getCapabilities());

        GL11.glBindTexture(getGLType(type), textureId);
        if (Debug.stats) {
            StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
        }
        if (record != null) {
            record.units[unit].boundTexture = textureId;
        }
    }

    private static int getGLType(final Type type) {
        switch (type) {
            case TwoDimensional:
                return GL11.GL_TEXTURE_2D;
            case OneDimensional:
                return GL11.GL_TEXTURE_1D;
            case ThreeDimensional:
                return GL12.GL_TEXTURE_3D;
            case CubeMap:
                return ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB;
        }
        throw new IllegalArgumentException("invalid texture type: " + type);
    }

    private static int getGLCubeMapFace(final TextureCubeMap.Face face) {
        switch (face) {
            case PositiveX:
                return ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_POSITIVE_X_ARB;
            case NegativeX:
                return ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_NEGATIVE_X_ARB;
            case PositiveY:
                return ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_POSITIVE_Y_ARB;
            case NegativeY:
                return ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y_ARB;
            case PositiveZ:
                return ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_POSITIVE_Z_ARB;
            case NegativeZ:
                return ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z_ARB;
        }
        throw new IllegalArgumentException("invalid cubemap face: " + face);
    }

    public static int getGLWrap(final WrapMode wrap, final ContextCapabilities caps) {
        switch (wrap) {
            case Repeat:
                return GL11.GL_REPEAT;
            case MirroredRepeat:
                if (caps.isTextureMirroredRepeatSupported()) {
                    return ARBTextureMirroredRepeat.GL_MIRRORED_REPEAT_ARB;
                } else {
                    return GL11.GL_REPEAT;
                }
            case MirrorClamp:
                if (caps.isTextureMirrorClampSupported()) {
                    return EXTTextureMirrorClamp.GL_MIRROR_CLAMP_EXT;
                }
                // FALLS THROUGH
            case Clamp:
                return GL11.GL_CLAMP;
            case MirrorBorderClamp:
                if (caps.isTextureMirrorBorderClampSupported()) {
                    return EXTTextureMirrorClamp.GL_MIRROR_CLAMP_TO_BORDER_EXT;
                }
                // FALLS THROUGH
            case BorderClamp:
                if (caps.isTextureBorderClampSupported()) {
                    return ARBTextureBorderClamp.GL_CLAMP_TO_BORDER_ARB;
                } else {
                    return GL11.GL_CLAMP;
                }
            case MirrorEdgeClamp:
                if (caps.isTextureMirrorEdgeClampSupported()) {
                    return EXTTextureMirrorClamp.GL_MIRROR_CLAMP_TO_EDGE_EXT;
                }
                // FALLS THROUGH
            case EdgeClamp:
                if (caps.isTextureEdgeClampSupported()) {
                    return GL12.GL_CLAMP_TO_EDGE;
                } else {
                    return GL11.GL_CLAMP;
                }
        }
        throw new IllegalArgumentException("invalid WrapMode type: " + wrap);
    }
}
