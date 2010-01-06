/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.type;

public interface ReadOnlyRectangle2 {

    /**
     * @return the x coordinate of the origin of this rectangle.
     */
    public int getX();

    /**
     * @return the y coordinate of the origin of this rectangle.
     */
    public int getY();

    /**
     * @return the width of this rectangle.
     */
    public int getWidth();

    /**
     * @return the height of this rectangle.
     */
    public int getHeight();
}
