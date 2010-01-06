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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.google.inject.Inject;

public class ColladaExample extends ExampleBase {

    private Node colladaNode;
    private boolean showSkeleton = false, showMesh = true;

    private BasicText frameRateLabel;

    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    private List<File> daeFiles;
    private int fileIndex = 0;

    public static void main(final String[] args) {
        ExampleBase.start(ColladaExample.class);
    }

    @Inject
    public ColladaExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Collada Import");

        _lightState.detachAll();
        final DirectionalLight light = new DirectionalLight();
        light.setDiffuse(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        light.setAmbient(new ColorRGBA(0.25f, 0.25f, 0.25f, 1.0f));
        light.setDirection(new Vector3(1, 1, 1).normalizeLocal());
        light.setEnabled(true);
        _lightState.attach(light);

        // Load collada model
        loadColladaModel("collada/sony/Seymour.dae");

        final File rootDir = new File(".");
        daeFiles = findFiles(rootDir, ".dae", null);

        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.GRAY);

        final BasicText t1 = BasicText.createDefaultTextLabel("Text1", "[Space - Switch model]");
        t1.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t1.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t1.setTranslation(new Vector3(5, 0 * (t1.getHeight() + 5) + 10, 0));
        _root.attachChild(t1);
        _root.getSceneHints().setCullHint(CullHint.Never);

        final BasicText t2 = BasicText.createDefaultTextLabel("Text2", "[M] Hide Mesh.");
        t2.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t2.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t2.setTranslation(new Vector3(5, 1 * (t2.getHeight() + 5) + 10, 0));
        _root.attachChild(t2);
        _root.getSceneHints().setCullHint(CullHint.Never);

        final BasicText t3 = BasicText.createDefaultTextLabel("Text3", "[K] Show Skeleton.");
        t3.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t3.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t3.setTranslation(new Vector3(5, 2 * (t3.getHeight() + 5) + 10, 0));
        _root.attachChild(t3);
        _root.getSceneHints().setCullHint(CullHint.Never);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showSkeleton = !showSkeleton;
                if (showSkeleton) {
                    t3.setText("[K] Hide Skeleton.");
                } else {
                    t3.setText("[K] Show Skeleon.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.M), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showMesh = !showMesh;
                colladaNode.getSceneHints().setCullHint(showMesh ? CullHint.Dynamic : CullHint.Always);
                if (showMesh) {
                    t2.setText("[M] Hide Mesh.");
                } else {
                    t2.setText("[M] Show Mesh.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final File file = daeFiles.get(fileIndex);
                try {
                    loadColladaModel(file.toURI());
                    t1.setText("[Space - Switch model] " + file.getName());
                } catch (final URISyntaxException e) {
                    e.printStackTrace();
                }
                fileIndex = (fileIndex + 1) % daeFiles.size();
            }
        }));

        // Add fps display
        frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "");
        frameRateLabel.setTranslation(5, _canvas.getCanvasRenderer().getCamera().getHeight() - 5
                - frameRateLabel.getHeight(), 0);
        frameRateLabel.setTextColor(ColorRGBA.WHITE);
        frameRateLabel.getSceneHints().setOrthoOrder(-1);
        _root.attachChild(frameRateLabel);
    }

    private void loadColladaModel(final URI modelURI) throws URISyntaxException {
        // add a temporary resource locator since this is potentially outside our normal model location.
        final SimpleResourceLocator loc = new SimpleResourceLocator(modelURI.resolve("./"));
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
        final SimpleResourceLocator loc2 = new SimpleResourceLocator(modelURI.resolve("./"));
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, loc2);

        loadColladaModel(modelURI.toString());

        // remove locator
        ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
        ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc2);
    }

    private void loadColladaModel(final String file) {
        try {
            // detach the old colladaNode, if present.
            _root.detachChild(colladaNode);

            final long time = System.currentTimeMillis();
            final ColladaImporter colladaImporter = new ColladaImporter();

            // Load the collada scene
            final ColladaStorage storage = colladaImporter.readColladaFile(file);
            colladaNode = storage.getScene();

            // Set dynamic node culling
            colladaNode.getSceneHints().setCullHint(showMesh ? CullHint.Dynamic : CullHint.Always);

            // Uncomment for debugging the scenegraph
            // final ScenegraphTree tree = new ScenegraphTree();
            // tree.show(colladaNode);

            System.out.println("Importing: " + file);
            System.out.println("Took " + (System.currentTimeMillis() - time) + " ms");

            // Add colladaNode to root
            _root.attachChild(colladaNode);

            // TODO temp camera positioning until reading camera instances...
            ReadOnlyVector3 upAxis = Vector3.UNIT_Y;
            if (storage.getAssetData().getUpAxis() != null) {
                upAxis = storage.getAssetData().getUpAxis();
            }

            positionCamera(upAxis);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private void positionCamera(final ReadOnlyVector3 upAxis) {
        colladaNode.updateGeometricState(0.0);
        final BoundingVolume bounding = colladaNode.getWorldBound();
        if (bounding != null) {
            final ReadOnlyVector3 center = bounding.getCenter();
            double radius = 0;
            if (bounding instanceof BoundingSphere) {
                radius = ((BoundingSphere) bounding).getRadius();
            } else if (bounding instanceof BoundingBox) {
                final BoundingBox boundingBox = (BoundingBox) bounding;
                radius = Math.max(Math.max(boundingBox.getXExtent(), boundingBox.getYExtent()), boundingBox
                        .getZExtent());
            }

            final Vector3 vec = new Vector3(center);
            // XXX: a bit of a hack
            if (upAxis.equals(Vector3.UNIT_Z)) {
                vec.addLocal(0.0, -radius * 2, 0.0);
            } else {
                vec.addLocal(0.0, 0.0, radius * 2);
            }

            _controlHandle.setUpAxis(upAxis);

            final Camera cam = _canvas.getCanvasRenderer().getCamera();
            cam.setLocation(vec);
            cam.lookAt(center, upAxis);
            final double near = Math.max(radius / 500.0, 0.25);
            final double far = Math.min(radius * 5, 10000.0);
            cam.setFrustumPerspective(50.0, cam.getWidth() / (double) cam.getHeight(), near, far);
            cam.update();

            _controlHandle.setMoveSpeed(radius / 1.0);
        }
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        final long now = System.currentTimeMillis();
        final long dt = now - startTime;
        if (dt > 200) {
            final long fps = Math.round(1e3 * frames / dt);
            frameRateLabel.setText(fps + " fps");

            startTime = now;
            frames = 0;
        }
        frames++;
    }

    @Override
    protected void renderDebug(final Renderer renderer) {
        super.renderDebug(renderer);

        if (showSkeleton) {
            SkeletalDebugger.drawSkeletons(_root, renderer);
        }
    }

    private List<File> findFiles(final File rootDir, final String name, List<File> fileList) {
        if (fileList == null) {
            fileList = new ArrayList<File>();
        }
        final File[] files = rootDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            if (file.isDirectory()) {
                findFiles(file, name, fileList);
            } else if (name.equals(file.getName().substring(file.getName().length() - 4).toLowerCase())) {
                System.err.println("found: " + file);
                fileList.add(file);
            }
        }
        return fileList;
    }
}
