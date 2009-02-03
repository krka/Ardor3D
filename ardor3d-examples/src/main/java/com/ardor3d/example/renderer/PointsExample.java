/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import java.nio.FloatBuffer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.FrameWork;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.util.geom.BufferUtils;
import com.google.inject.Inject;

public class PointsExample extends ExampleBase {

    static final int POINTS = 25;

    public static void main(final String[] args) {
        start(PointsExample.class);
    }

    @Inject
    public PointsExample(final LogicalLayer layer, final FrameWork frameWork) {
        super(layer, frameWork);
    }

    /**
     * Set up some points...
     */
    @Override
    protected void initExample() {
        _lightState.setEnabled(false);

        final FloatBuffer pointData = BufferUtils.createFloatBuffer(POINTS * 3);
        for (int i = 0; i < POINTS; i++) {
            pointData.put((MathUtils.nextRandomFloat() * 12) - 6); // x
            pointData.put((MathUtils.nextRandomFloat() * 12) - 6); // y
            pointData.put((MathUtils.nextRandomFloat() * 10) - 15); // z
        }
        final Point pointsA = new Point("points", pointData, null, null, null);
        pointsA.setRandomColors();
        pointsA.setAntialiased(true);
        pointsA.setPointSize(4.25f);

        final BlendState bState = new BlendState();
        bState.setBlendEnabled(true);
        pointsA.setRenderState(bState);
        _root.attachChild(pointsA);
    }
}
