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

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.AnimationListener;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.AttachmentPoint;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.blendtree.ManagedTransformSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.state.loader.InputStore;
import com.ardor3d.extension.animation.skeletal.state.loader.JSLayerImporter;
import com.ardor3d.extension.animation.skeletal.state.loader.OutputStore;
import com.ardor3d.extension.animation.skeletal.util.MissingCallback;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.AnchorLayout;
import com.ardor3d.extension.ui.layout.AnchorLayoutData;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.ButtonGroup;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * Illustrates loading several animations from Collada and arranging them in an animation state machine.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.AnimationDemoExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_AnimationDemoExample.jpg", //
maxHeapMemory = 64)
public class AnimationDemoExample extends ExampleBase {

    static AnimationDemoExample instance;

    private Node colladaNode;
    private boolean showSkeleton = false, showJointLabels = false;

    private UILabel frameRateLabel;
    private UICheckBox headCheck;
    private UIHud hud;

    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    private AnimationManager manager;
    private SkeletonPose pose;

    private UIButton runWalkButton, punchButton;
    private OutputStore layerOutput;
    private Cylinder staff;

    private GLSLShaderObjectsState gpuShader;

    public static void main(final String[] args) {
        ExampleBase.start(AnimationDemoExample.class);
    }

    public AnimationDemoExample() {
        instance = this;
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Animation Demo Example");
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.GRAY);

        _lightState.detachAll();
        final DirectionalLight light = new DirectionalLight();
        light.setDiffuse(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        light.setAmbient(new ColorRGBA(0.25f, 0.25f, 0.25f, 1.0f));
        light.setDirection(new Vector3(-1, -1, -1).normalizeLocal());
        light.setEnabled(true);
        _lightState.attach(light);

        // Load collada model
        createCharacter();

        // Create our staff object for attaching.
        staff = new Cylinder("staff", 4, 9, 1, 40, true);

        // Create our options frame and fps label
        createHUD();
    }

