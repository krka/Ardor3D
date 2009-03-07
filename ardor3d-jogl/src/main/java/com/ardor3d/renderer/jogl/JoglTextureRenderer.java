/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

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
import com.ardor3d.scene.state.jogl.JoglTextureStateUtil;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

/**
 * This class is used by JOGL to render textures. Users should <b>not </b> create this class directly. Instead, allow
 * DisplaySystem to create it for you.
 * 
 * @see com.ardor3d.system.DisplaySystem#createTextureRenderer
 */
public class JoglTextureRenderer extends AbstractFBOTextureRenderer {
    private static final Logger logger = Logger.getLogger(JoglTextureRenderer.class.getName());

    public JoglTextureRenderer(final DisplaySettings settings, final Target target, final Renderer parentRenderer,
            final ContextCapabilities caps) {
        super(settings, target, parentRenderer, caps);

        if (caps.getMaxFBOColorAttachments() > 1) {
            _attachBuffer = BufferUtils.createIntBuffer(caps.getMaxFBOColorAttachments());
            for (int i = 0; i < caps.getMaxFBOColorAttachments(); i++) {
                _attachBuffer.put(GL.GL_COLOR_ATTACHMENT0_EXT + i);
            }
        }
    }

    /**
     * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer. Generates a valid OpenGL
     * texture id for this texture and initializes the data type for the texture.
     */
    public void setupTexture(final Texture2D tex) {
        final GL gl = GLU.getCurrentGL();

        final IntBuffer ibuf = BufferUtils.createIntBuffer(1);

        if (tex.getTextureId() != 0) {
            ibuf.put(tex.getTextureId());
            gl.glDeleteTextures(ibuf.limit(), ibuf); // TODO Check <size>
            ibuf.clear();
        }

        // Create the texture
        gl.glGenTextures(ibuf.limit(), ibuf); // TODO Check <size>
        tex.setTextureId(ibuf.get(0));
        TextureManager.registerForCleanup(tex.getTextureKey(), tex.getTextureId());

        JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);
        int components = GL.GL_RGBA8;
        int format = GL.GL_RGBA;
        int dataType = GL.GL_UNSIGNED_BYTE;
        switch (tex.getRTTSource()) {
            case RGBA:
            case RGBA8:
                break;
            case RGB:
            case RGB8:
                format = GL.GL_RGB;
                components = GL.GL_RGB8;
                break;
            case Alpha:
            case Alpha8:
                format = GL.GL_ALPHA;
                components = GL.GL_ALPHA8;
                break;
            case Depth:
                format = GL.GL_DEPTH_COMPONENT;
                components = GL.GL_DEPTH_COMPONENT;
                break;
            case Intensity:
            case Intensity8:
                format = GL.GL_INTENSITY;
                components = GL.GL_INTENSITY8;
                break;
            case Luminance:
            case Luminance8:
                format = GL.GL_LUMINANCE;
                components = GL.GL_LUMINANCE8;
                break;
            case LuminanceAlpha:
            case Luminance8Alpha8:
                format = GL.GL_LUMINANCE_ALPHA;
                components = GL.GL_LUMINANCE8_ALPHA8;
                break;
            case Alpha4:
                format = GL.GL_ALPHA;
                components = GL.GL_ALPHA4;
                break;
            case Alpha12:
                format = GL.GL_ALPHA;
                components = GL.GL_ALPHA12;
                break;
            case Alpha16:
                format = GL.GL_ALPHA;
                components = GL.GL_ALPHA16;
                break;
            case Luminance4:
                format = GL.GL_LUMINANCE;
                components = GL.GL_LUMINANCE4;
                break;
            case Luminance12:
                format = GL.GL_LUMINANCE;
                components = GL.GL_LUMINANCE12;
                break;
            case Luminance16:
                format = GL.GL_LUMINANCE;
                components = GL.GL_LUMINANCE16;
                break;
            case Luminance4Alpha4:
                format = GL.GL_LUMINANCE_ALPHA;
                components = GL.GL_LUMINANCE4_ALPHA4;
                break;
            case Luminance6Alpha2:
                format = GL.GL_LUMINANCE_ALPHA;
                components = GL.GL_LUMINANCE6_ALPHA2;
                break;
            case Luminance12Alpha4:
                format = GL.GL_LUMINANCE_ALPHA;
                components = GL.GL_LUMINANCE12_ALPHA4;
                break;
            case Luminance12Alpha12:
                format = GL.GL_LUMINANCE_ALPHA;
                components = GL.GL_LUMINANCE12_ALPHA12;
                break;
            case Luminance16Alpha16:
                format = GL.GL_LUMINANCE_ALPHA;
                components = GL.GL_LUMINANCE16_ALPHA16;
                break;
            case Intensity4:
                format = GL.GL_INTENSITY;
                components = GL.GL_INTENSITY4;
                break;
            case Intensity12:
                format = GL.GL_INTENSITY;
                components = GL.GL_INTENSITY12;
                break;
            case Intensity16:
                format = GL.GL_INTENSITY;
                components = GL.GL_INTENSITY16;
                break;
            case R3_G3_B2:
                format = GL.GL_RGB;
                components = GL.GL_R3_G3_B2;
                break;
            case RGB4:
                format = GL.GL_RGB;
                components = GL.GL_RGB4;
                break;
            case RGB5:
                format = GL.GL_RGB;
                components = GL.GL_RGB5;
                break;
            case RGB10:
                format = GL.GL_RGB;
                components = GL.GL_RGB10;
                break;
            case RGB12:
                format = GL.GL_RGB;
                components = GL.GL_RGB12;
                break;
            case RGB16:
                format = GL.GL_RGB;
                components = GL.GL_RGB16;
                break;
            case RGBA2:
                format = GL.GL_RGBA;
                components = GL.GL_RGBA2;
                break;
            case RGBA4:
                format = GL.GL_RGBA;
                components = GL.GL_RGBA4;
                break;
            case RGB5_A1:
                format = GL.GL_RGBA;
                components = GL.GL_RGB5_A1;
                break;
            case RGB10_A2:
                format = GL.GL_RGBA;
                components = GL.GL_RGB10_A2;
                break;
            case RGBA12:
                format = GL.GL_RGBA;
                components = GL.GL_RGBA12;
                break;
            case RGBA16:
                format = GL.GL_RGBA;
                components = GL.GL_RGBA16;
                break;
            case RGBA32F:
                format = GL.GL_RGBA;
                components = GL.GL_RGBA32F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case RGB32F:
                format = GL.GL_RGB;
                components = GL.GL_RGB32F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case Alpha32F:
                format = GL.GL_ALPHA;
                components = GL.GL_ALPHA32F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case Intensity32F:
                format = GL.GL_INTENSITY;
                components = GL.GL_INTENSITY32F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case Luminance32F:
                format = GL.GL_LUMINANCE;
                components = GL.GL_LUMINANCE32F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case LuminanceAlpha32F:
                format = GL.GL_LUMINANCE_ALPHA;
                components = GL.GL_LUMINANCE_ALPHA32F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case RGBA16F:
                format = GL.GL_RGBA;
                components = GL.GL_RGBA16F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case RGB16F:
                format = GL.GL_RGB;
                components = GL.GL_RGB16F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case Alpha16F:
                format = GL.GL_ALPHA;
                components = GL.GL_ALPHA16F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case Intensity16F:
                format = GL.GL_INTENSITY;
                components = GL.GL_INTENSITY16F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case Luminance16F:
                format = GL.GL_LUMINANCE;
                components = GL.GL_LUMINANCE16F_ARB;
                dataType = GL.GL_FLOAT;
                break;
            case LuminanceAlpha16F:
                format = GL.GL_LUMINANCE_ALPHA;
                components = GL.GL_LUMINANCE_ALPHA16F_ARB;
                dataType = GL.GL_FLOAT;
                break;
        }

