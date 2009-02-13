/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.glu.GLU;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.nativeloader.NativeLoader;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.jogl.JoglContextCapabilities;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.google.inject.Inject;

public class JoglCanvasRenderer implements CanvasRenderer {

    /**
     * Set to true to be safe when rendering in multiple canvases. Set to false for a faster, single canvas mode.
     */
    public static boolean MULTI_CANVAS_MODE = true;

    private static final Logger logger = Logger.getLogger(JoglCanvasRenderer.class.getName());

    // ensure availability of JOGL natives
    {
        final String[] libraryPaths = JoglLibraryPaths.getLibraryPaths(System.getProperty("os.name"), System
                .getProperty("os.arch"));

        try {
            NativeLoader.makeLibrariesAvailable(libraryPaths);
        } catch (final Exception e) {
            ; // ignore
        }
    }

    protected final Scene _scene;
    protected Camera _camera;
    protected boolean _doSwap;
    protected GLContext _context;
    protected JoglRenderer _renderer;

    @Inject
    public JoglCanvasRenderer(final Scene scene) {
        _scene = scene;
    }

    public void setCurrentContext() {
        _context.makeCurrent();
    }

    @MainThread
    public void init(final DisplaySettings settings, final boolean doSwap) {
        _doSwap = doSwap;
        if (_context == null) {
            _context = GLDrawableFactory.getFactory().createExternalGLContext();
        }
        setCurrentContext();

        final JoglContextCapabilities caps = new JoglContextCapabilities(_context.getGL());
        final RenderContext currentContext = new RenderContext(_context, caps);

        ContextManager.addContext(_context, currentContext);

        _renderer = new JoglRenderer();

        if (settings.getSamples() != 0 && caps.isMultisampleSupported()) {
            final GL gl = GLU.getCurrentGL();
            gl.glEnable(GL.GL_MULTISAMPLE);
        }

        _renderer.setBackgroundColor(ColorRGBA.BLACK);

        /** Set up how our camera sees. */
        _camera = new Camera(settings.getWidth(), settings.getHeight());
        _camera.setFrustumPerspective(45.0f, (float) settings.getWidth() / (float) settings.getHeight(), 1, 1000);
        _camera.setParallelProjection(false);

        final Vector3 loc = new Vector3(0.0f, 0.0f, 10.0f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
        /** Move our camera to a correct place and orientation. */
        _camera.setFrame(loc, left, up, dir);
    }

    public GLContext getContext() {
        return _context;
    }

    public void setContext(final GLContext context) {
        _context = context;
    }

    @MainThread
    public boolean draw() {

        // set up context for rendering this canvas
        ContextManager.switchContext(_context);
        if (MULTI_CANVAS_MODE && !_context.equals(GLContext.getCurrent())) {
            while (_context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
                try {
                    logger.info("Waiting for the GLContext to initialize...");
                    Thread.sleep(500);
                } catch (final InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        // render stuff
        if (ContextManager.getCurrentContext().getCurrentCamera() != _camera) {
            ContextManager.getCurrentContext().setCurrentCamera(_camera);
            _camera.update();
        }

        _camera.apply(_renderer);
        _renderer.clearBuffers();

        final boolean drew = _scene.renderUnto(_renderer);
        _renderer.flushFrame(_doSwap);
        if (MULTI_CANVAS_MODE) {
            _context.release();
        }
        return drew;
    }

    public Camera getCamera() {
        return _camera;
    }

    public Scene getScene() {
        return _scene;
    }

    public void cleanup() {
        _renderer.cleanup();
    }

    public Renderer getRenderer() {
        return _renderer;
    }
}
