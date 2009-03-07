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

import java.nio.IntBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.RenderTexture;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.renderer.AbstractPbufferTextureRenderer;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.scene.state.lwjgl.LwjglTextureStateUtil;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

/**
 * This class is used by LWJGL to render textures. Users should <b>not </b> create this class directly. Instead, allow
 * DisplaySystem to create it for you.
 * 
 * @see com.ardor3d.system.DisplaySystem#createTextureRenderer
 */
public class LwjglPbufferTextureRenderer extends AbstractPbufferTextureRenderer {
    private static final Logger logger = Logger.getLogger(LwjglPbufferTextureRenderer.class.getName());

    /* Pbuffer instance */
    private Pbuffer _pbuffer;

    final int _caps;

    private RenderTexture _texture;

    public LwjglPbufferTextureRenderer(final DisplaySettings settings, final TextureRenderer.Target target,
            final Renderer parentRenderer) {
        super(settings, target, parentRenderer);

        _caps = Pbuffer.getCapabilities();

        int pTarget = RenderTexture.RENDER_TEXTURE_2D;

        // Support this?
        // pTarget = RenderTexture.RENDER_TEXTURE_RECTANGLE;

        switch (target) {
            case Texture1D:
                pTarget = RenderTexture.RENDER_TEXTURE_1D;
                break;
            case TextureCubeMap:
                pTarget = RenderTexture.RENDER_TEXTURE_CUBE_MAP;
                break;
        }

        // boolean useRGB, boolean useRGBA, boolean useDepth, boolean isRectangle, int target, int mipmaps
        _texture = new RenderTexture(false, true, true, pTarget == RenderTexture.RENDER_TEXTURE_RECTANGLE, pTarget, 0);

        setMultipleTargets(true);
    }

