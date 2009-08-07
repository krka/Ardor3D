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

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.controllers.interpolation.CurveInterpolationController;
import com.ardor3d.scenegraph.controllers.interpolation.Vector3InterpolationController.UpdateField;
import com.ardor3d.spline.CatmullRomSpline;
import com.ardor3d.spline.Curve;
import com.google.inject.Inject;

/**
 * A simple example showing the CurveInterpolationController in action.
 */
public class CurveInterpolationControllerExample extends InterpolationControllerBase<CurveInterpolationController> {

    public static void main(final String[] args) {
        start(CurveInterpolationControllerExample.class);
    }

    @Inject
    public CurveInterpolationControllerExample(final LogicalLayer logicalLayer, final FrameHandler frameWork) {
        super(logicalLayer, frameWork);
    }

    @Override
    protected CurveInterpolationController createController() {
        // Create our control point vectors
        final Vector3[] vectors = { new Vector3(15, 0, -10), //
                new Vector3(35, -10, -20), //
                new Vector3(-5, 0, -30), //
                new Vector3(-15, 20, -40), //
                new Vector3(0, 0, 20), //
                new Vector3(20, 30, -80), //
                new Vector3(15, 0, -10) };

        final List<ReadOnlyVector3> controls = new ArrayList<ReadOnlyVector3>(vectors.length);
        for (final Vector3 v : vectors) {
            controls.add(v);
        }

        // Create our curve from the control points and a spline
        final Curve curve = new Curve(controls, new CatmullRomSpline());

        // Create a line from the curve so its easy to check the box is following it
        _root.attachChild(curve.toRenderableLine(50));

        // Create points from the curve so the actual control points can be easily seen
        final Point point = curve.toRenderablePoint(true);
        point.setPointSize(10f);

        _root.attachChild(point);

        // Create our controller
        final CurveInterpolationController controller = new CurveInterpolationController();
        controller.setCurve(curve);
        controller.setActive(true);
        controller.setMaxTime(5);
        controller.setUpdateField(UpdateField.LOCAL_TRANSLATION);

        return controller;
    }
}
