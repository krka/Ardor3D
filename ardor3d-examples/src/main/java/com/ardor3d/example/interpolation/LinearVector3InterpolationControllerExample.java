/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.interpolation;

import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.controller.interpolation.LinearVector3InterpolationController;
import com.ardor3d.scenegraph.controller.interpolation.Vector3InterpolationController.UpdateField;
import com.google.inject.Inject;

/**
 * A simple example showing the LinearVector3InterpolationControllerExample in action.
 */
public class LinearVector3InterpolationControllerExample extends
        InterpolationControllerBase<LinearVector3InterpolationController> {

    public static void main(final String[] args) {
        start(LinearVector3InterpolationControllerExample.class);
    }

    @Inject
    public LinearVector3InterpolationControllerExample(final LogicalLayer logicalLayer, final FrameHandler frameWork) {
        super(logicalLayer, frameWork);
    }

    @Override
    protected LinearVector3InterpolationController createController() {
        // Create our control point vectors
        final Vector3[] vectors = { new Vector3(15, 0, -10), //
                new Vector3(35, -10, -20), //
                new Vector3(-5, 0, -30), //
                new Vector3(-15, 20, -40), //
                new Vector3(0, 0, 20), //
                new Vector3(20, 30, -80), //
                new Vector3(15, 0, -10) };

        // Create a line from our vectors
        final Line line = new Line("line", vectors, null, null, null);
        line.getMeshData().setIndexMode(IndexMode.LineStrip);
        _root.attachChild(line);

        // Create some points from our vectors
        final Point point = new Point("point", vectors, null, null, null);
        point.setPointSize(10f);
        _root.attachChild(point);

        // Create our controller
        final LinearVector3InterpolationController controller = new LinearVector3InterpolationController();
        controller.setControls(vectors);
        controller.setActive(true);
        controller.setUpdateField(UpdateField.LOCAL_TRANSLATION);

        return controller;
    }
}
