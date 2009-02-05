/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.awt;

import java.util.concurrent.CountDownLatch;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;

public class AwtCanvas extends AWTGLCanvas implements Canvas {

    private static final long serialVersionUID = 1L;

    private CanvasRenderer _canvasRenderer;
    private boolean _inited = false;

    private volatile boolean _updated = false;
    private CountDownLatch _latch = null; // this would have to be volatile if we are not careful with the order of
                                          // reads

    // and writes between this one and '_updated'

    // TODO: get rid of exception and ensure correct pixel format according to DisplaySystem
    public AwtCanvas() throws LWJGLException {
        super();
    }

    public void setCanvasRenderer(final CanvasRenderer canvasRenderer) {
        _canvasRenderer = canvasRenderer;
    }

    public void init() {
        privateInit();
    }

    public void draw(final CountDownLatch latch) {
        if (_updated) {
            throw new IllegalStateException("'_updated' should be false when draw() is called");
        }

        // need to set _latch before _updated, for memory consistency reasons
        _latch = latch;
        _updated = true;
        repaint();
    }

    @Override
    @MainThread
    protected void paintGL() {
        if (!_inited) {
            privateInit();
        }

        if (!_updated) {
            return;
        }

        if (_canvasRenderer.draw()) {
            try {
                swapBuffers();
            } catch (final LWJGLException e) {
                throw new RuntimeException(e);
            }
        }

        _updated = false;
        _latch.countDown();
    }

    private void privateInit() {
        final DisplaySettings settings = new DisplaySettings(getWidth(), getHeight(), 0, 0, 0, 8, 0, 0, false, false);

        _canvasRenderer.init(settings, false); // false - do not do back buffer swap, awt will do that.
        _inited = true;
    }

    public CanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }
}
