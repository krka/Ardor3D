/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.binary;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * BinaryLoaderModule defines two methods, the first provides a key value to look for to issue the load command. This
 * key is typically (and should be) the class name the loader is responsible for. While load handles creating a new
 * instance of the class.
 */
public interface BinaryLoaderModule {
    public String getKey();

    public Savable load(InputCapsule inputCapsule) throws IOException;
}
