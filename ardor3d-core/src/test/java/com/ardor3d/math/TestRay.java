/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
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

import com.ardor3d.math.type.ReadOnlyRay3;

public class TestRay {
    @Test
    public void testintersectsTriangle() throws Exception {
        final Vector3 v0 = new Vector3(-1, -1, -1);
        final Vector3 v1 = new Vector3(+1, -1, -1);
        final Vector3 v2 = new Vector3(+1, +1, -1);

        final Vector3 intersectionPoint = new Vector3();

        // inside triangle
        Ray3 pickRay = new Ray3(new Vector3(0.5, -0.5, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // horizontal edge
        pickRay = new Ray3(new Vector3(0, -1, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // diagonal edge
        pickRay = new Ray3(new Vector3(0, 0, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // vertical edge
        pickRay = new Ray3(new Vector3(+1, 0, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // v0
        pickRay = new Ray3(new Vector3(-1, -1, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // v1
        pickRay = new Ray3(new Vector3(+1, -1, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // v2
        pickRay = new Ray3(new Vector3(1, 1, 3), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // outside horizontal edge
        pickRay = new Ray3(new Vector3(0, -1.1, 3), new Vector3(0, 0, -1));
        assertFalse(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // outside diagonal edge
        pickRay = new Ray3(new Vector3(-0.1, 0.1, 3), new Vector3(0, 0, -1));
        assertFalse(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // outside vertical edge
        pickRay = new Ray3(new Vector3(+1.1, 0, 3), new Vector3(0, 0, -1));
        assertFalse(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

        // inside triangle but ray pointing other way
        pickRay = new Ray3(new Vector3(-0.5, -0.5, 3), new Vector3(0, 0, +1));
        assertFalse(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));
    }

    @Test
    public void testIntersectsPlane() throws Exception {
        final Vector3 intersectionPoint = new Vector3();

        Plane plane = new Plane(new Vector3(0, 1, 0), 2);

        Ray3 pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, 0, 1));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, 1, 0));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 2, 0), new Vector3(0, 1, 0));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 1, 0), new Vector3(0, 1, 0));
        assertTrue(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 0, 0), new Vector3(1, 0, 0));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(0, 0, 1));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, -1, 0));
        assertTrue(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(1, 1, 1));
        assertTrue(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(-1, -1, -1));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        plane = new Plane(new Vector3(1, 1, 1), -2);

        pickRay = new Ray3(new Vector3(0, 0, 0), new Vector3(1, -1, 1));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -1, 0), new Vector3(0, 1, 0));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -2, 0), new Vector3(0, 1, 0));
        assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

        pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(0, 1, 0));
        assertTrue(pickRay.intersectsPlane(plane, intersectionPoint));
    }

    @Test
    public void testIntersectsQuad() throws Exception {
        final Vector3 v0 = new Vector3(0, 0, 0);
        final Vector3 v1 = new Vector3(5, 0, 0);
        final Vector3 v2 = new Vector3(5, 5, 0);
        final Vector3 v3 = new Vector3(0, 5, 0);

        final Vector3 intersectionPoint = null;

        // inside quad
        final ReadOnlyRay3 pickRay = new Ray3(new Vector3(2, 2, 10), new Vector3(0, 0, -1));
        assertTrue(pickRay.intersectsQuad(v0, v1, v2, v3, intersectionPoint));

        // inside quad
        final Ray3 pickRay2 = new Ray3(new Vector3(-1, 0, 10), new Vector3(0, 0, -1));
        assertFalse(pickRay2.intersectsQuad(v0, v1, v2, v3, intersectionPoint));
    }
}
