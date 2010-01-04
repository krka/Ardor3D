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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestRay {
    @Test
    public void testIntersects() throws Exception {
        final Vector3 v0 = new Vector3(-1, -1, -1);
        final Vector3 v1 = new Vector3(+1, -1, -1);
        final Vector3 v2 = new Vector3(+1, +1, -1);

        final Vector3 intersectionPoint = new Vector3();
        final boolean testTriangle = true;

        // inside triangle
        Ray3 pickRay = new Ray3(new Vector3(0.5, -0.5, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // horizontal edge
        pickRay = new Ray3(new Vector3(0, -1, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // diagonal edge
        pickRay = new Ray3(new Vector3(0, 0, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // vertical edge
        pickRay = new Ray3(new Vector3(+1, 0, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // v0
        pickRay = new Ray3(new Vector3(-1, -1, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // v1
        pickRay = new Ray3(new Vector3(+1, -1, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // v2
        pickRay = new Ray3(new Vector3(1, 1, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // outside horizontal edge
        pickRay = new Ray3(new Vector3(0, -1.1, 3), new Vector3(0, 0, -1));
        assertFalse(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // outside diagonal edge
        pickRay = new Ray3(new Vector3(-0.1, 0.1, 3), new Vector3(0, 0, -1));
        assertFalse(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // outside vertical edge
        pickRay = new Ray3(new Vector3(+1.1, 0, 3), new Vector3(0, 0, -1));
        assertFalse(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));

        // inside triangle but ray pointing other way
        pickRay = new Ray3(new Vector3(-0.5, -0.5, 3), new Vector3(0, 0, +1));
        assertFalse(pickRay.intersects(v0, v1, v2, intersectionPoint, testTriangle));
    }

    @Test
    public void testIntersectsPlane() throws Exception {
        final Vector3 intersectionPoint = new Vector3();

        Plane plane = new Plane(new Vector3(0, 1, 0), 2);

        Ray3 pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, 0, 1));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, 1, 0));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 2, 0), new Vector3(0, 1, 0));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 1, 0), new Vector3(0, 1, 0));
        assertTrue(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 0, 0), new Vector3(1, 0, 0));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(0, 0, 1));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, -1, 0));
        assertTrue(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(1, 1, 1));
        assertTrue(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(-1, -1, -1));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        plane = new Plane(new Vector3(1, 1, 1), -2);

        pickRay = new Ray3(new Vector3(0, 0, 0), new Vector3(1, -1, 1));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -1, 0), new Vector3(0, 1, 0));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -2, 0), new Vector3(0, 1, 0));
        assertFalse(pickRay.intersects(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(0, 1, 0));
        assertTrue(pickRay.intersects(plane, intersectionPoint));
    }
}
