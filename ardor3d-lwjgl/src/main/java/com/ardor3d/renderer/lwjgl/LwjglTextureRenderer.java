/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBDrawBuffers;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.AbstractFBOTextureRenderer;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.scene.state.lwjgl.LwjglTextureStateUtil;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

/**
 * This class is used by Ardor3D's LWJGL implementation to render textures. Users should <b>not </b> create this class
 * directly. Instead, allow DisplaySystem to create it for you.
 * 
 * @see com.ardor3d.system.DisplaySystem#createTextureRenderer
 */
public class LwjglTextureRenderer extends AbstractFBOTextureRenderer {
    private static final Logger logger = Logger.getLogger(LwjglTextureRenderer.class.getName());

    public LwjglTextureRenderer(final DisplaySettings settings, final Target target, final Renderer parentRenderer,
            final ContextCapabilities caps) {
        super(settings, target, parentRenderer, caps);

        if (caps.getMaxFBOColorAttachments() > 1) {
            _attachBuffer = BufferUtils.createIntBuffer(caps.getMaxFBOColorAttachments());
            for (int i = 0; i < caps.getMaxFBOColorAttachments(); i++) {
                _attachBuffer.put(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + i);
            }
        }
    }

    /**
     * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer. Generates a valid OpenGL
     * texture id for this texture and initializes the data type for the texture.
     */
    public void setupTexture(final Texture2D tex) {

        final IntBuffer ibuf = BufferUtils.createIntBuffer(1);

        if (tex.getTextureId() != 0) {
            ibuf.put(tex.getTextureId());
            GL11.glDeleteTextures(ibuf);
            ibuf.clear();
        }

        // Create the texture
        GL11.glGenTextures(ibuf);
        tex.setTextureId(ibuf.get(0));
        TextureManager.registerForCleanup(tex.getTextureKey(), tex.getTextureId());

        LwjglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);
        int format = GL11.GL_RGBA;
        int components = GL11.GL_RGBA8;
        int dataType = GL11.GL_UNSIGNED_BYTE;
        switch (tex.getRTTSource()) {
            case RGBA:
            case RGBA8:
                break;
            case RGB:
            case RGB8:
                format = GL11.GL_RGB;
                components = GL11.GL_RGB8;
                break;
            case Alpha:
            case Alpha8:
                format = GL11.GL_ALPHA;
                components = GL11.GL_ALPHA8;
                break;
            case Depth:
                format = GL11.GL_DEPTH_COMPONENT;
                components = GL11.GL_DEPTH_COMPONENT;
                break;
            case Intensity:
            case Intensity8:
                format = GL11.GL_INTENSITY;
                components = GL11.GL_INTENSITY8;
                break;
            case Luminance:
            case Luminance8:
                format = GL11.GL_LUMINANCE;
                components = GL11.GL_LUMINANCE8;
                break;
            case LuminanceAlpha:
            case Luminance8Alpha8:
                format = GL11.GL_LUMINANCE_ALPHA;
                components = GL11.GL_LUMINANCE8_ALPHA8;
                break;
            case Alpha4:
                format = GL11.GL_ALPHA;
                components = GL11.GL_ALPHA4;
                break;
            case Alpha12:
                format = GL11.GL_ALPHA;
                components = GL11.GL_ALPHA12;
                break;
            case Alpha16:
                format = GL11.GL_ALPHA;
                components = GL11.GL_ALPHA16;
                break;
            case Luminance4:
                format = GL11.GL_LUMINANCE;
                components = GL11.GL_LUMINANCE4;
                break;
            case Luminance12:
                format = GL11.GL_LUMINANCE;
                components = GL11.GL_LUMINANCE12;
                break;
            case Luminance16:
                format = GL11.GL_LUMINANCE;
                components = GL11.GL_LUMINANCE16;
                break;
            case Luminance4Alpha4:
                format = GL11.GL_LUMINANCE_ALPHA;
                components = GL11.GL_LUMINANCE4_ALPHA4;
                break;
            case Luminance6Alpha2:
                format = GL11.GL_LUMINANCE_ALPHA;
                components = GL11.GL_LUMINANCE6_ALPHA2;
                break;
            case Luminance12Alpha4:
                format = GL11.GL_LUMINANCE_ALPHA;
                components = GL11.GL_LUMINANCE12_ALPHA4;
                break;
            case Luminance12Alpha12:
                format = GL11.GL_LUMINANCE_ALPHA;
                components = GL11.GL_LUMINANCE12_ALPHA12;
                break;
            case Luminance16Alpha16:
                format = GL11.GL_LUMINANCE_ALPHA;
                components = GL11.GL_LUMINANCE16_ALPHA16;
                break;
            case Intensity4:
                format = GL11.GL_INTENSITY;
                components = GL11.GL_INTENSITY4;
                break;
            case Intensity12:
                format = GL11.GL_INTENSITY;
                components = GL11.GL_INTENSITY12;
                break;
            case Intensity16:
                format = GL11.GL_INTENSITY;
                components = GL11.GL_INTENSITY16;
                break;
            case R3_G3_B2:
                format = GL11.GL_RGB;
                components = GL11.GL_R3_G3_B2;
                break;
            case RGB4:
                format = GL11.GL_RGB;
                components = GL11.GL_RGB4;
                break;
            case RGB5:
                format = GL11.GL_RGB;
                components = GL11.GL_RGB5;
                break;
            case RGB10:
                format = GL11.GL_RGB;
                components = GL11.GL_RGB10;
                break;
            case RGB12:
                format = GL11.GL_RGB;
                components = GL11.GL_RGB12;
                break;
            case RGB16:
                format = GL11.GL_RGB;
                components = GL11.GL_RGB16;
                break;
            case RGBA2:
                format = GL11.GL_RGBA;
                components = GL11.GL_RGBA2;
                break;
            case RGBA4:
                format = GL11.GL_RGBA;
                components = GL11.GL_RGBA4;
                break;
            case RGB5_A1:
                format = GL11.GL_RGBA;
                components = GL11.GL_RGB5_A1;
                break;
            case RGB10_A2:
                format = GL11.GL_RGBA;
                components = GL11.GL_RGB10_A2;
                break;
            case RGBA12:
                format = GL11.GL_RGBA;
                components = GL11.GL_RGBA12;
                break;
            case RGBA16:
                format = GL11.GL_RGBA;
                components = GL11.GL_RGBA16;
                break;
            case RGBA32F:
                format = GL11.GL_RGBA;
                components = ARBTextureFloat.GL_RGBA32F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case RGB32F:
                format = GL11.GL_RGB;
                components = ARBTextureFloat.GL_RGB32F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case Alpha32F:
                format = GL11.GL_ALPHA;
                components = ARBTextureFloat.GL_ALPHA32F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case Intensity32F:
                format = GL11.GL_INTENSITY;
                components = ARBTextureFloat.GL_INTENSITY32F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case Luminance32F:
                format = GL11.GL_LUMINANCE;
                components = ARBTextureFloat.GL_LUMINANCE32F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case LuminanceAlpha32F:
                format = GL11.GL_LUMINANCE_ALPHA;
                components = ARBTextureFloat.GL_LUMINANCE_ALPHA32F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case RGBA16F:
                format = GL11.GL_RGBA;
                components = ARBTextureFloat.GL_RGBA16F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case RGB16F:
                format = GL11.GL_RGB;
                components = ARBTextureFloat.GL_RGB16F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case Alpha16F:
                format = GL11.GL_ALPHA;
                components = ARBTextureFloat.GL_ALPHA16F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case Intensity16F:
                format = GL11.GL_INTENSITY;
                components = ARBTextureFloat.GL_INTENSITY16F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case Luminance16F:
                format = GL11.GL_LUMINANCE;
                components = ARBTextureFloat.GL_LUMINANCE16F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
            case LuminanceAlpha16F:
                format = GL11.GL_LUMINANCE_ALPHA;
                components = ARBTextureFloat.GL_LUMINANCE_ALPHA16F_ARB;
                dataType = GL11.GL_FLOAT;
                break;
        }

