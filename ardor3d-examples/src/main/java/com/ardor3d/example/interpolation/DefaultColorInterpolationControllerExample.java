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
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.scenegraph.controllers.interpolation.DefaultColorInterpolationController;
import com.google.inject.Inject;

/**
 * A simple example showing the DefaultColorInterpolationController in action.
 */
public class DefaultColorInterpolationControllerExample extends
        InterpolationControllerBase<DefaultColorInterpolationController> {

    public static void main(final String[] args) {
        start(DefaultColorInterpolationControllerExample.class);
    }

    @Inject
    public DefaultColorInterpolationControllerExample(final LogicalLayer logicalLayer, final FrameHandler frameWork) {
        super(logicalLayer, frameWork);
    }

    @Override
    protected DefaultColorInterpolationController createController() {
        // Create our control point colors
        final ReadOnlyColorRGBA[] colors = { ColorRGBA.WHITE, ColorRGBA.RED, ColorRGBA.GREEN, ColorRGBA.BLUE };

        // Create our controller
        final DefaultColorInterpolationController controller = new DefaultColorInterpolationController();
        controller.setControls(colors);
        controller.setActive(true);
        controller.setMaxTime(5);

        return controller;
    }
}
