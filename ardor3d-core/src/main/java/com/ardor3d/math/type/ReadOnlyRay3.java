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

public interface ReadOnlyRay3 extends ReadOnlyLine3Base {

    public boolean intersects(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB, final ReadOnlyVector3 pointC,
            final Vector3 locationStore, final boolean triangle);

    public boolean intersectsPlanar(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB,
            final ReadOnlyVector3 pointC, final Vector3 locationStore, final boolean triangle);

    public boolean intersects(final ReadOnlyPlane plane, final Vector3 locationStore);

}
