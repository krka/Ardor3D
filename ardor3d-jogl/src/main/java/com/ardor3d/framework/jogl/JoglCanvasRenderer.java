/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.util.logging.Logger;

import javax.media.opengl.DebugGL;
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
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.jogl.JoglContextCapabilities;
import com.ardor3d.renderer.jogl.JoglRenderer;

public class JoglCanvasRenderer implements CanvasRenderer {

    private static final Logger LOGGER = Logger.getLogger(JoglCanvasRenderer.class.getName());


    /**
     * Set to true to be safe when rendering in multiple canvases. Set to false for a faster, single canvas mode.
     */
    public static boolean MULTI_CANVAS_MODE = true;

    // NOTE: This code commented out by Petter 090224, since it isn't really ready to be used,
    // and since it is at the moment more work than it is worth to get it ready. Later on, when
    // we have solved some more fundamental problems, it is probably time to revisit this.

    // ensure availability of JOGL natives
    // {
    // final String[] libraryPaths = JoglLibraryPaths.getLibraryPaths(System.getProperty("os.name"), System
    // .getProperty("os.arch"));
    //
    // try {
    // NativeLoader.makeLibrariesAvailable(libraryPaths);
    // } catch (final Exception e) {
    // ; // ignore
    // }
    // }

    protected Scene _scene;
    protected Camera _camera;
    protected boolean _doSwap;
    protected GLContext _context;
    protected JoglRenderer _renderer;

    private RenderContext _currentContext;

    /**
     * <code>true</code> if debugging (checking for error codes on each GL call)
     * is desired.
     */
    private boolean _useDebug;
    
    /**
     * <code>true</code> if debugging is currently enabled for this GLContext.
     */
    private boolean _debugEnabled = false;
    
    public JoglCanvasRenderer(final Scene scene) {
        this(scene, false);
    }

    public JoglCanvasRenderer(final Scene scene, final boolean useDebug) {
        _scene = scene;
        _useDebug = useDebug;
    }

    public void setCurrentContext() {
        _context.makeCurrent();
    }

    public void releaseCurrentContext() {
        _context.release();
    }

    @MainThread
    public void init(final DisplaySettings settings, final boolean doSwap) {
        _doSwap = doSwap;
        if (_context == null) {
            _context = GLDrawableFactory.getFactory().createExternalGLContext();
        }

        setCurrentContext();

        // Look up a shared context, if a shared JoglCanvasRenderer is given.
        RenderContext sharedContext = null;
        if (settings.getShareContext() != null) {
            sharedContext = ContextManager.getContextForKey(settings.getShareContext().getRenderContext()
                    .getContextKey());
        }

        final JoglContextCapabilities caps = new JoglContextCapabilities(_context.getGL());
        _currentContext = new RenderContext(_context, caps, sharedContext);

        ContextManager.addContext(_context, _currentContext);

        _renderer = new JoglRenderer();

        if (settings.getSamples() != 0 && caps.isMultisampleSupported()) {
            final GL gl = GLU.getCurrentGL();
            gl.glEnable(GL.GL_MULTISAMPLE);
        }

        _renderer.setBackgroundColor(ColorRGBA.BLACK);

        if (_camera == null) {
            /** Set up how our camera sees. */
            _camera = new Camera(settings.getWidth(), settings.getHeight());
            _camera.setFrustumPerspective(45.0f, (float) settings.getWidth() / (float) settings.getHeight(), 1, 1000);
            _camera.setProjectionMode(ProjectionMode.Perspective);

            final Vector3 loc = new Vector3(0.0f, 0.0f, 10.0f);
            final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
            final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
            final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
            /** Move our camera to a correct place and orientation. */
            _camera.setFrame(loc, left, up, dir);
        } else {
            // use new width and height to set ratio.
            _camera.setFrustumPerspective(_camera.getFovY(),
                    (float) settings.getWidth() / (float) settings.getHeight(), _camera.getFrustumNear(), _camera
                            .getFrustumFar());
        }
    }

    public GLContext getContext() {
        return _context;
    }

    public void setContext(final GLContext context) {
        _context = context;
    }

    public int MAX_CONTEXT_GRAB_ATTEMPTS = 10;

    @MainThread
    public boolean draw() {

        // set up context for rendering this canvas
        ContextManager.switchContext(_context);
        if (MULTI_CANVAS_MODE && !_context.equals(GLContext.getCurrent())) {
            int value;
            int attempt = 1;
            while ((value = _context.makeCurrent()) == GLContext.CONTEXT_NOT_CURRENT) {
                if (attempt == MAX_CONTEXT_GRAB_ATTEMPTS) {
                    // failed, return false to avoid back buffer swap.
                    return false;
                }
                attempt++;
                try {
                    Thread.sleep(5);
                } catch (final InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            if (value == GLContext.CONTEXT_CURRENT_NEW) {
                ContextManager.getCurrentContext().contextLost();
                
                // Whenever the context is created or replaced, the GL chain
                // is lost.  Debug will have to be added if desired.
                _debugEnabled = false;
            }
        }

        // Enable Debugging if requested.
        if (_useDebug != _debugEnabled) {
            _context.setGL(new DebugGL(_context.getGL()));
            _debugEnabled = true;
            
            LOGGER.info("DebugGL Enabled");
        }

        // render stuff, first apply our camera if we have one
        if (_camera != null) {
            if (Camera.getCurrentCamera() != _camera) {
                _camera.update();
            }
            _camera.apply(_renderer);
        }
        _renderer.clearBuffers(Renderer.BUFFER_COLOR_AND_DEPTH);

        final boolean drew = _scene.renderUnto(_renderer);
        _renderer.flushFrame(drew && _doSwap);
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

    public void setScene(final Scene scene) {
        _scene = scene;
    }

    public Renderer getRenderer() {
        return _renderer;
    }

    public void setCamera(final Camera camera) {
        _camera = camera;
    }

    public RenderContext getRenderContext() {
        return _currentContext;
    }
}
