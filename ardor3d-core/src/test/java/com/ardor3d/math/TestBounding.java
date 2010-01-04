/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.OrientedBoundingBox;
import com.ardor3d.intersection.IntersectionRecord;

public class TestBounding {
    @Test
    public void testRayAABBIntersection() throws Exception {
        final BoundingBox obb = new BoundingBox();
        obb.setCenter(Vector3.ZERO);
        obb.setXExtent(1);
        obb.setYExtent(1);
        obb.setZExtent(1);

        Ray3 ray = new Ray3(new Vector3(2, -10, 0), Vector3.UNIT_Y);
        assertFalse(obb.intersects(ray));
        IntersectionRecord record = obb.intersectsWhere(ray);
        assertEquals(0, record.getNumberOfIntersections());

        final Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(MathUtils.QUARTER_PI, Vector3.UNIT_Z);
        final Transform transform = new Transform();
        transform.setRotation(rotation);
        obb.transform(transform, obb);

        ray = new Ray3(new Vector3(1, -10, 0), Vector3.UNIT_Y);
        assertTrue(obb.intersects(ray));
        record = obb.intersectsWhere(ray);
        assertEquals(2, record.getNumberOfIntersections());
    }

    @Test
    public void testRayOBBIntersection() throws Exception {
        final OrientedBoundingBox obb = new OrientedBoundingBox();
        obb.setCenter(Vector3.ZERO);
        obb.setExtent(Vector3.ONE);

        Ray3 ray = new Ray3(new Vector3(1.2, -10, 0), Vector3.UNIT_Y);
        assertFalse(obb.intersects(ray));
        IntersectionRecord record = obb.intersectsWhere(ray);
        assertEquals(0, record.getNumberOfIntersections());

        final Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(MathUtils.QUARTER_PI, Vector3.UNIT_Z);
        final Transform transform = new Transform();
        transform.setRotation(rotation);
        obb.transform(transform, obb);

        ray = new Ray3(new Vector3(1.2, -10, 0), Vector3.UNIT_Y);
        assertTrue(obb.intersects(ray));
        record = obb.intersectsWhere(ray);
        assertEquals(2, record.getNumberOfIntersections());
    }
}
