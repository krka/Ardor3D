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

import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.effect.water.ProjectedGrid;
import com.ardor3d.extension.effect.water.WaterHeightGenerator;
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
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial.CullHint;
import com.ardor3d.scenegraph.Spatial.LightCombineMode;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.google.inject.Inject;

/**
 * Example showing the Projected Grid mesh.
 */
public class ProjectedGridExample extends ExampleBase {
    private final Timer _timer;

    /** The Projected Grid mesh */
    private ProjectedGrid projectedGrid;

    /** Text fields used to present info about the example. */
    private final BasicText _exampleInfo[] = new BasicText[2];

    public static void main(final String[] args) {
        start(ProjectedGridExample.class);
    }

    @Inject
    public ProjectedGridExample(final LogicalLayer layer, final FrameHandler frameWork, final Timer timer) {
        super(layer, frameWork);
        _timer = timer;
    }

    double counter = 0;
    int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("ProjectedGrid - Example");

        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 40, 0));

        projectedGrid = new ProjectedGrid("ProjectedGrid", _canvas.getCanvasRenderer().getCamera(), 100, 70, 0.01f,
                new WaterHeightGenerator(), _timer);
        _root.attachChild(projectedGrid);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final InputState inputState, final double tpf) {
                projectedGrid.setFreezeUpdate(!projectedGrid.isFreezeUpdate());
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            public void perform(final Canvas source, final InputState inputState, final double tpf) {
                projectedGrid.setNrUpdateThreads(projectedGrid.getNrUpdateThreads() - 1);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            public void perform(final Canvas source, final InputState inputState, final double tpf) {
                projectedGrid.setNrUpdateThreads(projectedGrid.getNrUpdateThreads() + 1);
                updateText();
            }
        }));

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.GuessNoCompression, true));
        _root.setRenderState(ts);

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);

        _root.setCullHint(CullHint.Never);

        // Setup textfields for presenting example info.
        final Node textNodes = new Node("Text");
        _root.attachChild(textNodes);
        textNodes.setRenderBucketType(RenderBucketType.Ortho);
        textNodes.setLightCombineMode(LightCombineMode.Off);

        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
            textNodes.attachChild(_exampleInfo[i]);
        }

        textNodes.updateGeometricState(0.0);
        updateText();
    }

    /**
     * Update text information.
     */
    private void updateText() {
        _exampleInfo[0].setText("[1/2] Number of update threads: " + projectedGrid.getNrUpdateThreads());
        _exampleInfo[1].setText("[SPACE] Freeze update: " + projectedGrid.isFreezeUpdate());
    }
}
