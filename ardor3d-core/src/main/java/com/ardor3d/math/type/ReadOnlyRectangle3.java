/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.Vector3;

public interface ReadOnlyRectangle3 {
    public ReadOnlyVector3 getA();

    public ReadOnlyVector3 getB();

    public ReadOnlyVector3 getC();

    public Vector3 random(Vector3 result);
}
