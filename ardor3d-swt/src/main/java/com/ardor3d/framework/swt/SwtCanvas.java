/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.swt;

import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;

/**
 * A canvas for embedding into SWT applications.
 */
public class SwtCanvas extends GLCanvas implements Canvas {
    private CanvasRenderer canvasRenderer;
    private boolean inited = false;

    public SwtCanvas(final Composite composite, final int style, final GLData glData) {
        super(composite, style, glData);
        setCurrent();
    }

    public void setCanvasRenderer(final CanvasRenderer renderer) {
        canvasRenderer = renderer;
    }

    @MainThread
    private void privateInit() {
        // tell our parent to lay us out so we have the right starting size.
        getParent().layout();
        final Point size = getSize();

        setCurrent();

        final DisplaySettings settings = new DisplaySettings(Math.max(size.x, 1), Math.max(size.y, 1), 0, 0, 0, 8, 0,
                0, false, false);

        canvasRenderer.init(settings, false); // false - do not do back buffer swap, swt will do that.
        inited = true;
    }

    @MainThread
    public void init() {
        privateInit();
    }

    @MainThread
    public void draw(final CountDownLatch latch) {
        if (!inited) {
            privateInit();
        }

        if (!isDisposed() && isVisible()) {
            setCurrent();

            if (canvasRenderer.draw()) {
                swapBuffers();
            }
        }

        latch.countDown();
    }

    public CanvasRenderer getCanvasRenderer() {
        return canvasRenderer;
    }
}
