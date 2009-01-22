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

import com.ardor3d.annotation.MainThread;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;

/**
 * Represents a class that knows how to render a scene using a specific Open GL implementation.
 */
public interface CanvasRenderer {
    public void init(DisplaySettings settings, boolean doSwap);

    /**
     * Draw the current state of the scene.
     */
    @MainThread
    public boolean draw();

    /**
     * Returns the camera being used by this canvas renderer. Modifying the returned {@link Camera} instance effects the
     * view being rendered, so this method can be used to move the camera, etc.
     * 
     * @return the camera used by this canvas renderer
     */
    public Camera getCamera();

    /**
     * Returns the scene being used by this canvas renderer.
     * 
     * @return the camera used by this canvas renderer
     */
    public Scene getScene();

    /**
     * Returns the renderer being used by this canvas renderer.
     * 
     * @return the renderer used by this canvas renderer
     */
    Renderer getRenderer();

    /**
     * Have the CanvasRenderer claim the graphics context.
     */
    public void setCurrentContext();
}
