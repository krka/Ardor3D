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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.google.inject.Inject;

public class UpdateTextureExample extends ExampleBase {
    private Mesh t;
    private final Matrix3 rotate = new Matrix3();
    private double angle = 0;
    private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();
    private BufferedImage img;
    private Graphics imgGraphics;
    private ByteBuffer imageBuffer;
    private double counter = 0;
    private boolean updateTexture = true;

    public static void main(final String[] args) {
        start(UpdateTextureExample.class);
    }

    @Inject
    public UpdateTextureExample(final LogicalLayer logicalLayer, final FrameHandler frameWork) {
        super(logicalLayer, frameWork);
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        if (updateTexture) {
            final byte data[] = AWTImageLoader.asByteArray(img);
            imageBuffer.put(data);
            imageBuffer.flip();
            final Texture prevTexture = ((TextureState) _root.getLocalRenderState(RenderState.StateType.Texture))
                    .getTexture();
            renderer.updateTextureSubImage(prevTexture, imageBuffer, 0, 0, img.getWidth(), img.getHeight(), 0, 0, img
                    .getWidth(), img.getHeight(), prevTexture.getImage().getFormat());
        } else {
            final Image nextImage = AWTImageLoader.makeArdor3dImage(img, false);
            final Texture nextTexture = TextureManager.loadFromImage(nextImage, Texture.MinificationFilter.Trilinear,
                    Image.Format.GuessNoCompression, true);
            final Texture prevTexture = ((TextureState) _root.getLocalRenderState(RenderState.StateType.Texture))
                    .getTexture();
            TextureManager.releaseTexture(prevTexture, renderer);
            prevTexture.getTextureKey().setContextRep(null); // TODO: hack for texture caching bug
            TextureManager.releaseTexture(prevTexture, renderer);
            final TextureState ts = (TextureState) _root.getLocalRenderState(RenderState.StateType.Texture);
            ts.setTexture(nextTexture);
        }

        super.renderExample(renderer);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        final double tpf = timer.getTimePerFrame();
        if (tpf < 1) {
            angle = angle + tpf * 20;
            if (angle > 360) {
                angle = 0;
            }
        }

        rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        t.setRotation(rotate);

        for (int i = 0; i < 1000; i++) {
            final int w = MathUtils.nextRandomInt(0, img.getWidth() - 1);
            final int h = MathUtils.nextRandomInt(0, img.getHeight() - 1);
            final int y = Math.max(0, h - 8);
            final int rgb = img.getRGB(w, h);
            for (int j = h; j > y; j -= 2) {
                img.setRGB(w, j, rgb);
            }
        }

        final int x = (int) (Math.sin(counter) * img.getWidth() * 0.3 + img.getWidth() * 0.5);
        final int y = (int) (Math.sin(counter * 0.7) * img.getHeight() * 0.3 + img.getHeight() * 0.5);
        imgGraphics.setColor(new Color(MathUtils.nextRandomInt()));
        imgGraphics.fillOval(x, y, 10, 10);
        counter += tpf * 5;
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Update texture - Example");

        final BasicText keyText = BasicText.createDefaultTextLabel("Text", "[SPACE] Updating texture...");
        keyText.setRenderBucketType(RenderBucketType.Ortho);
        keyText.setLightCombineMode(LightCombineMode.Off);
        keyText.setTranslation(new Vector3(0, 20, 0));
        _root.attachChild(keyText);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final InputState inputState, final double tpf) {
                updateTexture = !updateTexture;
                if (updateTexture) {
                    keyText.setText("[SPACE] Updating texture...");
                } else {
                    keyText.setText("[SPACE] Recreating texture...");
                }
            }
        }));

        final Vector3 max = new Vector3(5, 5, 5);
        final Vector3 min = new Vector3(-5, -5, -5);

        t = new Box("Box", min, max);
        t.setModelBound(new BoundingBox());
        t.setTranslation(new Vector3(0, 0, -15));
        _root.attachChild(t);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.GuessNoCompression, true));

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);

        _root.setRenderState(ts);

        try {
            img = ImageIO.read(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE,
                    "images/ardor3d_white_256.jpg"));
            // FIXME: Check if this is a int[] or byte[]
            final byte data[] = AWTImageLoader.asByteArray(img);
            imageBuffer = BufferUtils.createByteBuffer(data.length);

            imgGraphics = img.getGraphics();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

    }
}
