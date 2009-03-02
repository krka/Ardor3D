/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import com.ardor3d.math.Vector2;

/**
 * Defines the contract for managing the native mouse. 
 */
public interface MouseManager {
    /**
     * Change the mouse cursor presently used. This is a mandatory operation that all implementing classes must
     * support.
     *
     * @param cursor the cursor to use
     */
    public void setCursor(MouseCursor cursor);


    /**
     * Optional method that
     * @param position
     */
    public void setPosition(Vector2 position);
    public void setGrabbed(GrabbedState grabbedState);

//    public boolean isSetPositionSupported();
//    public boolean isSetGrabbedSupported();
}