        // Initialize our texture with some default data.
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, components, _width, _height, 0, format, dataType, null);

        // Initialize mipmapping for this texture, if requested
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            gl.glGenerateMipmapEXT(GL.GL_TEXTURE_2D);
        }

        // Setup filtering and wrap
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(RenderState.StateType.Texture);
        final TextureRecord texRecord = record.getTextureRecord(tex.getTextureId(), tex.getType());

        JoglTextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
        JoglTextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());

        logger.fine("setup fbo tex with id " + tex.getTextureId() + ": " + _width + "," + _height);
    }

    public void render(final Spatial spat, final List<Texture> texs, final boolean doClear) {
        render(null, spat, texs, doClear);
    }

    public void render(final List<? extends Spatial> spat, final List<Texture> texs, final boolean doClear) {
        render(spat, null, texs, doClear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final List<Texture> texs,
            final boolean doClear) {
        final GL gl = GLU.getCurrentGL();

        final int maxDrawBuffers = ContextManager.getCurrentContext().getCapabilities().getMaxFBOColorAttachments();

        // if we only support 1 draw buffer at a time anyway, we'll have to render to each texture individually...
        if (maxDrawBuffers == 1 || texs.size() == 1) {
            try {
                activate();
                for (int i = 0; i < texs.size(); i++) {
                    final Texture tex = texs.get(i);

                    setupForSingleTexDraw(tex, doClear);

                    if (toDrawA != null) {
                        doDraw(toDrawA);
                    } else {
                        doDraw(toDrawB);
                    }

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
                    gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0_EXT + colorsAdded,
                            GL.GL_TEXTURE_2D, tex.getTextureId(), 0);
                    colorsAdded++;
                }

                // Now take care of depth.
                if (!depths.isEmpty()) {
                    final Texture tex = depths.removeFirst();
                    // Set up our depth texture
                    gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_TEXTURE_2D,
                            tex.getTextureId(), 0);
                    _usingDepthRB = false;
                } else if (!_usingDepthRB) {
                    // setup our default depth render buffer if not already set
                    gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT,
                            GL.GL_RENDERBUFFER_EXT, _depthRBID);
                    _usingDepthRB = true;
                }

                setDrawBuffers(colorsAdded);
                setReadBuffer(colorsAdded != 0 ? GL.GL_COLOR_ATTACHMENT0_EXT : GL.GL_NONE);

                // Check FBO complete
                checkFBOComplete();

                switchCameraIn(doClear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();
            }

            // automatically generate mipmaps for our textures.
            for (int x = 0, max = texs.size(); x < max; x++) {
                if (texs.get(x).getMinificationFilter().usesMipMapLevels()) {
                    JoglTextureStateUtil.doTextureBind(texs.get(x).getTextureId(), 0, Texture.Type.TwoDimensional);
                    gl.glGenerateMipmapEXT(GL.GL_TEXTURE_2D);
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
        final GL gl = GLU.getCurrentGL();

        JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);

        if (tex.getRTTSource() == Texture.RenderToTextureType.Depth) {
            // Setup depth texture into FBO
            gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_TEXTURE_2D, tex
                    .getTextureId(), 0);

            setDrawBuffer(GL.GL_NONE);
            setReadBuffer(GL.GL_NONE);
        } else {
            // Set textures into FBO
            gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0_EXT, GL.GL_TEXTURE_2D, tex
                    .getTextureId(), 0);

            // setup depth RB
            gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_RENDERBUFFER_EXT,
                    _depthRBID);

            setDrawBuffer(GL.GL_COLOR_ATTACHMENT0_EXT);
            setReadBuffer(GL.GL_COLOR_ATTACHMENT0_EXT);
        }

        // Check FBO complete
        checkFBOComplete();

        switchCameraIn(doClear);
    }

    private void setReadBuffer(final int attachVal) {
        final GL gl = GLU.getCurrentGL();

        gl.glReadBuffer(attachVal);
    }

    private void setDrawBuffer(final int attachVal) {
        final GL gl = GLU.getCurrentGL();

        gl.glDrawBuffer(attachVal);
    }

    private void setDrawBuffers(final int maxEntry) {
        final GL gl = GLU.getCurrentGL();

        if (maxEntry <= 1) {
            setDrawBuffer(maxEntry != 0 ? GL.GL_COLOR_ATTACHMENT0_EXT : GL.GL_NONE);
        } else {
            // We should only get to this point if we support ARBDrawBuffers.
            _attachBuffer.clear();
            _attachBuffer.limit(maxEntry);
            gl.glDrawBuffersARB(_attachBuffer.limit(), _attachBuffer); // TODO Check <size>
        }
    }

    @Override
    protected void takedownForSingleTexDraw(final Texture tex) {
        final GL gl = GLU.getCurrentGL();

        switchCameraOut();

        // automatically generate mipmaps for our texture.
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);
            gl.glGenerateMipmapEXT(GL.GL_TEXTURE_2D);
        }
    }

    private void checkFBOComplete() {
        final GL gl = GLU.getCurrentGL();

        final int framebuffer = gl.glCheckFramebufferStatusEXT(GL.GL_FRAMEBUFFER_EXT);
        switch (framebuffer) {
            case GL.GL_FRAMEBUFFER_COMPLETE_EXT:
                break;
            case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT exception");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT exception");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT exception");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT exception");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT exception");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT exception");
            case GL.GL_FRAMEBUFFER_UNSUPPORTED_EXT:
                throw new RuntimeException("FrameBuffer: " + _fboID
                        + ", has caused a GL_FRAMEBUFFER_UNSUPPORTED_EXT exception");
            default:
                throw new RuntimeException("Unexpected reply from glCheckFramebufferStatusEXT: " + framebuffer);
        }
    }

    public void copyToTexture(final Texture tex, final int width, final int height) {
        final GL gl = GLU.getCurrentGL();

        JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);

        gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
    }

    @Override
    protected void clearBuffers() {
        final GL gl = GLU.getCurrentGL();

        gl.glDisable(GL.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers();
    }

    @Override
    protected void activate() {
        final GL gl = GLU.getCurrentGL();

        // Lazy init
        if (_fboID <= 0) {
            final IntBuffer buffer = BufferUtils.createIntBuffer(1);
            gl.glGenFramebuffersEXT(buffer.limit(), buffer); // TODO Check <size> // generate id
            _fboID = buffer.get(0);

            gl.glGenRenderbuffersEXT(buffer.limit(), buffer); // TODO Check <size> // generate id
            _depthRBID = buffer.get(0);
            gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, _depthRBID);
            gl.glRenderbufferStorageEXT(GL.GL_RENDERBUFFER_EXT, GL.GL_DEPTH_COMPONENT, _width, _height);
        }

        if (_active == 0) {
            gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                    _backgroundColor.getAlpha());
            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, _fboID);
            ContextManager.getCurrentContext().pushEnforcedStates();
            ContextManager.getCurrentContext().clearEnforcedStates();
            ContextManager.getCurrentContext().enforceStates(_enforcedStates);
        }
        _active++;
    }

    @Override
    protected void deactivate() {
        final GL gl = GLU.getCurrentGL();

        if (_active == 1) {
            final ReadOnlyColorRGBA bgColor = _parentRenderer.getBackgroundColor();
            gl.glClearColor(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha());
            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
            ContextManager.getCurrentContext().popEnforcedStates();
        }
        _active--;
    }

    public void cleanup() {
        final GL gl = GLU.getCurrentGL();

        if (_fboID > 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_fboID);
            id.rewind();
            gl.glDeleteFramebuffersEXT(id.limit(), id);
        }

        if (_depthRBID > 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_depthRBID);
            id.rewind();
            gl.glDeleteRenderbuffersEXT(id.limit(), id);
        }
    }
}
