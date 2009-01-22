/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.basic;

import java.nio.FloatBuffer;
import java.util.logging.Logger;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.FrameWork;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.Spatial.LightCombineMode;
import com.ardor3d.scenegraph.event.DirtyEventListener;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.event.SceneGraphManager;
import com.ardor3d.scenegraph.shape.Arrow;
import com.ardor3d.scenegraph.shape.AxisRods;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Capsule;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.scenegraph.shape.OrientedBox;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.scenegraph.shape.Torus;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.google.inject.Inject;

public class ShapesExample extends ExampleBase implements DirtyEventListener {
    private static final Logger logger = Logger.getLogger(ShapesExample.class.getName());

    private int wrapCount;
    private int index;

    public static void main(final String[] args) {
        start(ShapesExample.class);
    }

    @Inject
    public ShapesExample(final LogicalLayer layer, final FrameWork frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        SceneGraphManager.getSceneGraphManager().addDirtyEventListener(this);
        SceneGraphManager.getSceneGraphManager().listenOnSpatial(_root);

        _canvas.setTitle("Shapes");

        wrapCount = 3;
        addMesh(new Box("TestBox", new Vector3(), 3, 3, 3));
        addMesh(new Sphere("Sphere", 16, 16, 3));
        addMesh(new Capsule("Capsule", 5, 5, 5, 2, 5));
        addMesh(new Teapot("Teapot"));
        addMesh(new Torus("Torus", 8, 8, 2, 4));
        addMesh(new Cylinder("Cylinder", 8, 8, 2, 4));
        addMesh(new Pyramid("Pyramid", 2, 4));
        addMesh(new Arrow("Arrow", 2, 4));
        addMesh(new AxisRods("AxisRods", false, 2, 4));
        addMesh(new OrientedBox("OrientedBox"));
        addMesh(createLines());

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, Format.Guess,
                true));
        _root.setRenderState(ts);

        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        _root.setRenderState(bs);

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);
    }

    private Spatial createLines() {
        final FloatBuffer verts = BufferUtils.createVector3Buffer(3);
        verts.put(0).put(0).put(0);
        verts.put(5).put(5).put(0);
        verts.put(0).put(5).put(0);
        final Line line = new Line("Lines", verts, null, null, null);
        line.getMeshData().setIndexMode(IndexMode.LineStrip);
        line.setLineWidth(2);
        line.setLightCombineMode(LightCombineMode.Off);

        return line;
    }

    private void addMesh(final Spatial spatial) {
        spatial.setTranslation((index % wrapCount) * 8, (index / wrapCount) * 8, -20);
        if (spatial instanceof Mesh) {
            ((Mesh) spatial).setRandomColors();
            ((Mesh) spatial).updateModelBound();
        }
        _root.attachChild(spatial);
        index++;
    }

    public boolean spatialDirty(final Spatial spatial, final DirtyType dirtyType) {
        logger.info("Spatial dirty: " + spatial + " - " + dirtyType);
        return false;
    }
}
