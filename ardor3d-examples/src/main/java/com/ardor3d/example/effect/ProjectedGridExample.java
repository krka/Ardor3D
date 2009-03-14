/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.effect;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.effect.water.HeightGenerator;
import com.ardor3d.extension.effect.water.ProjectedGrid;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.scenegraph.Spatial.CullHint;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.google.inject.Inject;

public class ProjectedGridExample extends ExampleBase {
    private final Timer _timer;

    public static void main(final String[] args) {
        start(ProjectedGridExample.class);
    }

    @Inject
    public ProjectedGridExample(final LogicalLayer layer, final FrameHandler frameWork, final Timer timer) {
        super(layer, frameWork);
        _timer = timer;
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("ProjectedGrid - Example");

        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 40, 0));

        final ProjectedGrid projectedGrid = new ProjectedGrid("ProjectedGrid", _canvas.getCanvasRenderer().getCamera(),
                64, 64, 0.01f, new HeightGenerator() {
                    public double getHeight(final double x, final double z, final double time) {
                        return Math.abs(Math.sin(x * 0.01) * Math.cos(z * 0.01) * 30.0 + Math.sin(x * 0.1)
                                * Math.cos(z * 0.1) * 5.0);
                    }
                }, _timer);
        _root.attachChild(projectedGrid);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final InputState inputState, final double tpf) {
                projectedGrid.switchFreeze();
            }
        }));

        for (int i = 0; i < 20; i++) {
            final Box b = new Box("box", new Vector3(), 2, 50, 2);
            b.setModelBound(new BoundingBox());
            b.updateModelBound();
            final double x = Math.random() * 1000 - 500;
            final double z = Math.random() * 1000 - 500;
            b.setTranslation(new Vector3(x, 50, z));
            _root.attachChild(b);
        }

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.GuessNoCompression, true));
        _root.setRenderState(ts);

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);

        _root.setCullHint(CullHint.Never);
    }
}