    /**
     * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer. Generates a valid gl
     * texture id for this texture and inits the data type for the texture.
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

        final int source = getSourceFromRTTType(tex);
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, source, 0, 0, _width, _height, 0);
        logger.fine("setup tex" + tex.getTextureId() + ": " + _width + "," + _height);
    }

    public void render(final Spatial spat, final Texture tex, final boolean doClear) {
        render(null, spat, tex, doClear);
    }

    public void render(final List<? extends Spatial> spat, final Texture tex, final boolean doClear) {
        render(spat, null, tex, doClear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final Texture tex,
            final boolean doClear) {
        // clear the current states since we are rendering into a new location
        // and can not rely on states still being set.
        try {
            if (_pbuffer == null || _pbuffer.isBufferLost()) {
                if (_pbuffer != null && _pbuffer.isBufferLost()) {
                    logger.warning("PBuffer contents lost - will recreate the buffer");
                    deactivate();
                    _pbuffer.destroy();
                }
                initPbuffer();
            }

            if (_useDirectRender && tex.getRTTSource() != Texture.RenderToTextureType.Depth) {
                // setup and render directly to a 2d texture.
                _pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);
                activate();
                switchCameraIn(doClear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                deactivate();
                switchCameraOut();
                LwjglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);
                _pbuffer.bindTexImage(Pbuffer.FRONT_LEFT_BUFFER);
            } else {
                // render and copy to a texture
                activate();
                switchCameraIn(doClear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                copyToTexture(tex, _width, _height);

                deactivate();
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        }
    }

    public void render(final Spatial spat, final List<Texture> texs, final boolean doClear) {
        render(null, spat, texs, doClear);
    }

    public void render(final List<? extends Spatial> spat, final List<Texture> texs, final boolean doClear) {
        render(spat, null, texs, doClear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final List<Texture> texs,
            final boolean doClear) {
        // clear the current states since we are rendering into a new location
        // and can not rely on states still being set.
        try {
            if (_pbuffer == null || _pbuffer.isBufferLost()) {
                if (_pbuffer != null && _pbuffer.isBufferLost()) {
                    logger.warning("PBuffer contents lost - will recreate the buffer");
                    deactivate();
                    _pbuffer.destroy();
                }
                initPbuffer();
            }

            if (texs.size() == 1 && _useDirectRender && texs.get(0).getRTTSource() != Texture.RenderToTextureType.Depth) {
                // setup and render directly to a 2d texture.
                LwjglTextureStateUtil.doTextureBind(texs.get(0).getTextureId(), 0, Texture.Type.TwoDimensional);
                activate();
                switchCameraIn(doClear);
                _pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                deactivate();
                _pbuffer.bindTexImage(Pbuffer.FRONT_LEFT_BUFFER);
            } else {
                // render and copy to a texture
                activate();
                switchCameraIn(doClear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                for (int i = 0; i < texs.size(); i++) {
                    copyToTexture(texs.get(i), _width, _height);
                }

                deactivate();
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        }
    }

    public void copyToTexture(final Texture tex, final int width, final int height) {
        LwjglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);

        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
    }

    private int getSourceFromRTTType(final Texture tex) {
        int source = GL11.GL_RGBA;
        switch (tex.getRTTSource()) {
            case RGBA:
            case RGBA2:
            case RGBA4:
            case RGB5_A1:
            case RGBA8:
            case RGB10_A2:
            case RGBA12:
            case RGBA16:
            case RGBA16F:
            case RGBA32F:
                break;
            case RGB:
            case R3_G3_B2:
            case RGB4:
            case RGB5:
            case RGB8:
            case RGB10:
            case RGB12:
            case RGB16:
            case RGB16F:
            case RGB32F:
                source = GL11.GL_RGB;
                break;
            case Alpha:
            case Alpha4:
            case Alpha8:
            case Alpha12:
            case Alpha16:
            case Alpha16F:
            case Alpha32F:
                source = GL11.GL_ALPHA;
                break;
            case Depth:
                source = GL11.GL_DEPTH_COMPONENT;
                break;
            case Intensity:
            case Intensity4:
            case Intensity8:
            case Intensity12:
            case Intensity16:
            case Intensity16F:
            case Intensity32F:
                source = GL11.GL_INTENSITY;
                break;
            case Luminance:
            case Luminance4:
            case Luminance8:
            case Luminance12:
            case Luminance16:
            case Luminance16F:
            case Luminance32F:
                source = GL11.GL_LUMINANCE;
                break;
            case LuminanceAlpha:
            case Luminance4Alpha4:
            case Luminance8Alpha8:
            case Luminance6Alpha2:
            case Luminance12Alpha4:
            case Luminance12Alpha12:
            case Luminance16Alpha16:
            case LuminanceAlpha32F:
            case LuminanceAlpha16F:
                source = GL11.GL_LUMINANCE_ALPHA;
                break;
        }
        return source;
    }

    @Override
    protected void clearBuffers() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers();
    }

    private void initPbuffer() {

        try {
            if (_pbuffer != null) {
                giveBackContext();
                ContextManager.removeContext(_pbuffer);
            }
            final PixelFormat format = new PixelFormat(_settings.getAlphaBits(), _settings.getDepthBits(), _settings
                    .getStencilBits()).withSamples(_settings.getSamples()).withBitsPerPixel(_settings.getColorDepth())
                    .withStereo(_settings.isStereo());
            _pbuffer = new Pbuffer(_width, _height, format, _texture, null);
            final Object contextKey = _pbuffer;
            try {
                _pbuffer.makeCurrent();
            } catch (final LWJGLException e) {
                throw new RuntimeException(e);
            }

            final LwjglContextCapabilities caps = new LwjglContextCapabilities(GLContext.getCapabilities());
            ContextManager.addContext(contextKey, new RenderContext(contextKey, caps, ContextManager
                    .getCurrentContext()));

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "initPbuffer()", "Exception", e);

            if (_texture != null && _useDirectRender) {
                logger
                        .warning("Your card claims to support Render to Texture but fails to enact it.  Updating your driver might solve this problem.");
                logger.warning("Attempting to fall back to Copy Texture.");
                _texture = null;
                _useDirectRender = false;
                initPbuffer();
                return;
            }

            logger.log(Level.WARNING, "Failed to create Pbuffer.", e);
            return;
        }

        try {
            activate();

            _width = _pbuffer.getWidth();
            _height = _pbuffer.getHeight();

            deactivate();
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Failed to initialize created Pbuffer.", e);
            return;
        }
    }

    private void activate() {
        if (_active == 0) {
            try {
                _oldContext = ContextManager.getCurrentContext();
                _pbuffer.makeCurrent();

                ContextManager.switchContext(_pbuffer);

                ContextManager.getCurrentContext().clearEnforcedStates();
                ContextManager.getCurrentContext().enforceStates(_enforcedStates);

                if (_bgColorDirty) {
                    GL11.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor
                            .getBlue(), _backgroundColor.getAlpha());
                    _bgColorDirty = false;
                }
            } catch (final LWJGLException e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "activate()", "Exception", e);
                throw new Ardor3dException();
            }
        }
        _active++;
    }

    private void deactivate() {
        if (_active == 1) {
            try {
                giveBackContext();
            } catch (final LWJGLException e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "deactivate()", "Exception", e);
                throw new Ardor3dException();
            }
        }
        _active--;
    }

    // XXX: Need another look at this to make it generic?
    private void giveBackContext() throws LWJGLException {
        if (Display.isCreated()) {
            Display.makeCurrent();
            ContextManager.switchContext(_oldContext.getContextHolder());
        } else if (_oldContext.getContextHolder() instanceof AWTGLCanvas) {
            ((AWTGLCanvas) _oldContext.getContextHolder()).makeCurrent();
            ContextManager.switchContext(_oldContext.getContextHolder());
        }
    }

    public void cleanup() {
        ContextManager.removeContext(_pbuffer);
        _pbuffer.destroy();
    }

    public void setMultipleTargets(final boolean force) {
        if (force) {
            logger.fine("Copy Texture Pbuffer used!");
            _useDirectRender = false;
            _texture = null;
            if (_pbuffer != null) {
                try {
                    giveBackContext();
                } catch (final LWJGLException ex) {
                }
                ContextManager.removeContext(_pbuffer);
            }
        } else {
            if ((_caps & Pbuffer.RENDER_TEXTURE_SUPPORTED) != 0) {
                logger.fine("Render to Texture Pbuffer supported!");
                if (_texture == null) {
                    logger.fine("No RenderTexture used in init, falling back to Copy Texture PBuffer.");
                    _useDirectRender = false;
                } else {
                    _useDirectRender = true;
                }
            } else {
                logger.fine("Copy Texture Pbuffer supported!");
                _texture = null;
            }
        }
    }
}