        // Initialize our texture with some default data.
        if (dataType == GL11.GL_UNSIGNED_BYTE) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, components, _width, _height, 0, format, dataType,
                    (ByteBuffer) null);
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, components, _width, _height, 0, format, dataType,
                    (FloatBuffer) null);
        }

        // Initialize mipmapping for this texture, if requested
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
        }

        // Setup filtering and wrap
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(RenderState.StateType.Texture);
        final TextureRecord texRecord = record.getTextureRecord(tex.getTextureId(), tex.getType());

        LwjglTextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
        LwjglTextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());

        logger.fine("setup fbo tex with id " + tex.getTextureId() + ": " + _width + "," + _height);
    }

    public void render(final List<? extends Spatial> toDraw, final List<Texture> texs, final boolean doClear) {

        final int maxDrawBuffers = ContextManager.getCurrentContext().getCapabilities().getMaxFBOColorAttachments();

        // if we only support 1 draw buffer at a time anyway, we'll have to render to each texture individually...
        if (maxDrawBuffers == 1 || texs.size() == 1) {
            try {
                activate();
                for (int i = 0; i < texs.size(); i++) {
                    final Texture tex = texs.get(i);

                    setupForSingleTexDraw(tex, doClear);

                    doDraw(toDraw);

                    takedownForSingleTexDraw(tex);
                }
            } catch (final Exception e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture, boolean)", "Exception",
                        e);
            } finally {
                deactivate();
            }
            return;
        }
        try {
            activate();

            // Otherwise, we can streamline this by rendering to multiple textures at once.
            // first determine how many groups we need
            final LinkedList<Texture> depths = new LinkedList<Texture>();
            final LinkedList<Texture> colors = new LinkedList<Texture>();
            for (int i = 0; i < texs.size(); i++) {
                final Texture tex = texs.get(i);
                if (tex.getRTTSource() == Texture.RenderToTextureType.Depth) {
                    depths.add(tex);
                } else {
                    colors.add(tex);
                }
            }
            // we can only render to 1 depth texture at a time, so # groups is at minimum == numDepth
            final int groups = Math.max(depths.size(), (int) (0.999f + (colors.size() / (float) maxDrawBuffers)));
            for (int i = 0; i < groups; i++) {
                // First handle colors
                int colorsAdded = 0;
                while (colorsAdded < maxDrawBuffers && !colors.isEmpty()) {
                    final Texture tex = colors.removeFirst();
                    EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                            EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + colorsAdded, GL11.GL_TEXTURE_2D, tex
                                    .getTextureId(), 0);
                    colorsAdded++;
                }

                // Now take care of depth.
                if (!depths.isEmpty()) {
                    final Texture tex = depths.removeFirst();
                    // Set up our depth texture
                    EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                            EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, GL11.GL_TEXTURE_2D, tex.getTextureId(), 0);
                    _usingDepthRB = false;
                } else if (!_usingDepthRB) {
                    // setup our default depth render buffer if not already set
                    EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                            EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                            _depthRBID);
                    _usingDepthRB = true;
                }

                setDrawBuffers(colorsAdded);
                setReadBuffer(colorsAdded != 0 ? EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT : GL11.GL_NONE);

                // Check FBO complete
                checkFBOComplete();

                switchCameraIn(doClear);

                doDraw(toDraw);

                switchCameraOut();
            }

            // automatically generate mipmaps for our textures.
            for (int x = 0, max = texs.size(); x < max; x++) {
                if (texs.get(x).getMinificationFilter().usesMipMapLevels()) {
                    LwjglTextureStateUtil.doTextureBind(texs.get(x).getTextureId(), 0, Texture.Type.TwoDimensional);
                    EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
                }
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        } finally {
            deactivate();
        }
    }

    @Override
    protected void setupForSingleTexDraw(final Texture tex, final boolean doClear) {

        LwjglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);

        if (tex.getRTTSource() == Texture.RenderToTextureType.Depth) {
            // Setup depth texture into FBO
            EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, GL11.GL_TEXTURE_2D, tex.getTextureId(), 0);

            setDrawBuffer(GL11.GL_NONE);
            setReadBuffer(GL11.GL_NONE);
        } else {
            // Set textures into FBO
            EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, tex.getTextureId(), 0);

            // setup depth RB
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRBID);

            setDrawBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
            setReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
        }

        // Check FBO complete
        checkFBOComplete();

        switchCameraIn(doClear);
    }

    private void setReadBuffer(final int attachVal) {
        GL11.glReadBuffer(attachVal);
    }

    private void setDrawBuffer(final int attachVal) {
        GL11.glDrawBuffer(attachVal);
    }

    private void setDrawBuffers(final int maxEntry) {
        if (maxEntry <= 1) {
            setDrawBuffer(maxEntry != 0 ? EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT : GL11.GL_NONE);
        } else {
            // We should only get to this point if we support ARBDrawBuffers.
            _attachBuffer.clear();
            _attachBuffer.limit(maxEntry);
            ARBDrawBuffers.glDrawBuffersARB(_attachBuffer);
        }
    }

    @Override
    protected void takedownForSingleTexDraw(final Texture tex) {
        switchCameraOut();

        // automatically generate mipmaps for our texture.
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            LwjglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);
            EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
        }
    }

    private void checkFBOComplete() {
        final int framebuffer = EXTFramebufferObject
                .glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
        switch (framebuffer) {
            case EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT:
                break;
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_UNSUPPORTED_EXT exception");
            default:
                throw new RuntimeException("Unexpected reply from glCheckFramebufferStatusEXT: " + framebuffer);
        }
    }

    public void copyToTexture(final Texture tex, final int width, final int height) {
        LwjglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);

        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
    }

    @Override
    protected void clearBuffers() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers();
    }

    @Override
    protected void activate() {
        // Lazy init
        if (_fboID <= 0) {
            final IntBuffer buffer = BufferUtils.createIntBuffer(1);
            EXTFramebufferObject.glGenFramebuffersEXT(buffer); // generate id
            _fboID = buffer.get(0);

            EXTFramebufferObject.glGenRenderbuffersEXT(buffer); // generate id
            _depthRBID = buffer.get(0);
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRBID);
            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    GL11.GL_DEPTH_COMPONENT, _width, _height);
        }
        if (_active == 0) {
            GL11.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                    _backgroundColor.getAlpha());
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _fboID);

            ContextManager.getCurrentContext().pushEnforcedStates();
            ContextManager.getCurrentContext().clearEnforcedStates();
            ContextManager.getCurrentContext().enforceStates(_enforcedStates);
        }
        _active++;
    }

    @Override
    protected void deactivate() {
        if (_active == 1) {
            final ReadOnlyColorRGBA bgColor = _parentRenderer.getBackgroundColor();
            GL11.glClearColor(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha());
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

            ContextManager.getCurrentContext().popEnforcedStates();
        }
        _active--;
    }

    public void cleanup() {
        if (_fboID > 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_fboID);
            id.rewind();
            EXTFramebufferObject.glDeleteFramebuffersEXT(id);
        }

        if (_depthRBID > 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_depthRBID);
            id.rewind();
            EXTFramebufferObject.glDeleteRenderbuffersEXT(id);
        }
    }
}
