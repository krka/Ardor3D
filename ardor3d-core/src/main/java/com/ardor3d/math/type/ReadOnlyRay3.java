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

import com.ardor3d.math.Vector3;

public interface ReadOnlyRay3 extends ReadOnlyLine3Base {

    boolean intersects(ReadOnlyVector3 pointA, ReadOnlyVector3 pointB, ReadOnlyVector3 pointC, Vector3 locationStore,
            boolean triangle);

    boolean intersectsPlanar(ReadOnlyVector3 pointA, ReadOnlyVector3 pointB, ReadOnlyVector3 pointC,
            Vector3 locationStore, boolean triangle);

    boolean intersects(ReadOnlyPlane plane, Vector3 locationStore);

}
