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

import com.ardor3d.example.ExampleBase;
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
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial.LightCombineMode;
import com.ardor3d.scenegraph.extension.BillboardNode;
import com.ardor3d.scenegraph.extension.BillboardNode.BillboardAlignment;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.TextureManager;
import com.google.inject.Inject;

public class BillboardNodeExample extends ExampleBase {
    private BasicText t;

    public static void main(final String[] args) {
        start(BillboardNodeExample.class);
    }

    @Inject
    public BillboardNodeExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("BillboardNode - Example");

        t = BasicText.createDefaultTextLabel("Text", "[SPACE] " + BillboardAlignment.ScreenAligned);
        t.setRenderBucketType(RenderBucketType.Ortho);
        t.setLightCombineMode(LightCombineMode.Off);
        t.setTranslation(new Vector3(0, 20, 0));
        _root.attachChild(t);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.Guess, true));

        _root.setRenderState(ts);

        final Quad quad = new Quad("Quad", 5, 5);
        final BillboardNode billboard = new BillboardNode("Billboard");
        billboard.setAlignment(BillboardAlignment.ScreenAligned);
        billboard.attachChild(quad);
        _root.attachChild(billboard);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final InputState inputState, final double tpf) {
                int ordinal = billboard.getAlignment().ordinal() + 1;
                if (ordinal > BillboardAlignment.values().length - 1) {
                    ordinal = 0;
                }
                billboard.setAlignment(BillboardAlignment.values()[ordinal]);
                t.setText("[SPACE] " + billboard.getAlignment());
            }
        }));
    }
}
