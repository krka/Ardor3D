/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pipeline;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.Vector3;
import com.google.inject.Inject;

/**
 * Simplest example of loading a collada model.
 */
@Purpose(htmlDescription = "Simplest example of loading a collada model.", //
thumbnailPath = "/com/ardor3d/example/media/thumbnails/pipeline_SimpleColladaExample.jpg", //
maxHeapMemory = 64)
public class SimpleColladaExample extends ExampleBase {
    public static void main(final String[] args) {
        ExampleBase.start(SimpleColladaExample.class);
    }

    @Inject
    public SimpleColladaExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Simple Collada Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 5, 20));

        // Load the collada scene
        final ColladaStorage storage = new ColladaImporter().load("collada/sony/Seymour.dae");
        _root.attachChild(storage.getScene());
    }
}