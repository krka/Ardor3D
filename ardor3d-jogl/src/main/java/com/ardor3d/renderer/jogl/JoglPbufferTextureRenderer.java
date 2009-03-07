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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.glu.GLU;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.renderer.AbstractPbufferTextureRenderer;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.scene.state.jogl.JoglTextureStateUtil;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

public class JoglPbufferTextureRenderer extends AbstractPbufferTextureRenderer {
    private static final Logger logger = Logger.getLogger(JoglPbufferTextureRenderer.class.getName());

    /* Pbuffer instance */
    private GLPbuffer _pbuffer;

    private GLContext _context;

    // HACK: needed to get the parent context in here somehow...
    public static GLContext _parentContext;

    public JoglPbufferTextureRenderer(final DisplaySettings settings, final TextureRenderer.Target target,
            final Renderer parentRenderer) {
        super(settings, target, parentRenderer);
        setMultipleTargets(true);
    }

    /**
     * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer. Generates a valid gl
     * texture id for this texture and inits the data type for the texture.
     */
    public void setupTexture(final Texture2D tex) {
        final GL gl = GLU.getCurrentGL();

        final IntBuffer ibuf = BufferUtils.createIntBuffer(1);

        if (tex.getTextureId() != 0) {
            ibuf.put(tex.getTextureId());
            gl.glDeleteTextures(1, ibuf);
            ibuf.clear();
        }

        // Create the texture
        gl.glGenTextures(1, ibuf);
        tex.setTextureId(ibuf.get(0));
        TextureManager.registerForCleanup(tex.getTextureKey(), tex.getTextureId());
        JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);

        final int source = getSourceFromRTTType(tex);
        gl.glCopyTexImage2D(GL.GL_TEXTURE_2D, 0, source, 0, 0, _width, _height, 0);
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
            if (_pbuffer == null) {
                initPbuffer();
            }

            if (_useDirectRender && tex.getRTTSource() != Texture.RenderToTextureType.Depth) {
                // setup and render directly to a 2d texture.
                _pbuffer.releaseTexture();
                activate();
                switchCameraIn(doClear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                deactivate();
                switchCameraOut();
                JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);
                _pbuffer.bindTexture();
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
            if (_pbuffer == null) {
                initPbuffer();
            }

            if (texs.size() == 1 && _useDirectRender && texs.get(0).getRTTSource() != Texture.RenderToTextureType.Depth) {
                // setup and render directly to a 2d texture.
                JoglTextureStateUtil.doTextureBind(texs.get(0).getTextureId(), 0, Texture.Type.TwoDimensional);
                activate();
                switchCameraIn(doClear);
                _pbuffer.releaseTexture();

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                deactivate();
                _pbuffer.bindTexture();
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
        JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);

        final GL gl = GLU.getCurrentGL();

        gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
    }

    private int getSourceFromRTTType(final Texture tex) {
        int source = GL.GL_RGBA;
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
                source = GL.GL_RGB;
                break;
            case Alpha:
            case Alpha4:
            case Alpha8:
            case Alpha12:
            case Alpha16:
            case Alpha16F:
            case Alpha32F:
                source = GL.GL_ALPHA;
                break;
            case Depth:
                source = GL.GL_DEPTH_COMPONENT;
                break;
            case Intensity:
            case Intensity4:
            case Intensity8:
            case Intensity12:
            case Intensity16:
            case Intensity16F:
            case Intensity32F:
                source = GL.GL_INTENSITY;
                break;
            case Luminance:
            case Luminance4:
            case Luminance8:
            case Luminance12:
            case Luminance16:
            case Luminance16F:
            case Luminance32F:
                source = GL.GL_LUMINANCE;
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
                source = GL.GL_LUMINANCE_ALPHA;
                break;
        }
        return source;
    }

    @Override
    protected void clearBuffers() {
        final GL gl = GLU.getCurrentGL();

        gl.glDisable(GL.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers();
    }

    private void initPbuffer() {

        try {
            if (_pbuffer != null) {
                _context.destroy();
                _pbuffer.destroy();
                giveBackContext();
                ContextManager.removeContext(_pbuffer);
            }

            // Make our GLPbuffer...
            final GLDrawableFactory fac = GLDrawableFactory.getFactory();
            final GLCapabilities caps = new GLCapabilities();
            caps.setDoubleBuffered(false);
            _pbuffer = fac.createGLPbuffer(caps, null, _width, _height, _parentContext);
            _context = _pbuffer.getContext();

            _context.makeCurrent();

            final JoglContextCapabilities contextCaps = new JoglContextCapabilities(_pbuffer.getGL());
            ContextManager.addContext(_context, new RenderContext(_context, contextCaps, ContextManager
                    .getCurrentContext()));

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "initPbuffer()", "Exception", e);

            if (_useDirectRender) {
                logger
                        .warning("Your card claims to support Render to Texture but fails to enact it.  Updating your driver might solve this problem.");
                logger.warning("Attempting to fall back to Copy Texture.");
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
            _oldContext = ContextManager.getCurrentContext();
            _context.makeCurrent();

            ContextManager.switchContext(_context);

            ContextManager.getCurrentContext().clearEnforcedStates();
            ContextManager.getCurrentContext().enforceStates(_enforcedStates);

            if (_bgColorDirty) {
                final GL gl = GLU.getCurrentGL();

                gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                        _backgroundColor.getAlpha());
                _bgColorDirty = false;
            }
        }
        _active++;
    }

    private void deactivate() {
        if (_active == 1) {
            giveBackContext();
        }
        _active--;
    }

    private void giveBackContext() {
        _parentContext.makeCurrent();
        ContextManager.switchContext(_oldContext.getContextHolder());
    }

    public void cleanup() {
        ContextManager.removeContext(_pbuffer);
        _pbuffer.destroy();
    }

    public void setMultipleTargets(final boolean force) {
        if (force) {
            logger.fine("Copy Texture Pbuffer used!");
            _useDirectRender = false;
            if (_pbuffer != null) {
                giveBackContext();
                ContextManager.removeContext(_pbuffer);
            }
        } else {
            // XXX: Is this WGL specific query right?
            if (GLU.getCurrentGL().isExtensionAvailable("WGL_ARB_render_texture")) {
                logger.fine("Render to Texture Pbuffer supported!");
                _useDirectRender = true;
            } else {
                logger.fine("Copy Texture Pbuffer supported!");
            }
        }
    }
}
