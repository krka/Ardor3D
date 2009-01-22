/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.screen;

import java.nio.ByteBuffer;

import com.ardor3d.image.Image;

public interface ScreenExportable {

    /**
     * Export the image data we are given in a manner of our choosing. Note that this data object should be treated as
     * immutable and temporary. If you need access to it after returning from the method, make a copy.
     * 
     * @param data
     *            the data from the screen. Please respect the data's limit() value.
     * @param width
     * @param height
     */
    public void export(ByteBuffer data, int width, int height);

    /**
     * 
     * @return the image format we care about using on the next export.
     */
    public Image.Format getFormat();

}
