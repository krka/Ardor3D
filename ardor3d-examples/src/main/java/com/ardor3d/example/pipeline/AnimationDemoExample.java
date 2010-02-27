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

import java.util.EnumSet;
import java.util.List;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.AnimationClip;
import com.ardor3d.extension.animation.skeletal.AnimationListener;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.JointChannel;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.TransformChannel;
import com.ardor3d.extension.animation.skeletal.TransformData;
import com.ardor3d.extension.animation.skeletal.blendtree.BinaryTransformLERPSource;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.InclusiveClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.ManagedTransformSource;
import com.ardor3d.extension.animation.skeletal.blendtree.TransformApplier;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.AnchorLayout;
import com.ardor3d.extension.ui.layout.AnchorLayoutData;
import com.ardor3d.extension.ui.util.Alignment;
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
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.util.ReadOnlyTimer;
import com.google.common.collect.Lists;

/**
 * Illustrates loading several animations from Collada and arranging them in a controllable blend tree.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.AnimationDemoExample", //
thumbnailPath = "/com/ardor3d/example/media/thumbnails/pipeline_AnimationDemoExample.jpg", //
maxHeapMemory = 64)
public class AnimationDemoExample extends ExampleBase {

    private Node colladaNode;
    private boolean showSkeleton = false, showJointLabels = false;

    private UILabel frameRateLabel;
    private UIHud hud;

    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    private AnimationManager manager;
    private SkeletonPose pose;

    private BinaryTransformLERPSource moveBlend, punchBlend, headBlend;
    private int headJoint;
    private ManagedTransformSource headSource;

    private BlendController moveBlendController;
    private BlendController punchBlendController;
    private AnimationClip punchClip;

    private final Quaternion headRotation = new Quaternion();
    private UIButton runWalkButton, punchButton;

    public static void main(final String[] args) {
        ExampleBase.start(AnimationDemoExample.class);
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

        // Create our options frame and fps label
        createHUD();
    }

    private void createHUD() {
        hud = new UIHud();
        hud.setupInput(_canvas, _physicalLayer, _logicalLayer);

        // Add fps display
        frameRateLabel = new UILabel("X");
        frameRateLabel.setHudXY(5, _canvas.getCanvasRenderer().getCamera().getHeight() - 5
                - frameRateLabel.getContentHeight());
        frameRateLabel.setForegroundColor(ColorRGBA.WHITE);
        hud.add(frameRateLabel);

        final UIFrame optionsFrame = new UIFrame("Controls", EnumSet.noneOf(FrameButtons.class));

        final UIPanel basePanel = optionsFrame.getContentPanel();
        basePanel.setLayout(new AnchorLayout());

        runWalkButton = new UIButton("Start running...");
        runWalkButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, basePanel, Alignment.TOP_LEFT, 5, -5));
        runWalkButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                if (moveBlendController.getBlendDirection() == BlendDirection.Up) {
                    moveBlendController.setBlendDirection(BlendDirection.Down);
                    runWalkButton.setText("Start running...");
                } else {
                    moveBlendController.setBlendDirection(BlendDirection.Up);
                    runWalkButton.setText("Start walking...");
                }
            }
        });
        basePanel.add(runWalkButton);

        punchButton = new UIButton("PUNCH!");
        punchButton
                .setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, runWalkButton, Alignment.BOTTOM_LEFT, 0, -5));
        punchButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                punchBlendController.setBlendDirection(BlendDirection.Up);
                manager.resetClip(punchClip);
                punchButton.setEnabled(false);
            }
        });
        basePanel.add(punchButton);

        final UICheckBox headCheck = new UICheckBox("Procedurally turn head");
        headCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, punchButton, Alignment.BOTTOM_LEFT, 0, -5));
        headCheck.setSelected(true);
        headCheck.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                headBlend.setBlendWeight(headCheck.isSelected() ? 1 : 0);
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

        final UICheckBox skeletonCheck = new UICheckBox("Show skeleton");
        final UICheckBox boneLabelCheck = new UICheckBox("Show joint labels");
        skeletonCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, skinCheck, Alignment.BOTTOM_LEFT, 0, -5));
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

            // Add colladaNode to root
            _root.attachChild(colladaNode);

            // TODO temp camera positioning until reading camera instances...
            ReadOnlyVector3 upAxis = Vector3.UNIT_Y;
            if (storage.getAssetData().getUpAxis() != null) {
                upAxis = storage.getAssetData().getUpAxis();
            }

            positionCamera(upAxis);

            final CullState cullState = new CullState();
            cullState.setCullFace(Face.Back);
            _root.setRenderState(cullState);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createAnimation(final ColladaImporter colladaImporter) {
        final ColladaStorage storage1 = colladaImporter.load("collada/skeleton/skeleton.walk.dae");
        final ColladaStorage storage2 = colladaImporter.load("collada/skeleton/skeleton.run.dae");
        final ColladaStorage storage3 = colladaImporter.load("collada/skeleton/skeleton.punch.dae");

        // Make our manager
        manager = new AnimationManager(_timer, pose);

        final AnimationClip walkClip = new AnimationClip();
        for (final JointChannel channel : storage1.getJointChannels()) {
            // add it to a clip
            walkClip.addChannel(channel);
        }
        final AnimationClip runClip = new AnimationClip();
        for (final JointChannel channel : storage2.getJointChannels()) {
            // add it to a clip
            runClip.addChannel(channel);
        }
        punchClip = new AnimationClip();
        for (final JointChannel channel : storage3.getJointChannels()) {
            // add it to a clip
            punchClip.addChannel(channel);
        }

        // add the clip
        manager.addClip(walkClip);
        manager.addClip(runClip);
        manager.addClip(punchClip);

        // XXX: The next bits might be setup by an Animation state machine of some sort.

        // Set some clip instance specific data - repeat, time scaling
        manager.getClipState(walkClip).setLoopCount(Integer.MAX_VALUE);
        manager.getClipState(runClip).setLoopCount(Integer.MAX_VALUE);
        manager.getClipState(punchClip).setLoopCount(1);
        manager.getClipState(punchClip).setActive(false);

        // Add our "applier logic".
        manager.setApplier(new TransformApplier());

        // Add our "blend tree"
        // walk to run blend
        final ClipSource walkSource = new ClipSource(walkClip, manager);
        final ClipSource runSource = new ClipSource(runClip, manager);
        moveBlend = new BinaryTransformLERPSource(walkSource, runSource);
        moveBlendController = new BlendController(moveBlend, 0.01);

        // punch blend
        final InclusiveClipSource punchSourceArm = new InclusiveClipSource(punchClip, manager);
        punchSourceArm.setEnabledJoints(Lists.newArrayList(11, 12, 13, 14, 15));
        punchBlend = new BinaryTransformLERPSource(moveBlend, punchSourceArm);
        punchBlendController = new BlendController(punchBlend, 0.01);
        punchBlendController.addBlendControllerListener(new BlendControllerListener() {

            public void blendDone(final BlendController blendController) {
                if (blendController.getBlendDirection() == BlendDirection.Down) {
                    punchButton.setEnabled(true);
                }
            }
        });
        manager.getClipState(punchClip).addAnimationListener(new AnimationListener() {

            public void animationFinished() {
                punchBlendController.setBlendDirection(BlendDirection.Down);
            }
        });

        headSource = new ManagedTransformSource();
        headJoint = storage1.getSkins().get(0).getPose().getSkeleton().findJointByName("Bip01_Head");
        headSource.setJointTransformData(headJoint, ((TransformChannel) walkClip
                .findChannelByName(JointChannel.JOINT_CHANNEL_NAME + headJoint)).getTransformData(0,
                new TransformData()));
        headBlend = new BinaryTransformLERPSource(punchBlend, headSource);
        headBlend.setBlendWeight(1.0);

        manager.setBlendRoot(headBlend);
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
        hud.updateGeometricState(timer.getTimePerFrame());

        // update the head's position
        {
            double angle = timer.getTimeInSeconds();
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

        moveBlendController.update();
        punchBlendController.update();

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
        pose.updateTransforms();
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

    private enum BlendDirection {
        Up, Down
    }

    private class BlendController {
        private final double blendSpeed;
        private final BinaryTransformLERPSource blendSource;
        private BlendDirection blendDirection = BlendDirection.Down;
        private boolean doBlend;
        private final List<BlendControllerListener> blendControllerListeners;

        public BlendController(final BinaryTransformLERPSource blendSource, final double blendSpeed) {
            this.blendSource = blendSource;
            this.blendSpeed = blendSpeed;
            blendControllerListeners = Lists.newArrayList();
        }

        public BlendDirection getBlendDirection() {
            return blendDirection;
        }

        public void setBlendDirection(final BlendDirection blendDirection) {
            this.blendDirection = blendDirection;
            doBlend = true;
        }

        public void addBlendControllerListener(final BlendControllerListener blendControllerListener) {
            blendControllerListeners.add(blendControllerListener);
        }

        public void update() {
            if (!doBlend) {
                return;
            }

            final double blendChange = blendDirection == BlendDirection.Up ? blendSpeed : -blendSpeed;
            double blend = blendSource.getBlendWeight() + blendChange;
            if (blend < 0 || blend > 1) {
                blend = MathUtils.clamp(blend, 0.0, 1.0);
                doBlend = false;
                for (final BlendControllerListener blendControllerListener : blendControllerListeners) {
                    blendControllerListener.blendDone(this);
                }
            }
            blendSource.setBlendWeight(blend);
        }
    }

    private interface BlendControllerListener {
        void blendDone(BlendController blendController);
    }
}
