/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

import java.util.concurrent.CountDownLatch;

import com.ardor3d.annotation.MainThread;

/**
 * This interface defines the View, and should maybe be called the ViewUpdater. It owns the rendering phase, and
 * controls all interactions with the Renderer (can it?). In the current implementation, which should maybe be modified,
 * it also owns the camera and the display. That could be great, or it could be something that should be separated out a
 * bit more nicely.
 */
public interface Canvas {

    @MainThread
    public void init();

    @MainThread
    public void draw(CountDownLatch latch);

    CanvasRenderer getCanvasRenderer();
}
