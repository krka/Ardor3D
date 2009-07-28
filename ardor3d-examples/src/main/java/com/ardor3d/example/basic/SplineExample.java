/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

/**
 * 
 */

package com.ardor3d.example.basic;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.ComplexSpatialController.RepeatType;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.spline.CatmullRomSpline;
import com.ardor3d.spline.ControlPoint;
import com.ardor3d.spline.Curve;
import com.ardor3d.spline.CurveController;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.google.inject.Inject;

/**
 * Adapted from the BoxExample to demonstrate splines
 */
public class SplineExample extends ExampleBase {

    private Mesh box;
    private Point boxCentre;
    private CurveController curveController;

    @Inject
    public SplineExample(final LogicalLayer logicalLayer, final FrameHandler frameHandler) {
        super(logicalLayer, frameHandler);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Spline Example");

        // Create a new box centered at (0,0,0) with width/height/depth of size 5.
        box = new Box("Box", new Vector3(0, 0, 0), 2.5, 2.5, 2.5);
        // Set a bounding box for frustum culling.
        box.setModelBound(new BoundingBox());
        // Move the box out from the camera 15 units.
        box.setTranslation(new Vector3(0, 0, -15));
        // Give the box some nice colors.
        box.setRandomColors();
        // Attach the box to the scenegraph root.
        _root.attachChild(box);

        // Marks the centre of the box (you'll need to enter wireframe mode to see it!)
        boxCentre = new Point("box center", new Vector3[] { new Vector3() }, null, null, null);
        boxCentre.setPointSize(10f);

        _root.attachChild(boxCentre);

        // Add a texture to the box.
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.GuessNoCompression, true));
        box.setRenderState(ts);

        // Add a material to the box, to show both vertex color and lighting/shading.
        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        box.setRenderState(ms);

        // Create our control point vectors
        final Vector3 v0 = new Vector3(15, 0, -10);
        final Vector3 v1 = new Vector3(35, -10, -20);
        final Vector3 v2 = new Vector3(-5, 0, -30);
        final Vector3 v3 = new Vector3(-15, 20, -40);
        final Vector3 v4 = new Vector3(0, 0, 20);
        final Vector3 v5 = new Vector3(20, 30, -80);
        final Vector3 v6 = new Vector3(15, 0, -10);

        // Create our control point rotations
        final Quaternion q0 = new Quaternion(0.0, 0.0, 0.0, 1.0);
        final Quaternion q1 = new Quaternion(1.0, 1.0, 1.0, 1.0);
        final Quaternion q2 = new Quaternion(0.0, 0.0, 0.0, 1.0);
        final Quaternion q3 = new Quaternion(1.0, 1.0, 1.0, 1.0);
        final Quaternion q4 = new Quaternion(0.0, 0.0, 0.0, 1.0);
        final Quaternion q5 = new Quaternion(1.0, 1.0, 1.0, 1.0);
        final Quaternion q6 = new Quaternion(0.0, 0.0, 0.0, 1.0);

        // Create out actual control points
        final List<ControlPoint> controlPoints = new ArrayList<ControlPoint>();
        controlPoints.add(new ControlPoint(v0, q0));
        controlPoints.add(new ControlPoint(v1, q1));
        controlPoints.add(new ControlPoint(v2, q2));
        controlPoints.add(new ControlPoint(v3, q3));
        controlPoints.add(new ControlPoint(v4, q4));
        controlPoints.add(new ControlPoint(v5, q5));
        controlPoints.add(new ControlPoint(v6, q6));

        // Create our curve from the control points and a spline
        final Curve curve = new Curve(controlPoints, new CatmullRomSpline());

        // Create our controller to move the box along the curve
        curveController = new CurveController(curve, true);
        curveController.setActive(true);
        curveController.setMaxTime(10);
        curveController.setRepeatType(RepeatType.WRAP);

        // Create a line from the curve so its easy to check the box is following it
        _root.attachChild(curve.toRenderableLine(50));

        // Create points from the curve so the actual control points can be easily seen
        final Point point = curve.toRenderablePoint(false);
        point.setPointSize(10f);

        _root.attachChild(point);

        box.addController(curveController);

        // Add a trigger to change the repeat type on the controller
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                switch (curveController.getRepeatType()) {
                    case CLAMP:
                        curveController.setRepeatType(RepeatType.CYCLE);
                        break;
                    case CYCLE:
                        curveController.setRepeatType(RepeatType.WRAP);
                        break;
                    case WRAP:
                        curveController.setRepeatType(RepeatType.CLAMP);
                        break;
                }
            }
        }));
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        super.updateExample(timer);

        boxCentre.setTransform(box.getTransform());
    }

    public static void main(final String[] args) {
        start(SplineExample.class);
    }

}
