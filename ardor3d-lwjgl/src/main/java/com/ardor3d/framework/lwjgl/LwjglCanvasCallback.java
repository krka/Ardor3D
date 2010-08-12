/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.lwjgl;

import org.lwjgl.LWJGLException;

public interface LwjglCanvasCallback {

    /**
     * Request this canvas as the current opengl owner.
     * 
     * @throws LWJGLException
     */
    void makeCurrent() throws LWJGLException;

    /**
     * Release this canvas as the current opengl owner.
     * 
     * @throws LWJGLException
     */
    void releaseContext() throws LWJGLException;

}
