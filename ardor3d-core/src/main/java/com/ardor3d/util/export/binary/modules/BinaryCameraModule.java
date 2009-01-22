/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.binary.modules;

import com.ardor3d.renderer.Camera;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryLoaderModule;

public class BinaryCameraModule implements BinaryLoaderModule {
    public String getKey() {
        return Camera.class.getName();
    }

    public Savable load(final InputCapsule inputCapsule) {
        return new Camera(640, 480);
    }
}
