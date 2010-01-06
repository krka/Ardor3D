/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.texturing;

import java.nio.ByteBuffer;

public interface TextureStreamer {

    void updateLevel(int unit, final int sX, final int sY);

    boolean isReady(int unit);

    void copyImage(int unit, ByteBuffer sliceData, final int dX, final int dY, final int sX, final int sY, final int w,
            final int h);

}