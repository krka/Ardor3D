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

import java.util.logging.Logger;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.texturing.ProceduralTextureStreamer;
import com.ardor3d.extension.texturing.TextureClipmap;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.google.inject.Inject;

/**
 * Very simple example showing use of a Texture3D texture.
 */
public class TextureClipmapExample extends ExampleBase {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TextureClipmapExample.class.getName());

    TextureClipmap textureClipmap;

    public static void main(final String[] args) {
        start(TextureClipmapExample.class);
    }

    @Inject
    public TextureClipmapExample(final LogicalLayer logicalLayer, final FrameHandler frameWork) {
        super(logicalLayer, frameWork);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {}

    @Override
    protected void renderExample(final Renderer renderer) {
        final Camera camera = _canvas.getCanvasRenderer().getCamera();

        textureClipmap.update(renderer, camera.getLocation());

        super.renderExample(renderer);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Texture Clipmap Example - Ardor3D");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 100, 1));
        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 100, 0), Vector3.UNIT_Y);
        _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(
                50.0,
                (float) _canvas.getCanvasRenderer().getCamera().getWidth()
                        / _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0f, 20000);

        final Box box1 = new Box("box1", new Vector3(-10000, -2, -10000), new Vector3(10000, 0, 10000));
        _root.attachChild(box1);

        final Box box2 = new Box("box2", new Vector3(-20, 0, -10), new Vector3(0, 10, 10));
        _root.attachChild(box2);
        final Box box3 = new Box("box3", new Vector3(10, 0, -10), new Vector3(30, 10, 10));
        _root.attachChild(box3);

        final int textureSliceSize = 256;
        final ProceduralTextureStreamer streamer = new ProceduralTextureStreamer(65536, textureSliceSize);
        // final InMemoryTextureStreamer streamer = new InMemoryTextureStreamer(4096, textureSliceSize);
        // final SimpleFileTextureStreamer streamer = new SimpleFileTextureStreamer(2048, textureSliceSize);
        // final CachedFileTextureStreamer streamer = new CachedFileTextureStreamer(8192, textureSliceSize);
        final int validLevels = streamer.getValidLevels();
        textureClipmap = new TextureClipmap(streamer, textureSliceSize, validLevels, 64);

        box1.setRenderState(textureClipmap.getShaderState());
        box1.setRenderState(textureClipmap.getTextureState());
        box3.setRenderState(textureClipmap.getShaderState());
        box3.setRenderState(textureClipmap.getTextureState());

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                textureClipmap.reloadShader();
                box1.setRenderState(textureClipmap.getShaderState());
                box3.setRenderState(textureClipmap.getShaderState());
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                textureClipmap.setShowDebug(!textureClipmap.isShowDebug());
                textureClipmap.reloadShader();
                box1.setRenderState(textureClipmap.getShaderState());
                box3.setRenderState(textureClipmap.getShaderState());
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                textureClipmap.setScale(textureClipmap.getScale() * 2f);
                textureClipmap.reloadShader();
                box1.setRenderState(textureClipmap.getShaderState());
                box3.setRenderState(textureClipmap.getShaderState());
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                textureClipmap.setScale(textureClipmap.getScale() / 2f);
                textureClipmap.reloadShader();
                box1.setRenderState(textureClipmap.getShaderState());
                box3.setRenderState(textureClipmap.getShaderState());
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(10);
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(100);
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(500);
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(1000);
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(2000);
            }
        }));

    }

}
