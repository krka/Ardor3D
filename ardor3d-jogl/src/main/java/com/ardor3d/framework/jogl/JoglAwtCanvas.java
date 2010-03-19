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

import java.util.concurrent.CountDownLatch;

import javax.media.opengl.DebugGL;
import javax.media.opengl.GLCanvas;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;

public class JoglAwtCanvas extends GLCanvas implements Canvas {

    private static final long serialVersionUID = 1L;

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false, _debugSet = false;
    private final DisplaySettings _settings;

    private final boolean _useDebug;

    public JoglAwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer) {
        this(settings, canvasRenderer, false);
    }

    public JoglAwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer, final boolean useDebug) {
        super(CapsUtil.getCapsForSettings(settings));
        _settings = settings;
        _canvasRenderer = canvasRenderer;
        _useDebug = useDebug;

        setFocusable(true);
        requestFocus();
        setSize(_settings.getWidth(), _settings.getHeight());
        setIgnoreRepaint(true);
        setAutoSwapBufferMode(false);

        _canvasRenderer.setContext(getContext());
    }

    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        _canvasRenderer.init(_settings, true); // true - do swap in renderer.
        _inited = true;
    }

    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            init();
        }

        if (_useDebug && !_debugSet) {
            // XXX: there might be a nicer place to put this. Constructor does not work though. Init only works if
            // called via draw.
            setGL(new DebugGL(getGL()));
            _debugSet = true;
        }

        if (isShowing()) {
            _canvasRenderer.draw();
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    public JoglCanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }
}
