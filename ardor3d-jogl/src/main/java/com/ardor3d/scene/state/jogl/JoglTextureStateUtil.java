/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

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
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.renderer.state.record.TextureUnitRecord;
import com.ardor3d.scene.state.jogl.util.JoglRendererUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.util.Constants;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public class JoglTextureStateUtil {
    private static final Logger logger = Logger.getLogger(JoglTextureStateUtil.class.getName());

    public final static void load(final Texture texture, final int unit) {
        if (texture == null) {
            return;
        }

        final GL gl = GLU.getCurrentGL();
        final GLU glu = new GLU();

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

            // Look for a texture in the cache just like ours
            final TextureKey texKey = texture.getTextureKey();
            final Texture cached = TextureManager.findCachedTexture(texKey);

            if (cached == null) {
                TextureManager.addToCache(texture);
            } else {
                final int textureId = cached.getTextureIdForContext(context.getGlContextRep());
                if (textureId != 0) {
                    doTextureBind(cached, unit, false);
                    return;
                }
            }
        }

        final IntBuffer id = BufferUtils.createIntBuffer(1);
        id.clear();
        gl.glGenTextures(id.limit(), id);
        final int textureId = id.get(0);

        // store the new id by our current gl context.
        texture.setTextureIdForContext(context.getGlContextRep(), textureId);

        // bind our texture id to this unit.
        doTextureBind(texture, unit, false);

        // pass image data to OpenGL
        final Image image = texture.getImage();
        final boolean hasBorder = texture.hasBorder();
        if (image == null) {
            logger.warning("Image data for texture is null.");
        }

        // set alignment to support images with width % 4 != 0, as images are
        // not aligned
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);

        // Get texture image data. Not all textures have image data.
        // For example, ApplyMode.Combine modes can use primary colors,
        // texture output, and constants to modify fragments via the
        // texture units.
        if (image != null) {
            if (!caps.isNonPowerOfTwoTextureSupported()
                    && (!MathUtils.isPowerOfTwo(image.getWidth()) || !MathUtils.isPowerOfTwo(image.getHeight()))) {
                logger.warning("(card unsupported) Attempted to apply texture with size that is not power of 2: "
                        + image.getWidth() + " x " + image.getHeight());

                final int maxSize = caps.getMaxTextureSize();

                final int actualWidth = image.getWidth();
                int w = MathUtils.nearestPowerOfTwo(actualWidth);
                if (w > maxSize) {
                    w = maxSize;
                }

                final int actualHeight = image.getHeight();
                int h = MathUtils.nearestPowerOfTwo(actualHeight);
                if (h > maxSize) {
                    h = maxSize;
                }
                logger.warning("Rescaling image to " + w + " x " + h + " !!!");

                // must rescale image to get "top" mipmap texture image
                final int pixFormat = JoglTextureUtil.getGLPixelFormat(texture.getTextureKey().getFormat(), image
                        .getFormat(), context.getCapabilities());
                final int pixDataType = JoglTextureUtil.getGLPixelDataType(image.getFormat());
                final int bpp = JoglTextureUtil.bytesPerPixel(pixFormat, pixDataType);
                final ByteBuffer scaledImage = BufferUtils.createByteBuffer((w + 4) * h * bpp);
                final int error = glu.gluScaleImage(pixFormat, actualWidth, actualHeight, pixDataType,
                        image.getData(0), w, h, pixDataType, scaledImage);
                if (error != 0) {
                    final int errorCode = gl.glGetError();
                    if (errorCode != GL.GL_NO_ERROR) {
                        throw new GLException(glu.gluErrorString(errorCode));
                    }
                }

                image.setWidth(w);
                image.setHeight(h);
                image.setData(scaledImage);
            }

            if (!texture.getMinificationFilter().usesMipMapLevels()
                    && !JoglTextureUtil.isCompressedType(image.getFormat())) {

                // Load textures which do not need mipmap auto-generating and
                // which aren't using compressed images.

                switch (texture.getType()) {
                    case TwoDimensional:
                        // ensure the buffer is ready for reading
                        image.getData(0).rewind();
                        // send top level to card
                        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, JoglTextureUtil.getGLInternalFormat(image.getFormat()),
                                image.getWidth(), image.getHeight(), hasBorder ? 1 : 0, JoglTextureUtil
                                        .getGLPixelFormat(image.getFormat()), JoglTextureUtil.getGLPixelDataType(image
                                        .getFormat()), image.getData(0));
                        break;
                    case OneDimensional:
                        // ensure the buffer is ready for reading
                        image.getData(0).rewind();
                        // send top level to card
                        gl.glTexImage1D(GL.GL_TEXTURE_1D, 0, JoglTextureUtil.getGLInternalFormat(image.getFormat()),
                                image.getWidth(), hasBorder ? 1 : 0, JoglTextureUtil
                                        .getGLPixelFormat(image.getFormat()), JoglTextureUtil.getGLPixelDataType(image
                                        .getFormat()), image.getData(0));
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
                            gl.glTexImage3D(GL.GL_TEXTURE_3D, 0,
                                    JoglTextureUtil.getGLInternalFormat(image.getFormat()), image.getWidth(), image
                                            .getHeight(), image.getDepth(), hasBorder ? 1 : 0, JoglTextureUtil
                                            .getGLPixelFormat(image.getFormat()), JoglTextureUtil
                                            .getGLPixelDataType(image.getFormat()), data);
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
                                gl.glTexImage2D(getGLCubeMapFace(face), 0, JoglTextureUtil.getGLInternalFormat(image
                                        .getFormat()), image.getWidth(), image.getWidth(), hasBorder ? 1 : 0,
                                        JoglTextureUtil.getGLPixelFormat(image.getFormat()), JoglTextureUtil
                                                .getGLPixelDataType(image.getFormat()), image.getData(face.ordinal()));
                            }
                        } else {
                            logger.warning("This card does not support Cubemaps.");
                        }
                        break;
                }
            } else if (texture.getMinificationFilter().usesMipMapLevels() && !image.hasMipmaps()
                    && !JoglTextureUtil.isCompressedType(image.getFormat())) {

                // For textures which need mipmaps auto-generating and which
                // aren't using compressed images, generate the mipmaps.
                // A new mipmap builder may be needed to build mipmaps for
                // compressed textures.

                if (caps.isAutomaticMipmapsSupported()) {
                    // Flag the card to generate mipmaps
                    gl.glTexParameteri(getGLType(type), GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);
                }

                switch (type) {
                    case TwoDimensional:
                        // ensure the buffer is ready for reading
                        image.getData(0).rewind();
                        if (caps.isAutomaticMipmapsSupported()) {
                            // send top level to card
                            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0,
                                    JoglTextureUtil.getGLInternalFormat(image.getFormat()), image.getWidth(), image
                                            .getHeight(), hasBorder ? 1 : 0, JoglTextureUtil.getGLPixelFormat(image
                                            .getFormat()), JoglTextureUtil.getGLPixelDataType(image.getFormat()), image
                                            .getData(0));
                        } else {
                            // send to card
                            glu.gluBuild2DMipmaps(GL.GL_TEXTURE_2D, JoglTextureUtil.getGLInternalFormat(image
                                    .getFormat()), image.getWidth(), image.getHeight(), JoglTextureUtil
                                    .getGLPixelFormat(image.getFormat()), JoglTextureUtil.getGLPixelDataType(image
                                    .getFormat()), image.getData(0));
                        }
                        break;
                    case OneDimensional:
                        // ensure the buffer is ready for reading
                        image.getData(0).rewind();
                        if (caps.isAutomaticMipmapsSupported()) {
                            // send top level to card
                            gl.glTexImage1D(GL.GL_TEXTURE_1D, 0,
                                    JoglTextureUtil.getGLInternalFormat(image.getFormat()), image.getWidth(),
                                    hasBorder ? 1 : 0, JoglTextureUtil.getGLPixelFormat(image.getFormat()),
                                    JoglTextureUtil.getGLPixelDataType(image.getFormat()), image.getData(0));
                        } else {
                            // Note: JOGL's GLU class does not support
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
                                gl.glTexImage3D(GL.GL_TEXTURE_3D, 0, JoglTextureUtil.getGLInternalFormat(image
                                        .getFormat()), image.getWidth(), image.getHeight(), image.getDepth(),
                                        hasBorder ? 1 : 0, JoglTextureUtil.getGLPixelFormat(image.getFormat()),
                                        JoglTextureUtil.getGLPixelDataType(image.getFormat()), data);
                            } else {
                                // Note: JOGL's GLU class does not support
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
                                    gl.glTexImage2D(getGLCubeMapFace(face), 0, JoglTextureUtil
                                            .getGLInternalFormat(image.getFormat()), image.getWidth(),
                                            image.getWidth(), hasBorder ? 1 : 0, JoglTextureUtil.getGLPixelFormat(image
                                                    .getFormat()), JoglTextureUtil
                                                    .getGLPixelDataType(image.getFormat()), image.getData(face
                                                    .ordinal()));
                                }
                            } else {
                                for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
                                    // ensure the buffer is ready for reading
                                    image.getData(face.ordinal()).rewind();
                                    // send to card
                                    glu.gluBuild2DMipmaps(getGLCubeMapFace(face), JoglTextureUtil
                                            .getGLInternalFormat(image.getFormat()), image.getWidth(),
                                            image.getWidth(), JoglTextureUtil.getGLPixelFormat(image.getFormat()),
                                            JoglTextureUtil.getGLPixelDataType(image.getFormat()), image.getData(face
                                                    .ordinal()));
                                }
                            }
                        } else {
                            logger.warning("This card does not support Cubemaps.");
                            return;
                        }
                        break;
                }

            } else {
                // Here we handle textures that are either compressed or have predefined mipmaps.
                // Get mipmap data sizes and amount of mipmaps to send to opengl. Then loop through all mipmaps and send
                // them.
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
                                final int height = Math.max(1, image.getHeight() >> m);

                                data.position(pos);
                                data.limit(pos + mipSizes[m]);

                                if (JoglTextureUtil.isCompressedType(image.getFormat())) {
                                    gl.glCompressedTexImage2D(getGLCubeMapFace(face), m, JoglTextureUtil
                                            .getGLInternalFormat(image.getFormat()), width, height, hasBorder ? 1 : 0,
                                            mipSizes[m], data);
                                } else {
                                    gl.glTexImage2D(getGLCubeMapFace(face), m, JoglTextureUtil
                                            .getGLInternalFormat(image.getFormat()), width, height, hasBorder ? 1 : 0,
                                            JoglTextureUtil.getGLPixelFormat(image.getFormat()), JoglTextureUtil
                                                    .getGLPixelDataType(image.getFormat()), data);
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
                                if (JoglTextureUtil.isCompressedType(image.getFormat())) {
                                    gl.glCompressedTexImage2D(GL.GL_TEXTURE_2D, m, JoglTextureUtil
                                            .getGLInternalFormat(image.getFormat()), width, height, hasBorder ? 1 : 0,
                                            mipSizes[m], data);
                                } else {
                                    gl.glTexImage2D(GL.GL_TEXTURE_2D, m, JoglTextureUtil.getGLInternalFormat(image
                                            .getFormat()), width, height, hasBorder ? 1 : 0, JoglTextureUtil
                                            .getGLPixelFormat(image.getFormat()), JoglTextureUtil
                                            .getGLPixelDataType(image.getFormat()), data);
                                }
                                break;
                            case OneDimensional:
                                if (JoglTextureUtil.isCompressedType(image.getFormat())) {
                                    gl.glCompressedTexImage1D(GL.GL_TEXTURE_1D, m, JoglTextureUtil
                                            .getGLInternalFormat(image.getFormat()), width, hasBorder ? 1 : 0,
                                            mipSizes[m], data);
                                } else {
                                    gl.glTexImage1D(GL.GL_TEXTURE_1D, m, JoglTextureUtil.getGLInternalFormat(image
                                            .getFormat()), width, hasBorder ? 1 : 0, JoglTextureUtil
                                            .getGLPixelFormat(image.getFormat()), JoglTextureUtil
                                            .getGLPixelDataType(image.getFormat()), data);
                                }
                                break;
                            case ThreeDimensional:
                                // already checked for support above...
                                if (JoglTextureUtil.isCompressedType(image.getFormat())) {
                                    gl.glCompressedTexImage3D(GL.GL_TEXTURE_3D, m, JoglTextureUtil
                                            .getGLInternalFormat(image.getFormat()), width, height, depth,
                                            hasBorder ? 1 : 0, mipSizes[m], data);
                                } else {
                                    gl.glTexImage3D(GL.GL_TEXTURE_3D, m, JoglTextureUtil.getGLInternalFormat(image
                                            .getFormat()), width, height, depth, hasBorder ? 1 : 0, JoglTextureUtil
                                            .getGLPixelFormat(image.getFormat()), JoglTextureUtil
                                            .getGLPixelDataType(image.getFormat()), data);
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

    public static void apply(final JoglRenderer renderer, final TextureState state) {
        final GL gl = GLU.getCurrentGL();

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

            final int glHint = JoglTextureUtil.getPerspHint(state.getCorrectionType());
            if (!record.isValid() || record.hint != glHint) {
                // set up correction mode
                gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, glHint);
                record.hint = glHint;
            }

            // loop through all available texture units...
            for (int i = 0; i < caps.getNumberOfTotalTextureUnits(); i++) {
                unitRecord = record.units[i];

                // grab a texture for this unit, if available
                texture = state.getTexture(i);

                // pull our texture id for this texture, for this context.
                int textureId = texture != null ? texture.getTextureIdForContext(context.getGlContextRep()) : -1;

                // check for invalid textures - ones that have no opengl id and
                // no image data
                if (texture != null && textureId == 0 && texture.getImage() == null) {
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

                        if (i < state._keyCache.length) {
                            state._keyCache[i] = null;
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
                if (textureId == 0) {
                    // texture not yet loaded.
                    // this will load and bind and set the records...
                    load(texture, i);
                    textureId = texture.getTextureIdForContext(context.getGlContextRep());
                    if (textureId == 0) {
                        continue;
                    }
                } else {
                    // texture already exists in OpenGL, just bind it if needed
                    if (!unitRecord.isValid() || unitRecord.boundTexture != textureId) {
                        checkAndSetUnit(i, record, caps);
                        gl.glBindTexture(getGLType(type), textureId);
                        if (Constants.stats) {
                            StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
                        }
                        unitRecord.boundTexture = textureId;
                    }
                }

                // Grab our record for this texture
                texRecord = record.getTextureRecord(textureId, texture.getType());

                // Set the keyCache value for this unit of this texture state
                // This is done so during state comparison we don't have to
                // spend a lot of time pulling out classes and finding field
                // data.
                state._keyCache[i] = texture.getTextureKey();

                // Some texture things only apply to fixed function pipeline
                if (i < caps.getNumberOfFixedTextureUnits()) {

                    // Enable 2D texturing on this unit if not enabled.
                    if (!unitRecord.isValid() || !unitRecord.enabled[type.ordinal()]) {
                        checkAndSetUnit(i, record, caps);
                        gl.glEnable(getGLType(type));
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
                    applyBorderColor(texture, texRecord, i, record, caps);

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
        final GL gl = GLU.getCurrentGL();

        if (exceptedType != Type.TwoDimensional) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.TwoDimensional.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                gl.glDisable(GL.GL_TEXTURE_2D);
                unitRecord.enabled[Type.TwoDimensional.ordinal()] = false;
            }
        }

        if (exceptedType != Type.OneDimensional) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.OneDimensional.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                gl.glDisable(GL.GL_TEXTURE_1D);
                unitRecord.enabled[Type.OneDimensional.ordinal()] = false;
            }
        }

        if (caps.isTexture3DSupported() && exceptedType != Type.ThreeDimensional) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.ThreeDimensional.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                gl.glDisable(GL.GL_TEXTURE_3D);
                unitRecord.enabled[Type.ThreeDimensional.ordinal()] = false;
            }
        }

        if (caps.isTextureCubeMapSupported() && exceptedType != Type.CubeMap) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.CubeMap.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);
                unitRecord.enabled[Type.CubeMap.ordinal()] = false;
            }
        }

    }

    private static void disableTexturing(final TextureUnitRecord unitRecord, final TextureStateRecord record,
            final int unit, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        if (!unitRecord.isValid() || unitRecord.enabled[Type.TwoDimensional.ordinal()]) {
            // Check we are in the right unit
            checkAndSetUnit(unit, record, caps);
            gl.glDisable(GL.GL_TEXTURE_2D);
            unitRecord.enabled[Type.TwoDimensional.ordinal()] = false;
        }

        if (!unitRecord.isValid() || unitRecord.enabled[Type.OneDimensional.ordinal()]) {
            // Check we are in the right unit
            checkAndSetUnit(unit, record, caps);
            gl.glDisable(GL.GL_TEXTURE_1D);
            unitRecord.enabled[Type.OneDimensional.ordinal()] = false;
        }

        if (caps.isTexture3DSupported()) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.ThreeDimensional.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                gl.glDisable(GL.GL_TEXTURE_3D);
                unitRecord.enabled[Type.ThreeDimensional.ordinal()] = false;
            }
        }

        if (caps.isTextureCubeMapSupported()) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.CubeMap.ordinal()]) {
                // Check we are in the right unit
                checkAndSetUnit(unit, record, caps);
                gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);
                unitRecord.enabled[Type.CubeMap.ordinal()] = false;
            }
        }

    }

    public static void applyCombineFactors(final Texture texture, final TextureUnitRecord unitRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

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
            gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, texture.getCombineScaleRGB().floatValue());
            unitRecord.envRGBScale = texture.getCombineScaleRGB();
        } // Then Alpha Combine scale
        if (!unitRecord.isValid() || unitRecord.envAlphaScale != texture.getCombineScaleAlpha()) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_ALPHA_SCALE, texture.getCombineScaleAlpha().floatValue());
            unitRecord.envAlphaScale = texture.getCombineScaleAlpha();
        }

        // Time to set the RGB combines
        final CombinerFunctionRGB rgbCombineFunc = texture.getCombineFuncRGB();
        if (!unitRecord.isValid() || unitRecord.rgbCombineFunc != rgbCombineFunc) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, JoglTextureUtil.getGLCombineFuncRGB(rgbCombineFunc));
            unitRecord.rgbCombineFunc = rgbCombineFunc;
        }

        CombinerSource combSrcRGB = texture.getCombineSrc0RGB();
        if (!unitRecord.isValid() || unitRecord.combSrcRGB0 != combSrcRGB) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, JoglTextureUtil.getGLCombineSrc(combSrcRGB));
            unitRecord.combSrcRGB0 = combSrcRGB;
        }

        CombinerOperandRGB combOpRGB = texture.getCombineOp0RGB();
        if (!unitRecord.isValid() || unitRecord.combOpRGB0 != combOpRGB) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, JoglTextureUtil.getGLCombineOpRGB(combOpRGB));
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
                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, JoglTextureUtil.getGLCombineSrc(combSrcRGB));
                unitRecord.combSrcRGB1 = combSrcRGB;
            }

            combOpRGB = texture.getCombineOp1RGB();
            if (!unitRecord.isValid() || unitRecord.combOpRGB1 != combOpRGB) {
                if (!checked) {
                    checkAndSetUnit(unit, record, caps);
                    checked = true;
                }
                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, JoglTextureUtil.getGLCombineOpRGB(combOpRGB));
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
                    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_RGB, JoglTextureUtil.getGLCombineSrc(combSrcRGB));
                    unitRecord.combSrcRGB2 = combSrcRGB;
                }

                combOpRGB = texture.getCombineOp2RGB();
                if (!unitRecord.isValid() || unitRecord.combOpRGB2 != combOpRGB) {
                    if (!checked) {
                        checkAndSetUnit(unit, record, caps);
                        checked = true;
                    }
                    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_RGB, JoglTextureUtil.getGLCombineOpRGB(combOpRGB));
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
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_ALPHA, JoglTextureUtil
                    .getGLCombineFuncAlpha(alphaCombineFunc));
            unitRecord.alphaCombineFunc = alphaCombineFunc;
        }

        CombinerSource combSrcAlpha = texture.getCombineSrc0Alpha();
        if (!unitRecord.isValid() || unitRecord.combSrcAlpha0 != combSrcAlpha) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_ALPHA, JoglTextureUtil.getGLCombineSrc(combSrcAlpha));
            unitRecord.combSrcAlpha0 = combSrcAlpha;
        }

        CombinerOperandAlpha combOpAlpha = texture.getCombineOp0Alpha();
        if (!unitRecord.isValid() || unitRecord.combOpAlpha0 != combOpAlpha) {
            if (!checked) {
                checkAndSetUnit(unit, record, caps);
                checked = true;
            }
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_ALPHA, JoglTextureUtil.getGLCombineOpAlpha(combOpAlpha));
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
                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_ALPHA, JoglTextureUtil.getGLCombineSrc(combSrcAlpha));
                unitRecord.combSrcAlpha1 = combSrcAlpha;
            }

            combOpAlpha = texture.getCombineOp1Alpha();
            if (!unitRecord.isValid() || unitRecord.combOpAlpha1 != combOpAlpha) {
                if (!checked) {
                    checkAndSetUnit(unit, record, caps);
                    checked = true;
                }
                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_ALPHA, JoglTextureUtil.getGLCombineOpAlpha(combOpAlpha));
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
                    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SOURCE2_ALPHA, JoglTextureUtil.getGLCombineSrc(combSrcAlpha));
                    unitRecord.combSrcAlpha2 = combSrcAlpha;
                }

                combOpAlpha = texture.getCombineOp2Alpha();
                if (!unitRecord.isValid() || unitRecord.combOpAlpha2 != combOpAlpha) {
                    if (!checked) {
                        checkAndSetUnit(unit, record, caps);
                        checked = true;
                    }
                    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_OPERAND2_ALPHA, JoglTextureUtil
                            .getGLCombineOpAlpha(combOpAlpha));
                    unitRecord.combOpAlpha2 = combOpAlpha;
                }
            }
        }
    }

    public static void applyEnvMode(final ApplyMode mode, final TextureUnitRecord unitRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        if (!unitRecord.isValid() || unitRecord.envMode != mode) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, JoglTextureUtil.getGLEnvMode(mode));
            unitRecord.envMode = mode;
        }
    }

    public static void applyBlendColor(final Texture texture, final TextureUnitRecord unitRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        final ReadOnlyColorRGBA texBlend = texture.getBlendColor();
        if (!unitRecord.isValid() || !unitRecord.blendColor.equals(texBlend)) {
            checkAndSetUnit(unit, record, caps);
            TextureRecord.colorBuffer.clear();
            TextureRecord.colorBuffer.put(texBlend.getRed()).put(texBlend.getGreen()).put(texBlend.getBlue()).put(
                    texBlend.getAlpha());
            TextureRecord.colorBuffer.rewind();
            gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, TextureRecord.colorBuffer);
            unitRecord.blendColor.set(texBlend);
        }
    }

    public static void applyLodBias(final Texture texture, final TextureUnitRecord unitRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        if (caps.isTextureLodBiasSupported()) {
            final float bias = texture.getLodBias() < caps.getMaxLodBias() ? texture.getLodBias() : caps
                    .getMaxLodBias();
            if (!unitRecord.isValid() || unitRecord.lodBias != bias) {
                checkAndSetUnit(unit, record, caps);
                gl.glTexEnvf(GL.GL_TEXTURE_FILTER_CONTROL_EXT, GL.GL_TEXTURE_LOD_BIAS_EXT, bias);
                unitRecord.lodBias = bias;
            }
        }
    }

    public static void applyBorderColor(final Texture texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        final ReadOnlyColorRGBA texBorder = texture.getBorderColor();
        if (!texRecord.isValid() || !texRecord.borderColor.equals(texBorder)) {
            TextureRecord.colorBuffer.clear();
            TextureRecord.colorBuffer.put(texBorder.getRed()).put(texBorder.getGreen()).put(texBorder.getBlue()).put(
                    texBorder.getAlpha());
            TextureRecord.colorBuffer.rewind();
            gl.glTexParameterfv(getGLType(texture.getType()), GL.GL_TEXTURE_BORDER_COLOR, TextureRecord.colorBuffer);
            texRecord.borderColor.set(texBorder);
        }
    }

    public static void applyTextureTransforms(final Texture texture, final int unit, final TextureStateRecord record,
            final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        final boolean needsReset = !record.units[unit].identityMatrix;

        // Should we apply the transform?
        final boolean doTrans = !texture.getTextureMatrix().isIdentity();

        // Now do them.
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        if (doTrans) {
            checkAndSetUnit(unit, record, caps);
            JoglRendererUtil.switchMode(matRecord, GL.GL_TEXTURE);

            record.tmp_matrixBuffer.rewind();
            texture.getTextureMatrix().toDoubleBuffer(record.tmp_matrixBuffer, true);
            record.tmp_matrixBuffer.rewind();
            gl.glLoadMatrixd(record.tmp_matrixBuffer);

            record.units[unit].identityMatrix = false;
        } else if (needsReset) {
            checkAndSetUnit(unit, record, caps);
            JoglRendererUtil.switchMode(matRecord, GL.GL_TEXTURE);
            gl.glLoadIdentity();
            record.units[unit].identityMatrix = true;
        }
        // Switch back to the modelview matrix for further operations
        JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
    }

    public static void applyTexCoordGeneration(final Texture texture, final TextureUnitRecord unitRecord,
            final int unit, final TextureStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        switch (texture.getEnvironmentalMapMode()) {
            case None:
                // No coordinate generation
                if (!unitRecord.isValid() || unitRecord.textureGenQ) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glDisable(GL.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = false;
                }
                if (!unitRecord.isValid() || unitRecord.textureGenR) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glDisable(GL.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = false;
                }
                if (!unitRecord.isValid() || unitRecord.textureGenS) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glDisable(GL.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = false;
                }
                if (!unitRecord.isValid() || unitRecord.textureGenT) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glDisable(GL.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = false;
                }
                break;
            case SphereMap:
                // generate spherical texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenSMode != GL.GL_SPHERE_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_SPHERE_MAP);
                    unitRecord.textureGenSMode = GL.GL_SPHERE_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != GL.GL_SPHERE_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_SPHERE_MAP);
                    unitRecord.textureGenTMode = GL.GL_SPHERE_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenQ) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glDisable(GL.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = false;
                }
                if (!unitRecord.isValid() || unitRecord.textureGenR) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glDisable(GL.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = false;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glEnable(GL.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glEnable(GL.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
            case NormalMap:
                // generate normals based texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenRMode != GL.GL_NORMAL_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_NORMAL_MAP);
                    unitRecord.textureGenRMode = GL.GL_NORMAL_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenSMode != GL.GL_NORMAL_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_NORMAL_MAP);
                    unitRecord.textureGenSMode = GL.GL_NORMAL_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != GL.GL_NORMAL_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_NORMAL_MAP);
                    unitRecord.textureGenTMode = GL.GL_NORMAL_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenQ) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glDisable(GL.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = false;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenR) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glEnable(GL.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glEnable(GL.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glEnable(GL.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
            case ReflectionMap:
                // generate reflection texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenRMode != GL.GL_REFLECTION_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP);
                    unitRecord.textureGenRMode = GL.GL_REFLECTION_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenSMode != GL.GL_REFLECTION_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP);
                    unitRecord.textureGenSMode = GL.GL_REFLECTION_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != GL.GL_REFLECTION_MAP) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP);
                    unitRecord.textureGenTMode = GL.GL_REFLECTION_MAP;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenQ) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glDisable(GL.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = false;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenR) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glEnable(GL.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glEnable(GL.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    checkAndSetUnit(unit, record, caps);
                    gl.glEnable(GL.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
            case EyeLinear:
                // do here because we don't check planes
                checkAndSetUnit(unit, record, caps);

                // generate eye linear texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenQMode != GL.GL_EYE_LINEAR) {
                    gl.glTexGeni(GL.GL_Q, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
                    unitRecord.textureGenQMode = GL.GL_EYE_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenRMode != GL.GL_EYE_LINEAR) {
                    gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
                    unitRecord.textureGenRMode = GL.GL_EYE_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenSMode != GL.GL_EYE_LINEAR) {
                    gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
                    unitRecord.textureGenSMode = GL.GL_EYE_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != GL.GL_EYE_LINEAR) {
                    gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
                    unitRecord.textureGenTMode = GL.GL_EYE_LINEAR;
                }

                record.prepPlane(texture.getEnvPlaneQ(), TextureStateRecord.DEFAULT_Q_PLANE);
                gl.glTexGenfv(GL.GL_Q, GL.GL_EYE_PLANE, record.plane);
                record.prepPlane(texture.getEnvPlaneR(), TextureStateRecord.DEFAULT_R_PLANE);
                gl.glTexGenfv(GL.GL_R, GL.GL_EYE_PLANE, record.plane);
                record.prepPlane(texture.getEnvPlaneS(), TextureStateRecord.DEFAULT_S_PLANE);
                gl.glTexGenfv(GL.GL_S, GL.GL_EYE_PLANE, record.plane);
                record.prepPlane(texture.getEnvPlaneT(), TextureStateRecord.DEFAULT_T_PLANE);
                gl.glTexGenfv(GL.GL_T, GL.GL_EYE_PLANE, record.plane);

                if (!unitRecord.isValid() || !unitRecord.textureGenQ) {
                    gl.glEnable(GL.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenR) {
                    gl.glEnable(GL.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    gl.glEnable(GL.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    gl.glEnable(GL.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
            case ObjectLinear:
                // do here because we don't check planes
                checkAndSetUnit(unit, record, caps);

                // generate object linear texture coordinates
                if (!unitRecord.isValid() || unitRecord.textureGenQMode != GL.GL_OBJECT_LINEAR) {
                    gl.glTexGeni(GL.GL_Q, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
                    unitRecord.textureGenQMode = GL.GL_OBJECT_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenRMode != GL.GL_OBJECT_LINEAR) {
                    gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
                    unitRecord.textureGenRMode = GL.GL_OBJECT_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenSMode != GL.GL_OBJECT_LINEAR) {
                    gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
                    unitRecord.textureGenSMode = GL.GL_OBJECT_LINEAR;
                }

                if (!unitRecord.isValid() || unitRecord.textureGenTMode != GL.GL_OBJECT_LINEAR) {
                    gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
                    unitRecord.textureGenTMode = GL.GL_OBJECT_LINEAR;
                }

                record.prepPlane(texture.getEnvPlaneQ(), TextureStateRecord.DEFAULT_Q_PLANE);
                gl.glTexGenfv(GL.GL_Q, GL.GL_OBJECT_PLANE, record.plane);
                record.prepPlane(texture.getEnvPlaneR(), TextureStateRecord.DEFAULT_R_PLANE);
                gl.glTexGenfv(GL.GL_R, GL.GL_OBJECT_PLANE, record.plane);
                record.prepPlane(texture.getEnvPlaneS(), TextureStateRecord.DEFAULT_S_PLANE);
                gl.glTexGenfv(GL.GL_S, GL.GL_OBJECT_PLANE, record.plane);
                record.prepPlane(texture.getEnvPlaneT(), TextureStateRecord.DEFAULT_T_PLANE);
                gl.glTexGenfv(GL.GL_T, GL.GL_OBJECT_PLANE, record.plane);

                if (!unitRecord.isValid() || !unitRecord.textureGenQ) {
                    gl.glEnable(GL.GL_TEXTURE_GEN_Q);
                    unitRecord.textureGenQ = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenR) {
                    gl.glEnable(GL.GL_TEXTURE_GEN_R);
                    unitRecord.textureGenR = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenS) {
                    gl.glEnable(GL.GL_TEXTURE_GEN_S);
                    unitRecord.textureGenS = true;
                }
                if (!unitRecord.isValid() || !unitRecord.textureGenT) {
                    gl.glEnable(GL.GL_TEXTURE_GEN_T);
                    unitRecord.textureGenT = true;
                }
                break;
        }
    }

    // If we support multitexturing, specify the unit we are affecting.
    public static void checkAndSetUnit(final int unit, final TextureStateRecord record, final ContextCapabilities caps) {
        final GL gl = GLU.getCurrentGL();

        // No need to worry about valid record, since invalidate sets record's
        // currentUnit to -1.
        if (record.currentUnit != unit) {
            if (unit >= caps.getNumberOfTotalTextureUnits() || !caps.isMultitextureSupported() || unit < 0) {
                // ignore this request as it is not valid for the user's hardware.
                return;
            }
            gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
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
        final GL gl = GLU.getCurrentGL();

        final Type type = texture.getType();

        if (caps.isDepthTextureSupported()) {
            final int depthMode = JoglTextureUtil.getGLDepthTextureMode(texture.getDepthMode());
            // set up magnification filter
            if (!texRecord.isValid() || texRecord.depthTextureMode != depthMode) {
                checkAndSetUnit(unit, record, caps);
                gl.glTexParameteri(getGLType(type), GL.GL_DEPTH_TEXTURE_MODE_ARB, depthMode);
                texRecord.depthTextureMode = depthMode;
            }
        }

        if (caps.isARBShadowSupported()) {
            final int depthCompareMode = JoglTextureUtil.getGLDepthTextureCompareMode(texture.getDepthCompareMode());
            // set up magnification filter
            if (!texRecord.isValid() || texRecord.depthTextureCompareMode != depthCompareMode) {
                checkAndSetUnit(unit, record, caps);
                gl.glTexParameteri(getGLType(type), GL.GL_TEXTURE_COMPARE_MODE_ARB, depthCompareMode);
                texRecord.depthTextureCompareMode = depthCompareMode;
            }

            final int depthCompareFunc = JoglTextureUtil.getGLDepthTextureCompareFunc(texture.getDepthCompareFunc());
            // set up magnification filter
            if (!texRecord.isValid() || texRecord.depthTextureCompareFunc != depthCompareFunc) {
                checkAndSetUnit(unit, record, caps);
                gl.glTexParameteri(getGLType(type), GL.GL_TEXTURE_COMPARE_FUNC_ARB, depthCompareFunc);
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
        final GL gl = GLU.getCurrentGL();

        final Type type = texture.getType();

        final int magFilter = JoglTextureUtil.getGLMagFilter(texture.getMagnificationFilter());
        // set up magnification filter
        if (!texRecord.isValid() || texRecord.magFilter != magFilter) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(getGLType(type), GL.GL_TEXTURE_MAG_FILTER, magFilter);
            texRecord.magFilter = magFilter;
        }

        final int minFilter = JoglTextureUtil.getGLMinFilter(texture.getMinificationFilter());
        // set up mipmap filter
        if (!texRecord.isValid() || texRecord.minFilter != minFilter) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(getGLType(type), GL.GL_TEXTURE_MIN_FILTER, minFilter);
            texRecord.minFilter = minFilter;
        }

        // set up aniso filter
        if (caps.isAnisoSupported()) {
            float aniso = texture.getAnisotropicFilterPercent() * (caps.getMaxAnisotropic() - 1.0f);
            aniso += 1.0f;
            if (!texRecord.isValid() || texRecord.anisoLevel != aniso) {
                checkAndSetUnit(unit, record, caps);
                gl.glTexParameterf(getGLType(type), GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, aniso);
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
        final GL gl = GLU.getCurrentGL();

        if (!caps.isTexture3DSupported()) {
            return;
        }

        final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);
        final int wrapT = getGLWrap(texture.getWrap(WrapAxis.T), caps);
        final int wrapR = getGLWrap(texture.getWrap(WrapAxis.R), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
        if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, wrapT);
            texRecord.wrapT = wrapT;
        }
        if (!texRecord.isValid() || texRecord.wrapR != wrapR) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_R, wrapR);
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
        final GL gl = GLU.getCurrentGL();

        final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_WRAP_S, wrapS);
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
        final GL gl = GLU.getCurrentGL();

        final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);
        final int wrapT = getGLWrap(texture.getWrap(WrapAxis.T), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
        if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapT);
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
        final GL gl = GLU.getCurrentGL();

        if (!caps.isTextureCubeMapSupported()) {
            return;
        }

        final int wrapS = getGLWrap(cubeMap.getWrap(WrapAxis.S), caps);
        final int wrapT = getGLWrap(cubeMap.getWrap(WrapAxis.T), caps);
        final int wrapR = getGLWrap(cubeMap.getWrap(WrapAxis.R), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
        if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_WRAP_T, wrapT);
            texRecord.wrapT = wrapT;
        }
        if (!texRecord.isValid() || texRecord.wrapR != wrapR) {
            checkAndSetUnit(unit, record, caps);
            gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_WRAP_R, wrapR);
            texRecord.wrapR = wrapR;
        }
    }

    public static void deleteTexture(final Texture texture) {
        final GL gl = GLU.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

        final int id = texture.getTextureIdForContext(context.getGlContextRep());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        final IntBuffer idBuffer = BufferUtils.createIntBuffer(1);
        idBuffer.clear();
        idBuffer.put(id);
        idBuffer.rewind();
        gl.glDeleteTextures(idBuffer.limit(), idBuffer);
        record.removeTextureRecord(id);
        texture.removeFromIdCache(context.getGlContextRep());
    }

    public static void deleteTextureIds(final Collection<Integer> ids) {
        final GL gl = GLU.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

        final IntBuffer idBuffer = BufferUtils.createIntBuffer(ids.size());
        idBuffer.clear();
        for (final Integer i : ids) {
            if (i != null) {
                idBuffer.put(i);
                record.removeTextureRecord(i);
            }
        }
        idBuffer.flip();
        if (idBuffer.remaining() > 0) {
            gl.glDeleteTextures(idBuffer.remaining(), idBuffer);
        }
    }

    /**
     * Useful for external jogl based classes that need to safely set the current texture.
     */
    public static void doTextureBind(final Texture texture, final int unit, final boolean invalidateState) {
        final GL gl = GLU.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);
        if (invalidateState) {
            // Set this to null because no current state really matches anymore
            context.setCurrentState(StateType.Texture, null);
        }
        checkAndSetUnit(unit, record, caps);

        final int id = texture.getTextureIdForContext(context.getGlContextRep());
        gl.glBindTexture(getGLType(texture.getType()), id);
        if (Constants.stats) {
            StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
        }
        if (record != null) {
            record.units[unit].boundTexture = id;
        }
    }

    private static int getGLType(final Type type) {
        switch (type) {
            case TwoDimensional:
                return GL.GL_TEXTURE_2D;
            case OneDimensional:
                return GL.GL_TEXTURE_1D;
            case ThreeDimensional:
                return GL.GL_TEXTURE_3D;
            case CubeMap:
                return GL.GL_TEXTURE_CUBE_MAP;
        }
        throw new IllegalArgumentException("invalid texture type: " + type);
    }

    private static int getGLCubeMapFace(final TextureCubeMap.Face face) {
        switch (face) {
            case PositiveX:
                return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
            case NegativeX:
                return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
            case PositiveY:
                return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
            case NegativeY:
                return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
            case PositiveZ:
                return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
            case NegativeZ:
                return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        }
        throw new IllegalArgumentException("invalid cubemap face: " + face);
    }

    private static int getGLWrap(final WrapMode wrap, final ContextCapabilities caps) {
        switch (wrap) {
            case Repeat:
                return GL.GL_REPEAT;
            case MirroredRepeat:
                if (caps.isTextureMirroredRepeatSupported()) {
                    return GL.GL_MIRRORED_REPEAT_ARB;
                } else {
                    return GL.GL_REPEAT;
                }
            case MirrorClamp:
                if (caps.isTextureMirrorClampSupported()) {
                    return GL.GL_MIRROR_CLAMP_EXT;
                }
                // FALLS THROUGH
            case Clamp:
                return GL.GL_CLAMP;
            case MirrorBorderClamp:
                if (caps.isTextureMirrorBorderClampSupported()) {
                    return GL.GL_MIRROR_CLAMP_TO_BORDER_EXT;
                }
                // FALLS THROUGH
            case BorderClamp:
                if (caps.isTextureBorderClampSupported()) {
                    return GL.GL_CLAMP_TO_BORDER;
                } else {
                    return GL.GL_CLAMP;
                }
            case MirrorEdgeClamp:
                if (caps.isTextureMirrorEdgeClampSupported()) {
                    return GL.GL_MIRROR_CLAMP_TO_EDGE_EXT;
                }
                // FALLS THROUGH
            case EdgeClamp:
                if (caps.isTextureEdgeClampSupported()) {
                    return GL.GL_CLAMP_TO_EDGE;
                } else {
                    return GL.GL_CLAMP;
                }
        }
        throw new IllegalArgumentException("invalid WrapMode type: " + wrap);
    }
}
