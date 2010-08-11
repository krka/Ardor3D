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
import java.util.concurrent.Callable;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.AnimationListener;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.AttachmentPoint;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.blendtree.ManagedTransformSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.TriggerCallback;
import com.ardor3d.extension.animation.skeletal.clip.TriggerChannel;
import com.ardor3d.extension.animation.skeletal.state.loader.InputStore;
import com.ardor3d.extension.animation.skeletal.state.loader.JSLayerImporter;
import com.ardor3d.extension.animation.skeletal.state.loader.OutputStore;
import com.ardor3d.extension.animation.skeletal.util.MissingCallback;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.effect.particle.ParticleControllerListener;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
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
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.AnchorLayout;
import com.ardor3d.extension.ui.layout.AnchorLayoutData;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.ButtonGroup;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.controller.ComplexSpatialController.RepeatType;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * Illustrates loading several animations from Collada and arranging them in an animation state machine.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.AnimationDemoExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_AnimationDemoExample.jpg", //
maxHeapMemory = 64)
public class AnimationDemoExample extends ExampleBase {

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

        // Create our staff object for attaching.
        staff = new Cylinder("staff", 4, 9, 1, 40, true);

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
            boolean walk = true;

            public void actionPerformed(final ActionEvent event) {
                if (!walk) {
                    if (manager.getBaseAnimationLayer().doTransition("walk")) {
                        runWalkButton.setText("Start running...");
                        walk = true;
                    }
                } else {
                    if (manager.getBaseAnimationLayer().doTransition("run")) {
                        runWalkButton.setText("Start walking...");
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

            // Add colladaNode to root
            _root.attachChild(colladaNode);

            // TODO temp camera positioning until reading camera instances...
            ReadOnlyVector3 upAxis = Vector3.UNIT_Y;
            if (storage.getAssetData().getUpAxis() != null) {
                upAxis = storage.getAssetData().getUpAxis();
            }

            positionCamera(upAxis);

            // Uncomment to try out gpu skinning
            // final GLSLShaderObjectsState gpuShader = new GLSLShaderObjectsState();
            // gpuShader.setEnabled(true);
            // try {
            // gpuShader.setVertexShader(PrimitiveSkeletonExample.class.getClassLoader().getResourceAsStream(
            // "com/ardor3d/extension/animation/skeletal/skinning_gpu.vert"));
            // gpuShader.setFragmentShader(PrimitiveSkeletonExample.class.getClassLoader().getResourceAsStream(
            // "com/ardor3d/extension/animation/skeletal/skinning_gpu.frag"));
            // } catch (final IOException ioe) {
            // ioe.printStackTrace();
            // }
            //
            // colladaNode.acceptVisitor(new Visitor() {
            // @Override
            // public void visit(final Spatial spatial) {
            // if (spatial instanceof SkinnedMesh) {
            // final SkinnedMesh skinnedSpatial = (SkinnedMesh) spatial;
            // skinnedSpatial.setGPUShader(gpuShader);
            // skinnedSpatial.setUseGPU(true);
            // }
            // }
            // }, true);

            final CullState cullState = new CullState();
            cullState.setCullFace(Face.Back);
            _root.setRenderState(cullState);
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
                final ColladaStorage storage1 = colladaImporter.load("collada/skeleton/" + key + ".dae");
                return storage1.extractChannelsAsClip(key);
            }
        });

        // Add a special trigger channel to the punch clip.
        addFireballTrigger(applier, input.getClips().get("skeleton.punch"));

        // Load our layer and states from script
        try {
            final ResourceSource layersFile = new URLResourceSource(Thread.currentThread().getContextClassLoader()
                    .getResource("com/ardor3d/example/pipeline/AnimationDemoExample.js"));
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

    private void addFireballTrigger(final SimpleAnimationApplier applier, final AnimationClip punchClip) {
        final float max = punchClip.getMaxTimeIndex();
        final TriggerChannel triggerChannel = new TriggerChannel("punch_fire", new float[] { 0, max / 2,
                max / 2 + 0.25f }, new String[] { null, "fist_fire", null });
        punchClip.addChannel(triggerChannel);
        applier.addTriggerCallback("fist_fire", new TriggerCallback() {
            public void doTrigger() {
                GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).update(
                        new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                addParticles();
                                return null;
                            }
                        });
            }
        });
    }

    private void addParticles() {
        // find location of fist
        final Transform loc = pose.getGlobalJointTransforms()[15];

        // Spawn a short lived explosion
        final ParticleSystem explosion = ParticleFactory.buildParticles("big", 80);
        explosion.setEmissionDirection(new Vector3(0.0f, 1.0f, 0.0f));
        explosion.setMaximumAngle(MathUtils.PI);
        explosion.setSpeed(0.9f);
        explosion.setMinimumLifeTime(300.0f);
        explosion.setMaximumLifeTime(500.0f);
        explosion.setStartSize(2.0f);
        explosion.setEndSize(5.0f);
        explosion.setStartColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 1.0f));
        explosion.setEndColor(new ColorRGBA(1.0f, 0.24313726f, 0.03137255f, 0.0f));
        explosion.setControlFlow(false);
        explosion.setInitialVelocity(0.04f);
        explosion.setParticleSpinSpeed(0.0f);
        explosion.setRepeatType(RepeatType.CLAMP);

        // attach to root, at fist location
        explosion.setTransform(loc);
        explosion.warmUp(1);
        _root.attachChild(explosion);
        _root.updateWorldTransform(true);

        explosion.getParticleController().addListener(new ParticleControllerListener() {
            @Override
            public void onDead(final ParticleSystem particles) {
                explosion.removeFromParent();
            }
        });
        explosion.forceRespawn();

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.One);
        explosion.setRenderState(blend);

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/flaresmall.jpg", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessCompressedFormat, true));
        ts.getTexture().setWrap(WrapMode.BorderClamp);
        ts.setEnabled(true);
        explosion.setRenderState(ts);

        final ZBufferState zstate = new ZBufferState();
        zstate.setWritable(false);
        explosion.setRenderState(zstate);
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
}