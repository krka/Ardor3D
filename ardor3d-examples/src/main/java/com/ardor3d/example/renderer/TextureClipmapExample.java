/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.texturing.ProceduralTextureStreamer;
import com.ardor3d.extension.texturing.TextureClipmap;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.util.GeneratedImageFactory;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.functions.Functions;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Illustrates the TextureClipmap class, which prohibits the display of textures outside a defined region.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.TextureClipmapExample", //
thumbnailPath = "/com/ardor3d/example/media/thumbnails/renderer_TextureClipmapExample.jpg", //
maxHeapMemory = 64)
public class TextureClipmapExample extends ExampleBase {

    /** Text fields used to present info about the example. */
    private final BasicText _exampleInfo[] = new BasicText[9];

    private TextureClipmap textureClipmap;

    public static void main(final String[] args) {
        start(TextureClipmapExample.class);
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

        final ProceduralTextureStreamer streamer = createProceduralStreamer(65536, textureSliceSize);
        // final InMemoryTextureStreamer streamer = new InMemoryTextureStreamer(4096, textureSliceSize);
        // final SimpleFileTextureStreamer streamer = new SimpleFileTextureStreamer(2048, textureSliceSize);
        // final CachedFileTextureStreamer streamer = new CachedFileTextureStreamer(8192, textureSliceSize);
        final int validLevels = streamer.getValidLevels();
        textureClipmap = new TextureClipmap(streamer, textureSliceSize, validLevels, 64);

        box1.setRenderState(textureClipmap.getShaderState());
        box1.setRenderState(textureClipmap.getTextureState());
        box3.setRenderState(textureClipmap.getShaderState());
        box3.setRenderState(textureClipmap.getTextureState());

        // Setup labels for presenting example info.
        final Node textNodes = new Node("Text");
        _root.attachChild(textNodes);
        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);

        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
            textNodes.attachChild(_exampleInfo[i]);
        }

        textNodes.updateGeometricState(0.0);
        updateText();

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
                updateText();
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                textureClipmap.setScale(textureClipmap.getScale() * 2f);
                textureClipmap.reloadShader();
                box1.setRenderState(textureClipmap.getShaderState());
                box3.setRenderState(textureClipmap.getShaderState());
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                textureClipmap.setScale(textureClipmap.getScale() / 2f);
                textureClipmap.reloadShader();
                box1.setRenderState(textureClipmap.getShaderState());
                box3.setRenderState(textureClipmap.getShaderState());
                updateText();
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(10);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(100);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(500);
                updateText();
            }
        }));
    }

    private ProceduralTextureStreamer createProceduralStreamer(final int sourceSize, final int textureSliceSize) {
        final double scale = 1.0 / 2048.0;
        final Function3D function = Functions.scaleInput(
                new FbmFunction3D(Functions.simplexNoise(), 5, 0.5, 0.5, 3.14), scale, scale, 1);
        final ReadOnlyColorRGBA[] terrainColors = new ReadOnlyColorRGBA[256];
        terrainColors[0] = new ColorRGBA(0, 0, .5f, 1);
        terrainColors[95] = new ColorRGBA(0, 0, 1, 1);
        terrainColors[127] = new ColorRGBA(0, .5f, 1, 1);
        terrainColors[137] = new ColorRGBA(240 / 255f, 240 / 255f, 64 / 255f, 1);
        terrainColors[143] = new ColorRGBA(32 / 255f, 160 / 255f, 0, 1);
        terrainColors[175] = new ColorRGBA(224 / 255f, 224 / 255f, 0, 1);
        terrainColors[223] = new ColorRGBA(128 / 255f, 128 / 255f, 128 / 255f, 1);
        terrainColors[255] = ColorRGBA.WHITE;
        GeneratedImageFactory.fillInColorTable(terrainColors);

        final ProceduralTextureStreamer streamer = new ProceduralTextureStreamer(sourceSize, textureSliceSize,
                function, terrainColors);
        return streamer;
    }

    /**
     * Update text information.
     */
    private void updateText() {
        _exampleInfo[0].setText("[1-3] Move speed: " + _controlHandle.getMoveSpeed());
        _exampleInfo[1].setText("[Space] Show debug: " + textureClipmap.isShowDebug());
        _exampleInfo[2].setText("[U/J] Scale up/down");
    }
}
