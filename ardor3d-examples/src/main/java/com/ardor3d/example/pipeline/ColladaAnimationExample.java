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
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.animations.runtime.AnimationSystem;
import com.ardor3d.animations.runtime.AnimationRegistry;
import com.ardor3d.animations.runtime.AnimatableInstance;
import com.ardor3d.util.ReadOnlyTimer;
import com.google.inject.Inject;

public class ColladaAnimationExample extends ExampleBase {

    private Node _colladaNode;
    private AnimationSystem _animationSystem;

    public static void main(final String[] args) {
        start(ColladaAnimationExample.class);
    }

    @Inject
    public ColladaAnimationExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Collada Import");

        _animationSystem = new AnimationSystem(new AnimationRegistry());

        // Load collada model
        try {
            _colladaNode = ColladaImporter.readColladaScene("/HC_Medium_Char_Skin.dae", _animationSystem.getAnimationRegistry());
            _root.attachChild(_colladaNode);

            // ensure that the skeletons are shown

            for (AnimatableInstance animatableInstance : _animationSystem.getAnimationRegistry().getAnimatableInstances()) {
                for (Spatial spatial : animatableInstance.getAnimatable().getBindShape()) {
                    animatableInstance.getNode().attachChild(spatial);
                }
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 50, 150));
    }

    @Override
    protected void updateExample(ReadOnlyTimer timer) {
        // update skeleton instances
        // update animation instance meshes based on skeleton positions
    }
}