    private void createHUD() {
        hud = new UIHud();
        hud.setupInput(_canvas, _physicalLayer, _logicalLayer);
        hud.setMouseManager(_mouseManager);

        // Add fps display
        frameRateLabel = new UILabel("X");
        frameRateLabel.setHudXY(5,
                _canvas.getCanvasRenderer().getCamera().getHeight() - 5 - frameRateLabel.getContentHeight());
        frameRateLabel.setForegroundColor(ColorRGBA.WHITE);
        hud.add(frameRateLabel);

        final UIFrame optionsFrame = new UIFrame("Controls", EnumSet.noneOf(FrameButtons.class));

        final UIPanel basePanel = optionsFrame.getContentPanel();
        basePanel.setLayout(new AnchorLayout());

        runWalkButton = new UIButton("Start running...");
        runWalkButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, basePanel, Alignment.TOP_LEFT, 5, -5));
        runWalkButton.addActionListener(new ActionListener() {
            boolean walk = true;

            public void actionPerformed(final ActionEvent event) {
                if (!walk) {
                    if (manager.getBaseAnimationLayer().doTransition("walk")) {
                        runWalkButton.setButtonText("Start running...");
                        walk = true;
                    }
                } else {
                    if (manager.getBaseAnimationLayer().doTransition("run")) {
                        runWalkButton.setButtonText("Start walking...");
                        walk = false;
                    }
                }
            }
        });
        basePanel.add(runWalkButton);

        punchButton = new UIButton("PUNCH!");
        punchButton
                .setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, runWalkButton, Alignment.BOTTOM_LEFT, 0, -5));
        punchButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                manager.findAnimationLayer("punch").setCurrentState("punch_right", true);
                punchButton.setEnabled(false);
            }
        });
        basePanel.add(punchButton);

        headCheck = new UICheckBox("Procedurally turn head");
        headCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, punchButton, Alignment.BOTTOM_LEFT, 0, -5));
        headCheck.setSelected(true);
        headCheck.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                manager.getValuesStore().put("head_blend", headCheck.isSelected() ? 1.0 : 0.0);
            }
        });
        basePanel.add(headCheck);

        final UICheckBox skinCheck = new UICheckBox("Show skin mesh");
        skinCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, headCheck, Alignment.BOTTOM_LEFT, 0, -5));
        skinCheck.setSelected(true);
        skinCheck.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                colladaNode.getSceneHints().setCullHint(skinCheck.isSelected() ? CullHint.Dynamic : CullHint.Always);
            }
        });
        basePanel.add(skinCheck);

        final UICheckBox gpuSkinningCheck = new UICheckBox("Use GPU skinning");
        gpuSkinningCheck
                .setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, skinCheck, Alignment.BOTTOM_LEFT, 0, -5));
        gpuSkinningCheck.setSelected(false);
        gpuSkinningCheck.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                _root.acceptVisitor(new Visitor() {
                    @Override
                    public void visit(final Spatial spatial) {
                        if (spatial instanceof SkinnedMesh) {
                            final SkinnedMesh skinnedSpatial = (SkinnedMesh) spatial;
                            if (gpuSkinningCheck.isSelected()) {
                                skinnedSpatial.setGPUShader(gpuShader);
                                skinnedSpatial.setUseGPU(true);
                            } else {
                                skinnedSpatial.setGPUShader(null);
                                skinnedSpatial.clearRenderState(StateType.GLSLShader);
                                skinnedSpatial.setUseGPU(false);
                            }
                        }
                    }
                }, true);
            }
        });
        basePanel.add(gpuSkinningCheck);

        final UICheckBox skeletonCheck = new UICheckBox("Show skeleton");
        final UICheckBox boneLabelCheck = new UICheckBox("Show joint labels");
        skeletonCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, gpuSkinningCheck, Alignment.BOTTOM_LEFT,
                0, -5));
        skeletonCheck.setSelected(showSkeleton);
        skeletonCheck.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                showSkeleton = skeletonCheck.isSelected();
                boneLabelCheck.setEnabled(showSkeleton);
            }
        });
        basePanel.add(skeletonCheck);

        boneLabelCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, skeletonCheck, Alignment.BOTTOM_LEFT, 0,
                -5));
        boneLabelCheck.setSelected(false);
        boneLabelCheck.setEnabled(showSkeleton);
        boneLabelCheck.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                showJointLabels = boneLabelCheck.isSelected();
            }
        });
        basePanel.add(boneLabelCheck);

        final UILabel attachLabel = new UILabel("Attach Staff to...");
        attachLabel
                .setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, boneLabelCheck, Alignment.BOTTOM_LEFT, 0, -8));
        basePanel.add(attachLabel);

        final ButtonGroup attachGroup = new ButtonGroup();
        final UIRadioButton attachNoneButton = new UIRadioButton("none");
        attachNoneButton.setSelected(true);
        attachNoneButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, attachLabel, Alignment.BOTTOM_LEFT, 0,
                -5));
        attachNoneButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                for (final AttachmentPoint p : layerOutput.getAttachmentPoints()) {
                    p.setAttachment(null);
                }
                staff.removeFromParent();
            }
        });
        attachNoneButton.setGroup(attachGroup);
        basePanel.add(attachNoneButton);

        UIRadioButton last = attachNoneButton;
        for (final AttachmentPoint p : layerOutput.getAttachmentPoints()) {
            final UIRadioButton attachPButton = new UIRadioButton(p.getName());
            attachPButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, last, Alignment.BOTTOM_LEFT, 0, -5));
            attachPButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    _root.attachChild(staff);
                    p.setAttachment(staff);
                    for (final AttachmentPoint p2 : layerOutput.getAttachmentPoints()) {
                        if (p2 != p) {
                            p2.setAttachment(null);
                        }
                    }
                }
            });
            attachPButton.setGroup(attachGroup);
            basePanel.add(attachPButton);
            last = attachPButton;
        }

        optionsFrame.updateMinimumSizeFromContents();
        optionsFrame.layout();
        optionsFrame.pack();

        optionsFrame.setUseStandin(true);
        optionsFrame.setOpacity(0.8f);

        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        optionsFrame.setLocalXY(cam.getWidth() - optionsFrame.getLocalComponentWidth() - 10, cam.getHeight()
                - optionsFrame.getLocalComponentHeight() - 10);
        hud.add(optionsFrame);

        UIComponent.setUseTransparency(true);
    }

    private void createCharacter() {
        try {
            // detach the old colladaNode, if present.
            _root.detachChild(colladaNode);

            final long time = System.currentTimeMillis();
            final ColladaImporter colladaImporter = new ColladaImporter();

            // Load the collada scene
            final String mainFile = "collada/skeleton/skeleton.walk.dae";
            final ColladaStorage storage = colladaImporter.load(mainFile);
            colladaNode = storage.getScene();
            final List<SkinData> skinDatas = storage.getSkins();
            pose = skinDatas.get(0).getPose();

            createAnimation(colladaImporter);

            System.out.println("Importing: " + mainFile);
            System.out.println("Took " + (System.currentTimeMillis() - time) + " ms");

            // TODO temp camera positioning until reading camera instances...
            ReadOnlyVector3 upAxis = Vector3.UNIT_Y;
            if (storage.getAssetData().getUpAxis() != null) {
                upAxis = storage.getAssetData().getUpAxis();
            }

            positionCamera(upAxis);

            gpuShader = new GLSLShaderObjectsState();
            gpuShader.setEnabled(true);
            try {
                gpuShader.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(AnimationDemoExample.class,
                        "com/ardor3d/extension/animation/skeletal/skinning_gpu.vert"));
                gpuShader.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(
                        AnimationDemoExample.class, "com/ardor3d/extension/animation/skeletal/skinning_gpu.frag"));
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            }

            final CullState cullState = new CullState();
            cullState.setCullFace(Face.Back);
            colladaNode.setRenderState(cullState);

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    // Add colladaNode to root
                    final Node copy = colladaNode.makeCopy(false);
                    copy.setTranslation(-i * 50, 0, -50 - (j * 50));
                    _root.attachChild(copy);
                }
            }

        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createAnimation(final ColladaImporter colladaImporter) {

        // Make our manager
        manager = new AnimationManager(_timer, pose);

        // Add our "applier logic".
        final SimpleAnimationApplier applier = new SimpleAnimationApplier();
        manager.setApplier(applier);

        // Add a call back to load clips.
        final InputStore input = new InputStore();
        input.getClips().setMissCallback(new MissingCallback<String, AnimationClip>() {
            public AnimationClip getValue(final String key) {
                try {
                    final ColladaStorage storage1 = colladaImporter.load("collada/skeleton/" + key + ".dae");
                    return storage1.extractChannelsAsClip(key);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        // Load our layer and states from script
        try {
            final ResourceSource layersFile = new URLResourceSource(ResourceLocatorTool.getClassPathResource(
                    AnimationDemoExample.class, "com/ardor3d/example/pipeline/AnimationDemoExample.js"));
            layerOutput = JSLayerImporter.addLayers(layersFile, manager, input);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // kick things off by setting our starting states
        manager.getBaseAnimationLayer().setCurrentState("walk_anim", true);
        manager.findAnimationLayer("head").setCurrentState("head_rotate", true);

        // add a head rotator
        final int headJoint = pose.getSkeleton().findJointByName("Bip01_Head");
        final ManagedTransformSource headSource = (ManagedTransformSource) manager.findAnimationLayer("head")
                .getSteadyState("head_rotate").getSourceTree();
        colladaNode.addController(new SpatialController<Node>() {
            private final Quaternion headRotation = new Quaternion();

            public void update(final double time, final Node caller) {
                // update the head's position
                if (headCheck != null && headCheck.isSelected()) {
                    double angle = _timer.getTimeInSeconds();
                    // range 0 to 180 degrees
                    angle %= MathUtils.PI;
                    // range -90 to 90 degrees
                    angle -= MathUtils.HALF_PI;
                    // range is now 0 to 90 degrees, reflected
                    angle = Math.abs(angle);
                    // range is now -45 to 45 degrees
                    angle -= MathUtils.HALF_PI / 2.0;

                    headRotation.fromAngleAxis(angle, Vector3.UNIT_X);
                    headSource.setJointRotation(headJoint, headRotation);
                }
            }
        });

        // add callback for our UI
        manager.findClipInstance("skeleton.punch").addAnimationListener(new AnimationListener() {
            public void animationFinished() {
                punchButton.setEnabled(true);
            }
        });
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
                radius = Math.max(Math.max(boundingBox.getXExtent(), boundingBox.getYExtent()),
                        boundingBox.getZExtent());
            }

            final Vector3 vec = new Vector3(center);
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
            final double far = Math.min(radius * 15, 10000.0);
            cam.setFrustumPerspective(50.0, cam.getWidth() / (double) cam.getHeight(), near, far);
            cam.update();

            _controlHandle.setMoveSpeed(radius / 1.0);
        }
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        hud.updateGeometricState(timer.getTimePerFrame());

        final long now = System.currentTimeMillis();
        final long dt = now - startTime;
        if (dt > 200) {
            final long fps = Math.round(1e3 * frames / dt);
            frameRateLabel.setText(fps + " fps");

            startTime = now;
            frames = 0;
        }
        frames++;

        manager.update();
    }

    @Override
    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        super.renderExample(renderer);
        renderer.renderBuckets();
        renderer.draw(hud);
    }

    @Override
    protected void renderDebug(final Renderer renderer) {
        super.renderDebug(renderer);

        if (showSkeleton) {
            SkeletalDebugger.drawSkeletons(_root, renderer, false, showJointLabels);
        }
    }

    public NativeCanvas getCanvas() {
        return _canvas;
    }

    public Node getRoot() {
        return _root;
    }
}