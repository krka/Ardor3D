/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pipeline;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.model.collada.ColladaImporter;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;
import com.google.inject.Inject;

public class ColladaExample extends ExampleBase {

    private Node colladaNode;

    public static void main(final String[] args) {
        start(ColladaExample.class);
    }

    @Inject
    public ColladaExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Collada Import");

        // Load collada model
        try {
            colladaNode = ColladaImporter.readColladaScene("collada/duck/duck.dae");
            _root.attachChild(colladaNode);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 50, 150));
    }
}
